package com.umich.gridwatch.GCM;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.umich.gridwatch.GridWatchService;
import com.umich.gridwatch.HomeActivity;
import com.umich.gridwatch.Utils.IntentConfig;

/**
 * Created by nklugman on 7/10/15.
 */
public class GCMListenerService extends GcmListenerService {

    private static final String TAG = "GcmListenerService";
    private static final String onMessageReceivedTag = TAG+":onMessageReceived";

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
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Intent intent = new Intent(GCMListenerService.this, GridWatchService.class);

        if (from.equals("/topics/sensors")) {
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

        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        //sendNotification(message);
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */

}
