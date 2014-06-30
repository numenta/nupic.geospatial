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

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.numenta.grokhound.GrokApplication;

public class TrackingIntentService extends IntentService {

	public TrackingIntentService() {
		super("TrackingIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
	    GrokApplication.saveLocations();
		// Handle the case when the intent was sent from the AlarmManger
		WakefulBroadcastReceiver.completeWakefulIntent(intent);
	}
}
