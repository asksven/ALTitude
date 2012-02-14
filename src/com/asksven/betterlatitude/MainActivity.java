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

import java.io.IOException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


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
    public static final String MARKET_LINK ="market://details?id=com.asksven.commandcenter";
    public static final String TWITTER_LINK ="https://twitter.com/#!/asksven";

	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/** The event receiver for updated from the service */
	private ConnectionUpdateReceiver m_connectionUpdateReceiver;

	/** spinner indexes for quick actions */
	int m_iIntervalIndex = 0;
	int m_iAccuracyIndex = 0;
	int m_iDurationIndex = 0;

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
        
        // Set the connection state
    	TextView statusTextView = (TextView) findViewById(R.id.textViewStatus);
    	LocationService myService = LocationService.getInstance();
    	if (myService != null)
    	{
    		statusTextView.setText(myService.getStatus());
    	}
    	else
    	{
    		statusTextView.setText(LocationService.STATUS_NOT_LOGGED_IN);
    	}
    	
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	// show Rate button if it wasn't clicked yet
    	if (prefs.getInt("show_rate", 0) == 0)
    	{
	        final Button buttonRate = (Button) findViewById(R.id.buttonRate);
	        buttonRate.setOnClickListener(new View.OnClickListener()
	        {
	            public void onClick(View v)
	            {
	            	SharedPreferences.Editor editor = prefs.edit();
	    	        editor.putInt("show_rate", 1);
	    	        editor.commit();

	    	        openURL(MARKET_LINK);
	            }
	        });
    	}
    	
        // show Follow button if it wasn't clicked yet
        if (prefs.getInt("show_follow", 0) == 0)
        {
	        final Button buttonFollow = (Button) findViewById(R.id.buttonTwitter);
	        buttonFollow.setOnClickListener(new View.OnClickListener()
	        {
	            public void onClick(View v)
	            {
	            	SharedPreferences.Editor editor = prefs.edit();
	    	        editor.putInt("show_follow", 1);
	    	        editor.commit();

	                openURL(TWITTER_LINK);
	            }
	        });
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
//			new OauthLogin().execute("");
		}
    	
		startService();
		
		// set up the listener for connection status changes 
		if (m_connectionUpdateReceiver == null)
		{
			m_connectionUpdateReceiver = new ConnectionUpdateReceiver();
		}
		IntentFilter intentFilter = new IntentFilter(LocationService.BROADCAST_STATUS_CHANGED);
		registerReceiver(m_connectionUpdateReceiver, intentFilter);
	}

	/* Remove the event listener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// unregister event listener
		if (m_connectionUpdateReceiver != null)
		{
			unregisterReceiver(m_connectionUpdateReceiver);
		}
	}
	

	/** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem quickAction = menu.findItem(R.id.quick_dialog);
    	
    	if (!Configuration.isFullVersion(this))
    	{
    		quickAction.setEnabled(false);
    	}
    	else
    	{
    		quickAction.setEnabled(true);
    	}
    	
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
	        case R.id.preferences:  
	        	Intent intentPrefs = new Intent(this, PreferencesActivity.class);
	            this.startActivity(intentPrefs);
	        	break;	
	        case R.id.show_map:  
	        	Intent intentMap = new Intent(this, ShowOnMapActivity.class);
	            this.startActivity(intentMap);
	        	break;	
	        case R.id.log_on:  
	        	logOn();
	        	break;	
	        case R.id.log_off: 
	        	logOff();
	        	break;	
	        case R.id.call_latitude:
	        	Uri myUri = Uri.parse("latitude://latitude/friends/location/");
		        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, myUri);
		        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		        startActivity(intent);
		        break;
	        case R.id.quick_dialog:
	        	showQuickDialog(this);
	        	break;
	        case R.id.location_Status:
	        	showLocationStatus(this);
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
		if( !isMyServiceRunning() )
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

	private class ConnectionUpdateReceiver extends BroadcastReceiver
	{
	    @Override
	    public void onReceive(Context context, Intent intent)
	    {
	        if (intent.getAction().equals(LocationService.BROADCAST_STATUS_CHANGED))
	        {
	        	TextView statusTextView = (TextView) findViewById(R.id.textViewStatus);
	        	LocationService myService = LocationService.getInstance();
	        	if (myService != null)
	        	{
	        		statusTextView.setText(myService.getStatus());
	        	}
	        	else
	        	{
	        		statusTextView.setText(LocationService.STATUS_NOT_LOGGED_IN);
	        	}
	        }
	    }
	}
	
	/**
	 * Shows a dialog to capture the quick action parameters
	 * @param context
	 */
	private void showQuickDialog(Context context)
	{
    	final Dialog dialog = new Dialog(context);

    	dialog.setContentView(R.layout.quick_action_dialog);
    	dialog.setTitle("Location Quick Settings");

    	// configure first spinner
		final Spinner spinnerInterval = (Spinner) dialog.findViewById(R.id.spinnerInterval);
		
		ArrayAdapter spinnerIntervalAdapter = ArrayAdapter.createFromResource(
	            this, R.array.quickIntervalLabels, android.R.layout.simple_spinner_item);
		spinnerIntervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerInterval.setAdapter(spinnerIntervalAdapter);

		// configure second spinner
		final Spinner spinnerAccuracy = (Spinner) dialog.findViewById(R.id.spinnerAccuracy);
		
		ArrayAdapter spinnerAccuracyAdapter = ArrayAdapter.createFromResource(
	            this, R.array.quickAccuracyLabels, android.R.layout.simple_spinner_item);
		spinnerAccuracyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerAccuracy.setAdapter(spinnerAccuracyAdapter);

		// configure third spinner
		final Spinner spinnerDuration = (Spinner) dialog.findViewById(R.id.spinnerDuration);
		
		ArrayAdapter spinnerDurationAdapter = ArrayAdapter.createFromResource(
	            this, R.array.quickDurationLabels, android.R.layout.simple_spinner_item);
		spinnerDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    
		spinnerDuration.setAdapter(spinnerDurationAdapter);
		
		Button buttonOk 	= (Button) dialog.findViewById(R.id.ButtonOk);
		Button buttonCancel = (Button) dialog.findViewById(R.id.ButtonCancel);
		Button buttonReset 	= (Button) dialog.findViewById(R.id.ButtonReset);
		
		// Check if we already have a qhick change running
		LocationService myService = LocationService.getInstance();
    	if (myService != null)
    	{
    		if (myService.isQuickChangeRunning())
    		{
    			// disable all except reset
    			spinnerAccuracy.setEnabled(false);
    			spinnerInterval.setEnabled(false);
    			spinnerDuration.setEnabled(false);
    			
    			buttonOk.setEnabled(false);
    			buttonCancel.setEnabled(true);
    			buttonReset.setEnabled(true);
    			buttonReset.setOnClickListener( new Button.OnClickListener()
    			 {
    			     @Override
    			     public void onClick(View v)
    			     {
    			    	 LocationService.getInstance().resetQuickChange();
    			         dialog.dismiss();
    			     }
    			 });
    			
    			// set selections
    			spinnerAccuracy.setSelection(m_iAccuracyIndex);
    			spinnerInterval.setSelection(m_iIntervalIndex);
    			spinnerDuration.setSelection(m_iDurationIndex);
    			
    		}
    		else
    		{
    			// disable reset
    			spinnerAccuracy.setEnabled(true);
    			spinnerInterval.setEnabled(true);
    			spinnerDuration.setEnabled(true);

    			buttonOk.setEnabled(true);
    			buttonCancel.setEnabled(true);
    			buttonReset.setEnabled(false);

    			// set selection from prefs
    	    	
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

    			spinnerAccuracy.setSelection(iAccuracy);
    			spinnerInterval.setSelection(iInterval);
    			spinnerDuration.setSelection(iDuration);

    			buttonOk.setOnClickListener( new Button.OnClickListener()
    			{
    				
					@Override
					public void onClick(View v)
					{
						LocationService.getInstance()
							.setQuickChange(
									spinnerInterval.getSelectedItemPosition(),
									spinnerAccuracy.getSelectedItemPosition(),
									spinnerDuration.getSelectedItemPosition());
						
						// save selection
						m_iIntervalIndex = spinnerInterval.getSelectedItemPosition();
						m_iAccuracyIndex = spinnerAccuracy.getSelectedItemPosition();
						m_iDurationIndex = spinnerDuration.getSelectedItemPosition();
						
						dialog.dismiss();
					}
				});
				buttonCancel.setOnClickListener(new Button.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						// do nothing
						dialog.dismiss();
					}
				});

    		}

    		dialog.show();
    	}
    	else
    	{
    		Toast.makeText(this, "Service is not started yet.", Toast.LENGTH_SHORT).show();
    	}
	}
	
	/**
	 * Shows a dialog with the current location info
	 * @param context
	 */
	private void showLocationStatus(Context context)
	{
    	Dialog dialog = new Dialog(context);

    	dialog.setContentView(R.layout.information_dialog);
    	dialog.setTitle("Details");

    	TextView title = (TextView) dialog.findViewById(R.id.title);
    	TextView text = (TextView) dialog.findViewById(R.id.text);
    	title.setText("Location status");
    	
    	
    	LocationService myService = LocationService.getInstance();
    	if (myService != null)
    	{
	    	String strText = "Location Provider: " + myService.getLocationProvider() + "\n";
	    	strText = strText + "Buffered locations: " + myService.getBufferSize() / 1000 + "\n";
	    	strText = strText + "Accuracy [m]: " + myService.getAccuracy() + "\n";
	    	strText = strText + "Interval [s]: " + myService.getInterval() / 1000;
	    	    	
	    	if (!myService.getCurrentLocation().equals(""))
	    	{
	    		strText = strText + "\n" + "Current Location: " +  myService.getCurrentLocation();
	    	}
	
	    	text.setText(strText);
    	
    	}
    	else
    	{
    		text.setText("The location service is currently unavailable");
    	}
    	dialog.show();
	}
	
    public void openURL( String inURL )
    {
        Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( inURL ) );

        startActivity( browse );
    }

}