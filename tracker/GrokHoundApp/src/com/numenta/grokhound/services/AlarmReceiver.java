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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


/**
 * Wake up {@link TrackingService} every 15 minutes.
 * <p>
 * When the alarm fires, this WakefulBroadcastReceiver receives the broadcast Intent and execute the
 * alarm code {@link TrackingService}
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    private AlarmManager _alarmMgr;
    private PendingIntent _alarmIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "wakeup");
        Intent service = new Intent(context, TrackingIntentService.class);
        startWakefulService(context, service);
    }

    public void stopAlarm(Context context) {
        Log.i(TAG, "stopAlarm");
        if (_alarmMgr != null) {
            _alarmMgr.cancel(_alarmIntent);
        }
    }

    public void startAlarm(Context context) {
        Log.i(TAG, "startAlarm");

        _alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        _alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // Tries to wakeup the phone every 15 minutes to synchronize the data.
        // The 15 minutes interval was chosen because this way this be alarm will be phase-aligned
        // with other alarms to reduce the number of wakeups. See AlarmManager#setInexactRepeating
        _alarmMgr.setInexactRepeating(AlarmManager.RTC,
                System.currentTimeMillis(),
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, _alarmIntent);
    }
}
