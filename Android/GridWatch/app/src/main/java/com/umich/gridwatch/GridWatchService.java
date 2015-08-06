package com.umich.gridwatch;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.umich.gridwatch.Receivers.BootReceiver;
import com.umich.gridwatch.Receivers.WatchDogReceiver;
import com.umich.gridwatch.ReportTypes.GridWatchEvent;
import com.umich.gridwatch.Utils.GridWatchEventType;
import com.umich.gridwatch.Utils.GridWatchLogger;
import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by nklugman on 5/29/15.
 */
public class GridWatchService extends Service {

    // Constants for transmitting data to the main thred

    //TODO: remove location manager from this class

    private final static String MANUAL = "manual";
    private final static String REAL = "real";

    private final static int MAX_QUEUE_SIZE = 20;


    // How long to wait before forcing the phone to update locations.
    // This is not set to immediate in case another app does the update
    // first and we can just use that.
    private final static long LOCATION_WAIT_TIME = 300000l;

    // How long to wait between checks of the event list
    // for events that are finished and can be sent to the
    // server.
    private final static int EVENT_PROCESS_TIMER_PERIOD = 1000;

    // Debug Tags
    private static String errorTag = "error";
    private static String noteTag = "note";

    // List of all of the active events we are currently handling
    private ArrayList<GridWatchEvent> mEvents = new ArrayList<GridWatchEvent>();

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

    @Override
    public void onCreate() {
        mGWLogger = new GridWatchLogger(this.getApplicationContext());
        mGWLogger.log(mDateFormat.format(new Date()), "created", null);

        // Receive a callback when Internet connectivity is restored
        register_connectivity_callbacks();

        // Receive callbacks when the power state changes (plugged in, etc.)
        register_power_callbacks();

        // Setup the WatchDog
        setupAlarm(AlarmManager.INTERVAL_DAY);

        Log.w("GridWatchService:onCreate", "hit");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }


    public GridWatchEvent getEvent(int i) {
        return mEvents.get(i);
    }

    @Override
    public void onDestroy() {
        Log.d("GridWatchService", "service destroyed");
        mGWLogger.log(mDateFormat.format(new Date()), "destroyed", null);
        Toast.makeText(this, "GridWatch ended", Toast.LENGTH_SHORT).show();

        // Unregister us from different events
        this.unregisterReceiver(mPowerActionReceiver);
        this.unregisterReceiver(mConnectionListenerReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GridWatchService", "service started");
        mGWLogger.log(mDateFormat.format(new Date()), "started", null);

        //TODO can this be moved into the broadcastreceiver?
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_OFF)) {
                Log.d(noteTag, "manual power disconnected");
                onPowerDisconnected(MANUAL);
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_ON)) {
                Log.d(noteTag, "manual power connected");
                onPowerConnected(MANUAL);
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_WD)) {
                Log.d(noteTag, "power WD");
                onWD();
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK)) {
                Log.d(noteTag, "GCM ask");
                onGCMAsk();
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT)) {
                Log.d(noteTag, "GCM ask result");
                onGCMAskResult(intent.getExtras().getBoolean(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT),
                        intent.getExtras().getInt(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_INDEX));
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE)) {
                Log.d(noteTag, "GCM event");
                onGCM(GridWatchEventType.valueOf(intent.getExtras().getString(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE)));
            } else {
                Log.d(errorTag, "Unknown intent: " + intent.getAction());
            }
        }
        Log.w("GridWatchService:onStart", "hit");


        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    private void onGCMAskResult(boolean result, int index) {
        Log.w("GridWatchGCMAskResult", "hit");
        mEvents.get(index).setGCMAskResult(result);
    }

    // Handles the call back for when various power actions occur
    private BroadcastReceiver mPowerActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                onPowerConnected(REAL);
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                onPowerDisconnected(REAL);
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
        lIntent.setPackage("com.umich.gridwatch");
        sendBroadcast(lIntent);
    }

    private void onPowerConnected(String msg) {
        Log.d(noteTag, "onPowerConnected");

        // Take the opportunity to try to update our location. Since we now have
        // power (the device was just plugged in), getting a GPS lock shouldn't
        // be an issue. Also, since the phone won't move between now and when
        // it is unplugged (given how power cables work) the location should
        // be valid when the device is unplugged.
        updateLocation();

        // Create the plug event
        GridWatchEvent gwevent;
        if (msg.equals(MANUAL)){
            gwevent = new GridWatchEvent(GridWatchEventType.USR_PLUGGED, this.getApplicationContext());
        } else {
            gwevent = new GridWatchEvent(GridWatchEventType.PLUGGED, this.getApplicationContext());
        }
        mEvents.add(gwevent);
        startEventProcessTimer();
    }

    private void onGCMAsk() {
        GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.GCM_ASK_RESPONSE, this.getApplicationContext());
        gwevent.setGCMType(true);
        mEvents.add(gwevent);
        startEventProcessTimer();
    }

    private void onGCM(GridWatchEventType type) {
        GridWatchEvent gwevent;
        gwevent = new GridWatchEvent(type, this.getApplicationContext());
        gwevent.setGCMType(true);
        mEvents.add(gwevent);
        startEventProcessTimer();
    }

    private void onWD() {
        //TODO, do we want this?
        updateLocation();
        GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.WD, this.getApplicationContext());
        mEvents.add(gwevent);
        processEvents();

    }

    private void onPowerDisconnected(String msg) {
        Log.d(noteTag, "onPowerDisconnected");

        // Create the plug event
        GridWatchEvent gwevent;
        if (msg.equals(MANUAL)){
            gwevent = new GridWatchEvent(GridWatchEventType.USR_UNPLUGGED, this.getApplicationContext());
        } else {
            gwevent = new GridWatchEvent(GridWatchEventType.UNPLUGGED, this.getApplicationContext());
        }
        mEvents.add(gwevent);

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
            if (gwevent.readyForTransmission(i)) {
                Intent aIntent = new Intent(IntentConfig.INTENT_NAME);
                aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TIME, DateFormat.getTimeInstance().format(new Date()));
                if (gwevent.didFail()) {
                    Log.w("GW_SERVICE", gwevent.getFailureMessage());
                    aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_reject");
                    aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, gwevent.getFailureMessage());
                    broadcastIntent(aIntent);
                }
                if (gwevent.getEventType().equals("unplugged") || gwevent.getEventType().equals("usr_unplugged")) {
                    aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                    if (gwevent.getEventType().equals("unplugged")){
                         aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "unplugged");
                    } else {
                         aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "usr_unplugged");
                    }
                    Transmitter transmitter = new Transmitter();
                    transmitter.execute(toTransmit);
                    broadcastIntent(aIntent);
                } else { //handle non unplugged events
                    if (gwevent.getEventType().equals("plugged")){
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "plugged");
                        broadcastIntent(aIntent);
                    } else if (gwevent.getEventType().equals("usr_plugged")){
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "usr_plugged");
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


    public void register_power_callbacks() {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        ifilter.addAction(Intent.ACTION_DOCK_EVENT);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_OFF);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_ON);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_WD);
        this.registerReceiver(mPowerActionReceiver, ifilter);
    }

    public void register_connectivity_callbacks() {
        IntentFilter cfilter = new IntentFilter();
        cfilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(mConnectionListenerReceiver, cfilter);
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

                //TODO DUMB... this is repeated over and over... why?
                if (mAlertQ.size() > MAX_QUEUE_SIZE) {
                    mAlertQ.poll();
                    mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and head removed.", null);
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
                //TODO DUMB... this is repeated over and over... why?
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


        @Override
        protected Void doInBackground(TransmitterType... args) {

            GridWatchEvent gw = args[0].gw;
            Context context = args[0].context;
            postEvent(gw, context);
            Log.w(noteTag, "STARTING TRANSMITTER");
            return null;
        }

        private void postEvent (GridWatchEvent gwevent, Context context) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String,?> keys = sp.getAll();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(30);
            List<NameValuePair> msgValuePairs = new ArrayList<NameValuePair>(30);


            if (!sp.getBoolean(SensorConfig.consent, false)) {
                return;
            }

            Log.w(noteTag, "postEvent Hit");

            String home_address = (String) keys.get("home_address_text");
            String id_text = (String) keys.get("id_text");
            String power_company_phone = (String) keys.get("power_company_phone");
            String power_company_name = (String) keys.get("power_company_name");
            String work_address = (String) keys.get("work_address_text");
            Boolean wifiOrNetwork = (Boolean) keys.get("wifiOrNetwork");
            Boolean publicData = (Boolean) keys.get("makeDataPublic");
            Boolean power_company_update = (Boolean) keys.get("power_company_update");
            String gps_list_automatic = (String) keys.get("gps_list_automatic");
            String gpslist = (String) keys.get("gps_list");
            String map_update_pref = (String) keys.get("map_update_list");

            //This is so hacky...
            if (id_text!=null) {
                if (!id_text.equals("ID")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_id", id_text));
                }
            }
            if (home_address!=null) {
                if (!home_address.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_home_adr", home_address));
                }
            }
            if (power_company_name!=null) {
                if (!power_company_name.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_utility_name", power_company_name));
                }
            }
            if (power_company_phone !=null) {
                if (!power_company_phone.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_utility_phone", power_company_phone));
                }
            }
            if (work_address != null) {
                if (!work_address.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_work_adr", work_address));
                }
            }
            if (publicData != null) {
                msgValuePairs.add(new BasicNameValuePair("public", String.valueOf(publicData)));
            }



            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String alertServerURL = settings.getString("alert_server", getString(R.string.default_alert_server));
            Location gpsLocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
            Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
            String android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            if (gwevent.getEventType().equals(GridWatchEventType.USR_PLUGGED.toString()) ||
                    gwevent.getEventType().equals(GridWatchEventType.USR_UNPLUGGED.toString())) {
                if (gpslist.equals(SensorConfig.GPS_FINE)) {
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);

                } else if (gpslist.equals(SensorConfig.GPS_HIGH)) {
                    criteria.setAccuracy(Criteria.ACCURACY_HIGH);
                } else {
                    criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
                }
            } else {
                if (gps_list_automatic.equals(SensorConfig.GPS_FINE)) {
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                } else if (gps_list_automatic.equals(SensorConfig.GPS_HIGH)) {
                    criteria.setAccuracy(Criteria.ACCURACY_HIGH);
                } else {
                    criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
                }
            }
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);


            //topic=gridwatch&message=lat=10.2,lng=10,type=unplugged,house=rddfsad,time=1434402422000&key=event" "http://54.175.143.44:8080/message"
            msgValuePairs.add(new BasicNameValuePair("house", android_id));
            msgValuePairs.add(new BasicNameValuePair("phone_type", getDeviceName()));
            msgValuePairs.add(new BasicNameValuePair("os", "android"));
            msgValuePairs.add(new BasicNameValuePair("os_version", Build.VERSION.RELEASE));
            msgValuePairs.add(new BasicNameValuePair("failed", String.valueOf(gwevent.didFail())));
            if (gwevent.needFFT()) {
                msgValuePairs.add(new BasicNameValuePair("FFT", gwevent.getFFTMessage()));
            }
            Log.w("TESTING", gwevent.getEventType());
            if (gwevent.getEventType().equals("gcm_ask")) {
                msgValuePairs.add(new BasicNameValuePair("gcm_ask_result", String.valueOf(gwevent.getGCMAskResult())));
            }
            try {
                msgValuePairs.add(new BasicNameValuePair("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
            } catch (PackageManager.NameNotFoundException e) {
                msgValuePairs.add(new BasicNameValuePair("app_version", "unknown"));
            }
            msgValuePairs.add(new BasicNameValuePair("time", String.valueOf(gwevent.getTimeStampMS())));
            msgValuePairs.add(new BasicNameValuePair("type", gwevent.getEventType()));
            if (gpsLocation != null) {
                msgValuePairs.add(new BasicNameValuePair("lat", String.valueOf(gpsLocation.getLatitude())));
                msgValuePairs.add(new BasicNameValuePair("lng", String.valueOf(gpsLocation.getLongitude())));
                msgValuePairs.add(new BasicNameValuePair("gps_accuracy", String.valueOf(gpsLocation.getAccuracy())));
                msgValuePairs.add(new BasicNameValuePair("gps_time", String.valueOf(gpsLocation.getTime())));
                msgValuePairs.add(new BasicNameValuePair("gps_altitude", String.valueOf(gpsLocation.getAltitude())));
                msgValuePairs.add(new BasicNameValuePair("gps_speed", String.valueOf(gpsLocation.getSpeed())));
            }
            if (networkLocation != null) {
                msgValuePairs.add(new BasicNameValuePair("network_latitude", String.valueOf(networkLocation.getLatitude())));
                msgValuePairs.add(new BasicNameValuePair("network_longitude", String.valueOf(networkLocation.getLongitude())));
                msgValuePairs.add(new BasicNameValuePair("network_accuracy", String.valueOf(networkLocation.getAccuracy())));
                msgValuePairs.add(new BasicNameValuePair("network_time", String.valueOf(networkLocation.getTime())));
                msgValuePairs.add(new BasicNameValuePair("network_altitude", String.valueOf(networkLocation.getAltitude())));
                msgValuePairs.add(new BasicNameValuePair("network_speed", String.valueOf(networkLocation.getSpeed())));
            }
            msgValuePairs.add(new BasicNameValuePair("network", getConnection_type(context)));
            String post_info = "";
            for (int i = 0; i < msgValuePairs.size(); i++) {
                NameValuePair item = msgValuePairs.get(i);
                post_info += item.getName() + "=" + item.getValue() + ","; //just for printing
            }
            String msg = post_info.substring(0,post_info.length()-1);


            //String msg = "house="+android_id+",time="+String.valueOf(gwevent.getTimeStampMS())+",";
            //msg += "lat="+String.valueOf(gpsLocation.getLatitude())+",lng=" + String.valueOf(gpsLocation.getLongitude());
            //msg += ",type="+gwevent.getEventType() ;

            Log.w("POSTING", msg);

            nameValuePairs.add(new BasicNameValuePair("topic", "gridwatch"));
            nameValuePairs.add(new BasicNameValuePair("key", "event"));
            nameValuePairs.add(new BasicNameValuePair("message", msg));


            HttpPost httppost = new HttpPost(alertServerURL);
            try {
                UrlEncodedFormEntity postparams = new UrlEncodedFormEntity(nameValuePairs);
                httppost.setEntity(postparams);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            mGWLogger.log(mDateFormat.format(new Date()), "event_post", post_info);
            new PostAlertTask().execute(httppost);
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

    // Call to generate listeners that request the phones location.
    private void updateLocation () {
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

    private void setupAlarm(long ms) {
        ComponentName receiver = new ComponentName(this.getApplicationContext(), BootReceiver.class);
        PackageManager pm = this.getApplicationContext().getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Calendar updateTime = Calendar.getInstance();
        updateTime.setTimeZone(TimeZone.getDefault());
        Intent intent = new Intent(GridWatchService.this, WatchDogReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(GridWatchService.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), ms, pendingIntent);
    }

    public String getDeviceName() {
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

    public String getConnection_type(Context context) {
        String connection_type = "unknown";
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
        return connection_type;
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("GridWatch Outage Detected!")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}