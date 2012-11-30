/*
 * Copyright (C) 2012 asksven
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

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterlatitude.UpdateWidgetService;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MyWidgetProvider extends AppWidgetProvider
{

	public static final String ACTION_CLICK 	= "ACTION_CLICK";
	public static final String ACTION_REFRESH 	= "ACTION_REFRESH";
	
	private static final String TAG = "MyWidgetProvider";
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);

		Log.i(TAG, "onReceive method called, action = '" + intent.getAction() + "' at " + DateUtils.now());
		
		if ( (ACTION_CLICK.equals(intent.getAction())) )
		{
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			ComponentName thisAppWidget = new ComponentName(
					context.getPackageName(),
					this.getClass().getName());
			int[] appWidgetIds = appWidgetManager
					.getAppWidgetIds(thisAppWidget);

			// toggle quick settings
			LocationService myService = LocationService.getInstance();
	    	if (myService != null)
	    	{
	    		if (myService.isQuickChangeRunning())
	    		{
	    			myService.resetQuickChange();
	    		}
	    		else
	    		{
	    			myService.setQuickChange();
	    		}
	    	}	    		

			if (appWidgetIds.length > 0)
			{
				onUpdate(context, appWidgetManager, appWidgetIds);
			}
			else
			{
				Log.i(TAG, "No widget found to update");
			}
		}

		if ( (ACTION_REFRESH.equals(intent.getAction())) )
		{

			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context);
			ComponentName thisAppWidget = new ComponentName(
					context.getPackageName(),
					this.getClass().getName());
			int[] appWidgetIds = appWidgetManager
					.getAppWidgetIds(thisAppWidget);

			if (appWidgetIds.length > 0)
			{
				onUpdate(context, appWidgetManager, appWidgetIds);
			}
			else
			{
				Log.i(TAG, "No widget found to update");
			}
		}
	}

	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{

		Log.w(TAG, "onUpdate method called");
		// Get all ids
		ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		// Build the intent to call the service
		Intent intent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

		// Update the widgets via the service
		context.startService(intent);
	}
}