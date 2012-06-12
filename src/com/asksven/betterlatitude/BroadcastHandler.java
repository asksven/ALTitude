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
import android.preference.PreferenceManager;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class BroadcastHandler extends BroadcastReceiver
{	
	private static final String TAG = "BroadcastHandler";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{

 
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Logger.i(TAG, "Received Broadcast ACTION_BOOT_COMPLETED");
			
			// retrieve default selections for spinners
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean bAutostart = sharedPrefs.getBoolean("start_on_boot", false);
			
			if (bAutostart)
			{
				Logger.i(TAG, "Autostart is set to run, starting service");
				context.startService(new Intent(context, LocationService.class));
//				Intent i = new Intent();
//				i.setClassName( "com.asksven.betterlatitude", LocationService.SERVICE_NAME );
//				context.startService( i );
			}
			else
			{
				if (bAutostart)
				{
					Logger.i(TAG, "Autostart is not set to run");
				}

			}
		}
	}
}
