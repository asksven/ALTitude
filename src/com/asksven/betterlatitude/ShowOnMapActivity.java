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
        

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import com.asksven.betterlatitude.utils.Configuration;
import com.asksven.betterlatitude.utils.Logger;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

public class ShowOnMapActivity extends MapActivity implements LocationListener
{

	private MapController m_mapController;
	private MapView m_mapView;
	private LocationManager m_locationManager;
	private PositionOverlay m_friendsOverlay;
	private boolean m_bShowSatLayer = false;
	/**
	 * The logging TAG
	 */
	private static final String TAG = "ShowOnMapActivity";

	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		
		if (isDebugBuild(this))
		{
			setContentView(R.layout.map_debug);
		}
		else
		{
			setContentView(R.layout.map_release);
		}
		
		
		// detect free/full version and enable/disable ads
		if (!Configuration.isFullVersion(this))
		{
			AdView adView = (AdView)this.findViewById(R.id.adView);
		    adView.loadAd(new AdRequest());
		}
				
		// create a map view
		RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.mainlayout);
		m_mapView = (MapView) findViewById(R.id.mapview);
		m_mapView.setBuiltInZoomControls(true);
		m_mapView.setEnabled(true);
		
        Drawable drawable = this.getResources().getDrawable(R.drawable.icon);
        
        List<Overlay> mapOverlays = m_mapView.getOverlays();
        
        // delete any existing overlay
        mapOverlays.clear();
        m_mapView.postInvalidate();
        
		m_friendsOverlay = new PositionOverlay(drawable, this);
		mapOverlays.add(m_friendsOverlay);

		m_mapController = m_mapView.getController();
		m_mapController.setZoom(14); // Zoom 1 is world view
		
		// retrieve prefs for creating the LocationManager
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String strLocProvider 		= prefs.getString("map_loc_provider", "1");
    	String strMapUpdateInterval = prefs.getString("map_update_interval", "0");
    	String strMapUpdateAccuracy = prefs.getString("map_map_update_accuracy", "0");
    	
    	
    	int iLocProvider 		= 1;
    	int iMapUpdateInterval 	= 0;
    	int iMapUpdateAccuracy 	= 0;
    	try
    	{
    		iLocProvider = Integer.valueOf(strLocProvider);
    		iMapUpdateInterval = Integer.valueOf(strMapUpdateInterval);
    		iMapUpdateAccuracy = Integer.valueOf(strMapUpdateAccuracy);		
    	}
    	catch (Exception e)
    	{
    		// do noting, defaults are set
    	}
    	
    	// whatever Prefs say, the free version does not give any choice
    	if (!Configuration.isFullVersion(this))
		{
    		iLocProvider 		= 1;
        	iMapUpdateInterval 	= 0;
        	iMapUpdateAccuracy 	= 0;
        	
		}
    	
    	m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	switch (iLocProvider)
    	{
    		case 0: // passive
    			m_locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 
    					iMapUpdateInterval, iMapUpdateAccuracy, this);
    			break;
    		case 1:	// cell
    			m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
    					iMapUpdateInterval, iMapUpdateAccuracy, this);
    			break;
    		case 2:	// gps
    			m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
    					iMapUpdateInterval, iMapUpdateAccuracy, this);
    			break;
    	}
    	Logger.i(TAG, "LocationMAnager was set: type=" + iLocProvider
    			+ " interval=" + iMapUpdateInterval
    			+ " accuracy=" + iMapUpdateAccuracy);
	}
	
	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause()
	{
		super.onPause();
		m_locationManager.removeUpdates(this);
		Logger.i(TAG, "Activity paused, removing location listener");
	}


	@Override
	
	protected boolean isRouteDisplayed()
	{
		return false;
	}

	/** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mapmenu, menu);
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
	        case R.id.toggle_sat: 
	        	m_bShowSatLayer = !m_bShowSatLayer;
	        	this.showSatellite(m_bShowSatLayer);
	        	break;	

        }  
        return false;  
    }    

	private void showSatellite(boolean bShow)
	{
		m_mapView.setSatellite(bShow);
		m_mapView.invalidate();
	}

	@Override
	public void onLocationChanged(Location location)
	{
		int lat = (int) (location.getLatitude() * 1E6);
		int lng = (int) (location.getLongitude() * 1E6);
		GeoPoint point = new GeoPoint(lat, lng);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	boolean bCenterMap = prefs.getBoolean("center_map", false);
    	
    	// depending on preferences either animate or center map on new location
    	if (!bCenterMap)
    	{
    		m_mapController.animateTo(point);
    	}
    	else
    	{
    		m_mapController.setCenter(point);
    	}
		

		OverlayItem overlayitem = new OverlayItem(point, "Me", "here");
		
		m_friendsOverlay.clear();
		m_friendsOverlay.addOverlay(overlayitem);
		m_mapView.invalidate();
	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}
	
	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	// Checks if this apk was built using the debug certificate
	// Used e.g. for Google Maps API key determination
	// from: 
	// http://whereblogger.klaki.net/2009/10/choosing-android-maps-api-key-at-run.html
	// and
	// http://stackoverflow.com/questions/6122401/android-compare-signature-of-current-package-with-debug-keystore
	public Boolean isDebugBuild(Context context)
	{
		boolean bIsDebugBuild = true;
		
		try
		{
			
			Signature raw =
				context.getPackageManager()
				.getPackageInfo(
						context.getPackageName()
						, PackageManager.GET_SIGNATURES).signatures[0];
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert =
				(X509Certificate) cf.generateCertificate(
						new ByteArrayInputStream(raw.toByteArray()));
			bIsDebugBuild = cert.getSubjectX500Principal().equals(DEBUG_DN);
		}
		catch (Exception ex)
		{
			Logger.e(TAG, "An Exception occured");
			ex.printStackTrace();
		}
	    return bIsDebugBuild;
	}
}
	