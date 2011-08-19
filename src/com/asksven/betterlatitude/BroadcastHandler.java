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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class BroadcastHandler extends BroadcastReceiver
{	
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{

 
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Logger.i(getClass().getSimpleName(), "Received Broadcast ACTION_BOOT_COMPLETED");
			
			// retrieve default selections for spinners
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean bAutostart = sharedPrefs.getBoolean("autostart", false);
			
			if (bAutostart)
			{
				Logger.i(getClass().getSimpleName(), "Autostart is set so run, starting service");
				context.startService(new Intent(context, LocationService.class));
			}
		}
	}
}
