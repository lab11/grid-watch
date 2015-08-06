package com.umich.gridwatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.umich.gridwatch.ReportTypes.GridWatchDeleteEvent;
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
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by nklugman on 5/29/15.
 */

//TODO It doesn't seem that the wifi callbacks are working
public class GridWatchDeleteService extends Service {

    private final static String MANUAL = "manual";
    private final static String REAL = "real";

    private final static int MAX_QUEUE_SIZE = 20;

    // How long to wait between checks of the event list
    // for events that are finished and can be sent to the
    // server.
    private final static int EVENT_PROCESS_TIMER_PERIOD = 1000;

    // Debug Tags
    private static String errorTag = "error";
    private static String noteTag = "note";

    // List of all of the active events we are currently handling
    private ArrayList<GridWatchDeleteEvent> mEvents = new ArrayList<GridWatchDeleteEvent>();


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

    }


    public GridWatchDeleteEvent getEvent(int i) {
        return mEvents.get(i);
    }

    @Override
    public void onDestroy() {
        Log.d("GridWatchService", "service destroyed");
        mGWLogger.log(mDateFormat.format(new Date()), "destroyed", null);

        // Unregister us from different events
        this.unregisterReceiver(mConnectionListenerReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GridWatchDeleteService", "delete service started");
        mGWLogger.log(mDateFormat.format(new Date()), "deleted", null);

        if (intent != null && intent.getExtras() != null) {
            if (intent.getExtras().getString(IntentConfig.INTENT_DELETE_KEY).equals(IntentConfig.INTENT_DELETE)) {
                String msg = intent.getExtras().getString(IntentConfig.INTENT_DELETE_MSG);
                Log.d(noteTag, "delete requested");
                onDelete(msg);
            } else {
                Log.d(errorTag, "Unknown intent: " + intent.getAction());
            }
        }
        Log.w("GridWatchService:onStart", "hit");
        return START_STICKY;
    }

    // Handles the call when Internet connectivity is restored
    private BroadcastReceiver mConnectionListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
            if (cm == null) {
                return;
            }
            if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
                Log.w("CONNECTIVITY RESTORED", "hit");
                new ProcessAlertQTask().execute();
            }
        }
    };

    // Call to update the UI thread with data from this service
    private void broadcastIntent (Intent lIntent) {
        lIntent.setPackage("com.umich.gridwatch");
        sendBroadcast(lIntent);
    }

    private void onDelete(String msg) {
        Log.d(noteTag, "onDelete");
        GridWatchDeleteEvent gwDeleteEvent = new GridWatchDeleteEvent(GridWatchEventType.DELETE, this.getApplicationContext(), msg);
        mEvents.add(gwDeleteEvent);
        startEventProcessTimer();
    }

    // Iterate over the list of pending events and determine if any
    // should be transmitted to the server
    private void processEvents () {
        boolean done = true;
        for (int i = 0; i < mEvents.size(); i++) {
            GridWatchDeleteEvent gwDeleteEvent = mEvents.get(i);
            TransmitterType toTransmit = new TransmitterType(gwDeleteEvent, this.getApplicationContext());
            if (gwDeleteEvent.readyForTransmission(i)) {

                Transmitter transmitter = new Transmitter();
                transmitter.execute(toTransmit);

                /*
                //TODO Need to wait for GCM... should we have a different queue?
                Intent aIntent = new Intent(IntentConfig.INTENT_NAME);
                aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TIME, DateFormat.getTimeInstance().format(new Date()));
                aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_TYPE, "event_reject");
                aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_INFO, gwDeleteEvent.getMessage());
                broadcastIntent(aIntent);
                */
                mEvents.remove(gwDeleteEvent);
            } else {
                done = false;
            }
        }
        if (!done) {
            startEventProcessTimer();
        }
    }

    public void register_connectivity_callbacks() {
        IntentFilter cfilter = new IntentFilter();
        cfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
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
            Log.w("GridWatchDeleteService", "PostAlertTask start");

            HttpClient httpclient = new DefaultHttpClient();

            try {
                // Execute the HTTP POST request
                @SuppressWarnings("unused")
                HttpResponse response = httpclient.execute(httpposts[0]);
                Log.d("GridWatchDeleteService", "POST response: " + response);
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("GridWatchDeleteService", "IO Exception, not attempting later delivery");

                //TODO DUMB... this is repeated over and over... why?
                if (mAlertQ.size() > MAX_QUEUE_SIZE) {
                    mAlertQ.poll();
                    mGWLogger.log(mDateFormat.format(new Date()), "queue reached max and head removed.", null);
                }

                if (mAlertQ.offer(httpposts[0]) == false) {
                    Log.e("GridWatchDeleteService", "Failed to add element to alertQ?");
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
            Log.d("GridWatchDeleteService", "ProcessAlertQTask Start");

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
                Log.d("GridWatchDeleteService", "IO Exception, queuing for later delivery");
                if (post == null) {
                    Log.w("GridWatchDeleteService", "Caught post is null?");
                }

                else if (mAlertQ.offer(post) == false) {
                    Log.e("GridWatchDeleteService", "Failed to add element to alertQ?");
                }
            }
            return null;
        }
    }


    private static class TransmitterType {
        GridWatchDeleteEvent gwDeleteEvent;
        Context context;
        TransmitterType(GridWatchDeleteEvent gwDeleteEvent, Context context) {
            this.gwDeleteEvent = gwDeleteEvent;
            this.context = context;
        }
    }


    private class Transmitter extends AsyncTask<TransmitterType, Void, Void> {


        @Override
        protected Void doInBackground(TransmitterType... args) {

            GridWatchDeleteEvent gwDeleteEvent = args[0].gwDeleteEvent;
            Context context = args[0].context;
            postEvent(gwDeleteEvent, context);
            Log.w(noteTag, "STARTING TRANSMITTER");
            return null;
        }

        private void postEvent (GridWatchDeleteEvent gwDeleteEvent, Context context) {

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(30);
            List<NameValuePair> msgValuePairs = new ArrayList<NameValuePair>(30);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            String alertServerURL = settings.getString("alert_server", getString(R.string.default_alert_server));
            if (!settings.getBoolean(SensorConfig.consent, false)) {
                return;
            }

            //topic=gridwatch&message=lat=10.2,lng=10,type=unplugged,house=rddfsad,time=1434402422000&key=event" "http://54.175.143.44:8080/message"
            msgValuePairs.add(new BasicNameValuePair("house", gwDeleteEvent.getHouse()));
            msgValuePairs.add(new BasicNameValuePair("time", gwDeleteEvent.getTimeString()));
            msgValuePairs.add(new BasicNameValuePair("msg", gwDeleteEvent.getMessage()));
            String post_info = "";
            for (int i = 0; i < msgValuePairs.size(); i++) {
                NameValuePair item = msgValuePairs.get(i);
                post_info += item.getName() + "=" + item.getValue() + ","; //just for printing
            }
            String msg = post_info.substring(0,post_info.length()-1);

            Log.w("POSTING", msg);

            nameValuePairs.add(new BasicNameValuePair("topic", "gridwatch_delete"));
            nameValuePairs.add(new BasicNameValuePair("key", "event"));
            nameValuePairs.add(new BasicNameValuePair("message", msg));

            HttpPost httppost = new HttpPost(alertServerURL);
            try {
                UrlEncodedFormEntity postparams = new UrlEncodedFormEntity(nameValuePairs);
                httppost.setEntity(postparams);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            mGWLogger.log(mDateFormat.format(new Date()), "event_post_delete", post_info);
            new PostAlertTask().execute(httppost);
        }
    }



    @Override
    public IBinder onBind(Intent arg0) {
        // Service does not allow binding
        return null;
    }


}