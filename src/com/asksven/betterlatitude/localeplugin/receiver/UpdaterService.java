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
 * @author sven
 *
 */
/**
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

public class UpdaterService extends Service
{
    // log tag
    private static final String TAG = "UpdaterService";

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

        int iAction = 0;
        int iInterval = 0;
        int iAccuracy = 0;
        int iDuration = 0;
        
        if (intent != null)
        {
            iAction = intent.getIntExtra(PluginBundleManager.BUNDLE_EXTRA_INT_ACTION, 0);
            iInterval = intent.getIntExtra(PluginBundleManager.BUNDLE_EXTRA_INT_INTERVAL, 0);
            iAccuracy = intent.getIntExtra(PluginBundleManager.BUNDLE_EXTRA_INT_ACCURACY, 0);
            iDuration= intent.getIntExtra(PluginBundleManager.BUNDLE_EXTRA_INT_DURATION, 0);
        }
        Log.i(TAG, "Service was called with: " 
        		+ iAction
        		+ iInterval
        		+ iAccuracy
        		+ iDuration);

        if (iInterval != 0)
        {
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
		        if (iAction == 0)
		        {
		        	Log.i(Constants.LOG_TAG, "Calling setQuickChange");
		        	m_locationService.setQuickChange(iInterval, iAccuracy, iDuration);
		        }
		        else
		        {
		        	Log.i(Constants.LOG_TAG, "Calling resetQuickChange");
		        	m_locationService.resetQuickChange();
		        }
	        }
        }
        else
        {
        	Log.i(TAG, "Interval was 0, no action");
        }
//        // Asynchronously send broadcast
//        Runnable r = new Runnable()
//        {
//            public void run()
//            {
//                if (!TextUtils.isEmpty(message))
//                {
//                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
//                    Log.d(TAG, "Toast: \"" + message + "\"");
//                }
//
//                // And this is the reason of the plug-in existence:
//                Intent notificationIntent = new Intent(Constants.ACTION_AUDIO_VOLUME_UPDATE);
//                notificationIntent.putExtra(Constants.EXTRA_STREAM_TYPE,
//                        Constants.EXTRA_VALUE_UNKNOWN);
//                notificationIntent.putExtra(Constants.EXTRA_VOLUME_INDEX,
//                        Constants.EXTRA_VALUE_UNKNOWN);
//                notificationIntent.putExtra(Constants.EXTRA_RINGER_MODE,
//                        Constants.EXTRA_VALUE_UNKNOWN);
//
//                Log.d(TAG, "On send Broadcast");
//                getApplicationContext().sendBroadcast(notificationIntent, null);
//                // finally stop the service
//                TheService.this.stopSelf();
//            }
//        };

    }

}
