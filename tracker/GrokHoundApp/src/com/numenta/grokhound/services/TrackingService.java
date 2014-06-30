/**
 *
 * Copyright (C) 2014 Numenta Inc. All rights reserved.
 *
 * The information and source code contained herein is the
 * exclusive property of Numenta Inc.  No part of this software
 * may be used, reproduced, stored or distributed in any form,
 * without explicit written authorization from Numenta Inc.
 *
 */

package com.numenta.grokhound.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.numenta.grokhound.Constants;
import com.numenta.grokhound.data.LocationDataStore;
import com.numenta.grokhound.data.S3LocationDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackingService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = TrackingService.class.getSimpleName();
    public static final int UPDATE_INTERVAL_LOW = 60000;
    public static final int UPDATE_INTERVAL_HIGH = 2000;
    private PowerManager.WakeLock _wakeLock;

    // Max HTTP Connections to cache (http.maxConnections) @see
    // http://developer.android.com/reference/java/net/HttpURLConnection.html
    private static final int HTTP_CONNECTION_POOL_SIZE = 100;

    private static final ThreadFactory TIMER_THREAD_FACTORY = new BackgroundThreadFactory(
            "Timer");
    public static final String LOCATION_CHANGED_EVENT = "location_changed";
    public static final String EXTRA_LOCATION = "location";
    public static final String INTERVAL_PREF = "interval";
    BlockingQueue<Location> _locationsQueue = new LinkedBlockingQueue<Location>();

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class TrackingBinder extends Binder {
        public TrackingService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return TrackingService.this;
        }
    }

    private TrackingBinder _binder = new TrackingBinder();

    /**
     * Thread factory creating background threads. Give background threads a
     * slightly lower than normal priority, so that it will have less chance of
     * impacting the responsiveness of the UI
     * 
     * @see http://developer.android.com/reference/android/os/Process.html
     */
    private static final class BackgroundThreadFactory implements ThreadFactory {
        private final String _name;

        public BackgroundThreadFactory(String name) {
            super();
            this._name = name;
        }

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, _name + " # "
                    + threadNumber.getAndIncrement()) {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    super.run();
                }
            };
            return t;
        }
    }

    private LocationClient _locationClient;
    private Context _context;
    private LocationRequest _locationRequest;
    // Single thread pool used to schedule periodic tasks
    private ScheduledExecutorService _timer = null;
    // This task will periodically upload the application data to S3
    private ScheduledFuture<?> _updateDataTask = null;

    private Handler _handler = null;

    private AlarmReceiver _alarm = null;
    
    private LocationDataStore _serverStore = new S3LocationDataStore(
            Constants.S3_DATA_BUCKET, Constants.S3_DATA_FOLDER,
            Constants.AWS_ACCESS_KEY, Constants.AWS_SECRET_KEY);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        _context = this;
        _handler = new Handler();
        int res = GooglePlayServicesUtil.isGooglePlayServicesAvailable(_context);
        if (res != ConnectionResult.SUCCESS) {
            Log.e(TAG,
                    "Failed to start tracking services. GooglePlayServices is unavailable.");
        }
        // Start wake up alarm
        _alarm = new AlarmReceiver();
        _alarm.startAlarm(getApplicationContext());

        // Optimize HTTP connection by keeping the HTTP connections alive and
        // reusing them
        System.getProperties().setProperty(
                "sun.net.http.errorstream.enableBuffering", "true");
        System.getProperties().setProperty("http.maxConnections",
                String.valueOf(HTTP_CONNECTION_POOL_SIZE));

        _timer = Executors
                .newSingleThreadScheduledExecutor(TIMER_THREAD_FACTORY);

        _locationClient = new LocationClient(_context, this, this);
        
        // Restore location update interval
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long interval = prefs.getLong(INTERVAL_PREF, UPDATE_INTERVAL_LOW);
        if (interval == UPDATE_INTERVAL_HIGH) {
            // Keep the phone awake if requesting at high intervals
            acquireWakeLock();
        }
        _locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(interval);

        _updateDataTask = _timer.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "_updateDataTask.saveLocations");
                saveLocations();
            }
        }, 15, 15, TimeUnit.MINUTES);
        _locationClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (_locationClient != null && _locationClient.isConnected()) {
            _locationClient.removeLocationUpdates(this);
            _locationClient.disconnect();
            _locationClient = null;
        }
        if (_updateDataTask != null) {
            _updateDataTask.cancel(true);
        }
        if (_timer != null) {
            _timer.shutdown();
        }
        if (_alarm != null) {
            _alarm.stopAlarm(getApplicationContext());
        }
        
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        if (connectionResult.hasResolution()) {
            PendingIntent resolution = connectionResult.getResolution();
            try {
                startIntentSender(resolution.getIntentSender(), null, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.e(TAG, "GooglePlayServicesClient:onConnectionFailed", e);
            }
        } else {
            Log.e(TAG, "GooglePlayServicesClient:onConnectionFailed:"
                    + connectionResult.toString());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        _handler.post(new Runnable() {
            @Override
            public void run() {
                if (_locationClient.isConnected()) {
                    Log.d(TAG, "_locationClient.requestLocationUpdates");
                    _locationClient.requestLocationUpdates(_locationRequest, TrackingService.this);
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
    }

    @Override
    public void onLocationChanged(final Location location) {
        _locationsQueue.add(location);
        _handler.post(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(TrackingService.LOCATION_CHANGED_EVENT);
                intent.putExtra(TrackingService.EXTRA_LOCATION, location);
                Log.d(TAG, "onLocationChanged: " + location);
                LocalBroadcastManager.getInstance(TrackingService.this).sendBroadcast(intent);
            }
        });
    }

    public Location getLastLocation() {
        if (_locationClient.isConnected()) {
            return _locationClient.getLastLocation();
        }
        return null;
    }
    /**
     * Save all queued locations
     */
    public void saveLocations() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Collection<Location> locations = new ArrayList<Location>();
                    _locationsQueue.drainTo(locations);
                    Log.d(TAG, "Saving " + locations.size() + " locations");
                    _serverStore.save(locations);
                } catch (Exception e) {
                    Log.e(TAG, "Error uploading data", e);
                }                
            }
        });
    }
    public synchronized void setLocationUpdateInterval(long interval) {
        _locationClient.removeLocationUpdates(this);
        _locationClient.disconnect();
        saveLocations();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putLong(INTERVAL_PREF, interval).apply();
        
        _locationRequest.setInterval(interval);
        _locationClient.connect();
    }
    
    public synchronized void acquireWakeLock() {
        if (_wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            _wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        _wakeLock.acquire();
    }
    public synchronized void releaseWakeLock() {
        if (_wakeLock != null && _wakeLock.isHeld()) {
            _wakeLock.release();
        }
    }
    public synchronized boolean isWakeLockHeld() {
        return _wakeLock != null && _wakeLock.isHeld();
    }
}
