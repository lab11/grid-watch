package com.umich.gridwatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class GridWatchService extends Service implements SensorEventListener {

	// Constants for transmitting data to the main thred
	private final static String INTENT_NAME = "GridWatch-update-event";
	private final static String INTENT_EXTRA_EVENT_TYPE = "event_type";
	private final static String INTENT_EXTRA_EVENT_INFO = "event_info";
	private final static String INTENT_EXTRA_EVENT_TIME = "event_time";

	private final static String INTENT_EXTRA_EVENT_MANUAL_ON = "event_manual_on";
	private final static String INTENT_EXTRA_EVENT_MANUAL_OFF = "event_manual_off";
	private final static String INTENT_EXTRA_EVENT_MANUAL_WD = "event_manual_wd";
	private final static String INTENT_MANUAL_KEY = "manual_state";
	
	private final static int MAX_QUEUE_SIZE = 20;
	
	
	// How long to wait before forcing the phone to update locations.
	// This is not set to immediate in case another app does the update
	// first and we can just use that.
	private final static long LOCATION_WAIT_TIME = 300000l;

	// How long to wait between checks of the event list
	// for events that are finished and can be sent to the
	// server.
	private final static int EVENT_PROCESS_TIMER_PERIOD = 1000;

	// Audio recording

	//private final static int TIME_MS = 3000;
	//private static MediaRecorder mRecorder = null;
	

	
	// Debug Tags
	private static String errorTag = "error";
	private static String noteTag = "note";
	
	// List of all of the active events we are currently handling
	private ArrayList<GridWatchEvent> mEvents = new ArrayList<GridWatchEvent>();

	// State for the accelerometer
	private SensorManager mSensorManager;
	private Sensor mAccel;

	// Tool to get the location
	private LocationManager mLocationManager;

	// Timer that is fired to check if each event is ready to be sent to
	// the server.
	private Timer mEventProcessTimer = new Timer();

	// Array of messages ready to send to the server that are waiting for
	// Internet connectivity.
	private LinkedBlockingQueue<HttpPost> mAlertQ = new LinkedBlockingQueue<HttpPost>();

	// Tool for getting a pretty date
	private DateFormat mDateFormat = DateFormat.getDateTimeInstance();

	// Object that handles writing and retrieving log messages
	private GridWatchLogger mGWLogger;

	// Object that handles writing and retrieving a 
	private GridWatchID mGWID;

	@Override
	public void onCreate() {
		
		mGWLogger = new GridWatchLogger(this.getApplicationContext());
		mGWLogger.log(mDateFormat.format(new Date()), "created", null);




		// Receive a callback when Internet connectivity is restored
		IntentFilter cfilter = new IntentFilter();
		cfilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mConnectionListenerReceiver, cfilter);

		// Receive callbacks when the power state changes (plugged in, etc.)
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
		ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		ifilter.addAction(Intent.ACTION_DOCK_EVENT);
		ifilter.addAction(INTENT_EXTRA_EVENT_MANUAL_OFF);
		ifilter.addAction(INTENT_EXTRA_EVENT_MANUAL_ON);
		ifilter.addAction(INTENT_EXTRA_EVENT_MANUAL_WD);
		this.registerReceiver(mPowerActionReceiver, ifilter);

		// Get references to the accelerometer api
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Log.w("using accel", noteTag);
		}

		// Get a reference to the location manager
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Toast.makeText(this, "GridWatch started", Toast.LENGTH_SHORT).show();
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


		if (intent != null && intent.getExtras() != null) {
			if (intent.getExtras().getString(INTENT_MANUAL_KEY).equals(INTENT_EXTRA_EVENT_MANUAL_ON)) {
				Log.w(noteTag, "manual power connected");
				onPowerConnected("manual");
			}
			else if (intent.getExtras().getString(INTENT_MANUAL_KEY).equals(INTENT_EXTRA_EVENT_MANUAL_OFF)) {
				Log.w(noteTag, "manual power disconnected");
				onPowerDisconnected("manual");
			}
			else if (intent.getExtras().getString(INTENT_MANUAL_KEY).equals(INTENT_EXTRA_EVENT_MANUAL_WD)) {
				Log.w(noteTag, "power WD");
				onWD();
			} else {
				Log.w(errorTag, "Unknown intent: " + intent.getAction());
			}
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}



	// Handles the call back for when various power actions occur
	private BroadcastReceiver mPowerActionReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			//TODO HTTP_DUMB
			if (mAlertQ.size() > MAX_QUEUE_SIZE) {
				mAlertQ.clear();
				mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and cleared", null);
			}
			if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
				onPowerConnected("real");
			} else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
				onPowerDisconnected("real");
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

	// Call to update the UI thread with data from this service
	private void broadcastIntent (Intent lIntent) {
		//LocalBroadcastManager.getInstance(this).sendBroadcast(lIntent)
		lIntent.setPackage("com.umich.gridwatch");
		sendBroadcast(lIntent);
	}

	private void onPowerConnected(String msg) {

		// Take the opportunity to try to update our location. Since we now have
		// power (the device was just plugged in), getting a GPS lock shouldn't
		// be an issue. Also, since the phone won't move between now and when
		// it is unplugged (given how power cables work) the location should
		// be valid when the device is unplugged.
		updateLocation();

        // Create the plug event
        GridWatchEvent gwevent;
        if (msg.equals("manual")){
             gwevent = new GridWatchEvent(GridWatchEventType.USR_PLUGGED, this.getApplicationContext());
        } else {
            gwevent = new GridWatchEvent(GridWatchEventType.PLUGGED, this.getApplicationContext());
        }
		mEvents.add(gwevent);

		// This one we don't need any sensors so go ahead and process the event
		// list because we can send the plugged event.

		processEvents();
	}

	private void onWD() {
		updateLocation();

		// Create the plug event
		GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.WD, this.getApplicationContext());
		mEvents.add(gwevent);

		// This one we don't need any sensors so go ahead and process the event
		// list because we can send the plugged event.
		processEvents();

	}

	private void onPowerDisconnected(String msg) {

		Log.w(noteTag, "onPowerDisconnected");

        // Create the plug event
        GridWatchEvent gwevent;
        if (msg.equals("manual")){
            gwevent = new GridWatchEvent(GridWatchEventType.USR_UNPLUGGED, this.getApplicationContext());
        } else {
            gwevent = new GridWatchEvent(GridWatchEventType.UNPLUGGED, this.getApplicationContext());
        }
        mEvents.add(gwevent);

		// Start the accelerometer getting samples
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
		}


		// Make sure the event queue is processed until it is empty
		startEventProcessTimer();
	}

	private void onDockEvent(Intent intent) {
		int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
		boolean dockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		Log.d("GridWatchService", "mDockCar set to " + dockCar);
	}

	// Iterate over the list of pending events and determine if any
	// should be transmitted to the server
	private void processEvents () {
		boolean done = true;

		for (int i = 0; i < mEvents.size(); i++) {
			GridWatchEvent gwevent = mEvents.get(i);
			TransmitterType toTransmit = new TransmitterType(gwevent, this.getApplicationContext());
			if (gwevent.readyForTransmission()) {
                Log.w(noteTag, gwevent.getEventType());
                Intent aIntent = new Intent(INTENT_NAME);
                aIntent.putExtra(INTENT_EXTRA_EVENT_TIME, DateFormat.getTimeInstance().format(new Date()));
                if (gwevent.getEventType().equals("unplugged") || gwevent.getEventType().equals("usr_unplugged")) {
                    if (gwevent.mMoved == true) {
                        Log.w("ACCEL", "rejecting because shaken ");
                        String reject = "Failed Accel Test";
                        aIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_reject");
                        aIntent.putExtra(INTENT_EXTRA_EVENT_INFO, reject);
                        broadcastIntent(aIntent);
                    } else {
                        if (gwevent.getEventType().equals("unplugged")){
                            aIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
                            aIntent.putExtra(INTENT_EXTRA_EVENT_INFO, "unplugged");
                        } else {
                            aIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
                            aIntent.putExtra(INTENT_EXTRA_EVENT_INFO, "usr_unplugged");
                        }
						Transmitter transmitter = new Transmitter();
						transmitter.execute(toTransmit);
                        broadcastIntent(aIntent);
                    }
                } else { //handle non unplugged events
                    if (gwevent.getEventType().equals("plugged")){
                        aIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
                        aIntent.putExtra(INTENT_EXTRA_EVENT_INFO, "plugged");
                        broadcastIntent(aIntent);
                    } else if (gwevent.getEventType().equals("usr_plugged")){
                        aIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
                        aIntent.putExtra(INTENT_EXTRA_EVENT_INFO, "usr_plugged");
                        broadcastIntent(aIntent);
                    }
					Transmitter transmitter = new Transmitter();
					transmitter.execute(toTransmit);
                }
                mEvents.remove(gwevent);
            } else {
				done = false;
			}
		}


		if (!done) {
			startEventProcessTimer();
		}
	}

	// Create a timer to check the events queue when it fires
	private void startEventProcessTimer () {
		mEventProcessTimer.schedule(new TimerTask() {
			@Override
			public void run () {
				processEvents();
			}
		}, EVENT_PROCESS_TIMER_PERIOD);
	}

	// This is called when new samples arrive from the ometer
	@Override
	public final void onSensorChanged(SensorEvent event) {

		// TODO this should never be false... cut of the sensor early at the top. Hack
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			boolean done = true; // assume we are done until proven otherwise

			for (int i = 0; i < mEvents.size(); i++) {
				GridWatchEvent gwevent = mEvents.get(i);
				boolean gwevent_done = gwevent.addAccelerometerSample(event.timestamp,
						event.values[0], event.values[1], event.values[2]);

				if (!gwevent_done) {
					done = false;
				}
			}

			if (done) {
				// All events are finished getting accelerometer samples, so go
				// ahead and stop this listener
				mSensorManager.unregisterListener(this);
			}
		}

	}


	// Class that handles transmitting information about events. This
	// operates asynchronously at some point in the future.
	private class PostAlertTask extends AsyncTask<HttpPost, Void, Void> {
		
		// This gets called by the OS
		@Override
		protected Void doInBackground(HttpPost... httpposts) {
			Log.w("GridWatchService", "PostAlertTask start");
			
			HttpClient httpclient = new DefaultHttpClient();

			try {
				// Execute the HTTP POST request
				@SuppressWarnings("unused")
				HttpResponse response = httpclient.execute(httpposts[0]);
				Log.d("GridWatchService", "POST response: " + response);



			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// Handle when the POST fails
				Log.d("GridWatchService", "IO Exception, not attempting later delivery");
				
				//TODO HTTP_DUMB
				if (mAlertQ.size() > MAX_QUEUE_SIZE) {
					mAlertQ.clear();
					mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and cleared", null);
				}
				
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
				//TODO HTTP_DUMB
				if (mAlertQ.size() > MAX_QUEUE_SIZE) {
					mAlertQ.clear();
					mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and cleared", null);
				}
				
				while (mAlertQ.size() > 0) {
					
					post = mAlertQ.poll();
					if (post == null) {
						break;
					}
					@SuppressWarnings("unused")
					HttpResponse response = httpclient.execute(post); //TODO... is the queing working?
					Log.d("QUEUE", "POST response: " + response);
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				//e.printStackTrace();
				Log.d("GridWatchService", "IO Exception, queuing for later delivery");
				if (post == null) {
					Log.w("GridWatchService", "Caught post is null?");
				} 
				
				else if (mAlertQ.offer(post) == false) {
					// Worth noting the lack of offerFirst will put elements in
					// the alertQ out of order w.r.t. when they first fired, but
					// the server will re-order based on timestamp anyway
					Log.e("GridWatchService", "Failed to add element to alertQ?");
				}
			}
			return null;
		}
	}


	private static class TransmitterType {
		GridWatchEvent gw;
		Context context;

		TransmitterType(GridWatchEvent gw, Context context) {
			this.gw = gw;
			this.context = context;
		}
	}


	private class Transmitter extends AsyncTask<TransmitterType, Void, Void> {

        // Constants for transmitting data to the main thred
        private final static String INTENT_NAME = "GridWatch-update-event";
        private final static String INTENT_EXTRA_EVENT_TYPE = "event_type";
        private final static String INTENT_EXTRA_EVENT_INFO = "event_info";
        private final static String INTENT_EXTRA_EVENT_TIME = "event_time";

        private final static String INTENT_EXTRA_EVENT_MANUAL_ON = "event_manual_on";
        private final static String INTENT_EXTRA_EVENT_MANUAL_OFF = "event_manual_off";
        private final static String INTENT_EXTRA_EVENT_MANUAL_WD = "event_manual_wd";
        private final static String INTENT_MANUAL_KEY = "manual_state";

        private boolean recording_on;

        // Call to update the UI thread with data from this service
        private void broadcastIntent (Intent lIntent) {
            //LocalBroadcastManager.getInstance(this).sendBroadcast(lIntent)
            lIntent.setPackage("com.umich.gridwatch");
            sendBroadcast(lIntent);
        }

		@Override
		protected Void doInBackground(TransmitterType... args) {

			GridWatchEvent gw = args[0].gw;
			Context context = args[0].context;
			postEvent(gw, context);
			Log.w(noteTag, "STARTING TRANSMITTER");
			return null;
		}

		private void postEvent (GridWatchEvent gwevent, Context context) {
			Log.w(noteTag, "postEvent Hit");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(15);
			JSONObject jsComm = new JSONObject();
			String rev = String.valueOf(System.currentTimeMillis());

			// Get the url of the server to post to
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			//String alertServerURL = settings.getString("alert_server", getString(R.string.default_alert_server));
			String alertServerURL = settings.getString("alert_server", "http://requestb.in/yxmsriyx");
			Location gpsLocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
			Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
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
			// Get basics from phone
			mGWID = new GridWatchID(context);

		     //THIS IS GATD
			nameValuePairs.add(new BasicNameValuePair("id", mGWID.get_last_value()));
			nameValuePairs.add(new BasicNameValuePair("phone_type", getDeviceName()));
			nameValuePairs.add(new BasicNameValuePair("os", "android"));
			nameValuePairs.add(new BasicNameValuePair("os_version", Build.VERSION.RELEASE));
			try {
				nameValuePairs.add(new BasicNameValuePair("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
			} catch (PackageManager.NameNotFoundException e) {
				nameValuePairs.add(new BasicNameValuePair("app_version", "unknown"));
			}

			// Get basics from the event
			nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(gwevent.getTimestampMilli())));
			nameValuePairs.add(new BasicNameValuePair("event_type", gwevent.getEventType()));

			// Get the phone's current location
			if (gpsLocation != null) {
				nameValuePairs.add(new BasicNameValuePair("gps_latitude", String.valueOf(gpsLocation.getLatitude())));
				nameValuePairs.add(new BasicNameValuePair("gps_longitude", String.valueOf(gpsLocation.getLongitude())));
				nameValuePairs.add(new BasicNameValuePair("gps_accuracy", String.valueOf(gpsLocation.getAccuracy())));
				nameValuePairs.add(new BasicNameValuePair("gps_time", String.valueOf(gpsLocation.getTime())));
				nameValuePairs.add(new BasicNameValuePair("gps_altitude", String.valueOf(gpsLocation.getAltitude())));
				nameValuePairs.add(new BasicNameValuePair("gps_speed", String.valueOf(gpsLocation.getSpeed())));


            }
			if (networkLocation != null) {
				nameValuePairs.add(new BasicNameValuePair("network_latitude", String.valueOf(networkLocation.getLatitude())));
				nameValuePairs.add(new BasicNameValuePair("network_longitude", String.valueOf(networkLocation.getLongitude())));
				nameValuePairs.add(new BasicNameValuePair("network_accuracy", String.valueOf(networkLocation.getAccuracy())));
				nameValuePairs.add(new BasicNameValuePair("network_time", String.valueOf(networkLocation.getTime())));
				nameValuePairs.add(new BasicNameValuePair("network_altitude", String.valueOf(networkLocation.getAltitude())));
				nameValuePairs.add(new BasicNameValuePair("network_speed", String.valueOf(networkLocation.getSpeed())));
			}


			nameValuePairs.add(new BasicNameValuePair("network", connection_type));

			// Add any other key value pairs that the event needs to append
			nameValuePairs.addAll(gwevent.getNameValuePairs());


			// Create the post and the task to run in the background to send eventually
        	String post_info = "";
        	HttpPost httppost = new HttpPost(alertServerURL);
        	MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        	builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        	for (int i = 0; i < nameValuePairs.size(); i++) {
            	NameValuePair item = nameValuePairs.get(i);
            	builder.addTextBody(item.getName(), item.getValue());
            	post_info += item.getName() + "=" + item.getValue() + ", ";
        	}
        	if (!gwevent.getEventType().equals("wd")) {
                if (recording_on) {
                    File file = new File(gwevent.getRecordingFileName());
                    if (file != null) {
                        //builder.addBinaryBody("audio", file, ContentType.WILDCARD, gwevent.getRecordingFileName());
                        try {
                            //byte[] file_bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
                            //byte[] base64_bytes = Base64.encode(file_bytes, Base64.DEFAULT);
                            //builder.addBinaryBody("audio_base64", base64_bytes);
                        } catch (Exception e) {

                        }
                        //builder.addPart("audio", new FileBody(new File(gwevent.getRecordingFileName())));
                    }
                }
        	}
        	Log.w("POSTING", "posting");
        	mGWLogger.log(mDateFormat.format(new Date()), "event_post", post_info);
        	httppost.setEntity(builder.build());
        	new PostAlertTask().execute(httppost);


			//curl --user boundeducknoweredentwous:RWsLy60emRleQL1gt131SvFh -X POST http://nklugman.cloudant.com/gridwatch-events -d '{"foo" : "bar"}' -H "Content-Type:application/json"


			/* COUCHDB
			//ALL JSON FUN!
			String base64EncodedCredentials = "";
			String response_ID = "";
			String response_REV = "";
			try {
				jsComm.put("id", mGWID.get_last_value());
				jsComm.put("phone_type", getDeviceName());
				jsComm.put("os", "android");
				jsComm.put("os_version", Build.VERSION.RELEASE);
				jsComm.put("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
				jsComm.put("time", String.valueOf(gwevent.getTimestampMilli()));
				jsComm.put("event_type", gwevent.getEventType());
				if (gpsLocation != null) {
					jsComm.put("gps_latitude", String.valueOf(gpsLocation.getLatitude()));
					jsComm.put("gps_longitude", String.valueOf(gpsLocation.getLongitude()));
					jsComm.put("gps_accuracy", String.valueOf(gpsLocation.getAccuracy()));
					jsComm.put("gps_time", String.valueOf(gpsLocation.getTime()));
					jsComm.put("gps_altitude", String.valueOf(gpsLocation.getAltitude()));
					jsComm.put("gps_speed", String.valueOf(gpsLocation.getSpeed()));
				}
				if (networkLocation != null) {
					jsComm.put("network_latitude", String.valueOf(networkLocation.getLatitude()));
					jsComm.put("network_longitude", String.valueOf(networkLocation.getLongitude()));
					jsComm.put("network_accuracy", String.valueOf(networkLocation.getAccuracy()));
					jsComm.put("network_time", String.valueOf(networkLocation.getTime()));
					jsComm.put("network_altitude", String.valueOf(networkLocation.getAltitude()));
					jsComm.put("network_speed", String.valueOf(networkLocation.getSpeed()));
				}
				jsComm.put("network", connection_type);
				List<NameValuePair> extraValues = gwevent.getNameValuePairs();
				for (int i = 0; i < extraValues.size(); i++) {
					NameValuePair cur = extraValues.get(i);
					jsComm.put(cur.getName(), cur.getValue());
				}
			} catch (JSONException e) {}
			catch (PackageManager.NameNotFoundException e) {
				try {
					jsComm.put("app_version", "unknown");
				} catch (JSONException f) {}
			}
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://nklugman.cloudant.com/gridwatch-events");
			String responseBody = "";
			HttpResponse response = null;
			try {

				base64EncodedCredentials = "Basic " + Base64.encodeToString(
						("boundeducknoweredentwous" + ":" + "RWsLy60emRleQL1gt131SvFh").getBytes(),
						Base64.NO_WRAP);

				httppost.setHeader("Authorization", base64EncodedCredentials);
				httppost.setHeader(HTTP.CONTENT_TYPE,"application/json");

				if (!gwevent.getEventType().equals("wd")) {
					File file = new File(gwevent.getRecordingFileName());
					if (file != null) {
						try {
							byte[] file_bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
							byte[] base64_bytes = Base64.encode(file_bytes, Base64.DEFAULT);
							JSONObject file_bits = new JSONObject().put("content_type", "audio/wav");
							file_bits.put("data", base64_bytes);
							jsComm.put("_attachments",
									new JSONObject().put("audio_file.wav", file_bits));
						} catch (Exception e) {}
					}
				}
				Log.w("SEND_TO_COUCH_JSON", jsComm.toString());

				httppost.setEntity(new StringEntity(jsComm.toString(), "UTF-8"));
				response = httpclient.execute(httppost);

				if (response.getStatusLine().getStatusCode() == 200) {
					Log.d("response ok", response.toString());
				} else {
					Log.d("response not ok", "Something went wrong :/");
				}
				try {
					responseBody = EntityUtils.toString(response.getEntity());
					Log.w("SEND_TO_COUCH", responseBody);
					JSONObject responseJSON = new JSONObject(responseBody);
					response_ID = responseJSON.getString("id");
					response_REV = responseJSON.getString("rev");
					Log.w("SEND_TO_COUCH_ID", response_ID);
					Log.w("SEND_TO_COUCH_REV", response_REV);
				} catch (IOException e) {}
				catch (JSONException e) {}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/

			/*
			if (response_ID != "") {
				JSONObject audio = new JSONObject();
				HttpPost httppost_audio = new HttpPost("http://nklugman.cloudant.com/gridwatch-events/");
				String responseBody_audio = "";
				HttpResponse response_audio = null;
				try {
					httppost_audio.setHeader("Authorization", base64EncodedCredentials);
					httppost_audio.setHeader(HTTP.CONTENT_TYPE,"application/json");
					audio.put("_rev", response_REV);
					audio.put("_id",response_ID);

					if (!gwevent.getEventType().equals("wd")) {
						File file = new File(gwevent.getRecordingFileName());
						if (file != null) {
								try {
									byte[] file_bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
									byte[] base64_bytes = Base64.encode(file_bytes, Base64.DEFAULT);
									JSONObject file_bits = new JSONObject().put("content_type", "audio/vnd.wav");
									file_bits.put("data", base64_bytes);
									audio.put("_attachments",
											new JSONObject().put("audio_file", file_bits));
									Log.w("AUDIO_JSON", audio.toString());
							} catch (Exception e) {}
						}
					}
					httppost_audio.setEntity(new StringEntity(audio.toString(), "UTF-8"));
					response_audio = httpclient.execute(httppost_audio);
					try {
						responseBody_audio = EntityUtils.toString(response_audio.getEntity());
						Log.w("SEND_TO_COUCH_AUDIO", responseBody_audio);
					} catch (IOException e){};
					if (response_audio.getStatusLine().getStatusCode() == 200) {
						Log.d("audio response ok", "ok response :/");
					} else {
						Log.d("audio response not ok", "Something went wrong :/");
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				Log.w("COUCH_AUDIO", responseBody_audio);
			}
			*/

			// Update the main page
            /*
            Intent lIntent = new Intent(INTENT_NAME);
            lIntent.putExtra(INTENT_EXTRA_EVENT_TYPE, "event_post");
            lIntent.putExtra(INTENT_EXTRA_EVENT_INFO, gwevent.getEventType());
            lIntent.putExtra(INTENT_EXTRA_EVENT_TIME, mDateFormat.format(new Date()));
            broadcastIntent(lIntent);
            */
		}

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

	// Call to generate listeners that request the phones location.
	private void updateLocation () {

        //TODO fix loop?
		for (String s : mLocationManager.getAllProviders()) {
			mLocationManager.requestLocationUpdates(s, LOCATION_WAIT_TIME, 0.0f, new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					// Once we get a new location cancel our location updating
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
}
