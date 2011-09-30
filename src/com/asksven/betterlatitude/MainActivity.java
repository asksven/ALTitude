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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.test.PerformanceTestCase;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.betterlatitude.credentialstore.CredentialStore;
import com.asksven.betterlatitude.credentialstore.SharedPreferencesCredentialStore;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;
import com.asksven.betterlatitude.R;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;
import com.google.ads.*;

public class MainActivity extends Activity

{
	/**
	 * Store the status after first logon to avoid calling the API every time the Activity is opene
	 */
	private boolean m_bLoggedOn = false;
	
	/**
	 * The logging TAG
	 */
	private static final String TAG = "MainActivity";

	/**
	 * Constants for menu items
	 */
	private final int MENU_ITEM_UPDATE_LATITUDE = 0;
	private final int MENU_ITEM_GET_LOC = 1;
	private final int MENU_ITEM_PREFS = 2;
	private final int MENU_ITEM_MAP = 3;
	private final int MENU_ITEM_ABOUT = 4;
	private final int MENU_ITEM_BROWSER = 5;
	private final int MENU_ITEM_LOGON = 6;
	private final int MENU_ITEM_LOGOFF = 7;
	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	private double m_dLat = -1;
	private double m_dLong = -1;
	
//	boolean m_bIsStarted;
	/** 
	 * The location provider delivers the loc info
	 */
//	private LocationManager m_LocationManager;
//	String m_strLocProvider;

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	
		try
		{
			// recover any saved state
			if ( (savedInstanceState != null) && (!savedInstanceState.isEmpty()))
			{
				m_bLoggedOn 	= (Boolean) savedInstanceState.getSerializable("logged_on");
			}
		}
		catch (Exception e)
		{
			m_bLoggedOn	= false;
			Log.e(TAG, "Exception: " + e.getMessage());
		}

		// detect free/full version and enable/disable ads
		if (!Configuration.isFullVersion(this))
		{
			AdView adView = (AdView)this.findViewById(R.id.adView);
		    adView.loadAd(new AdRequest());
		}
		

	    // Initiate a generic request to load it with an ad
        // retrieve the version name and display it
		
		
        try
        {
        	PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	TextView versionTextView = (TextView) findViewById(R.id.textViewVersion);
        	TextView nameTextView = (TextView) findViewById(R.id.textViewName);
        	TextView hintTextView = (TextView) findViewById(R.id.textViewHint);
        	
        	if (Configuration.isFullVersion(this))
    		{
        		nameTextView.setText("ALTitude full");
        		hintTextView.setText("");
    		}
        	else
        	{
        		nameTextView.setText("ALTitude free");
        		hintTextView.setText("Add-free full version available");
        	}
        	
        	versionTextView.setText(pinfo.versionName);
        }
        catch (Exception e)
        {
        	Log.e(TAG, "An error occured retrieveing the version info: " + e.getMessage());
        }
  	}

    /**
     * Save state, the application is going to get moved out of memory
     * @see http://stackoverflow.com/questions/151777/how-do-i-save-an-android-applications-state
     */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
    {
    	super.onSaveInstanceState(savedInstanceState);
        
    	savedInstanceState.putSerializable("logged_on", m_bLoggedOn); 
    }

	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();

		// Performs an authorized API call.
		if (!m_bLoggedOn)
		{
			new OauthLogin().execute("");
		}
    	
		startService();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String strInterval = prefs.getString("update_interval", "15");
    	String strAccuracy = prefs.getString("update_accuracy", "2000");
    	    	
		int iInterval = 15 * 60 * 1000;
		int iAccuracy = 2000;
		try
    	{
			iInterval = Integer.valueOf(strInterval);
			iAccuracy = Integer.valueOf(strAccuracy);
    	}
    	catch (Exception e)
    	{
    		Logger.e(TAG, "Error reading prefernces, using defaults");
    	}

//		m_LocationManager.requestLocationUpdates(m_strLocProvider, iInterval, iAccuracy, this);
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
//		m_LocationManager.removeUpdates(this);
//		Logger.i(TAG, "Activity paused, removing location listener");
	}
	

	/** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
//    	menu.add(0, MENU_ITEM_UPDATE_LATITUDE, 0, "Update Latitude");
//    	menu.add(0, MENU_ITEM_GET_LOC, 0, "Refresh");
    	menu.add(0, MENU_ITEM_PREFS, 0, "Preferences");
    	menu.add(0, MENU_ITEM_MAP, 0, "Show on Map");
    	menu.add(0, MENU_ITEM_LOGON, 0, "Log on");
    	menu.add(0, MENU_ITEM_LOGOFF, 0, "Log off");
//    	menu.add(0, MENU_ITEM_BROWSER, 0, "Browser");
//    	menu.add(0, MENU_ITEM_ABOUT, 0, "About");
    	
    	return true;
    }
    
    /** 
     * Define menu action
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {  
        switch (item.getItemId())
        {  
	        case MENU_ITEM_UPDATE_LATITUDE: // update location  
	        	new OauthLogin().execute("");
	        	//setLocationApiCall(); //performApiCall();
	        	break;	
	        case MENU_ITEM_GET_LOC: // retrieve location from Latitude  
	        	getLocationApiCall();
//	        	getCellLocation();
//	        	if (isMyServiceRunning())
//	        	{
//	        		textViewServiceStatus.setText("Started");
//	        	}
//	        	else
//	        	{
//	        		textViewServiceStatus.setText("Stopped");
//	        	}
	        	break;	
	        case MENU_ITEM_PREFS: // prefs  
	        	Intent intentPrefs = new Intent(this, PreferencesActivity.class);
	            this.startActivity(intentPrefs);
	        	break;	
	        case MENU_ITEM_MAP: // map  
	        	Intent intentMap = new Intent(this, ShowOnMapActivity.class);
	            this.startActivity(intentMap);
	        	break;	
	        case MENU_ITEM_BROWSER: // browse maps.google.com
	        	Intent intentBrowser = new Intent(this, BrowserActivity.class);
	            this.startActivity(intentBrowser);
	        	break;	

	        case MENU_ITEM_LOGON:  
	        	logOn();
	        	break;	

	        case MENU_ITEM_LOGOFF: 
	        	logOff();
	        	break;	

        }
        
        return true;
    }
    
	// Launch the OAuth flow to get an access token required to do authorized API calls.
	// When the OAuth flow finishes, we redirect to this Activity to perform the API call.
	public void logOn()
	{
		startActivity(new Intent().setClass(
				this,
				OAuthAccessActivity.class));
	}


	// Clearing the credentials and performing an API call to see the unauthorized message.
	void logOff()
	{
		clearCredentials();
		getLocationApiCall();
	}

    /**
	 * Clears our credentials (token and token secret) from the shared preferences.
	 * We also setup the authorizer (without the token).
	 * After this, no more authorized API calls will be possible.
	 */
    private void clearCredentials()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	new SharedPreferencesCredentialStore(prefs).clearCredentials();
    }
	
    /**
     * Performs an authorized API call to retrieve the current location.
     */
	private boolean getLocationApiCall()
	{
		boolean bRet = true;
		Logger.i(TAG, "getLocationApiCall called");
		try
		{
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
			AccessTokenResponse accessTokenResponse = credentialStore.read();

			// check if token is valid (not empty)
			if (accessTokenResponse.accessToken.equals(""))
			{
				bRet = false;
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
		    
			LatitudeCurrentlocationResourceJson currentLocation = 
				latitude.currentLocation.get().execute();
			m_bLoggedOn = true;
		}
		catch (IOException ex)
		{
			Logger.e(TAG, "Exception in getLocationApiCall");
			bRet = false;
			ex.printStackTrace();
		}
		
		return bRet;
	}

	
	/** 
     * Starts the service 
     */
	private void startService()
	{
		if( isMyServiceRunning() )
		{
			Toast.makeText(this, "Service already started", Toast.LENGTH_SHORT).show();
		}
		else
		{
			Intent i = new Intent();
			i.setClassName( "com.asksven.betterlatitude", LocationService.SERVICE_NAME );
			startService( i );
			Log.i(getClass().getSimpleName(), "startService()");
		}
	}
	
	private boolean isMyServiceRunning()
	{
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
	    {
	        if (LocationService.SERVICE_NAME.equals(service.service.getClassName()))
	        {
	            return true;
	        }
	    }
	    return false;
	}
	
	/**
	 * Connect to latitude and retrieve location in a thread 
	 * @author sven
	 * @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	 * for more details
	 */
	private class OauthLogin extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			MainActivity.this.getLocationApiCall();
			
	        return true;
	    }
		
		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
    		m_progressDialog.hide();
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	m_progressDialog = new ProgressDialog(MainActivity.this);
	    	m_progressDialog.setMessage("Retrieving current location...");
	    	m_progressDialog.setIndeterminate(true);
	    	m_progressDialog.setCancelable(false);
	    	m_progressDialog.show();
	    }
	}

//	/**
//	 * Do nothing, those methods must be here because of the implemented interface
//	 */
//	@Override
//	public void onProviderEnabled(String provider)
//	{
//	}
//
//	@Override
//	public void onProviderDisabled(String provider)
//	{
//	}	


}