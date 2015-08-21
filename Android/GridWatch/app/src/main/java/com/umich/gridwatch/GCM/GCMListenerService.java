package com.umich.gridwatch.GCM;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.umich.gridwatch.GridWatchService;
import com.umich.gridwatch.Utils.IntentConfig;

/**
 * Created by nklugman on 7/10/15.
 */
public class GCMListenerService extends GcmListenerService {

    private static final String onMessageReceivedTag = "GcmListenerService:onMessageReceived";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(onMessageReceivedTag, "From: " + from);
        Log.d(onMessageReceivedTag, "Message: " + message);
        Intent intent = new Intent(GCMListenerService.this, GridWatchService.class);

        if (from.equals("/topics/sensors")) { //might want to route these different... right now sensors is the only topic. have to create a topic manually on the server first
            if (message.equals("FFT")) {
                Log.d(onMessageReceivedTag, "FFT Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_FFT);
            } else if (message.equals("ACCEL")) {
                Log.d(onMessageReceivedTag, "ACCEL Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_ACCEL);
            } else if (message.equals("GPS")) {
                Log.d(onMessageReceivedTag, "GPS Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_GPS);
            } else if (message.equals("WD")) {
                Log.d(onMessageReceivedTag, "WD Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_WD);
            } else if (message.equals("ASK")) {
                Log.d(onMessageReceivedTag, "ASK Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK);
            } else if (message.equals("MIC")) {
                Log.d(onMessageReceivedTag, "MIC Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_MIC);
            } else if (message.equals("ALL")) {
                Log.d(onMessageReceivedTag, "ALL Remotely Requested");
                intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_ALL);
            } else {
                Log.d(onMessageReceivedTag, "Unknown GCM request");
                return;
            }
            intent.putExtra(IntentConfig.INTENT_MANUAL_KEY, IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE);
        }
        startService(intent);
    }


}
