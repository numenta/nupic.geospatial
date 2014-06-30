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

package com.numenta.grokhound;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.numenta.grokhound.services.TrackingService;
import com.numenta.grokhound.services.TrackingService.TrackingBinder;

public class GrokApplication extends Application {
    public static final String SERVICE_BOUND_EVENT = "service_bound";

    private static GrokApplication _instance;
    protected TrackingBinder _serviceBinder;
    protected TrackingService _service;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection _serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className,
                final IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            _serviceBinder = (TrackingBinder) service;
            _service = _serviceBinder.getService();
            Intent intent = new Intent(GrokApplication.SERVICE_BOUND_EVENT);
            LocalBroadcastManager.getInstance(_instance).sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            _service = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // NOTE: It is guaranteed to only have one instance of the
        // android.app.Application class so it's safe (and recommended by the
        // Google Android team) to treat it as a Singleton.
        _instance = this;

        enableService();
    }

    public static final void saveLocations() {
        if (_instance != null &&  _instance._service != null) {
            _instance._service.saveLocations();
        }
    }

    public static final Location getLastLocation() {
        if (_instance != null &&  _instance._service != null) {
            return _instance._service.getLastLocation();
        }
        return null;
    }
    public static final void setLocationUpdateInterval(long interval) {
        if (_instance != null && _instance._service != null) {
            _instance._service.setLocationUpdateInterval(interval);
        }
    }
    public static final void acquireWakeLock() {
        if (_instance != null && _instance._service != null) {
             _instance._service.acquireWakeLock();
        }
    }
    public static final void releaseWakeLock() {
        if (_instance != null && _instance._service != null) {
            _instance._service.releaseWakeLock();
        }
    }
    public static final boolean isWakeLockHeld() {
        if (_instance != null && _instance._service != null) {
            return _instance._service.isWakeLockHeld();
        }
        return false;
    }
    
    public static final GrokApplication getInstance() {
        return _instance;
    }

    void enableService() {
        final Intent dataService = new Intent(this, TrackingService.class);
        bindService(dataService, _serviceConn, Context.BIND_AUTO_CREATE);
        startService(dataService);
    }
    
    void disableService() {
        if (_service != null) {
            unbindService(_serviceConn);
            _service = null;
        }
        final Intent dataService = new Intent(this, TrackingService.class);
        stopService(dataService);
        
    }
    public static void enable() {
        if (_instance != null &&  _instance._service == null) {
            _instance.enableService();
        }
    }

    public static void disable() {
        if (_instance != null && _instance._service != null) {
            _instance.disableService();
        }
    }
}
