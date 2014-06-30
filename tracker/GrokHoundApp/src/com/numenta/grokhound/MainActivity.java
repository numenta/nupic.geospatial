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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.numenta.grokhound.services.TrackingService;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap _googleMap;
    protected OnLocationChangedListener _onLocationChangedListener;
    private BroadcastReceiver _locationChangedReceiver;
    private BroadcastReceiver _serviceBoundReceiver;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPlayServices()) {
            return;
        }

        setContentView(R.layout.activity_main);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleAccuracy);
        toggle.setChecked(GrokApplication.isWakeLockHeld());

        MapFragment mapFrag = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        _googleMap = mapFrag.getMap();
        _googleMap.setMyLocationEnabled(true);
        _googleMap.setIndoorEnabled(true);
        UiSettings uisettings = _googleMap.getUiSettings();
        uisettings.setAllGesturesEnabled(true);
        uisettings.setMyLocationButtonEnabled(true);
        uisettings.setCompassEnabled(true);
        uisettings.setZoomControlsEnabled(true);
        _googleMap.setLocationSource(new LocationSource() {

            @Override
            public void activate(OnLocationChangedListener listener) {
                Log.d(TAG, "LocationSource.activate");
                _onLocationChangedListener = listener;
                Location lastLocation = GrokApplication.getLastLocation();
                if (lastLocation != null && _onLocationChangedListener != null) {
                    _onLocationChangedListener.onLocationChanged(lastLocation);
                }
            }

            @Override
            public void deactivate() {
                Log.d(TAG, "LocationSource.deactivate");
                _onLocationChangedListener = null;
            }
        });
        _locationChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "_locationChangedReceiver.onReceive");
                if (_onLocationChangedListener != null) {
                    Location location = intent.getParcelableExtra(TrackingService.EXTRA_LOCATION);
                    updateLocation(location);
                }
            }
        };
        _serviceBoundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "__serviceBoundReceiver.onReceive");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleAccuracy);
                        toggle.setChecked(GrokApplication.isWakeLockHeld());
                        Location lastLocation = GrokApplication.getLastLocation();
                        updateLocation(lastLocation);
                    }
                });
            }
        };
    }

    protected void updateLocation(Location location) {
        if (location != null && _onLocationChangedListener != null) {
            _onLocationChangedListener.onLocationChanged(location);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18f);
            _googleMap.animateCamera(cameraUpdate);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_services) {
            if (item.isChecked()) {
                item.setChecked(false);
                item.setTitle(R.string.action_disabled);
                item.setIcon(R.drawable.ic_action_off);
                GrokApplication.disable();
            } else {
                item.setChecked(true);
                item.setTitle(R.string.action_enabled);                
                item.setIcon(R.drawable.ic_action_on);
                GrokApplication.enable();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!checkPlayServices()) {
            return;
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                _locationChangedReceiver,
                new IntentFilter(TrackingService.LOCATION_CHANGED_EVENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                _serviceBoundReceiver,
                new IntentFilter(GrokApplication.SERVICE_BOUND_EVENT));
    }

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            GrokApplication.acquireWakeLock();
            GrokApplication.setLocationUpdateInterval(TrackingService.UPDATE_INTERVAL_HIGH);
        } else {
            GrokApplication.releaseWakeLock();
            GrokApplication.setLocationUpdateInterval(TrackingService.UPDATE_INTERVAL_LOW);
        }
    }
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                _locationChangedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                _serviceBoundReceiver);
        GrokApplication.saveLocations();
    }
}
