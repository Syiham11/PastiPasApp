package com.fuad.pastipas;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME="db_pastipas";
	
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 3);
		Log.v("Pasti Pas","Database Helper start");
	}
	
	
	@Override
	public void onCreate(SQLiteDatabase db) {	
		Log.v("Pasti Pas","Database Helper created");
		db.execSQL("CREATE TABLE pastipas (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"			no_spbu TEXT, propinsi TEXT, kota TEXT, alamat TEXT, latitude REAL, longitude REAL);");
	
		HttpClient httpclient = new DefaultHttpClient();
	    HttpGet request = new HttpGet("https://spreadsheets.google.com/feeds/list/1y2_8huowHbzWB3Ixf4gT08V5oqvdiZE8C_mnh2cZECs/od6/public/full?alt=json");
	    ResponseHandler<String> handler = new BasicResponseHandler();
	    try {
	    	String result = httpclient.execute(request, handler);
	    	//Log.v("Pasti Pas","result http:"+result);
	        JSONObject jsonObj = new JSONObject(result);
	        JSONObject feedJson = jsonObj.getJSONObject("feed");
	        JSONArray json_arr = feedJson.getJSONArray("entry");
	        for(int i =0;i<json_arr.length();i++){
	        	JSONObject o = json_arr.getJSONObject(i);
	            ContentValues cv = new ContentValues();
	            cv.put("no_spbu", o.getJSONObject("gsx$spbu").getString("$t"));
	            cv.put("propinsi",o.getJSONObject("gsx$propinsi").getString("$t"));
	            cv.put("alamat", o.getJSONObject("gsx$alamat").getString("$t"));
	            cv.put("kota", o.getJSONObject("gsx$kota").getString("$t"));
	            if(!o.getJSONObject("gsx$latitude").getString("$t").trim().equals(""))
	            	cv.put("latitude",o.getJSONObject("gsx$latitude").getDouble("$t"));
	            if(!o.getJSONObject("gsx$longitude").getString("$t").trim().equals(""))
	            	cv.put("longitude", o.getJSONObject("gsx$longitude").getDouble("$t"));
	            db.insert("pastipas","no_spbu", cv);
	            //Log.v("Pasti Pas",o.toString());
	        }
	    } catch (ClientProtocolException e) {
	    	Log.v("Pasti Pas","Client Protocol Exception "+e.getMessage());
	    } catch (IOException e) {
	    	Log.v("Pasti Pas","IOException "+e.getMessage());
	    } catch (JSONException e) {
	    	Log.v("Pasti Pas","JSON Exception "+e.getMessage());
	    }
	    httpclient.getConnectionManager().shutdown();
	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
			db.execSQL("DROP TABLE IF EXISTS pastipas");
	        onCreate(db);
	}
}