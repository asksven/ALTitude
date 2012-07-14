/*
 * Copyright (C) 2011-12 asksven
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

import com.asksven.betterlatitude.utils.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author sven
 *
 */
public class UpdateLocationService extends Service
{
	private static final String TAG = "UpdateLocationService";

	@Override
	public void onStart(Intent intent, int startId)
	{

		Logger.i(TAG, "on Start Called");
		try
		{
			LocationService.getInstance().forceLocationUpdate();			
		}
		catch (Exception e)
		{
			Logger.e(TAG, "An error occured: " + e.getMessage());
		}

		
		stopSelf();

		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}