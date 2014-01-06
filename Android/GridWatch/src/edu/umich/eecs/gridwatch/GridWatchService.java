package edu.umich.eecs.gridwatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class GridWatchService extends Service implements SensorEventListener {

	private final static String INTENT_NAME = "GridWatch-update-event";
	private final static String INTENT_EXTRA_EVENT_TYPE = "event_type";
	private final static String INTENT_EXTRA_EVENT_INFO = "event_info";
	private final static String INTENT_EXTRA_EVENT_TIME = "event_time";

	// List of all of the active events we are currently handling
	private ArrayList<GridWatchEvent> mEvents = new ArrayList<GridWatchEvent>();


	//private final static long ACCEL_DURATION_NANOSECONDS = 5000000000l;
	private SensorManager mSensorManager;
	private Sensor mAccel;
	//private long mAccelFirstSampleTime;
	//private float[][] mAccelHistory;

	private LocationManager mLocationManager;

	private boolean mDockCar = false;
	//private long mPowerDisconnectTimestamp;

	private Timer mEventProcessTimer = new Timer();

	private LinkedBlockingQueue<HttpPost> mAlertQ = new LinkedBlockingQueue<HttpPost>();


	@Override
	public void onCreate() {

		IntentFilter cfilter = new IntentFilter();
		cfilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mConnectionListenerReceiver, cfilter);

		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
		ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		ifilter.addAction(Intent.ACTION_DOCK_EVENT);
		this.registerReceiver(mPowerActionReceiver, ifilter);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		Log.d("GridWatchService", "service started");
		Toast.makeText(this, "GridWatch started", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
		Log.d("GridWatchService", "service destroyed");
		Toast.makeText(this, "GridWatch ended", Toast.LENGTH_SHORT).show();

		// Notify the main app that the service is ending
		Intent lIntent = new Intent(INTENT_NAME);
		lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "destroy");
		broadcastIntent(lIntent);

		// Unregister us from different events
		this.unregisterReceiver(mPowerActionReceiver);
		this.unregisterReceiver(mConnectionListenerReceiver);
	}

	// Call to update the UI thread with data from this service
	private void broadcastIntent (Intent lIntent) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(lIntent);
	}



	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		Intent lIntent = new Intent(INTENT_NAME);
		lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "started");
		broadcastIntent(lIntent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Intent lIntent = new Intent(INTENT_NAME);
		lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "started");
		broadcastIntent(lIntent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}


	// Handles the call back for when various power actions occur
	private BroadcastReceiver mPowerActionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
				onPowerConnected();
			} else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
				onPowerDisconnected();
			} else if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
				onDockEvent(intent);
			} else {
				Log.d("GridWatchService", "Unknown intent: " + intent.getAction());
			}
		}
	};

	// Handles the call when Internet connectivity is restored
	private BroadcastReceiver mConnectionListenerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
				if (cm == null) {
					return;
				}
				// If we have regained Internet connectivity, process any backlog of alerts
				// we need to send.
				if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
					new ProcessAlertQTask().execute();
				}
		}
	};


	private void onPowerConnected() {
		Log.d("GridWatchService", "onPowerConnected called");

		GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.PLUGGED);
		mEvents.add(gwevent);
		processEvents();

		//mSensorManager.unregisterListener(this);
		//mAccelHistory = null;

	//	postEvent("plugged");
	}

	private void onPowerDisconnected() {
		Log.d("GridWatchService", "onPowerDisconnected called");

		GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.UNPLUGGED);
		mEvents.add(gwevent);

		// Start the accelerometer getting samples
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);

		startEventProcessTimer();
	}

	private void onDockEvent(Intent intent) {
		int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
		mDockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		Log.d("GridWatchService", "mDockCar set to " + mDockCar);
	}

	private boolean processEvents () {
		boolean done = true;

		for (GridWatchEvent gwevent : mEvents) {
			boolean ready = gwevent.readyForTransmission();

			if (ready) {
				postEvent(gwevent);
				mEvents.remove(gwevent);
			} else {
				done = false;
			}
		}

		if (!done) {
			startEventProcessTimer();
		}
	}

	private void startEventProcessTimer () {
		mEventProcessTimer.schedule(new TimerTask () {
			@Override
			public void run () {
				processEvents();
			}
		}, 1000);
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// We don't really care about sensor accuracy that much; ignore
	}

	// This is called when new samples arrive from the accelerometer
	@Override
	public final void onSensorChanged(SensorEvent event) {
		boolean done = true; // assume we are done until proven otherwise

		// Loop through all of our active events and pass them accelerometer data
		for (GridWatchEvent gwevent : mEvents) {
			boolean gwevent_done = gwevent.addAccelerometerSample(event.timestamp,
					event.values[0], event.values[1], event.values[2]);

			if (!gwevent_done) {
				done = false;
			}
		}

		if (done) {
			mSensorManager.unregisterListener(this);
		}

		/*if (mAccelHistory == null) {
			Log.d("GridWatchService", "first sensor event");
			mAccelHistory = new float[10][3];
			mAccelFirstTime = event.timestamp;

			for (int i=0; i<10; i++) {
				mAccelHistory[i][0] = event.values[0];
				mAccelHistory[i][1] = event.values[1];
				mAccelHistory[i][2] = event.values[2];
			}
		} else {
			// Use i to index into the history. We only end up using 1 sample per second,
			// but because sample rates differ between phones we may get many per second.
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
				Toast.makeText(this, "GridWatch -> No Power Outage", Toast.LENGTH_SHORT).show();

				postEvent("unplugged_moved");
				return;
			}

			if (i == 9) {
				// Got 5 seconds worth of data, that's enough
				mSensorManager.unregisterListener(this);
				mAccelHistory = null;

				// Didn't detect motion so transmit an unplugged still event
				Log.d("GridWatchService", "no movement found, should trigger");
				Toast.makeText(this, "GridWatch -> Power Outage!", Toast.LENGTH_SHORT).show();

				postEvent("unplugged_still");
			}
		}*/
	}

	// Class that handles transmitting information about events. This
	// operates asynchronously at some point in the future.
	private class PostAlertTask extends AsyncTask<HttpPost, Void, Void> {

		// This gets called by the OS
		@Override
		protected Void doInBackground(HttpPost... httpposts) {
			Log.d("GridWatchService", "PostAlertTask start");

			HttpClient httpclient = new DefaultHttpClient();

			try {
				// Execute the HTTP POST request
				HttpResponse response = httpclient.execute(httpposts[0]);
				Log.d("GridWatchService", "POST response: " + response);

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// Handle when the POST fails
				Log.d("GridWatchService", "IO Exception, queuing for later delivery");
				if (mAlertQ.offer(httpposts[0]) == false) {
					Log.e("GridWatchService", "Failed to add element to alertQ?");
				}
			}

			updateIntent();
			return null;
		}
	}

	// This class handles iterating through a backlog of messages to send
	// once Internet connectivity has been restored.
	private class ProcessAlertQTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			Log.d("GridWatchService", "ProcessAlertQTask Start");

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost post = null;

			try {
				while (mAlertQ.size() > 0) {
					post = mAlertQ.poll();
					if (post == null) {
						Log.w("GridWatchService", "Unexpected empty queue?");
						break;
					}
					HttpResponse response = httpclient.execute(post);
					Log.d("GridWatchService", "POST response: " + response);
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				//e.printStackTrace();
				Log.d("GridWatchService", "IO Exception, queuing for later delivery");
				if (post == null) {
					Log.w("GridWatchService", "Caught post is null?");
				} else if (mAlertQ.offer(post) == false) {
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

	// Function to call to notify the server than an event happened on this phone.
	private void postEvent (GridWatchEvent gwevent) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(15);

		// Get the url of the server to post to
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String alertServerURL = settings.getString("alert_server", getString(R.string.default_alert_server));

		// Get basics from the event
		nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(gwevent.getTimestampMilli())));
		nameValuePairs.add(new BasicNameValuePair("event_type", gwevent.getEventType()));

	//	httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		// Get the phone's current location
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
		nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(lat)));
		nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(lon)));

		// Determine if we are on wifi, mobile, or have no connection
		String connection_type = "unknown";
		ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo active_net_info = cm.getActiveNetworkInfo();
			if (active_net_info != null) {
				if (active_net_info.isConnected()) {
					if (active_net_info.getType() == ConnectivityManager.TYPE_WIFI) {
						connection_type = "wifi";
					} else if (active_net_info.getType() == ConnectivityManager.TYPE_MOBILE) {
						connection_type = "mobile";
					} else {
						connection_type = "other";
					}
				} else {
					connection_type = "disconnected";
				}
			}
		}
		nameValuePairs.add(new BasicNameValuePair("network", connection_type));

		// Fill in other values to send to the server
		nameValuePairs.add(new BasicNameValuePair("id", Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID)));
		nameValuePairs.add(new BasicNameValuePair("phone_type", getDeviceName()));
		nameValuePairs.add(new BasicNameValuePair("os", "android"));
		nameValuePairs.add(new BasicNameValuePair("os_version", Build.VERSION.RELEASE));
		try {
			nameValuePairs.add(new BasicNameValuePair("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
		} catch (NameNotFoundException e) {
			nameValuePairs.add(new BasicNameValuePair("app_version", "unknown"));
		}

		HttpPost httppost = new HttpPost(alertServerURL);
		try {
			UrlEncodedFormEntity postparams = new UrlEncodedFormEntity(nameValuePairs);
			httppost.setEntity(postparams);

		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Log.d("GridWatchService", httppost.getRequestLine().getUri());

		// Create the task to run in the background at some point in the future
		new PostAlertTask().execute(httppost);
	}

	// Returns the phone type for adding meta data to the transmitted packets
	private String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// Service does not allow binding
		return null;
	}
}
