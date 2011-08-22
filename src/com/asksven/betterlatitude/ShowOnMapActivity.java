package com.asksven.betterlatitude;
        

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.view.Window;
import android.widget.RelativeLayout;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import java.util.List;

public class ShowOnMapActivity extends MapActivity implements LocationListener
{

	private MapController m_mapController;
	private MapView m_mapView;
	private LocationManager m_locationManager;
	private PositionOverlay m_friendsOverlay;

	public void onCreate(Bundle bundle)
	{
		super.onCreate(bundle);
		setContentView(R.layout.map); // bind the layout to the activity

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
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
		m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, //PASSIVE_PROVIDER, 
				60, 500, this);

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
}
	