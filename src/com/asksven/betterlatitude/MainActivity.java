package com.asksven.betterlatitude;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.test.PerformanceTestCase;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.asksven.android.common.utils.DataStorage;
import com.asksven.betterlatitude.credentials.CredentialStore;
import com.asksven.betterlatitude.credentials.SharedPreferencesCredentialStore;
import com.asksven.betterlatitude.R;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.latitude.Latitude;
import com.google.api.services.latitude.model.LatitudeCurrentlocationResourceJson;

public class MainActivity extends Activity implements LocationListener
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "MainActivity";

	/**
	 * Constants for menu items
	 */
	private final int MENU_ITEM_0 = 0;
	private final int MENU_ITEM_1 = 1;
	private final int MENU_ITEM_2 = 2;
	private final int MENU_ITEM_3 = 3;
	
	private SharedPreferences prefs;
	private TextView textViewLatitude;
	private TextView textViewLoc;

	private double m_dLat = -1;
	private double m_dLong = -1;
	
	boolean m_bIsStarted;
	/** 
	 * The location provider delivers the loc info
	 */
	private LocationManager m_LocationManager;
	String m_strLocProvider;

	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Button launchOauth = (Button) findViewById(R.id.btn_launch_oauth);
		Button clearCredentials = (Button) findViewById(R.id.btn_clear_credentials);

		this.textViewLatitude = (TextView) findViewById(R.id.response_code);
		this.textViewLoc = (TextView) findViewById(R.id.loc_info);
		// Launch the OAuth flow to get an access token required to do authorized API calls.
		// When the OAuth flow finishes, we redirect to this Activity to perform the API call.
		launchOauth.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				startActivity(new Intent().setClass(v.getContext(),OAuthAccessTokenActivity.class));
			}
		});

		// Clearing the credentials and performing an API call to see the unauthorized message.
		clearCredentials.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				clearCredentials();
				getLocationApiCall();
			}

		});
		
		startService();
		// Get the location manager
		m_LocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the locatioin provider -> use
		// default
		Criteria criteria = new Criteria();
		m_strLocProvider = m_LocationManager.getBestProvider(criteria, false);
		Location location = m_LocationManager.getLastKnownLocation(m_strLocProvider);
		
		// Define the exactitude and update interval
		m_LocationManager.requestLocationUpdates(m_strLocProvider, 400, 1, this);
		// Performs an authorized API call.
		getLocationApiCall();

	}

	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		// max every 5 minutes or when moved by 1 Km
		m_LocationManager.requestLocationUpdates(m_strLocProvider, LocationService.LOC_INTERVAL, LocationService.LOC_INTERVAL, this);
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
		m_LocationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		m_dLat = location.getLatitude();
		m_dLong = location.getLongitude();
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
	/** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	menu.add(0, MENU_ITEM_0, 0, "Update Location");
    	menu.add(0, MENU_ITEM_1, 0, "Request Location from Latitude");
    	menu.add(0, MENU_ITEM_2, 0, "Show Location from Cell");
    	
    	menu.add(0, MENU_ITEM_3, 0, "Preferences");
    	
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
	        case MENU_ITEM_0: // update location  
	        	setLocationApiCall(); //performApiCall();
	        	break;	
	        case MENU_ITEM_1: // retrieve location from Latitude  
	        	getLocationApiCall(); //performApiCall();
	        	break;
	        case MENU_ITEM_2: // retrieve location from Cell  
	        	showLocation(); // getLocationApiCall(); //performApiCall();
	        	break;	
	        case MENU_ITEM_3: // prefs  
	        	Intent intentPrefs = new Intent(this, PreferencesActivity.class);
	            this.startActivity(intentPrefs);
	        	break;	

        }
        
        return true;
    }
    

    /**
	 * Clears our credentials (token and token secret) from the shared preferences.
	 * We also setup the authorizer (without the token).
	 * After this, no more authorized API calls will be possible.
	 */
    private void clearCredentials()
    {
    	new SharedPreferencesCredentialStore(prefs).clearCredentials();
    }
	
    /**
     * Performs an authorized API call to retrieve the current location.
     */
	private void getLocationApiCall()
	{
		try
		{
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
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
		    
			LatitudeCurrentlocationResourceJson currentLocation = latitude.currentLocation.get().execute();
			String locationAsString = convertLocationToString(currentLocation);
			textViewLatitude.setText(locationAsString);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			textViewLatitude.setText("Error occured : " + ex.getMessage());
		}
	}

    /**
     * Performs an authorized API call to retrieve the current location.
     */
	private void setLocationApiCall()
	{
		try
		{
			Logger.i(TAG, " Service Updating Latitude with position Lat: "
					+ String.valueOf(m_dLat)
					+ " Long: " + String.valueOf(m_dLong));
			
			JsonFactory jsonFactory = new JacksonFactory();
			HttpTransport transport = new NetHttpTransport();
			
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
			String locationAsString = convertLocationToString(insertedLocation);
			textViewLatitude.setText(locationAsString);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			textViewLatitude.setText("Error occured : " + ex.getMessage());
		}
	}

	public void showLocation()
	{
		if (textViewLoc != null)
		{
			textViewLoc.setText("Lat: " + String.valueOf(m_dLat) + " Long: " + String.valueOf(m_dLong));
		}
	}
	
	private String convertLocationToString(
			LatitudeCurrentlocationResourceJson currentLocation)
	{
		String timestampMs = (String) currentLocation.get("timestampMs");
		DateFormat df= new  SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date d = new Date(Long.valueOf(timestampMs));
		String locationAsString = "Current location : " + currentLocation.get("latitude") + " - " + currentLocation.get("longitude") + " at " + df.format(d);
		return locationAsString;
	}
	
	/** 
     * Starts the service 
     */
	private void startService()
	{
		if( m_bIsStarted )
		{
			Toast.makeText(this, "Service already started", Toast.LENGTH_SHORT).show();
		}
		else
		{
			Intent i = new Intent();
			i.setClassName( "com.asksven.betterlatitude", "com.asksven.betterlatitude.LocationService" );
			startService( i );
			Log.i(getClass().getSimpleName(), "startService()");
			m_bIsStarted = true;
		}
	}

}