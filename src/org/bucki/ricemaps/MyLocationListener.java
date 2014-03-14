package org.bucki.ricemaps;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MyLocationListener implements LocationListener {

	//private double latitude;
	//private double longitude;
	
	//public double getLat(){return latitude;}
	//public double getLng(){return longitude;}
	
	@Override
	public void onLocationChanged(Location loc){
		//latitude = loc.getLatitude();
		//longitude = loc.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider){}
	
	@Override
	public void onProviderEnabled(String provider){}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras){}
	

}