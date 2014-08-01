package com.fuad.pastipas;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fuad.beans.SearchResult;

public class Evo extends Activity {
	
	//private static List<SearchResult> searchResult = new ArrayList<SearchResult>();
	//private List<ListData> l = new ArrayList<ListData>();
	
	private static String coords = "0,0";
	//private static String json = "";
	private DecimalFormat df;
	
	private static ListView lv;
	private List<SearchResult> listResult;
	
	private WhereAmI whereAmI;
	private ReverseGeo revGeo;
	private DatabaseHelper dbHelper;
	//private DownloadPano downloadPano;
	private Location currLocation;
	
	private SensorManager mSensorManager;
	private ShakeEventListener mSensorListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.evo);
        
        dbHelper = new DatabaseHelper(this);
        dbHelper.getReadableDatabase();
        
        df = new DecimalFormat("0.00");
     
        
        lv = (ListView) findViewById(R.id.lv);
        lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
			//	FlurryAgent.onEvent("Click on Business");
				//startActivity(new Intent("android.intent.action.VIEW", Uri.parse(searchResult.get(arg2).getBusinessMobileWebURL())));
			}
        });
     
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				Log.v("Pasti Pas", "Got Location: " + location.getLatitude() + "," + location.getLongitude());
				currLocation = location;
				coords = location.getLatitude() + "," + location.getLongitude();
				
				// Get Location
		        whereAmI = new WhereAmI();
		        whereAmI.execute(lv);
			}

			@Override
			public void onProviderDisabled(String provider) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {}
        	
        };
        
        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)){
        	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5000, locationListener);
        }
        
        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)){
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5000, locationListener);
        }
        
        // Detect Shake
        mSensorListener = new ShakeEventListener();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {
			@Override
			public void onShake() {
				Log.v("Pasti Pas", "Shake detected..");
				
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(300);
				
				if(whereAmI != null)
					whereAmI.cancel(true);
				
				if(revGeo != null)
					revGeo.cancel(true);
				
//				if(downloadPano != null)
//					downloadPano.cancel(true);
				
				ListView lv = (ListView) findViewById(R.id.lv);
				lv.setVisibility(View.GONE);
				
				RelativeLayout rl = (RelativeLayout) findViewById(R.id.loadingView);
				rl.setVisibility(View.VISIBLE);
				
				whereAmI = new WhereAmI();
				whereAmI.execute(lv);
			}
        });
	}
	
	
	private void changeStatusText(String s) {
		TextView tv = (TextView) findViewById(R.id.statusText);
		tv.setText(s);
	}
	
	private class ReverseGeo extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... c) {
			Geocoder geocoder = new Geocoder(getApplicationContext());
			Log.v("Pasti Pas","Koordinat "+c[0]);
			String[] split= c[0].split(",");
			
			try{
				List<Address> listAddress = geocoder.getFromLocation(Double.valueOf(split[0]), Double.valueOf(split[1]), 1);
				Log.v("Pasti Pas",listAddress.get(0).toString());
				return listAddress.get(0).getAddressLine(0)+" "+listAddress.get(0).getAddressLine(1)+" "+listAddress.get(0).getAddressLine(2);
		
			}catch(IOException ioe){
				Log.v("Pasti Pas","IOE Exception "+ioe.getMessage());
				return "";
			}
		}
		
		@Override
		protected void onPostExecute(String s) {
			if(s.compareTo("") != 0)
				changeStatusText("Sekitar "+s);
			else
				changeStatusText("Tidak dapat menemukan lokasi anda..");
		}
	}
	
	private class WhereAmI extends AsyncTask<ListView, Void, ListView> {
		
		protected ListView doInBackground(ListView... l) {
			revGeo = new ReverseGeo();
			revGeo.execute(coords);
			
		    Cursor cursor = dbHelper.getReadableDatabase().
		    		 rawQuery("select _id, no_spbu , propinsi, kota, alamat, latitude, longitude FROM pastipas",new String[]{});
			
		     TreeMap<Double,SearchResult> resultMap= new TreeMap<Double,SearchResult>();
		     while(cursor.moveToNext()){
		    	 double latitude = cursor.getDouble(5);
		    	 double longitude = cursor.getDouble(6);
		    	 float[] results = new float[3];
		    	 Location.distanceBetween(currLocation.getLatitude(),currLocation.getLongitude(),latitude, longitude, results);
		    	 
		    	 SearchResult s = new SearchResult();
		    	 s.setNo_spbu(cursor.getString(1));
		    	 s.setPropinsi(cursor.getString(2));
		    	 s.setKota(cursor.getString(3));
		    	 s.setAlamat(cursor.getString(4));
		    	 s.setDistance(Double.valueOf(results[0]));
		    	 
		    	 resultMap.put(Double.valueOf(results[0]), s);
		     }
		     listResult = new ArrayList<SearchResult>(resultMap.values());
		     return l[0];
		}
		
		@Override
		protected void onPostExecute(ListView lv) {
			loadListView(lv);
		}
	}
	
	private void loadListView(ListView lv) {
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.loadingView);
		rl.setVisibility(View.GONE);
		lv.setVisibility(View.VISIBLE);
		lv.setAdapter(new PastipasAdapter());
	}
	
	public void refreshLocation(View v) {
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.loadingView);
		ListView lv = (ListView) findViewById(R.id.lv);
		lv.setVisibility(View.GONE);
		rl.setVisibility(View.VISIBLE);
		new WhereAmI().execute(lv);
	}
	
	private class ListData {
		private int position;
		private View v;
		private Bitmap bitmap;
		
		public ListData(int position, View v) {
			setPosition(position);
			setView(v);
		}
		
		public void setPosition(int position) {
			this.position = position;
		}
		public int getPosition() {
			return position;
		}
		public void setView(View v) {
			this.v = v;
		}
		public View getView() {
			return v;
		}

		public void setBitmap(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		public Bitmap getBitmap() {
			return bitmap;
		}
	}
	
	private class PastipasAdapter extends BaseAdapter {
		private ImageManager imgMan;

		public int getCount() {
			return listResult.size();
		}

		public Object getItem(int arg0) {
			return null;
		}

		public long getItemId(int arg0) {
			return arg0;
		}
		
		/*private class DownloadImage extends AsyncTask<ListData, Void, ListData> {
			private ImageView img;
			
			@Override
			protected ListData doInBackground(ListData... l) {
				int pos = l[0].getPosition();
				View v = l[0].getView();
				Bitmap ret;
				
				try {
					String imgStr = searchResult.get(pos).getBusinessPhoto();
					Log.v("JAJAN", "Downloading image " + imgStr);
					imgMan = new ImageManager(imgStr);
					
					img = (ImageView) v.findViewById(R.id.bizLogo);
					
					if(imgMan.fileExists()) {
						ret = imgMan.getImage();
					} else {
						ret = imgMan.saveImage();
					}
					
					if(ret == null) {
						return null;
					}
					
					ListData ll = new ListData(pos, v);
					ll.setBitmap(ret);
					
					return ll;
				} catch(Exception e) {
					//e.printStackTrace();
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(ListData ret) {
				if(ret != null && ret.getBitmap() != null) {
					img.setImageBitmap(ret.getBitmap());
					if(l.get(ret.getPosition()).getBitmap() == null) {
						l.get(ret.getPosition()).setBitmap(ret.getBitmap());
					}
				}
			}
			
		}*/

		public View getView(int position, View v, ViewGroup vg) {
			LayoutInflater inflater = getLayoutInflater();
			
			v = inflater.inflate((position % 2 == 0) ? R.layout.row_even : R.layout.row_odd, vg, false);
			
			final SearchResult s = listResult.get(position);
			
			TextView tv = (TextView) v.findViewById(R.id.bizName);
			tv.setText(Html.fromHtml(s.getNo_spbu()).toString());
			
			tv = (TextView) v.findViewById(R.id.bizAddr);
			tv.setText(s.getAlamat());
			
			tv = (TextView) v.findViewById(R.id.bizReviewCount);
			tv.setText(s.getKota()+" "+s.getPropinsi());
			
			tv = (TextView) v.findViewById(R.id.bizReview);
			tv.setText(df.format(s.getDistance())+" km");
			
			/*
			// Lazy load images
			try {
				ListData tmp = l.get(position);
				
				ImageView img = (ImageView) v.findViewById(R.id.bizLogo);
				img.setImageBitmap(tmp.getBitmap());
			} catch(IndexOutOfBoundsException e) {
				ListData lst = new ListData(position, v);
				l.add(lst);
				new DownloadImage().execute(l.get(position));
			}
			
			// Urspot or 360?
			if(s.isPanoramic()) {
				RelativeLayout b = (RelativeLayout) v.findViewById(R.id.badges);
				b.setVisibility(View.VISIBLE);
				b.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						downloadPano = new DownloadPano();
						downloadPano.execute(s.getBusinessUri());
					}
				});
				
				if(s.isPanoramic()) {
					tv = (TextView) v.findViewById(R.id.isPanoramic);
					tv.setVisibility(View.VISIBLE);
				}
				
				b = (RelativeLayout) v.findViewById(R.id.alphaDiv);
				b.setVisibility(View.VISIBLE);
			}*/
			
			return v;
		}
		
	}
	
	/*private class DownloadPano extends AsyncTask<String, Void, String> {
		private ProgressDialog dlg;
		protected final String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
		private Bitmap bm;
		
		@Override
		protected void onPreExecute() {
			dlg = new ProgressDialog(Evo.this);
			dlg.setTitle(R.string.app_name);
			dlg.setIcon(R.drawable.icon);
			dlg.setMessage("Downloading..");
			dlg.show();
		}
		
		@Override
		protected String doInBackground(String... s) {
			String ret = "";
			
			OAUTHnesia o = new OAUTHnesia(Evo.CONSUMER_KEY, Evo.CONSUMER_SECRET, OAUTHnesia.OAUTH_SAFE_ENCODE);
			try {
				String res = o.oAuth("get/pano_images", "", "business_uri="+s[0]);
				
				JSONArray pano = new JSONObject(res).getJSONArray("pano_images").getJSONArray(0);
				for(int i=0, max=pano.length(); i<max; i++) {
					String name = pano.getString(i).substring(pano.getString(i).lastIndexOf("/") + 1);
					
					ret += SaveImage(pano.getString(i), name);
					if(i < (max-1))
						ret += ",";
				}
			} catch (Exception e) {}
			
			return ret;
		}
		
		@Override
		protected void onPostExecute(String f) {
			dlg.dismiss();
			
			if(f.compareTo("") != 0) {
				Intent i = new Intent(Evo.this, Panoramic.class);
				i.putExtra("filePath", f);
				startActivity(i);
			} else {
				Toast.makeText(Evo.this, "Gagal memuat Panoramic..", Toast.LENGTH_LONG).show();
			}
		}
		
		private Bitmap LoadImage(String URL) {
			Bitmap bitmap = null;
			InputStream in = null;
			BitmapFactory.Options bmOptions;
			bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = 1;
			try {
				in = OpenHttpConnection(URL);
				bitmap = BitmapFactory.decodeStream(in, null, bmOptions);
				in.close();
			} catch (Exception e1) {
			}
			return bitmap;
		}
		
		private InputStream OpenHttpConnection(String strURL) throws IOException {
			InputStream inputStream = null;
			URL url = new URL(strURL);
			URLConnection conn = url.openConnection();

			try {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				httpConn.setRequestMethod("GET");
				httpConn.connect();

				if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
					inputStream = httpConn.getInputStream();
				}
			} catch (Exception ex) {
			}
			return inputStream;
		}
		
		private String SaveImage(String image, String name) {
			OutputStream outStream = null;
			String path = extStorageDirectory + "/jajan/";
			File t = new File(path);
			t.mkdirs();
			
			if (image.length() > 0) {
				File file = new File(path, name);
				if (file.exists()) {
					Log.i("file exist", "ada");
				} else {
					//file.mkdirs();
					bm = LoadImage(image);
					File f = new File(path);
					f.mkdirs();
					f = new File(path, name);					
					try {
						outStream = new FileOutputStream(f);
						bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
						outStream.flush();
						outStream.close();
					} catch (FileNotFoundException e1) {
						Log.e("ImageManager", "Giving up saving image to SD Card..");
					} catch (IOException e2) {
						Log.e("ImageManager", "Giving up saving image to SD Card..");
					}
				}
			}
			
			return path + "/" + name;
		}
		
	}*/
	
	public void onStart() {
	   super.onStart();
	   //FlurryAgent.onStartSession(this, FLURRY_API_KEY);
	}
	
	public void onStop() {
	   mSensorManager.unregisterListener(mSensorListener);
	   //FlurryAgent.onEndSession(this);
	   super.onStop();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(mSensorListener,
		        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
		        SensorManager.SENSOR_DELAY_UI);
	}
	
}
