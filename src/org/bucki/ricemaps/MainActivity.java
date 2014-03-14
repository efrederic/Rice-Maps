package org.bucki.ricemaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements OnItemClickListener, OnMarkerClickListener, OnMapClickListener{
	
	static GoogleMap mMap;
	private String[] placeNames;
	private AutoCompleteTextView textView;
	private Marker marker;
	private LocationManager locManager;
	private LocationListener locListener;
	private Location location;
	private boolean isRunning;
	
	HashMap<String, LatLng> allLocs = new HashMap<String, LatLng>();
	
	static JSONArray routes = null;
	static Map<Integer, Bitmap> busIcons = new HashMap<Integer, Bitmap>();
	static ArrayList<Marker> busMarkers = new ArrayList<Marker>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mMap.setMyLocationEnabled(true);
		marker = mMap.addMarker(new MarkerOptions().position(new LatLng(0,0)));
		mMap.setOnMarkerClickListener((OnMarkerClickListener) this);
		mMap.setOnMapClickListener(this);
		BuildingMap.buildMap();
		
		locListener = new MyLocationListener();
		locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		allLocs.putAll(BuildingMap.buildings);
		allLocs.putAll(BuildingMap.classes);
		placeNames = Arrays.copyOf(allLocs.keySet().toArray(), allLocs.keySet().toArray().length, String[].class);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, placeNames);
		textView = (AutoCompleteTextView)findViewById(R.id.search);
		textView.setAdapter(adapter);
		textView.setOnItemClickListener(this);
		
		AsyncTask<String, Void, JSONArray> busRoutes = new BusRoutes();
		busRoutes.execute("http://bus.rice.edu/json/routes.php");
		try{
			routes = busRoutes.get(); 
		}
		catch(Exception e){
			
		}
		isRunning = true;
		callAsyncTask();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);    
		return true;
	}
	
	@Override
	public void onStop(){
		locManager.removeUpdates(locListener);
		super.onStop();
	}
	
	@Override
	public void onPause(){
		isRunning = false;
		super.onPause();
	}
	
	@Override
	public void onResume(){
		isRunning = true;
		super.onResume();
	}
	
	public double deg_to_rad (double deg){
		return Math.PI/180* deg;
	}

	public String distance_to_time(LatLng p1, LatLng p2){
	  double radius = 3959; // in miles
	  double diff_lat = deg_to_rad(p2.latitude - p1.latitude);
	  double diff_long = deg_to_rad(p2.longitude - p1.longitude);

	  double insqrt = Math.sin(diff_lat/2) * Math.sin(diff_lat/2) +
	          Math.cos(deg_to_rad(p1.latitude)) * Math.cos(deg_to_rad(p2.latitude)) * Math.sin(diff_long/2) * Math.sin(diff_long/2);
	  double dist = 2 * radius * Math.asin(Math.sqrt(insqrt));
	  double num_mins = dist*20; // avg walk pace is 20 mins per mile
	  return "Distance: " + Math.floor(dist * 100) / 100 + " miles, Time: " + (int)num_mins + " min";
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id){
		String text = textView.getText().toString();
		
		LatLng loc = (LatLng)allLocs.get(text);
		if(loc != null){
			mMap.animateCamera(CameraUpdateFactory.newLatLng(loc));
			marker.setPosition(loc);
			marker.setTitle(text);
			marker.showInfoWindow();
			
			try{
				locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 2, locListener);
				location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
				double lat = location.getLatitude();
				double lng = location.getLongitude();
				LatLng myLoc = new LatLng(lat,lng);
			
				Context context = getApplicationContext();
				CharSequence notice = distance_to_time(myLoc, loc);
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(context, notice, duration);
				toast.show();
			}catch(Exception e){
				
			}
		}else if(BuildingMap.classes.containsKey(text)){
			DialogFragment newFragment = new TBADialog();
		    newFragment.show(getFragmentManager(), "Oops!");
		}else{
			DialogFragment newFragment = new LocNotFound();
		    newFragment.show(getFragmentManager(), "Oops!");
		}
		InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}
	
	@Override
	public boolean onMarkerClick(Marker marker){
		return false;
	}
	
	public void callAsyncTask() {
	    final Handler handler = new Handler();
	    Timer timer = new Timer();
	    TimerTask doAsyncTask = new TimerTask() {       
	        @Override
	        public void run() {
	        	if(isRunning){
		            handler.post(new Runnable() {
		                public void run() {       
		                    try {
		                        new BusLocator().execute("http://bus.rice.edu/json/buses.php");
		                    } catch (Exception e) {
		                        // TODO Auto-generated catch block
		                    }
		                }
		            });
		        }
	        }
	    };
	    timer.schedule(doAsyncTask, 0, 2000);
	}
	
	@Override
	public void onMapClick(LatLng point){
		double clickX = point.longitude;
		double clickY = point.latitude;
		String shortestKey = "";
		double shortestDist = 100; //Big number = infinity
		
		Iterator it = BuildingMap.buildings.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, LatLng> entry = (Map.Entry)it.next();
	        double x = entry.getValue().longitude;
	        double y = entry.getValue().latitude;
	        
	        double dist = Math.sqrt(Math.pow(clickX - x, 2) + Math.pow(clickY - y, 2));
	        if(dist < shortestDist){
	        	shortestKey = entry.getKey();
	        	shortestDist = dist;
	        }
	    }
	    
	    LatLng loc = (LatLng)BuildingMap.buildings.get(shortestKey);	    
	    mMap.animateCamera(CameraUpdateFactory.newLatLng(loc));
		marker.setPosition(loc);
		marker.setTitle(shortestKey);
		marker.showInfoWindow();
	}
	
}
