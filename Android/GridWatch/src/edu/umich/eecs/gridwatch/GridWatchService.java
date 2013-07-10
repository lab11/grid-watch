package edu.umich.eecs.gridwatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class GridWatchService extends Service implements SensorEventListener {
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
				onPowerConnected();
			else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
				onPowerDisconnected();
			else if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT))
				onDockEvent(intent);
			else
				Log.d("GridWatchService", "Unknown intent: " + intent.getAction());
		}
	};
	
	private BroadcastReceiver ConnectionListenerReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
	        if (cm == null)
	            return;
	        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
	            new ProcessAlertQTask().execute();
	        }
	    }
	};
	
	private void updateIntent() {
		mLocalIntent.putExtra("pending", alertQ.size());
		Log.d("GridWatchService", "Send Update UI Intent");
		LocalBroadcastManager.getInstance(this).sendBroadcast(mLocalIntent);
	}
	
	private Intent mLocalIntent;
	
	private SensorManager mSensorManager;
	private Sensor mAccel;
	private long mAccelFirstTime;
	private float[][] mAccelHistory;
	
	private LocationManager mLocationManager;
	
	private boolean mDockCar = false;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// Service does not allow binding
		return null;
	}
	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    updateIntent();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    updateIntent();
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}

	@Override
	public void onCreate() {
		mLocalIntent = new Intent("GridWatch-update-event");
		
		IntentFilter cfilter = new IntentFilter();
		cfilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(ConnectionListenerReceiver, cfilter);
		
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
		ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		ifilter.addAction(Intent.ACTION_DOCK_EVENT);
		this.registerReceiver(mBroadcastReceiver, ifilter);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		Log.d("GridWatchService", "service started");
		Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDestroy() {
		this.unregisterReceiver(mBroadcastReceiver);
		Log.d("GridWatchService", "service destroyed");
		Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
	}
	
	private void onPowerConnected() {
		Log.d("GridWatchService", "onPowerConnected called");
		mSensorManager.unregisterListener(this);
		mAccelHistory = null;
	}
	
	private void onPowerDisconnected() {
		Log.d("GridWatchService", "onPowerDisconnected called");
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	private void onDockEvent(Intent intent) {
		int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
		mDockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		Log.d("GridWatchService", "mDockCar set to " + mDockCar);
	}
	
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// We don't really care about sensor accuracy that much; ignore
	}
	
	@Override
	public final void onSensorChanged(SensorEvent event) {
		boolean moved = false;
		
		if (mAccelHistory == null) {
			Log.d("GridWatchService", "first sensor event");
			mAccelHistory = new float[10][3];
			mAccelFirstTime = event.timestamp;
			
			for (int i=0; i<10; i++) {
				mAccelHistory[i][0] = event.values[0];
				mAccelHistory[i][1] = event.values[1];
				mAccelHistory[i][2] = event.values[2];
			}
		} else {
			int i = (int) (((event.timestamp-mAccelFirstTime) / 500000000) % 10);
			Log.d("GridWatchService", "sensor event i=" + i);
			mAccelHistory[i][0] = event.values[0];
			mAccelHistory[i][1] = event.values[1];
			mAccelHistory[i][2] = event.values[2];
			
			for (int j=0; j<10; j++) {
				if (Math.abs(mAccelHistory[j][0]-mAccelHistory[i][0]) > 2)
					moved = true;
				if (Math.abs(mAccelHistory[j][1]-mAccelHistory[i][1]) > 2)
					moved = true;
				if (Math.abs(mAccelHistory[j][2]-mAccelHistory[i][2]) > 2)
					moved = true;
			}
			
			if (moved) {
				// Once we've determined we've moved we can bail on this unplug event
				mSensorManager.unregisterListener(this);
				mAccelHistory = null;
				Log.d("GridWatchService", "sample " + i + " found movement");
				Toast.makeText(this, "Sample " + i + " found movement", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (i == 9) {
				// Got 5 seconds worth of data, that's enough
				mSensorManager.unregisterListener(this);
				mAccelHistory = null;
				
				if (!moved) {
					Log.d("GridWatchService", "no movement found, should trigger");
					new PostAlertTask().execute();
				} else {
					Log.w("GridWatchService", "now how'd we get here?");
				}
			}
		}
	}
	
	private LinkedBlockingQueue<HttpPost> alertQ = new LinkedBlockingQueue<HttpPost>();
	
	private class PostAlertTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			Log.d("GridWatchService", "PostAlertTask start");
			
			double lat = -1, lon = -1;
			String provider = null;
			if (mLocationManager != null)
				provider = mLocationManager.getBestProvider(new Criteria(), false);
			if (provider != null) {
				Location location = mLocationManager.getLastKnownLocation(provider);
				if (location != null) {
					lat = location.getLatitude();
					lon = location.getLongitude();
				} else {
					Log.d("GridWatchService", "Location Provider Unavailable");
				}
			} else {
				Log.d("GridWatchService", "Couldn't get a location provider");
			}
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://requestb.in/1e58wrt1");
			
			try {
				String phoneId = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);
				
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("id", phoneId));
				nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(System.currentTimeMillis())));
				nameValuePairs.add(new BasicNameValuePair("lat", String.valueOf(lat)));
				nameValuePairs.add(new BasicNameValuePair("lon", String.valueOf(lon)));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				mLocalIntent.putExtra("id", nameValuePairs.get(0).getValue());
				mLocalIntent.putExtra("time", nameValuePairs.get(1).getValue());
				mLocalIntent.putExtra("lat", nameValuePairs.get(2).getValue());
				mLocalIntent.putExtra("lon", nameValuePairs.get(3).getValue());
				mLocalIntent.putExtra("resp",  "sending failed");
			
				HttpResponse response = httpclient.execute(httppost);
				Log.d("GridWatchService", "POST response: " + response);
				
				mLocalIntent.putExtra("resp", "send success");
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				//e.printStackTrace();
				Log.d("GridWatchService", "IO Exception, queuing for later delivery");
				if (false == alertQ.offer(httppost)) {
					Log.e("GridWatchService", "Failed to add element to alertQ?");
				}
			}
			
			updateIntent();
			return null;
		}
	}
	
	private class ProcessAlertQTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			Log.d("GridWatchService", "ProcessAlertQTask Start");
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost post = null;
			
			try {
				while (alertQ.size() > 0) {
					post = alertQ.poll();
					if (post == null) {
						Log.w("GridWatchService", "Unexpected empty queue?");
						break;
					}
					HttpResponse response = httpclient.execute(post);
					Log.d("GridWatchService", "POST response: " + response);
					
					mLocalIntent.putExtra("resp", "send success");
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				//e.printStackTrace();
				Log.d("GridWatchService", "IO Exception, queuing for later delivery");
				mLocalIntent.putExtra("resp", "send failed");
				if (post == null) {
					Log.w("GridWatchService", "Caught post is null?");
				} else if (false == alertQ.offer(post)) {
					// Worth noting the lack of offerFirst will put elements in
					// the alertQ out of order w.r.t. when they first fired, but
					// the server will re-order based on timestamp anyway
					Log.e("GridWatchService", "Failed to add element to alertQ?");
				}
			}
			
			updateIntent();
			return null;
		}
	}
}
