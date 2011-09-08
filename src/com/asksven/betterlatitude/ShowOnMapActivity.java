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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.view.Window;
import android.widget.RelativeLayout;

import com.asksven.betterlatitude.utils.Logger;
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
//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// create a map view
		RelativeLayout linearLayout = (RelativeLayout) findViewById(R.id.mainlayout);
		m_mapView = (MapView) findViewById(R.id.mapview);
		m_mapView.setBuiltInZoomControls(true);
		m_mapView.setEnabled(true);
		
		List<Overlay> mapOverlays = m_mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.icon);

		m_friendsOverlay = new PositionOverlay(drawable, this);
		mapOverlays.add(m_friendsOverlay);

		m_mapController = m_mapView.getController();
		m_mapController.setZoom(14); // Zoon 1 is world view
		
		m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		m_locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 
				60, 500, this);

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



	@Override
	public void onLocationChanged(Location location)
	{
		int lat = (int) (location.getLatitude() * 1E6);
		int lng = (int) (location.getLongitude() * 1E6);
		GeoPoint point = new GeoPoint(lat, lng);
		m_mapController.animateTo(point);

		

		OverlayItem overlayitem = new OverlayItem(point, "Me", "here");
		
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
	