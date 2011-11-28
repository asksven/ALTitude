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
import android.os.Bundle;
import android.util.Log;

/**
 * Handles alarms set for quick changes by the service
 * @author sven
 *
 */
public class AlarmReceiver extends BroadcastReceiver
{		 
	private static String TAG = "AlarmReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		try
		{
			// reset the quick action
			LocationService.getInstance().resetQuickChange();
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured receiving the alarm");
		}
	}
}
