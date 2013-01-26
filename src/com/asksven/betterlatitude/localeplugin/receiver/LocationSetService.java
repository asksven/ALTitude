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
package com.asksven.betterlatitude.localeplugin.receiver;

/**
 * This service handles the requests coming from the locale plugin
 * The service is started from FireReceiver as a broadcast handler can not bind to a service
 * @author sven
 *
 */

import com.asksven.betterlatitude.LocationService;
import com.asksven.betterlatitude.localeplugin.Constants;
import com.asksven.betterlatitude.localeplugin.bundle.PluginBundleManager;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class LocationSetService extends Service
{
    // log tag
    private static final String TAG = "LocationSetService";

	private LocationService m_locationService;
	
	private ServiceConnection mConnection = new ServiceConnection()
	{
	    public void onServiceConnected(ComponentName className, IBinder service)
	    {
	        // This is called when the connection with the service has been established
	    	Log.i(Constants.LOG_TAG, "onServiceConnected called");
	        m_locationService = ((LocationService.LocalBinder)service).getService();

	    }

	    public void onServiceDisconnected(ComponentName className)
	    {
	        // This is called when the connection with the service has been unexpectedly disconnected
	    	Log.i(Constants.LOG_TAG, "onServiceDisconnected called");
	        m_locationService = null;
	    }
	};

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Log.d(TAG, "onStart");
        super.onStart(intent, startId);
        
        float fLat = 0f;
        float fLong = 0f;
        
        if (intent != null)
        {
            fLat = intent.getFloatExtra(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LATITUDE, 0f);
            fLong = intent.getFloatExtra(PluginBundleManager.BUNDLE_EXTRA_FLOAT_LONGITUDE, 0f);
        }
        Log.i(TAG, "Service was called with: " 
        		+ fLat
        		+ fLong);

        Intent serviceIntent = new Intent(this, LocationService.class);
        Log.i(Constants.LOG_TAG, "Before bindService");
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i(Constants.LOG_TAG, "After bindService");
        
        if (m_locationService == null)
        {
        	Log.e(Constants.LOG_TAG, "LocationService.getInstance() returned null");
        }
        else
        {
        	Log.i(Constants.LOG_TAG, "Calling forceLocationUpdate");
        	m_locationService.forceLocationUpdate(fLat, fLong);
        }
    }

}
