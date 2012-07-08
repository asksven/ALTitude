/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asksven.betterlatitude;

import com.asksven.android.common.networkutils.DataNetwork;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class WifiStateHandler extends BroadcastReceiver
{	
	private static final String TAG = "WifiStateHandler";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		NetworkInfo networkInfo = (NetworkInfo) intent
				.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
		if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
		{
			Logger.i(TAG, "Wifi network detected: calling updateLatitude() to update potentially buffered locations");
			LocationService.getInstance().updateLatitude();
		}
	}
}