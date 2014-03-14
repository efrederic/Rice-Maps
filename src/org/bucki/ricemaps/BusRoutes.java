package org.bucki.ricemaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class BusRoutes extends AsyncTask<String, Void, JSONArray>{
	
	private InputStream inputStream = null;
	
	@Override
	protected JSONArray doInBackground(String... url){
		JSONArray jArray = null;
		try{
			HttpClient httpClient = new DefaultHttpClient();
			ArrayList<NameValuePair> param = new ArrayList<NameValuePair>();
	        HttpPost httpPost = new HttpPost(url[0]);
	        httpPost.setEntity(new UrlEncodedFormEntity(param));
	        HttpResponse httpResponse = httpClient.execute(httpPost);
	        HttpEntity httpEntity = httpResponse.getEntity();
	        
	        inputStream = httpEntity.getContent();
		}catch (Exception e){
			//Log.e("e",e.toString());
		}
		
		String result = "";
		
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder sBuilder = new StringBuilder();
			
            String line = null;
            while ((line = br.readLine()) != null) {
                sBuilder.append(line + "\n");
            }

            inputStream.close();
            
            JSONObject obj = new JSONObject(sBuilder.toString());
            jArray = obj.getJSONArray("d");
            
            for(int i=0; i<jArray.length(); i++){
            	JSONObject route = jArray.getJSONObject(i);
            	String color = route.getString("Color");
            	Bitmap bmp = getBitmapFromURL("http://bus.rice.edu/img/icons/bus-" + color + ".png");
            	bmp = Bitmap.createScaledBitmap(bmp, 50, 50, false);
            	MainActivity.busIcons.put(route.getInt("ID"), bmp);
            }
            
		}catch(Exception e){
			//Log.e("e",e.toString());
		}
		return jArray;
	}
	
	public static Bitmap getBitmapFromURL(String link) {
	    /*--- this method downloads an Image from the given URL, 
	     *  then decodes and returns a Bitmap object
	     ---*/
	    try {
	        URL url = new URL(link);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);

	        return myBitmap;

	    } catch (IOException e) {
	        e.printStackTrace();
	        Log.e("getBmpFromUrl error: ", e.getMessage().toString());
	        return null;
	    }
	}
}
