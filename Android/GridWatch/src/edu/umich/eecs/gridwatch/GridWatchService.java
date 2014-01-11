package edu.umich.eecs.gridwatch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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

	private final static long LOCATION_WAIT_TIME = 300000l;

	private final static int SAMPLE_FREQUENCY = 44100;
	//private final static int AUDIO_SAMPLES_TO_READ = SAMPLE_FREQUENCY / 4;

	// List of all of the active events we are currently handling
	private ArrayList<GridWatchEvent> mEvents = new ArrayList<GridWatchEvent>();

	// State for the accelerometer
	private SensorManager mSensorManager;
	private Sensor mAccel;

	// Tool to get the location
	private LocationManager mLocationManager;

	private boolean mDockCar = false;

	// Timer that is fired to check if each event is ready to be sent to
	// the server.
	private Timer mEventProcessTimer = new Timer();

	// Array of messages ready to send to the server that are waiting for
	// Internet connectivity.
	private LinkedBlockingQueue<HttpPost> mAlertQ = new LinkedBlockingQueue<HttpPost>();

	// Tool for getting a pretty date
	private DateFormat mDateFormat = DateFormat.getDateTimeInstance();

	private GridWatchLogger mGWLogger;


	@Override
	public void onCreate() {

		mGWLogger = new GridWatchLogger();
		mGWLogger.log(mDateFormat.format(new Date()), "created", null);

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

	//	Intent lIntent = new Intent(INTENT_NAME);
	//	lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "created");
	//	lIntent.putExtra(INTENT_EXTRA_EVENT_TIME, mDateFormat.format(new Date()));
	//	broadcastIntent(lIntent);
	}

	@Override
	public void onDestroy() {
		mGWLogger.log(mDateFormat.format(new Date()), "destroyed", null);


		Log.d("GridWatchService", "service destroyed");
		Toast.makeText(this, "GridWatch ended", Toast.LENGTH_SHORT).show();

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
		mGWLogger.log(mDateFormat.format(new Date()), "started_old", null);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mGWLogger.log(mDateFormat.format(new Date()), "started", null);

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

		updateLocation();

		GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.PLUGGED);
		mEvents.add(gwevent);

		processEvents();
	}

	private void onPowerDisconnected() {
		Log.d("GridWatchService", "onPowerDisconnected called");

		GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.UNPLUGGED);
		mEvents.add(gwevent);

		// Start the accelerometer getting samples
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);

		Thread audioThread = new Thread(new GridWatchEventThread(gwevent));
		audioThread.start();

		startEventProcessTimer();
	}

	private void onDockEvent(Intent intent) {
		int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
		mDockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		Log.d("GridWatchService", "mDockCar set to " + mDockCar);
	}

	private void processEvents () {
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

	}


	class GridWatchEventThread implements Runnable {
		GridWatchEvent mThisEvent;

		public GridWatchEventThread (GridWatchEvent gwevent) {
			mThisEvent = gwevent;
		}

		@Override
		public void run() {
		//	Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

			int recBufferSize = AudioRecord.getMinBufferSize(SAMPLE_FREQUENCY,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);

		//	if (AUDIO_SAMPLES_TO_READ*3 > recBufferSize) {
		//		recBufferSize = AUDIO_SAMPLES_TO_READ*3;
		//	}

			AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
					SAMPLE_FREQUENCY,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					recBufferSize*2);

			audioRecord.startRecording();

			while (true) {
				short[] buffer = new short[recBufferSize];
				int shortsRead = audioRecord.read(buffer, 0, buffer.length);
				boolean done = mThisEvent.addMicrophoneSamples(buffer, shortsRead);
				if (done) break;
			}

			audioRecord.stop();
			audioRecord.release();
		}
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

		// Get the phone's current location
		Location gpsLocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
		if (gpsLocation != null) {
			nameValuePairs.add(new BasicNameValuePair("gps_latitude", String.valueOf(gpsLocation.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("gps_longitude", String.valueOf(gpsLocation.getLongitude())));
			nameValuePairs.add(new BasicNameValuePair("gps_accuracy", String.valueOf(gpsLocation.getAccuracy())));
			nameValuePairs.add(new BasicNameValuePair("gps_time", String.valueOf(gpsLocation.getTime())));
			nameValuePairs.add(new BasicNameValuePair("gps_altitude", String.valueOf(gpsLocation.getAltitude())));
			nameValuePairs.add(new BasicNameValuePair("gps_speed", String.valueOf(gpsLocation.getSpeed())));
		}
		Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
		if (networkLocation != null) {
			nameValuePairs.add(new BasicNameValuePair("network_latitude", String.valueOf(networkLocation.getLatitude())));
			nameValuePairs.add(new BasicNameValuePair("network_longitude", String.valueOf(networkLocation.getLongitude())));
			nameValuePairs.add(new BasicNameValuePair("network_accuracy", String.valueOf(networkLocation.getAccuracy())));
			nameValuePairs.add(new BasicNameValuePair("network_time", String.valueOf(networkLocation.getTime())));
			nameValuePairs.add(new BasicNameValuePair("network_altitude", String.valueOf(networkLocation.getAltitude())));
			nameValuePairs.add(new BasicNameValuePair("network_speed", String.valueOf(networkLocation.getSpeed())));
		}

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

		// Add any other key value pairs that the event needs to append
		nameValuePairs.addAll(gwevent.getNameValuePairs());

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

		Intent lIntent = new Intent(INTENT_NAME);
		lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
		String post_info = "";
		for (NameValuePair item : nameValuePairs) {
			post_info += item.getName() + "=" + item.getValue() + ", ";
		}
		lIntent.putExtra(INTENT_EXTRA_EVENT_INFO, post_info);
		lIntent.putExtra(INTENT_EXTRA_EVENT_TIME, mDateFormat.format(new Date()));
		broadcastIntent(lIntent);

		mGWLogger.log(mDateFormat.format(new Date()), "event_post", post_info);

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

	private Location getLocationByProvider(String provider) {
		Location location = null;
		try {
			if (mLocationManager.isProviderEnabled(provider)) {
				location = mLocationManager.getLastKnownLocation(provider);
			}
		} catch (IllegalArgumentException e) { }
		return location;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// Service does not allow binding
		return null;
	}

	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// We don't really care about sensor accuracy that much; ignore
	}



	private void updateLocation () {

		for (String s : mLocationManager.getAllProviders()) {
			Log.d("GridWatchService", "added location upate for " + s);
			mLocationManager.requestLocationUpdates(s, LOCATION_WAIT_TIME, 0.0f, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					// Once we get a new location cancel our location updating
					Log.d("GridWatchService", "got new location " + location);
					mLocationManager.removeUpdates(this);
				}

				@Override
				public void onProviderDisabled(String provider) { }

				@Override
				public void onProviderEnabled(String provider) { }

				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) { }
			});
		}
	}

	/*public void startRecording() {
	    gpsTimer.cancel();
	    gpsTimer = new Timer();
	    long checkInterval = getGPSCheckMilliSecsFromPrefs();
	    long minDistance = getMinDistanceFromPrefs();
	    // receive updates
	    LocationManager locationManager = (LocationManager) getApplicationContext()
	            .getSystemService(Context.LOCATION_SERVICE);
	    for (String s : locationManager.getAllProviders()) {
	        locationManager.requestLocationUpdates(s, checkInterval,
	                minDistance, new LocationListener() {

	                    @Override
	                    public void onStatusChanged(String provider,
	                            int status, Bundle extras) {}

	                    @Override
	                    public void onProviderEnabled(String provider) {}

	                    @Override
	                    public void onProviderDisabled(String provider) {}

	                    @Override
	                    public void onLocationChanged(Location location) {
	                        // if this is a gps location, we can use it
	                        if (location.getProvider().equals(
	                                LocationManager.GPS_PROVIDER)) {
	                            doLocationUpdate(location, true);
	                        }
	                    }
	                });
	        // //Toast.makeText(this, "GPS Service STARTED",
	        // Toast.LENGTH_LONG).show();
	        gps_recorder_running = true;
	    }
	    // start the gps receiver thread
	    gpsTimer.scheduleAtFixedRate(new TimerTask() {

	        @Override
	        public void run() {
	            Location location = getBestLocation();
	            doLocationUpdate(location, false);
	        }
	    }, 0, checkInterval);
	}

	public void doLocationUpdate(Location l, boolean force) {
	    long minDistance = getMinDistanceFromPrefs();
	    Log.d(TAG, "update received:" + l);
	    if (l == null) {
	        Log.d(TAG, "Empty location");
	        if (force)
	            Toast.makeText(this, "Current location not available",
	                    Toast.LENGTH_SHORT).show();
	        return;
	    }
	    if (lastLocation != null) {
	        float distance = l.distanceTo(lastLocation);
	        Log.d(TAG, "Distance to last: " + distance);
	        if (l.distanceTo(lastLocation) < minDistance && !force) {
	            Log.d(TAG, "Position didn't change");
	            return;
	        }
	        if (l.getAccuracy() >= lastLocation.getAccuracy()
	                && l.distanceTo(lastLocation) < l.getAccuracy() && !force) {
	            Log.d(TAG,
	                    "Accuracy got worse and we are still "
	                      + "within the accuracy range.. Not updating");
	            return;
	        }
	        if (l.getTime() <= lastprovidertimestamp && !force) {
	            Log.d(TAG, "Timestamp not never than last");
	            return;
	        }
	    }
	    // upload/store your location here
	}

*/





}
