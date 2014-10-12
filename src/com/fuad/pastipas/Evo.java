package com.fuad.pastipas;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

public class Evo extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
	
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
	//private Location currLocation;
	
	private LocationClient mLocationClient;
	private Location mCurrentLocation;
	private LocationRequest mLocationRequest;
	private boolean mUpdatesRequested;
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5*60;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 600;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

	
    private String kota = "";
	private SensorManager mSensorManager;
	private ShakeEventListener mSensorListener;
	
	private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
//        Log.v("Pasti Pas", "Result Code "+resultCode);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
/*            Log.d("Location Updates",
                    "Google Play services is available.");*/
            return true;
        // Google Play services was not available for some reason.
        // resultCode holds the error code.
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    9000);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(),
                        "Location Updates");
            }
            return false;
        }
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.evo);
        
        dbHelper = new DatabaseHelper(this);
        //dbHelper.getReadableDatabase();
        
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
        
        
        
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
    
        mLocationRequest.setInterval(UPDATE_INTERVAL);
    
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mUpdatesRequested = false;

        
        mLocationClient = new LocationClient(this, this, this);
     
        /*LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.v("Pasti Pas","Continue to Location");
        LocationListener locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				Log.v("Pasti Pas", "Got Location: " + location.getLatitude() + "," + location.getLongitude());
				currLocation = location;
				coords = location.getLatitude() + "," + location.getLongitude();
				
				// Get Location
				if(whereAmI != null){
					whereAmI.cancel(true);
				}
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
        
        
        Criteria criteria = new Criteria();
        //criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider_name = locationManager.getBestProvider(criteria, true);
        Log.v("Pasti Pas",provider_name);
        currLocation = locationManager.getLastKnownLocation(provider_name);
        if(currLocation!=null){
	        Log.v("Pasti Pas",currLocation.toString());
			coords = currLocation.getLatitude() + "," + currLocation.getLongitude();
	        whereAmI = new WhereAmI();
	        whereAmI.execute(lv);
        }
//        Log.v("Pasti Pas","before location update");
//        locationManager.requestLocationUpdates(provider_name, 1000, 5000, locationListener);
//        Log.v("Pasti Pas","after location update");
        
        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)){
        	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5000, locationListener);
        }
        
        /*if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)){
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5000, locationListener);
        }*/
        
        // Detect Shake
        mSensorListener = new ShakeEventListener();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {
			@Override
			public void onShake() {
//				Log.v("Pasti Pas", "Shake detected..");
				
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
				
				whereAmI = new WhereAmI(Evo.this);
				whereAmI.execute(lv);
			}
        });
	}
	
	public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                       9000);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
    }
	
	
	private void changeStatusText(String s) {
		TextView tv = (TextView) findViewById(R.id.statusText);
		tv.setText(s);
	}
	
	private class ReverseGeo extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... c) {
			Geocoder geocoder = new Geocoder(getApplicationContext());
//			Log.v("Pasti Pas","Koordinat "+c[0]);
			String[] split= c[0].split(",");
			
			try{
				List<Address> listAddress = geocoder.getFromLocation(Double.valueOf(split[0]), Double.valueOf(split[1]), 1);
				return listAddress.get(0).getAddressLine(0)+" "+listAddress.get(0).getAddressLine(1)+" "+listAddress.get(0).getAddressLine(2);
		
			}catch(IOException ioe){
//				Log.v("Pasti Pas","IOE Exception 1"+ioe.getMessage());
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
		
		private Activity myActivity;
		
		public WhereAmI(Activity a){
			this.myActivity = a;
		}
		protected ListView doInBackground(ListView... l) {
			revGeo = new ReverseGeo();
			revGeo.execute(coords);
			
			updgradeDB();
			Geocoder geocoder = new Geocoder(getApplicationContext());
			String[] split= coords.split(",");
			try{
				List<Address> listAddress = geocoder.getFromLocation(Double.valueOf(split[0]), Double.valueOf(split[1]), 1);
//				Log.v("Pasti Pas",listAddress.get(0).toString());
				String address[] = listAddress.get(0).getAddressLine(2).split(",");
				kota = address[0];
				kota = kota.replace("City", "");
				kota = kota.replace("Kota", "");
				if(kota.trim().toLowerCase().startsWith("east")){
					kota = kota.trim().replace("East", "");
					kota+=" Timur";
				}else if(kota.trim().toLowerCase().startsWith("west")){
					kota = kota.trim().replace("West", "");
					kota+=" Barat";
				}else if(kota.trim().toLowerCase().startsWith("north")){
					kota = kota.trim().replace("North", "");
					kota+=" Utaran";
				}else if(kota.trim().toLowerCase().startsWith("south")){
					kota = kota.trim().replace("South", "");
					kota+=" Selatan";
				}else if(kota.trim().toLowerCase().startsWith("central")){
					kota = kota.trim().replace("Central", "");
					kota+=" Tengah";
					if(kota.trim().equals("Jakarta Tengah")){
						kota = "Jakarta Pusat";
					}
				}
				//kota = "Jakarta Pusat";
				String[] args = new String[1];
				args[0] = "%"+kota.trim()+"%";
				//Log.v("Pasti Pas","Kota = "+kota);
			    Cursor cursor = dbHelper.getReadableDatabase().
			    		 rawQuery("select _id, no_spbu , propinsi, kota, alamat, latitude, longitude FROM pastipas where kota like ?",args);
			    
				
			     TreeMap<Double,SearchResult> resultMap= new TreeMap<Double,SearchResult>();
			     Double i = 1.0;
//			     Log.v("Pasti Pas","Search Result = "+cursor.getCount());
			     while(cursor.moveToNext()){
			    	 /*double latitude = cursor.getDouble(5);
			    	 double longitude = cursor.getDouble(6);
			    	 float[] results = new float[3];
			    	 Location.distanceBetween(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),latitude, longitude, results);*/
			    	 
			    	 SearchResult s = new SearchResult();
			    	 s.setNo_spbu(cursor.getString(1));
			    	 s.setPropinsi(cursor.getString(2));
			    	 s.setKota(cursor.getString(3));
			    	 s.setAlamat(cursor.getString(4).replace("()",""));
			    	 //s.setDistance(Double.valueOf(results[0]));
			    	 
			    	 //resultMap.put(Double.valueOf(results[0]), s);
			    	 resultMap.put(i, s);
			    	 i=i+1;
			     }
			     listResult = new ArrayList<SearchResult>(resultMap.values());
			}catch(IOException ioe){
			    listResult = new ArrayList<SearchResult>();
				//Log.v("Pasti Pas","IOE Exception "+ioe.getMessage());
			}
		     return l[0];
		}
		
		private void updgradeDB(){
			//Log.v("Pasti Pas","Need Updgrade Db?");
			SharedPreferences sharedPref = myActivity.getPreferences(Context.MODE_PRIVATE);
			long last_upgrade = sharedPref.getLong("last_upgrade", 0);
			//Log.v("Pasti Pas","Last Upgrade = "+last_upgrade);
			//long last_upgrade = 0;
			long now = System.currentTimeMillis();
			
			if(now >= last_upgrade+(30l*24l*60l*60l*1000l)){
				//Log.v("Pasti Pas","Need Updgrade Db? Yes");
				dbHelper.getReadableDatabase().setVersion(1);
//				dbHelper.onUpgrade(dbHelper.getReadableDatabase(),1,2);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putLong("last_upgrade", now);
				editor.commit();
			}
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
		new WhereAmI(this).execute(lv);
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
		//	tv.setText(df.format(s.getDistance())+" km");
			tv.setText("");
			
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
	   mLocationClient.connect();
	}
	
	public void onStop() {
		if(mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */

	   mLocationClient.disconnect();
	  // mSensorManager.unregisterListener(mSensorListener);
	   //FlurryAgent.onEndSession(this);
	   super.onStop();
	}
	
	@Override
	protected void onResume() {
		mUpdatesRequested = true;
		super.onResume();
		/*mSensorManager.registerListener(mSensorListener,
		        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
		        SensorManager.SENSOR_DELAY_UI);
*/	}
	
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            this,
            9000);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), "Pasti Pas");
        }
    }
	
	public static class ErrorDialogFragment extends DialogFragment {
	    // Global field to contain the error dialog
	    private Dialog mDialog;
	    // Default constructor. Sets the dialog field to null
	    public ErrorDialogFragment() {
	        super();
	        mDialog = null;
	    }
	    // Set the dialog to display
	    public void setDialog(Dialog dialog) {
	        mDialog = dialog;
	    }
	    // Return a Dialog to the DialogFragment.
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        return mDialog;
	    }
	}

	@Override
    public void onLocationChanged(Location location) {
		
//		Log.v("Pasti Pas", "Got Location: " + location.getLatitude() + "," + location.getLongitude());
		mCurrentLocation = location;
		coords = location.getLatitude() + "," + location.getLongitude();
		
		if(!servicesConnected()){
			return;
		}
		// Get Location
		if(whereAmI != null){
			whereAmI.cancel(true);
		}
        whereAmI = new WhereAmI(Evo.this);
        whereAmI.execute(lv);
        
        /*String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();*/
    }


	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
        if (mUpdatesRequested) {
        	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }

	}


	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
}