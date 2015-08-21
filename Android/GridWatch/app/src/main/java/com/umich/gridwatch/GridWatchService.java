package com.umich.gridwatch;

import android.app.AlarmManager;
import android.app.Notification;
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
 *
 * The main background service for the app. Creates and queues GW events. Checks events
 * for their readiness and handles the HTTP posting. Also, it currently gathers some metrics
 * for the HTTP Post, although these will be removed to individual sensor classes in the future. This
 * refactor allows for easier and modular composition of the sensors used for different types of
 * GW reports.
 */
public class GridWatchService extends Service {

    //TODO: remove location manager from this class

    private final static String MANUAL = "manual";
    private final static String REAL = "real";

    // This requires a bit more testing. I found that if the queue of events gets too large, the app can run out of memory and crash. This showed on the Blu Dash JR phones. The queue is cleared a couple times redundently. 20 is chosen somewhat arbitrarily and can likely be much higher.
    private final static int MAX_QUEUE_SIZE = 20;

    // Debug Tags
    private final static String onCreateTag = "GridWatchService:onCreate";
    private final static String onDestoryTag = "GridWatchService:onDestroy";
    private final static String onStartCommandTag = "GridWatchService:onStartCommand";
    private final static String onGCMAskResultTag = "GridWatchService:onGCMAskResult";
    private final static String broadcastReceiverPowerActionReceiverTag = "GridWatchService:BroadcastReceiver:PowerActionReceiver";
    private final static String broadcastReceiverConnectionListenerReceiverTag = "GridWatchService:BroadcastReceiver:ConnectionListenerReceiver";
    private final static String onPowerConnectedTag = "GridWatchService:onPowerConnected";
    private final static String onGCMaskTag = "GridWatchService:onGCMask";
    private final static String onGCMTag = "GridWatchService:onGCM";
    private final static String onWDTag = "GridWatchService:onWD";
    private final static String onPowerDisconnectedTag = "GridWatchService:onPowerDisconnected";
    private final static String onDockEventTag = "GridWatchService:onDockEvent";
    private final static String processEventsTag = "GridWatchService:processEvents";
    private final static String registerPowerCallbacksTag = "GridWatchService:register_power_callbacks";
    private final static String registerConnectivityCallbacksTag = "GridWatchService:register_connectivity_callbacks";
    private final static String startEventProcessTimerTag = "GridWatchService:startEventProcessTimer";
    private final static String getLocationByProviderTag = "GridWatchService:getLocationByProvider";
    private final static String updateLocationTag = "GridWatchService:updateLocation";
    private final static String setupAlarmTag = "GridWatchService:setupAlarm";
    private final static String getDeviceNameTag = "GridWatchService:getDeviceName";
    private final static String getConnectionType = "GridWatchService:getConnection_type";
    private final static String PostAlertTaskdoInBackgroundTag = "GridWatchService:PostAlertTask:doInBackground";
    private final static String ProcessAlertQTaskdoInBackgroundTag = "GridWatchService:ProcessAlertQTask:doInBackground";
    private final static String TransmitterdoInBackground = "GridWatchService:Transmitter:doInBackground";
    private final static String TransmitterPostEvent = "GridWatchService:Transmitter:PostEvent";
    private final static String sendNotificationTag = "GridWatchService:sendNotification";

    // How long to wait before forcing the phone to update locations.
    // This is not set to immediate in case another app does the update
    // first and we can just use that.
    private final static long LOCATION_WAIT_TIME = 300000l;

    // How long to wait between checks of the event list
    // for events that are finished and can be sent to the
    // server.
    private final static int EVENT_PROCESS_TIMER_PERIOD = 1000;

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
        Log.d(onCreateTag, "hit");

        // Local logger
        mGWLogger = new GridWatchLogger(this.getApplicationContext());
        mGWLogger.log(mDateFormat.format(new Date()), "created", null);

        // Receive a callback when Internet connectivity is restored
        registerPowerCallbacks();

        // Receive callbacks when the power state changes (plugged in, etc.)
        registerConnectivityCallbacks();

        // Setup the WatchDog
        setupAlarm(AlarmManager.INTERVAL_DAY);

        // This should be moved to the GPSService... Sometime soon.
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }


    public GridWatchEvent getEvent(int i) {
        return mEvents.get(i);
    }

    // Clean up when app is exited.
    @Override
    public void onDestroy() {
        Log.d(onDestoryTag, "service destroyed");
        mGWLogger.log(mDateFormat.format(new Date()), "destroyed", null);
        Toast.makeText(this, "GridWatch ended", Toast.LENGTH_SHORT).show();

        // Unregister us from different events
        this.unregisterReceiver(mPowerActionReceiver);
        this.unregisterReceiver(mConnectionListenerReceiver);
    }

    // When the intent starts, figure out what started it
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(onStartCommandTag, "service started");
        mGWLogger.log(mDateFormat.format(new Date()), "started", null);

        //TODO can this be moved into the broadcastreceiver?
        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_OFF)) {
                Log.d(onStartCommandTag, "manual power disconnected");
                onPowerDisconnected(MANUAL);
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_ON)) {
                Log.d(onStartCommandTag, "manual power connected");
                onPowerConnected(MANUAL);
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_WD)) {
                Log.d(onStartCommandTag, "power WD");
                onWD();
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK)) {
                Log.d(onStartCommandTag, "GCM ask");
                onGCMAsk();
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT)) {
                Log.d(onStartCommandTag, "GCM ask result");
                onGCMAskResult(intent.getExtras().getBoolean(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_RESULT),
                        intent.getExtras().getInt(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_INDEX));
            }
            else if (intent.getExtras().getString(IntentConfig.INTENT_MANUAL_KEY).equals(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE)) {
                Log.d(onStartCommandTag, "GCM event");
                onGCM(GridWatchEventType.valueOf(intent.getExtras().getString(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE)));
            } else {
                Log.d(onStartCommandTag, "Unknown intent: " + intent.getAction());
            }
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }


    // Call back from dialog resultant from GCM request
    private void onGCMAskResult(boolean result, int index) {
        Log.w(onGCMAskResultTag, "hit");
        mEvents.get(index).setGCMAskResult(result);
    }

    // Handles the call back for when various power actions occur
    private BroadcastReceiver mPowerActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(broadcastReceiverPowerActionReceiverTag, "hit");

            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                onPowerConnected(REAL);
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                onPowerDisconnected(REAL);
            } else if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
                onDockEvent(intent);
            } else {
                Log.w(broadcastReceiverPowerActionReceiverTag, "Unknown intent: " + intent.getAction());
            }
        }
    };

    // Handles the call when Internet connectivity is restored
    private BroadcastReceiver mConnectionListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(broadcastReceiverConnectionListenerReceiverTag, "hit");

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();;

            if (cm == null) {
                return;
            }
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                Log.d(broadcastReceiverConnectionListenerReceiverTag, "connectivity restored");
                new ProcessAlertQTask().execute();
            }
        }
    };


    // Call to update the UI thread with data from this service
    private void broadcastIntent (Intent lIntent) {
        lIntent.setPackage("com.umich.gridwatch");
        sendBroadcast(lIntent);
    }

    // Create GW events when the power is connected
    private void onPowerConnected(String msg) {
        Log.d(onPowerConnectedTag, "onPowerConnected");

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

    // Create a GW event containing the result of GCM dialog
    private void onGCMAsk() {
        GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.GCM_ASK_RESPONSE, this.getApplicationContext());
        gwevent.setGCMType(true);
        mEvents.add(gwevent);
        startEventProcessTimer();
    }

    // Create GW events from GCM
    private void onGCM(GridWatchEventType type) {
        GridWatchEvent gwevent;
        gwevent = new GridWatchEvent(type, this.getApplicationContext());
        gwevent.setGCMType(true);
        mEvents.add(gwevent);
        startEventProcessTimer();
    }

    // Create GW event with watchdog
    private void onWD() {
        //TODO, do we want this? Is the energy cost too high?
        updateLocation();
        GridWatchEvent gwevent = new GridWatchEvent(GridWatchEventType.WD, this.getApplicationContext());
        mEvents.add(gwevent);
        processEvents();
    }

    // Create GW events when the power is disconnected
    private void onPowerDisconnected(String msg) {
        Log.d(onPowerDisconnectedTag, "hit");

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

    // Creates GW if Android gives you different dock states. Very rarely happens.
    private void onDockEvent(Intent intent) {
        Log.d(onDockEventTag, "hit");

        int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
        boolean dockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
        Log.d(onDockEventTag, "mDockCar set to " + dockCar);
    }

    // Iterate over the list of pending events and determine if any
    // should be transmitted to the server
    //
    // Names and creates GW Events
    //
    private void processEvents () {

        //check for connection and stop transmission if needed
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        Map<String,?> keys = sp.getAll();
        Boolean wifiOrNetwork = (Boolean) keys.get("wifi_or_network");
        String network = getConnectionType(this.getApplicationContext());

        boolean done = true; //are we done processing?
        if (wifiOrNetwork != null) {
            if (wifiOrNetwork && network.equals("disconnected")) {
                done = false;
            }
        } else { //settings allow for the attempt of transmission
            for (int i = 0; i < mEvents.size(); i++) { //the queue of all events yets to be translated
                GridWatchEvent gwevent = mEvents.get(i);
                TransmitterType toTransmit = new TransmitterType(gwevent, this.getApplicationContext());
                if (gwevent.readyForTransmission(i)) { //we want to send this event out to the transmitter to be formated and POSTed
                    Intent aIntent = new Intent(IntentConfig.INTENT_NAME);
                    aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TIME, DateFormat.getTimeInstance().format(new Date()));
                    if (gwevent.didFail()) { //figure out if a sensor reported a failure
                        Log.w(processEventsTag, gwevent.getFailureMessage());
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_reject");
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, gwevent.getFailureMessage());
                        broadcastIntent(aIntent);
                    }
                    // broadcast the result of unplugged events
                    if (gwevent.getEventType().equals("unplugged") || gwevent.getEventType().equals("usr_unplugged")) {
                        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                        if (gwevent.getEventType().equals("unplugged")) {
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "unplugged");
                        } else {
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "usr_unplugged");
                        }
                        Transmitter transmitter = new Transmitter();
                        transmitter.execute(toTransmit);
                        broadcastIntent(aIntent);
                    } else { //broadcast the results of plugged events
                        if (gwevent.getEventType().equals("plugged")) {
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "plugged");
                        } else if (gwevent.getEventType().equals("usr_plugged")) {
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_post");
                            aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, "usr_plugged");
                        }
                        broadcastIntent(aIntent);
                        Transmitter transmitter = new Transmitter();
                        transmitter.execute(toTransmit);
                    }
                    mEvents.remove(gwevent);
                } else {
                    done = false;
                }
            }
        }
        if (!done) {
            startEventProcessTimer(); //keeps the retry timer alive
        }
    }

    // Key for getting the OS callbacks
    // Might even want to refactor this out into a sensor class.
    public void registerPowerCallbacks() {
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        ifilter.addAction(Intent.ACTION_DOCK_EVENT);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_OFF);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_ON);
        ifilter.addAction(IntentConfig.INTENT_EXTRA_EVENT_MANUAL_WD);
        this.registerReceiver(mPowerActionReceiver, ifilter);
    }

    // Key for getting the network connectivity callbacks
    // Might even want to refactor this out into a sensor class.
    public void registerConnectivityCallbacks() {
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
            Log.w(PostAlertTaskdoInBackgroundTag, "PostAlertTask start");

            HttpClient httpclient = new DefaultHttpClient();

            try {
                // Execute the HTTP POST request
                @SuppressWarnings("unused")
                HttpResponse response = httpclient.execute(httpposts[0]);
                Log.d(PostAlertTaskdoInBackgroundTag, "POST response: " + response);
            } catch (ClientProtocolException e) {
                Log.e(PostAlertTaskdoInBackgroundTag, "ClientProtocol Exception, not attempting later delivery");
                e.printStackTrace();
            } catch (IOException e) {
                // Handle when the POST fails
                Log.e(PostAlertTaskdoInBackgroundTag, "IO Exception, not attempting later delivery");

                //TODO DUMB... this is repeated over and over... why?
                if (mAlertQ.size() > MAX_QUEUE_SIZE) {
                    mAlertQ.poll();
                    mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and head removed.", null);
                }

                if (mAlertQ.offer(httpposts[0]) == false) {
                    Log.e(PostAlertTaskdoInBackgroundTag, "Failed to add element to alertQ?");
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
            Log.d(ProcessAlertQTaskdoInBackgroundTag, "ProcessAlertQTask Start");

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
                    HttpResponse response = httpclient.execute(post); //TODO... check queing logic
                    Log.d(ProcessAlertQTaskdoInBackgroundTag, "POST response: " + response);
                }
            } catch (ClientProtocolException e) {
                Log.e(ProcessAlertQTaskdoInBackgroundTag, "ClientProtocolException, queuing for later delivery");
                e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(ProcessAlertQTaskdoInBackgroundTag, "IO Exception, queuing for later delivery");
                if (post == null) {
                    Log.e(ProcessAlertQTaskdoInBackgroundTag, "Caught post is null?");
                } else if (mAlertQ.offer(post) == false) {
                    // Worth noting the lack of offerFirst will put elements in
                    // the alertQ out of order w.r.t. when they first fired, but
                    // the server will re-order based on timestamp anyway
                    Log.e(ProcessAlertQTaskdoInBackgroundTag, "Failed to add element to alertQ?");
                }
            }
            return null;
        }
    }

    //The datastructure to be POSTED
    private static class TransmitterType {
        GridWatchEvent gw;
        Context context;
        TransmitterType(GridWatchEvent gw, Context context) {
            this.gw = gw;
            this.context = context;
        }
    }


    //Make network access async
    //This class does all HTTP POSTing of GW events. It contains much of the implementation of
    //settings logic as well.
    private class Transmitter extends AsyncTask<TransmitterType, Void, Void> {

        @Override
        protected Void doInBackground(TransmitterType... args) {

            GridWatchEvent gw = args[0].gw;
            Context context = args[0].context;
            postEvent(gw, context);
            Log.d(TransmitterdoInBackground, "STARTING TRANSMITTER");
            return null;
        }

        private void postEvent (GridWatchEvent gwevent, Context context) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> keys = sp.getAll();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(30);
            List<NameValuePair> msgValuePairs = new ArrayList<NameValuePair>(30);

            //HARD KILL any POST attempt if the user has not agreed to the consent
            if (!sp.getBoolean(SensorConfig.consent, false)) {
                return;
            }

            Log.d(TransmitterPostEvent, "postEvent Hit");

            String home_address = (String) keys.get("home_address_text");
            String id_text = (String) keys.get("id_text");
            String power_company_phone = (String) keys.get("power_company_phone");
            String power_company_name = (String) keys.get("power_company_name");
            String work_address = (String) keys.get("work_address_text");
            Boolean publicData = (Boolean) keys.get("make_data_public");
            Boolean power_company_update = (Boolean) keys.get("power_company_update");
            String gps_list_automatic = (String) keys.get("gps_list_automatic");
            String gpslist = (String) keys.get("gps_list");
            Boolean notifications_on_wd = (Boolean) keys.get("notifications_on_wd");

            //This is so hacky... should be changed in the settingsperferenceactivty class on event
            if (power_company_update != null) {
                msgValuePairs.add(new BasicNameValuePair("power_company_update", String.valueOf(power_company_update)));

            }
            if (id_text != null) { //if a user has changed their ID TODO this doesn't make a lot of sense as is. Login stuff isn't implemented
                if (!id_text.equals("ID")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_id", id_text));
                }
            }
            if (home_address != null) { //if the user provides their home address
                if (!home_address.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_home_adr", home_address));
                }
            }
            if (power_company_name != null) { //if the user provides the name of their power company
                if (!power_company_name.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_utility_name", power_company_name));
                }
            }
            if (power_company_phone != null) { //if the user provides their power company phone
                if (!power_company_phone.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_utility_phone", power_company_phone));
                }
            }
            if (work_address != null) { //if the user provides their work address
                if (!work_address.equals("Enter Here")) {
                    msgValuePairs.add(new BasicNameValuePair("pref_work_adr", work_address));
                }
            }
            if (publicData != null) { //if a user wants their data displayed
                msgValuePairs.add(new BasicNameValuePair("public", String.valueOf(publicData)));
            }
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String alertServerURL = settings.getString("alert_server", getString(R.string.default_alert_server));
            Location networkLocation = getLocationByProvider(LocationManager.NETWORK_PROVIDER);
            String android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

            // This next block does the GPS setting implementation. Should likely be moved to the GPSService sensor
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            //Manual and automatic reports have different levels of GPS access
            if (gwevent.getEventType().equals(GridWatchEventType.USR_PLUGGED.toString()) ||
                    gwevent.getEventType().equals(GridWatchEventType.USR_UNPLUGGED.toString())) {
                if (gpslist != null) {
                    if (gpslist.equals(SensorConfig.GPS_FINE)) {      //Manual
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    } else if (gpslist.equals(SensorConfig.GPS_HIGH)) {
                        criteria.setAccuracy(Criteria.ACCURACY_HIGH);
                    } else {
                        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
                    }
                }
            } else { //Automatic
                if (gps_list_automatic != null) {
                    if (gps_list_automatic.equals(SensorConfig.GPS_FINE)) {
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    } else if (gps_list_automatic.equals(SensorConfig.GPS_HIGH)) {
                        criteria.setAccuracy(Criteria.ACCURACY_HIGH);
                    } else {
                        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
                    }
                }
            }
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = getLocationByProvider(provider);

            //Adds in some metadata... Like often... this likely should be made into its own sensor class
            //topic=gridwatch&message=lat=10.2,lng=10,type=unplugged,house=rddfsad,time=1434402422000&key=event" "http://54.175.143.44:8080/message"
            msgValuePairs.add(new BasicNameValuePair("house", android_id));
            msgValuePairs.add(new BasicNameValuePair("phone_type", getDeviceName()));
            msgValuePairs.add(new BasicNameValuePair("os", "android"));
            msgValuePairs.add(new BasicNameValuePair("os_version", Build.VERSION.RELEASE));
            msgValuePairs.add(new BasicNameValuePair("failed", String.valueOf(gwevent.didFail())));

            //add the sensor stuff into the POST
            if (gwevent.needFFT()) {
                msgValuePairs.add(new BasicNameValuePair("FFT_CNT", gwevent.getFFTMessage()));
                msgValuePairs.add(new BasicNameValuePair("FFT_TYPE", gwevent.getmFFTType()));
            }
            if (gwevent.needCellInfo()) {
                msgValuePairs.add(new BasicNameValuePair("CELL_INFO", gwevent.getCellInfoMessage()));
            }
            if (gwevent.needSSIDs()) {
                msgValuePairs.add(new BasicNameValuePair("SSIDS", gwevent.getSSIDMessage()));
            }
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
            if (location != null) {
                msgValuePairs.add(new BasicNameValuePair("lat", String.valueOf(location.getLatitude())));
                msgValuePairs.add(new BasicNameValuePair("lng", String.valueOf(location.getLongitude())));
                msgValuePairs.add(new BasicNameValuePair("gps_accuracy", String.valueOf(location.getAccuracy())));
                msgValuePairs.add(new BasicNameValuePair("gps_time", String.valueOf(location.getTime())));
                msgValuePairs.add(new BasicNameValuePair("gps_altitude", String.valueOf(location.getAltitude())));
                msgValuePairs.add(new BasicNameValuePair("gps_speed", String.valueOf(location.getSpeed())));
            }
            if (networkLocation != null) {
                msgValuePairs.add(new BasicNameValuePair("network_latitude", String.valueOf(networkLocation.getLatitude())));
                msgValuePairs.add(new BasicNameValuePair("network_longitude", String.valueOf(networkLocation.getLongitude())));
                msgValuePairs.add(new BasicNameValuePair("network_accuracy", String.valueOf(networkLocation.getAccuracy())));
                msgValuePairs.add(new BasicNameValuePair("network_time", String.valueOf(networkLocation.getTime())));
                msgValuePairs.add(new BasicNameValuePair("network_altitude", String.valueOf(networkLocation.getAltitude())));
                msgValuePairs.add(new BasicNameValuePair("network_speed", String.valueOf(networkLocation.getSpeed())));
            }
            msgValuePairs.add(new BasicNameValuePair("network", getConnectionType(context)));
            String post_info = "";
            String notification_str = "";
            for (int i = 0; i < msgValuePairs.size(); i++) {
                NameValuePair item = msgValuePairs.get(i);
                post_info += item.getName() + "=" + item.getValue() + ","; //just for printing
                notification_str += item.getName() + " : " + item.getValue() + "\n";
            }
            notification_str = notification_str.substring(0, notification_str.length() - 1);
            String msg = post_info.substring(0, post_info.length() - 1);

            // The next three lines demonstrate a correctly formatted message
            //String msg = "house="+android_id+",time="+String.valueOf(gwevent.getTimeStampMS())+",";
            //msg += "lat="+String.valueOf(gpsLocation.getLatitude())+",lng=" + String.valueOf(gpsLocation.getLongitude());
            //msg += ",type="+gwevent.getEventType() ;
            Log.d(TransmitterPostEvent + "posting", msg);

            // Generate a notification about the post
            if (notifications_on_wd != null) {
                if (!notifications_on_wd && !gwevent.getEventType().equals("wd")) {
                    sendNotification(notification_str);
                } else {
                    sendNotification(notification_str);
                }
            } else {
                sendNotification(notification_str);
            }

            // Add in the headers for the GW servers
            nameValuePairs.add(new BasicNameValuePair("topic", "gridwatch"));
            nameValuePairs.add(new BasicNameValuePair("key", "event"));
            nameValuePairs.add(new BasicNameValuePair("message", msg));

            //To upload large audio files, this needs to be a multipart
            HttpPost httppost = new HttpPost(alertServerURL);
            try {
                UrlEncodedFormEntity postparams = new UrlEncodedFormEntity(nameValuePairs);
                httppost.setEntity(postparams);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            //Take a log
            mGWLogger.log(mDateFormat.format(new Date()), "event_post", post_info);

            //Schedule the post
            new PostAlertTask().execute(httppost);
        }
    }

    // TODO remove when GPS service is implemented
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

    // Setup the WD
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

    // TODO remove when the approprate sensor service is implemented
    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    // TODO remove when the approprate sensor service is implemented
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

    // TODO move to NetworkAndPhoneUtils
    public String getConnectionType(Context context) {
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

    // SEND A NOTIFICATION WITH REPORT
    private void sendNotification(String message) {
        Log.d(sendNotificationTag, "hit");


        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        Intent targetIntent = new Intent(this, GridWatchService.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        b.setContentIntent(pIntent);
        b.setSmallIcon(android.R.drawable.ic_dialog_alert);
        b.setTicker(message);
        b.setWhen(0);
        b.setAutoCancel(true);
        b.setContentTitle("GridWatch Outage Detected!");
        b.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        b.setContentText(message);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        b.setSound(defaultSoundUri);

        Notification n=b.build();
        n.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(4908, n);






        /*
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("GridWatch Outage Detected!")
                .setContentText(message)
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
        */
    }
}