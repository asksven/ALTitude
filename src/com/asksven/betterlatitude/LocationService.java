/*
 * Copyright (C) 2011-2012 asksven
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

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.location.GeoUtils;
import com.asksven.android.common.networkutils.DataNetwork;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.MathUtils;
import com.asksven.betterlatitude.credentialstore.CredentialStore;
import com.asksven.betterlatitude.credentialstore.SharedPreferencesCredentialStore;
import com.asksven.betterlatitude.localeplugin.Constants;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
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
	private static int QOS_ALARM 	= 1234568;

	private LocationManager m_locationManager;
	
	private WifiStateHandler m_wifiHandler = new WifiStateHandler();

	private static final String TAG = "LocationService";
		
	/** the connection status */
	private String m_strStatus = "";
	
	private boolean m_bRegistered = false;

	private ArrayList<Location> m_locationStack = null;
	
	private boolean bQuickChangeRunning = false;

	/** the location provider in use */
	String m_strLocProvider = "";
	
	/** the current location (is geo is on) */
	String m_strCurrentLocation  = "";
	
	/** the last updated location */
	Location m_lastUpdatedLoc = null;

	/** the time of last update */
	long m_lastUpdatedTime = 0;

	/** precision for current location manager */
	private int m_iIterval = 0;
	private int m_iAccuracy = 0;
	private int m_iDuration = 0;
	
	
	/** spinner indexes for quick actions */
	int m_iIntervalIndex = 0;
	int m_iAccuracyIndex = 0;
	int m_iDurationIndex = 0;
	
	long m_lUpdated = 0;
	long m_lQuickUntil = 0;
	
	Notification m_stickyNotification = null;


    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        public LocationService getService()
        {
        	Logger.i(TAG, "getService called", LocationService.this);
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
    	Logger.i(getClass().getSimpleName(), "onCreate called", this);

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        // register the location listener
        this.registerLocationListener();
        
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean bHandleWifi = prefs.getBoolean("update_on_wifi_only", false);
    	if (bHandleWifi)
    	{
	        // register the Wifi state receiver
	        this.registerReceiver(m_wifiHandler, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    	}
        // Set up a listener whenever a key changes
    	PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

    	// set status
    	setStatus(AltitudeConstants.getInstance(this).STATUS_UPDATE_PENDING);
    	
    	// trigger QoS alarm (will do nothing if settings say not to
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
    		Logger.e(TAG, "Error reading prefernces, using defaults", this);
    	}
    	
		registerLocationListener(iInterval, iAccuracy);

            
    }
    
    public ArrayList<Location> getLocationStack()
    {
    	return m_locationStack;
    	
    }
    
    public void clearLocationStack()
    {
    	Logger.i(TAG, "clearing location stack", this);
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
    	criteria.setCostAllowed(false);
    	criteria.setBearingRequired(false);
    	
    	if (accuracyM <= 100)
    	{
    		criteria.setAccuracy(Criteria.ACCURACY_FINE);
    		criteria.setPowerRequirement(Criteria.POWER_HIGH);
    		//criteria.setBearingRequired(true);
    	}
    	else
    	{
    		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    		criteria.setPowerRequirement(Criteria.POWER_LOW);
    	}
    	

		// Get the location manager
		m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (m_locationManager != null)
		{
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	boolean bUsePassiveProvider = prefs.getBoolean("passive_provider", false);

			if (!bUsePassiveProvider)
			{
		        m_strLocProvider = m_locationManager.getBestProvider(criteria, true);
		        Logger.i(TAG, "registerLocationListener determined best provider: " + m_strLocProvider, this);
		        if (m_strLocProvider != null)
		        {
			        m_locationManager.requestLocationUpdates(m_strLocProvider, intervalMs, accuracyM, this);
			        m_iAccuracy = accuracyM;
			        m_iIterval = intervalMs;
			        Logger.i(TAG, "Using provider '" + m_strLocProvider + "'", this);
				}
		        else
		        {
		        	Logger.e(TAG, "requestLocationUpdates could not be called because there is no location provider available: m_strLocProvider='" + m_strLocProvider + "'", this);
		        	m_bRegistered = false;
		        }
			}
			else
			{
				m_locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);
				Logger.i(TAG, "Requesting passive provider", this);
				m_iAccuracy = 0;
		        m_iIterval = 0;
			}
	        m_bRegistered = true;
		}
		else
		{
			m_bRegistered = false;
			Logger.i(TAG, "No location manager could be set", this);
		}
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
    	if (key.equals("update_interval") || key.equals("update_accuracy"))
    	{
    		
    		Logger.i(TAG, "Preferences have change. Register location listener again", this);
    		// re-register location listener with new prefs
    		this.registerLocationListener();
    	}

    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	if (key.equals("notify_status"))
    	{
    		// activate / deactivate the notification
    		if (prefs.getBoolean("notify_status", true))
    		{
    			notifyStatus(AltitudeConstants.getInstance(this).STATUS_NOTIFICATION_ON);
    		}
    		else
    		{
    	        // Cancel the persistent notification.
    	        mNM.cancel(R.string.app_name);

    		}
    	}
    	
    	if (key.equals("update_on_wifi_only"))
    	{
        	if (prefs.getBoolean("update_on_wifi_only", false))
        	{
    	        // register the Wifi state receiver
    	        this.registerReceiver(m_wifiHandler, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        	}
        	else
        	{
        		// unregister Wifi state receiver
        		unregisterReceiver(m_wifiHandler);
        	}
    	}
    	
    	if (key.equals("foreground_service"))
    	{
    		// stop and start the service, starting it will lead to prefs being read
			Intent i = new Intent();
			i.setClassName( "com.asksven.betterlatitude", LocationService.SERVICE_NAME );
       		stopService(i);
       		startService(i);
    	}

    	if (key.equals("force_interval"))
    	{
        	if (prefs.getBoolean("force_interval", false))
        	{
        		// set the QoS alarm
        		this.setQosAlarm();
        	}
        	else
        	{
        		// cancel the running QoS alarm 
        		this.cancelQosAlarm();

        	}
    	}

    }

    /** 
     * Called when service is started
     */
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Logger.i(getClass().getSimpleName(), "Service started, received start id " + startId + ": " + intent, this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean bForegroundService = prefs.getBoolean("foreground_service", true);
    	if (bForegroundService)
    	{
    		setupAsForeground(AltitudeConstants.getInstance(this).STATUS_FG_SERVICE_STARTED);
    	}
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        
        return Service.START_STICKY;
    }

    void setupAsForeground(String strNotification)
    {
    	m_stickyNotification = new Notification(
    			R.drawable.icon, AltitudeConstants.getInstance(this).STATUS_SERVICE_RUNNING, System.currentTimeMillis());
		Intent i=new Intent(this, MainActivity.class);
		
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
		Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pi=PendingIntent.getActivity(this, 0, i, 0);

		m_stickyNotification.setLatestEventInfo(this, "ALTitude", strNotification, pi);
		m_stickyNotification.flags|=Notification.FLAG_NO_CLEAR;
		
		if (isServiceRunning(this))
		{
			Logger.i(TAG, "setupAsForeground was called to update the notification", this);
		}
		else
		{
			Logger.i(TAG, "setupAsForeground was called and started the service", this);
		}
			
		startForeground(12245, m_stickyNotification);
    	
    }
    @Override
    /**
     * Called when Service is terminated
     */
    public void onDestroy()
    {        
        // Cancel the persistent notification.
        mNM.cancel(R.string.app_name);
    	// unregister the receivers
		m_locationManager.removeUpdates(this);
		
		// hack: there is no way to test whether a receiver is registered so we have to try and ignore the exception
		try
		{
			unregisterReceiver(m_wifiHandler);
		}
		catch (IllegalArgumentException e)
		{
			// do nothing
		}
		
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
    	Logger.i(TAG, "onLocationChanged called", this);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	// we may have QoS: as the location was just updated we need to reset the alarm counter
    	setQosAlarm();
    	
    	// if we are not in quick change mode the we need to check if there are limitiations
    	// to the update frequency / distance to be applied
    	if (isQuickChangeRunning())
    	{
    		m_locationStack.add(location);
    	}
    	else
    	{
    		// check if we are limiting the update frequency
    		boolean intervalBelowLimit = false;
    		boolean distanceBelowLimit = false;
    		
    		if (prefs.getBoolean("limit_update_interval", false))
    		{
    			long interval = (System.currentTimeMillis() - m_lastUpdatedTime) / 1000 / 60;
    	    	int iMinInterval = 0;
    	    	try
    	    	{
    	    		iMinInterval = Integer.valueOf(prefs.getString("max_update_interval", "0"));
    	    		
    	    	}
    	    	catch (Exception e)
    	    	{
    	    		Log.e(TAG, "An error occured while reading quick action preferences");
    	    	}
    	    	
    	    	Log.i(TAG, "Time since last update [Min.]: " + interval);
    	    
    	    	if (interval < iMinInterval)
    	    	{
    	    		Log.i(TAG, "Interval is below limit");
    	    		intervalBelowLimit = true;
    	    	}
    		}
    		
    		// check if we are limiting distance between updates
    		if (prefs.getBoolean("limit_update_accuracy", false))
    		{
    			if (m_lastUpdatedLoc != null)
    			{
	    			double distance = MathUtils.getDistanceGreatCircle(m_lastUpdatedLoc, location);
	    			Log.i(TAG, "Distance from last update: " + distance);
	    	    	int iMinDistance = 0;
	    	    	try
	    	    	{    	        	
	    	    		iMinDistance = Integer.valueOf(prefs.getString("max_update_accuracy", "0"));
	    	    	}
	    	    	catch (Exception e)
	    	    	{
	    	    		Log.e(TAG, "An error occured while reading quick action preferences");
	    	    	}
	    	    	
	    	    	if (distance < iMinDistance)
	    	    	{
	    	    		Log.i(TAG, "Distance is below limit");
	    	    		distanceBelowLimit = true;
	    	    	}
    			}
    			
    		}
    		
    		// if both values are below limit we don't update
    		if (distanceBelowLimit && intervalBelowLimit)
    		{
    			// do nothing
    			Log.i(TAG, "Both interval and distance are below limits: location will not be queued");
    		}
    		else
    		{
    			Log.i(TAG, "Interval or distance is above limit: location is being queued");    			
        		m_locationStack.add(location);    			
    		}

    	}

		try
		{
			if (!updateLatitude())
			{	
				Logger.i(TAG, "Location added to stack. The stack has " + m_locationStack.size() + " entries.", this);
				notifyStatus(m_locationStack.size() + " " + getString(R.string.locations_buffered));
			}
		}
		catch (Exception e)
		{
			notifyStatus(m_locationStack + " " + getString(R.string.locations_buffered));
		}
	}

    /** 
     * forces an update with the last known location
     */
	public void forceLocationUpdate()
	{    	
    	// we may have QoS: as the location was just updated we need to reset the alarm counter
    	setQosAlarm();
    	
    	if (m_strLocProvider == null)
    	{
    		Logger.i(TAG, "forceLocationUpdate aborted: no location provider defined", this);
    		return;
    	}
    	
    	Location here = m_locationManager.getLastKnownLocation(m_strLocProvider);
    	
    	// we need to change the timestamp to "now"
    	long now =System.currentTimeMillis();
    	here.setTime(now);
    	
    	m_locationStack.add(here);

		try
		{
			if (!updateLatitude())
			{	
				Logger.i(TAG, "Adding location to stack. The stack has " + m_locationStack.size() + " entries.", this);
				notifyStatus(m_locationStack.size() + " " + getString(R.string.locations_buffered));
			}
		}
		catch (Exception e)
		{
			notifyStatus(m_locationStack + " " + getString(R.string.locations_buffered));
		}
	}

    /** 
     * forces an update with the last known location
     */
	public void forceLocationUpdate(float latitude, float longitude)
	{    	
    	// we may have QoS: as the location was just updated we need to reset the alarm counter
    	setQosAlarm();
    	
    	if (m_strLocProvider == null)
    	{
    		Logger.i(TAG, "forceLocationUpdate aborted: no location provider defined", this);
    		return;
    	}
    	
    	Location here = m_locationManager.getLastKnownLocation(m_strLocProvider);
    	here.setLatitude(latitude);
    	here.setLongitude(longitude);
    	here.setAccuracy(10f);
    	
    	// we need to change the timestamp to "now"
    	long now =System.currentTimeMillis();
    	here.setTime(now);
    	
    	m_locationStack.add(here);

		try
		{
			if (!updateLatitude())
			{	
				Logger.i(TAG, "Adding location to stack. The stack has " + m_locationStack.size() + " entries.", this);
				notifyStatus(m_locationStack.size() + " " + getString(R.string.locations_buffered));
			}
		}
		catch (Exception e)
		{
			notifyStatus(m_locationStack + " " + getString(R.string.locations_buffered));
		}
	}

    @Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		Logger.e(TAG, "onStatusChanged called with status=" + status, this);
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
		Logger.e(TAG, "Provider " + provider + " was enabled. Maybe we want to use it", this);
		if (bQuickChangeRunning)
		{
			registerLocationListener(m_iIterval, m_iAccuracy);
		}
		else
		{
			registerLocationListener();
		}
		
		if (LocationManager.GPS_PROVIDER.equals(provider) && !isQuickChangeRunning())
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	if (prefs.getBoolean("auto_on_when_gps", false))
	    	{
	    		setQuickChange();
	        	// refresh widget
	    		Intent intent = new Intent(this.getApplicationContext(),
	    				MyWidgetProvider.class);
	    		intent.setAction(MyWidgetProvider.ACTION_REFRESH);
	    		this.sendBroadcast(intent);

	    	}
			
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
			Logger.e(TAG, "Provider " + provider + " was in use but got disabled. Getting a new one", this);
			if (bQuickChangeRunning)
			{
				registerLocationListener(m_iIterval, m_iAccuracy);
			}
			else
			{
				registerLocationListener();
			}		
		}
		
		if (LocationManager.GPS_PROVIDER.equals(provider))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    	if (prefs.getBoolean("auto_on_when_gps", false) && isQuickChangeRunning())
	    	{
	    		resetQuickChange();
	        	// refresh widget
	    		Intent intent = new Intent(this.getApplicationContext(),
	    				MyWidgetProvider.class);
	    		intent.setAction(MyWidgetProvider.ACTION_REFRESH);
	    		this.sendBroadcast(intent);

	    	}
			
		}

	}	

	protected boolean updateLatitude()
	{
		boolean bRet = true;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean bWifiUpdatesOnly = prefs.getBoolean("update_on_wifi_only", false);

    	boolean bEnabled = prefs.getBoolean("enabled", true);
    	
    	// if not enabled forget the location
    	if (!bEnabled)
    	{
    		clearLocationStack();
    		return true;
    	}
    	
    	Logger.i(TAG, "updateLatitude called, prepating to update " + m_locationStack.size() + " locations", this);
		try
		{
			// if no data connection is present no need to try
			if (!DataNetwork.hasDataConnection(this))
			{
				Logger.i(TAG, "No data connection available, aborting", this);
				return false; 
			}

			// if updates are set to be done on wifi only and wifi is not connected do nothing
			if ((bWifiUpdatesOnly) && (!DataNetwork.hasWifiConnection(this)))
			{
				Logger.i(TAG, "Updates will happen only on wifi and wifi is not available. Aborting", this);
				return false;
			}

	    	boolean bLogLoc 		= prefs.getBoolean("log_location", false);
	    	
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
			AccessTokenResponse accessTokenResponse = credentialStore.read();
			
			// check if token is valid (not empty)
			if (accessTokenResponse.accessToken.equals(""))
			{
				Logger.e(TAG, "No access token available: not logged in", this);
				notifyError(getString(R.string.not_logged_on_error));
				bRet = false;
				setStatus(AltitudeConstants.getInstance(this).STATUS_NOT_LOGGED_IN);
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
								+ " Long: " + String.valueOf(location.getLongitude()), this);
			    	}
			    	else
			    	{
			    		Logger.i(TAG, "Service Updating Latitude, stack entry " + i+1 + " of " + m_locationStack.size(), this);
			    	}

				    LatitudeCurrentlocationResourceJson currentLocation = new LatitudeCurrentlocationResourceJson();
				    currentLocation.set("latitude", location.getLatitude());
				    currentLocation.set("longitude", location.getLongitude());
				    currentLocation.set("timestampMs", location.getTime());
				    
				    Insert myInsert = latitude.currentLocation.insert(currentLocation);

				    String now = DateUtils.now("HH:mm:ss");
				    setStatus(AltitudeConstants.getInstance(this).STATUS_LOCATION_UPDATED);

				    if (myInsert != null)
				    {
				    	new UpdateLatitudeTask().execute(myInsert);
				    	
				    	// write down the last updated location and update time
				    	m_lastUpdatedLoc 	= location;
				    	m_lastUpdatedTime 	= System.currentTimeMillis();
				    }
				    else
				    {
				    	Logger.i(TAG, "Location buffered, stack now has " + m_locationStack.size() + " elements", this);
				    	setStatus(AltitudeConstants.getInstance(this).STATUS_UPDATE_BUFFERED + "(" + m_locationStack.size() + ")");
				    	
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
			Logger.e(TAG, "An error occured in updateLatitude() '" +  ex.getMessage() + "'", this);
			if (ex.getMessage().equals("401 Unauthorized"))
			{
				setStatus(AltitudeConstants.getInstance(this).STATUS_NOT_LOGGED_IN);
			}
			notifyError(getString(R.string.latitude_error) + " '" + ex.getMessage() + "'");
//			Logger.i(TAG, ex.getStackTrace());
			
		}
		catch (Exception ex)
		{
			bRet = false;
			Logger.e(TAG, "An error occured in updateLatitude() '" +  ex.getMessage() + "'", this);
			Logger.e(TAG, ex.getStackTrace(), this);
			
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
    		if (m_stickyNotification != null)
    		{
    			setupAsForeground(strStatus);
    		}
    		else
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
	}
	
	/**
	 * Notify location change in notification bar (if enabled)
	 */
	public void notifyCurrentLocation()
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String strStatus = AltitudeConstants.getInstance(this).STATUS_LOCATION_UPDATED;
    	
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
    	else if (bNotify)
    	{
    		// simple notification
			strStatus = strStatus
					+ " at "
					+ DateUtils.now();
			notifyStatus(strStatus);
    	}
    		
	}

	
	/**
	 * Notify errors in notification bar (if enabled)
	 */
	void notifyError(String strStatus)
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean bNotify 	= prefs.getBoolean("notify_error", true);
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
    	// Log the error
    	Logger.e(TAG, "An error occured: " + strStatus, this);
	}
	

	/** 
	 * Broadcasts the connection status change
	 * @param strStatus the broadcasted message
	 */
	public void setStatus(String strStatus)
	{
		m_strStatus = strStatus;
		sendBroadcast(new Intent(AltitudeConstants.getInstance(this).BROADCAST_STATUS_CHANGED));
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
	 * Returns the last known location address (if enabled)
	 * @return the last known address
	 */
	public String getAddress()
	{
		return m_strCurrentLocation;
	}

	/**
	 * Returns until when the quick change is active
	 * @return
	 */
	public long getUntil()
	{
		return m_lQuickUntil;
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
	 * 
	 * @return the accuracy spinner index
	 */
	public int getAccuracyIndex()
	{
		return m_iAccuracyIndex;
	}

	/**
	 * 
	 * @return the duration spinner index
	 */
	public int getDurationIndex()
	{
		return m_iDurationIndex;
	}

	/**
	 * 
	 * @return the interval spinner index
	 */
	public int getIntervalIndex()
	{
		return m_iIntervalIndex;
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
		Logger.i(TAG, "setQuickChange called with " + interval + accuracy + duration, this);
		// @see arrays.xml
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		int minutes = 0;
		int intervalMs = 0;
		int accuracyM = 0;
		
		m_iIntervalIndex = interval;
		m_iAccuracyIndex = accuracy;
		m_iDurationIndex = duration;


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
		m_iDuration = minutes;

		cal.add(Calendar.MINUTE, minutes);
		m_lQuickUntil = cal.getTimeInMillis();

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
		Logger.i(TAG, "resetQuickChange called", this);
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
		m_iIntervalIndex = 0;
		m_iAccuracyIndex = 0;
		m_iDurationIndex = 0;
	
	}

	/**
	 * Start a quick change with the default values 
	 */
	public void setQuickChange()
	{
    	int iAccuracy = 0;
    	int iInterval = 0;
    	int iDuration = 0;
    	try
    	{
    		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        	
    		iAccuracy = Integer.valueOf(sharedPrefs.getString("quick_update_accuracy", "0"));
    		iInterval = Integer.valueOf(sharedPrefs.getString("quick_update_interval", "0"));
    		iDuration = Integer.valueOf(sharedPrefs.getString("quick_update_duration", "0"));
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "An error occured while reading quick action preferences");
    	}

    	// start a quick setting with defaults
		this.setQuickChange(iInterval, iAccuracy, iDuration);

	}

	/**
	 * Returns the update interval requested from the LocationProvider in ms
	 * @return the interval in ms
	 */
	public int getInterval()
	{
		return m_iIterval;
	}
	
	/**
	 * Returns the accuracy requested from the LocationProvider in m
	 * @return the accuracy in m
	 */
	public int getAccuracy()
	{
		return m_iAccuracy;
	}
			
	/**
	 * Returns the duration of the quick setting in ms
	 * @return the duration in ms
	 */
	public int getDuration()
	{
		return m_iDuration;
	}
	
	/**
	 * Returns the time (in ms) when the last update took place
	 * @return
	 */
	public long getUpdated()
	{
		return m_lUpdated;
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
	    		Logger.d(TAG, "updating latitude (insert.execute)", LocationService.this);
	    		inserts[0].execute();
	    		m_lUpdated = System.currentTimeMillis();
	        	return null;
	        }
	        catch (Exception e)
	        {
	        	Logger.e(TAG, "error in insert.execute", LocationService.this);
	            this.exception = e;
	            Logger.e(TAG, "An error occured in UpdateLatitude.doInBackground(): " + e.getMessage(), LocationService.this);
	            Log.e(TAG, "Exception info: "+Log.getStackTraceString(e));
	            return null;
	        }
	    }

	    protected void onPostExecute()
	    {

	    }
	 }

	protected static boolean isServiceRunning(Context ctx)
	{
	    ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
	    {
	        if (LocationService.SERVICE_NAME.equals(service.service.getClassName()))
	        {
	        	Log.i(TAG, "isMyServiceRunning confirmed that service is running");
	            return true;
	        }
	    }
	    Log.i(TAG, "isMyServiceRunning confirmed that service is not running");
	    return false;
	}/**
	 * 
	 */

	
	/**
	 * Adds an alarm to schedule a wakeup to retrieve the current location
	 */
	public boolean setQosAlarm()
	{
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	if (!prefs.getBoolean("force_interval", false))
    	{
    		cancelQosAlarm();
    		return false;
    	}

		Logger.i(TAG, "setQosAlarm called", this);
		
		// cancel any exiting alarms
		cancelQosAlarm();

		// create a new one starting to count NOW
		Calendar cal = Calendar.getInstance();
		
    	String strInterval = prefs.getString("update_interval", "15");
    	if (!Configuration.isFullVersion(this))
		{
    		strInterval = "15";
		}
    	    	
		int iInterval = 15;
		try
    	{
			iInterval = Integer.valueOf(strInterval);
    	}
    	catch (Exception e)
    	{
    	}

		Logger.i(TAG, "QoS alarm scheduled in " + iInterval + " minutes", this);
		long fireAt = System.currentTimeMillis() + (iInterval * 60 * 1000);

		Intent intent = new Intent(this, QosAlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, QOS_ALARM,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, fireAt, sender);

		return true;
	}
	
	/**
	 * Cancels the current alarm (if existing)
	 */
	public void cancelQosAlarm()
	{
		Logger.i(TAG, "cancelQosAlarm", this);
		// check if there is an intent pending
		Intent intent = new Intent(this, QosAlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(this, QOS_ALARM,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (sender != null)
		{
			// Get the AlarmManager service
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.cancel(sender);
		}
	}

}

