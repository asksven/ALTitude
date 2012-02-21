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
import java.util.ArrayList;
import java.util.Calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.location.GeoUtils;
import com.asksven.android.common.networkutils.DataNetwork;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterlatitude.credentialstore.CredentialStore;
import com.asksven.betterlatitude.credentialstore.LatitudeApi;
import com.asksven.betterlatitude.credentialstore.SharedPreferencesCredentialStore;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.Latitude.CurrentLocation;
import com.google.api.services.latitude.Latitude.CurrentLocation.Insert;
//import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;
//import com.google.api.services.latitude.model.Location;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;


/**
 * The LocationService keeps running even if the main Activity is not displayed/never called
 * The Services takes care of always running location updatestasks and of tasks taking place once in the lifecycle
 * without user interaction.
 * @author sven
 *
 */
/**
 * @author sven
 *
 */
public class LocationService extends Service implements LocationListener, OnSharedPreferenceChangeListener
{
	/** singleton */
	private static LocationService m_instance = null;
	
	private NotificationManager mNM;
	
	public static String SERVICE_NAME = "com.asksven.betterlatitude.LocationService";
	private static int QUICK_ACTION = 1234567;

	private LocationManager m_locationManager;

	private static final String TAG = "LocationService";
	
	/** constants for the connection status */
	public static final String STATUS_UPDATE_PENDING = "Update pending";
	public static final String STATUS_UPDATE_BUFFERED = "Update buffered";
//	public static final String STATUS_LOGGED_IN = "Logged in";
	public static final String STATUS_NOT_LOGGED_IN = "Please log on to Latitude";
	public static final String STATUS_SERVICE_NOT_STARTED = "Service not started yet";
	public static final String STATUS_LOCATION_UPDATED = "Location was updated";
	public static final String BROADCAST_STATUS_CHANGED = "Connection stats changed";
	
	/** the connection status */
	private String m_strStatus = STATUS_NOT_LOGGED_IN;
	
	private boolean m_bRegistered = false;

	private ArrayList<Location> m_locationStack = null;
	
	private boolean bQuickChangeRunning = false;

	/** the location provider in use */
	String m_strLocProvider = "";
	
	/** the current location (is geo is on) */
	String m_strCurrentLocation  = "";
	
	/** precision for current location manager */
	private int m_iIterval = 0;
	private int m_iAccuracy = 0;

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
    	super.onCreate();
    	m_instance = this;
    	if (m_locationStack == null)
    	{
    		m_locationStack = new ArrayList<Location>();
    	}
    	Log.i(getClass().getSimpleName(), "onCreate called");

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        // register the location listener
        this.registerLocationListener();

        // Set up a listener whenever a key changes
    	PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

    	// set status
    	setStatus(STATUS_UPDATE_PENDING);
   }
    
    private void registerLocationListener()
    {
    	if (m_bRegistered)
    	{
        	// unregister the receiver
    		m_locationManager.removeUpdates(this);    		
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
    	
		registerLocationListener(iInterval, iAccuracy);

            
    }
    
    public ArrayList<Location> getLocationStack()
    {
    	return m_locationStack;
    	
    }
    
    public void clearLocationStack()
    {
    	m_locationStack.clear();
    	
    }
    private void registerLocationListener(int intervalMs, int accuracyM)
    {
    	if (m_bRegistered)
    	{
        	// unregister the receiver
    		m_locationManager.removeUpdates(this);    		
    	}

    	
		// whatever Prefs say, the free version does not give any choice
    	if (!Configuration.isFullVersion(this))
		{
    		intervalMs = 15 * 60 * 1000;
    		accuracyM = 2000;
    		
        	
		}
    	Criteria criteria = new Criteria();
    	criteria.setSpeedRequired(false);
    	criteria.setAltitudeRequired(false);
    	criteria.setCostAllowed(true);
    	if (accuracyM < 100)
    	{
    		criteria.setAccuracy(Criteria.ACCURACY_FINE);
    	}
    	else
    	{
    		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    	}
    	

		// Get the location manager
		m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (m_locationManager != null)
		{
	        m_strLocProvider = m_locationManager.getBestProvider(criteria, true);
	        
	        m_locationManager.requestLocationUpdates(m_strLocProvider, intervalMs, accuracyM, this);
	        m_iAccuracy = accuracyM;
	        m_iIterval = intervalMs;
	        m_bRegistered = true;
		}
		else
		{
			m_bRegistered = false;
		}
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
        // Cancel the persistent notification.
        mNM.cancel(R.string.app_name);
    	// unregister the receiver
		m_locationManager.removeUpdates(this);
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
		m_locationStack.add(location);

		try
		{
//			if (!LatitudeApi.useAccountManager(this))
//			{
				if (!setLocationApiCall())
				{	
					notifyStatus(m_locationStack + " Location(s) buffered");
				}
//			}
//			else
//			{
//				// headless call
//				LatitudeApi.getInstance(this).useAccount(null, false);
//			}
		}
		catch (Exception e)
		{
			notifyStatus(m_locationStack + " Location(s) buffered");
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

	/* 
	 * Called when a new location provider was enabled
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider)
	{
		// we may have a better provider now, redefine
		Log.e(TAG, "Provider " + provider + " was enabled. Maybe we want to use it");
		if (bQuickChangeRunning)
		{
			registerLocationListener(m_iIterval, m_iAccuracy);
		}
		else
		{
			registerLocationListener();
		}
	}

	/* 
	 * Called when a location provider was disabled
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String provider)
	{
		// we may have to change providers if we use it right now
		if (provider.equals(m_strLocProvider))
		{
			Log.e(TAG, "Provider " + provider + " was in use. Getting a new one");
			if (bQuickChangeRunning)
			{
				registerLocationListener(m_iIterval, m_iAccuracy);
			}
			else
			{
				registerLocationListener();
			}		
		}
	}	

	private boolean setLocationApiCall()
	{
		boolean bRet = true;

		try
		{
			if (!DataNetwork.hasDataConnection(this))
			{
				
				return false;
			}
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	boolean bLogLoc 		= prefs.getBoolean("log_location", false);
	    	
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
		    
//		    final Latitude.Builder builder = Latitude.builder(transport, jsonFactory);
//		    builder.setHttpRequestInitializer(accessProtectedResource);
//		    builder.setApplicationName("ALTitude");
//			final Latitude latitude = builder.build();
		    latitude.apiKey = OAuth2ClientConstants.API_KEY;
		    
		    // empty the stack and update all locations with the right timestamp
		    if (m_locationStack != null)
		    {
		    	for (int i=0; i < m_locationStack.size(); i++)
		    	{
		    		Location location = m_locationStack.get(i);
		    		
			    	if (bLogLoc)
			    	{
						Logger.i(TAG, " Service Updating Latitude with position Lat: "
								+ String.valueOf(location.getLatitude())
								+ " Long: " + String.valueOf(location.getLongitude()));
			    	}
			    	else
			    	{
			    		Logger.i(TAG, " Service Updating Latitude");
			    	}

				    LatitudeCurrentlocationResourceJson currentLocation = new LatitudeCurrentlocationResourceJson();
//			    	com.google.api.services.latitude.model.Location currentLocation = new com.google.api.services.latitude.model.Location();
				    currentLocation.set("latitude", location.getLatitude());
				    currentLocation.set("longitude", location.getLongitude());
				    currentLocation.set("timespampMs", location.getTime());
				    
				    Insert myInsert = latitude.currentLocation.insert(currentLocation);

				    String now = DateUtils.now("HH:mm:ss");
				    setStatus(STATUS_LOCATION_UPDATED + ": " + now);

				    if (myInsert != null)
				    {
				    	//myInsert.execute();
				    	new UpdateLatitudeTask().execute(myInsert);
				    }
				    else
				    {
				    	setStatus(STATUS_UPDATE_BUFFERED + "(" + m_locationStack.size() + ")");
				    	throw new IOException("CurrentLocation.Insert failed");
				    	
				    }
		    	}
		    	
		    	// if we got here all updates were OK, delete the stack
		    	notifyCurrentLocation();
		    	m_locationStack.clear();
		    }
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
    	boolean bNotify 	= prefs.getBoolean("notify_status", true);
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
	 * Notify location change in notification bar (if enabled)
	 */
	public void notifyCurrentLocation()
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String strStatus = "Location updated";
    	
    	boolean bNotify 	= prefs.getBoolean("notify_status", true);
    	boolean bNotifyGeo 	= prefs.getBoolean("notify_geodata", false);
    	if (bNotify && bNotifyGeo)
    	{
			if ( (bNotifyGeo) && (m_locationStack != null) && (!m_locationStack.isEmpty()) )
 			{
				m_strCurrentLocation = GeoUtils.getNearestAddress(this, m_locationStack.get(m_locationStack.size()-1));
				strStatus = strStatus
						+ ": "
						+ m_strCurrentLocation;
				notifyStatus(strStatus);
			}
			else
			{
				m_strCurrentLocation = "";
			}
			
	    }
	}

	
	/**
	 * Notify status change in notification bar (if enabled)
	 */
	public void notifyError(String strStatus)
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
	
	/**
	 * Returns the sigleton instance of the service
	 * @return
	 */
	public static LocationService getInstance()
	{
		return m_instance;
	}
	
	/**
	 * Returns true if a quick action is running
	 * @return 
	 */
	public boolean isQuickChangeRunning()
	{
		return bQuickChangeRunning;
	}
	
	/**
	 * Run a quick action for the given parameters
	 * @param interval as index (@see array.xml)
	 * @param accuracy as index (@see array.xml)
	 * @param duration as index (@see array.xml)
	 * @return true if set successfully
	 */
	public boolean setQuickChange(int interval, int accuracy, int duration)
	{
		// @see arrays.xml
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		int minutes = 0;
		int intervalMs = 0;
		int accuracyM = 0;

		switch (interval)
		{
		case 0:
			intervalMs = 5 * 1000;
			break;
		case 1:
			intervalMs = 10 * 1000;
			break;
		case 2:
			intervalMs = 30 * 1000;
			break;
		case 3:
			intervalMs = 60 * 1000;
			break;
		case 4:
			intervalMs = 5 * 60 * 1000;
			break;
		case 5:
			intervalMs = 15 * 60 * 1000;
			break;
		}

		switch (accuracy)
		{
		case 0:
			accuracyM = 10;
			break;
		case 1:
			accuracyM = 50;
			break;
		case 2:
			accuracyM = 100;
			break;
		case 3:
			accuracyM = 500;
			break;
		case 5:
			accuracyM = 1000;
			break;
		}

		switch (duration)
		{
		case 0:
			minutes = 15;
			break;
		case 1:
			minutes = 30;
			break;
		case 2:
			minutes = 60;
			break;
		case 3:
			minutes = 120;
			break;
		}

		m_iIterval = intervalMs;
		m_iAccuracy = accuracyM;		

		cal.add(Calendar.MINUTE, minutes);

		Intent intent = new Intent(this, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, QUICK_ACTION,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);

		// change
		registerLocationListener(intervalMs, accuracyM);
		bQuickChangeRunning = true;
		return true;
	}
	
	/**
	 * Reset (void) any running quick action
	 */
	public void resetQuickChange()
	{

		// check if there is an intent pending
		Intent intent = new Intent(this, AlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, QUICK_ACTION,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (sender != null)
		{
			// Get the AlarmManager service
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.cancel(sender);
		}
		// reset to pref values
		registerLocationListener();
		bQuickChangeRunning = false;
	}

	/**
	 * Returns the update interval requested from the LocationProvider
	 * @return the interval in ms
	 */
	public int getInterval()
	{
		return m_iIterval;
	}
	
	/**
	 * Returns the accuracy requested from the LocationProvider
	 * @return the accuracy in m
	 */
	public int getAccuracy()
	{
		return m_iAccuracy;
	}
			
	/**
	 * Return the current address (if geo is on)
	 * @return the current location as a string
	 */
	public String getLocationProvider()
	{
		return m_strLocProvider;
	}
	
	/**
	 * Returns the current location (address) as a string
	 * @return the current address
	 */
	public String getCurrentLocation()
	{
		return m_strCurrentLocation;
	}

	/**
	 * Returns the number of buffered locations
	 * @return the number 
	 */
	public int getBufferSize()
	{
		int iRet = 0; 
		if ( (m_locationStack != null) && (m_locationStack.size() > 0) )
		{
			iRet = m_locationStack.size();
		}
		return iRet;
	}
	
	class UpdateLatitudeTask extends AsyncTask<Insert, Void, Void>
	{

	    private Exception exception;

	    protected Void doInBackground(Insert... inserts)
	    {
	    	try
	        {
	    		Log.d(TAG, "before insert.execute");
	    		inserts[0].execute();
	    		Log.d(TAG, "after insert.execute");
	        	return null;
	        }
	        catch (Exception e)
	        {
	        	Log.d(TAG, "error in insert.execute");
	            this.exception = e;
	            Log.e(TAG, "An error occured in UpdateLatitude.doInBackground(): " + e.getMessage());
	            return null;
	        }
	    }

	    protected void onPostExecute()
	    {

	    }
	 }


}

