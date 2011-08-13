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



import com.asksven.android.common.utils.DataStorage;
import com.asksven.betterlatitude.credentials.CredentialStore;
import com.asksven.betterlatitude.credentials.SharedPreferencesCredentialStore;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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


/**
 * The LocationService keeps running even if the main Activity is not displayed/never called
 * The Services takes care of always running location updatestasks and of tasks taking place once in the lifecycle
 * without user interaction.
 * @author sven
 *
 */
public class LocationService extends Service implements LocationListener
{
	private NotificationManager mNM;

	String m_strLocProvider;
	private LocationManager m_LocationManager;
	public static final String LOGFILE = "BetterLatitude.log";
	public static final int LOC_INTERVAL = 15 * 60 * 1000; // 15 Minutes
	public static final int LOC_ACCURACY = 2000;
	private static final String TAG = "LocationService";
	
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
    	Log.i(getClass().getSimpleName(), "onCreate called");

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        Criteria criteria = new Criteria();
        
		// Get the location manager
		m_LocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        m_strLocProvider = m_LocationManager.getBestProvider(criteria, false);
        // tried to fix bug http://code.google.com/p/android/issues/detail?id=3259
		// by programmatically registering to the event
        if (!m_bRegistered)
        {
        	// max every 5 minutes or when moved by 1 Km
    		m_LocationManager.requestLocationUpdates(m_strLocProvider, LOC_INTERVAL, LOC_ACCURACY, this);
    	
            m_bRegistered = true;
        }
   }

    /** 
     * Called when service is started
     */
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(getClass().getSimpleName(), "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        
        return Service.START_STICKY;
    }
    @Override
    /**
     * Called when Service is terminated
     */
    public void onDestroy()
    {        
    	// unregister the broadcastreceiver
		m_LocationManager.removeUpdates(this);

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
		m_dLat = location.getLatitude();
		m_dLong = location.getLongitude();
		setLocationApiCall();
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		// TODO Auto-generated method stub

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

	private void setLocationApiCall()
	{
		try
		{
			DataStorage.LogToFile(LOGFILE, TAG + " Service Updating Latitude with position Lat: "
					+ String.valueOf(m_dLat)
					+ " Long: " + String.valueOf(m_dLong));
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
			AccessTokenResponse accessTokenResponse = credentialStore.read();
			
			GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(accessTokenResponse.accessToken,
			        transport,
			        jsonFactory,
			        OAuth2ClientCredentials.CLIENT_ID,
			        OAuth2ClientCredentials.CLIENT_SECRET,
			        accessTokenResponse.refreshToken);
			
		    final Latitude latitude = new Latitude(transport, accessProtectedResource, jsonFactory);
		    latitude.apiKey = OAuth2ClientCredentials.API_KEY;
		    LatitudeCurrentlocationResourceJson currentLocation = new LatitudeCurrentlocationResourceJson();
		    currentLocation.set("latitude", m_dLat);
		    currentLocation.set("longitude", m_dLong);
			LatitudeCurrentlocationResourceJson insertedLocation = latitude.currentLocation.insert(currentLocation).execute();
		}
		catch (Exception ex)
		{
			DataStorage.LogToFile(LOGFILE, "An error occured in setLocationApiCall()");
			DataStorage.LogToFile(LOGFILE, ex.getStackTrace());
			ex.printStackTrace();
		}
	}

}

