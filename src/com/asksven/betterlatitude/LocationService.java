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

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.betterlatitude.credentialstore.CredentialStore;
import com.asksven.betterlatitude.credentialstore.SharedPreferencesCredentialStore;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;


/**
 * The LocationService keeps running even if the main Activity is not displayed/never called
 * The Services takes care of always running location updatestasks and of tasks taking place once in the lifecycle
 * without user interaction.
 * @author sven
 *
 */
public class LocationService extends Service implements LocationListener, OnSharedPreferenceChangeListener
{
	/** singleton */
	private static LocationService m_instance = null;
	
	private NotificationManager mNM;
	
	public static String SERVICE_NAME = "com.asksven.betterlatitude.LocationService";

	String m_strLocProvider;
	private LocationManager m_LocationManager;

	private static final String TAG = "LocationService";
	
	/** constants for the connection status */
	public static final String STATUS_LOGGED_IN = "Logged in";
	public static final String STATUS_NOT_LOGGED_IN = "Not logged in";
	public static final String BROADCAST_STATUS_CHANGED = "Connection stats changed";
	
	/** the connection status */
	private String m_strStatus = STATUS_NOT_LOGGED_IN;
	
	private boolean m_bRegistered = false;

	private double m_dLat = -1;
	private double m_dLong = -1;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        LocationService getService()
        {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate()
    {
    	m_instance = this;
    	Log.i(getClass().getSimpleName(), "onCreate called");

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notifyStatus("Service started");
        
        // register the location listener
        this.registerLocationListener();

        // Set up a listener whenever a key changes
    	PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

   }

    private void registerLocationListener()
    {
    	if (m_bRegistered)
    	{
        	// unregister the receiver
    		m_LocationManager.removeUpdates(this);    		
    	}

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String strInterval = prefs.getString("update_interval", "15");
    	String strAccuracy = prefs.getString("update_accuracy", "2000");
    	    	
		int iInterval = 15 * 60 * 1000;
		int iAccuracy = 2000;
		try
    	{
			iInterval = Integer.valueOf(strInterval) * 60 * 1000;
			iAccuracy = Integer.valueOf(strAccuracy);
    	}
    	catch (Exception e)
    	{
    		Logger.e(TAG, "Error reading prefernces, using defaults");
    	}
    	
		// whatever Prefs say, the free version does not give any choice
    	if (!Configuration.isFullVersion(this))
		{
    		iInterval = 15 * 60 * 1000;
    		iAccuracy = 2000;
    		
        	
		}
    	Criteria criteria = new Criteria();

		// Get the location manager
		m_LocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        m_strLocProvider = m_LocationManager.getBestProvider(criteria, false);
		m_LocationManager.requestLocationUpdates(m_strLocProvider, iInterval, iAccuracy, this);
		Logger.i(TAG, "Changed location settings to interval, accuracy = (" + iInterval + ", " + iAccuracy + ")");
        m_bRegistered = true;
            
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
    	if (key.equals("update_interval") || key.equals("update_accuracy"))
    	{
    		
    		Logger.i(TAG, "Preferences have change. Register location listener again");
    		// re-register location listener with new prefs
    		this.registerLocationListener();
    	}

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	if (key.equals("notify_status"))
    	{
    		// activate / deactivate the notification
    		if (prefs.getBoolean("notify_status", true))
    		{
    			notifyStatus("Notification activated");
    		}
    		else
    		{
    	        // Cancel the persistent notification.
    	        mNM.cancel(R.string.app_name);

    		}
    	}
    }
//    /** 
//     * Called when service is started
//     */
//    public int onStartCommand(Intent intent, int flags, int startId)
//    {
//        Log.i(getClass().getSimpleName(), "Received start id " + startId + ": " + intent);
//        // We want this service to continue running until it is explicitly
//        // stopped, so return sticky.
//        notifyStatus("Service started");
//        return Service.START_STICKY;
//    }
    
    @Override
    /**
     * Called when Service is terminated
     */
    public void onDestroy()
    {        
        // Cancel the persistent notification.
        mNM.cancel(R.string.app_name);
    	// unregister the receiver
		m_LocationManager.removeUpdates(this);
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
	
    @Override
	public void onLocationChanged(Location location)
	{
    	Logger.i(TAG, "onLocationChanged called");
		m_dLat = location.getLatitude();
		m_dLong = location.getLongitude();
		if (setLocationApiCall())
		{	
			notifyStatus("Location updated");
		}
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
		Toast.makeText(this, "Enabled new provider " + provider,
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onProviderDisabled(String provider)
	{
		Toast.makeText(this, "Disenabled provider " + provider,
				Toast.LENGTH_SHORT).show();
	}	

	private boolean setLocationApiCall()
	{
		boolean bRet = true;

		try
		{
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	boolean bLogLoc 		= prefs.getBoolean("log_location", false);
	    	
	    	if (bLogLoc)
	    	{
				Logger.i(TAG, " Service Updating Latitude with position Lat: "
						+ String.valueOf(m_dLat)
						+ " Long: " + String.valueOf(m_dLong));
	    	}
	    	else
	    	{
	    		Logger.i(TAG, " Service Updating Latitude");
	    	}
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
			AccessTokenResponse accessTokenResponse = credentialStore.read();
			
			// check if token is valid (not empty)
			if (accessTokenResponse.accessToken.equals(""))
			{
				notifyError("Access to latitude was not granted. Please log on");
				bRet = false;
				setStatus(STATUS_NOT_LOGGED_IN);
				return bRet;				
			}
			
			GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(accessTokenResponse.accessToken,
			        transport,
			        jsonFactory,
			        OAuth2ClientConstants.CLIENT_ID,
			        OAuth2ClientConstants.CLIENT_SECRET,
			        accessTokenResponse.refreshToken);
			
		    final Latitude latitude = new Latitude(transport, accessProtectedResource, jsonFactory);
		    latitude.apiKey = OAuth2ClientConstants.API_KEY;
		    LatitudeCurrentlocationResourceJson currentLocation = new LatitudeCurrentlocationResourceJson();
		    currentLocation.set("latitude", m_dLat);
		    currentLocation.set("longitude", m_dLong);
		    setStatus(STATUS_LOGGED_IN);
			LatitudeCurrentlocationResourceJson insertedLocation = latitude.currentLocation.insert(currentLocation).execute();
		}
		catch (IOException ex)
		{
			bRet = false;
			Logger.i(TAG, "An error occured in setLocationApiCall() '" +  ex.getMessage() + "'");
			if (ex.getMessage().equals("401 Unauthorized"))
			{
				setStatus(STATUS_NOT_LOGGED_IN);
			}
			notifyError("Updating Latitude failed with error '" + ex.getMessage() + "'");
//			Logger.i(TAG, ex.getStackTrace());
			
		}
		
		return bRet;
	}

	/**
	 * Notify status change in notification bar (if enabled)
	 */
	void notifyStatus(String strStatus)
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean bNotify = prefs.getBoolean("notify_status", true);
    	
    	if (bNotify)
    	{
	    	Notification notification = new Notification(
	    			R.drawable.icon, strStatus, System.currentTimeMillis());
	    	PendingIntent contentIntent = PendingIntent.getActivity(
	    			this, 0, new Intent(this, MainActivity.class), 0);
	    	notification.setLatestEventInfo(
	    			this, getText(R.string.app_name), strStatus, contentIntent);
	    	mNM.notify(R.string.app_name, notification);
    	}

	}

	/**
	 * Notify status change in notification bar (if enabled)
	 */
	void notifyError(String strStatus)
	{
    	Notification notification = new Notification(
    			R.drawable.icon, strStatus, System.currentTimeMillis());
    	PendingIntent contentIntent = PendingIntent.getActivity(
    			this, 0, new Intent(this, MainActivity.class), 0);
    	notification.setLatestEventInfo(
    			this, getText(R.string.app_name), strStatus, contentIntent);
    	mNM.notify(R.string.app_name, notification);
	}
	

	/** 
	 * Broadcasts the connection status change
	 * @param strStatus the broadcasted message
	 */
	public void setStatus(String strStatus)
	{
		m_strStatus = strStatus;
		sendBroadcast(new Intent(BROADCAST_STATUS_CHANGED));
	}
	
	/**
	 * Returns the status of the Latitude connection
	 * @return the status of the latitude connection
	 */
	public String getStatus()
	{
		return m_strStatus;
	}
	
	public static LocationService getInstance()
	{
		return m_instance;
	}
}

