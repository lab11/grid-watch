package com.umich.gridwatch.GCM;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.umich.gridwatch.Chat.model.User;
import com.umich.gridwatch.Main;
import com.umich.gridwatch.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nklugman on 7/10/15.
 */
public class GCMIntentService extends IntentService {

    private static final String onHandleIntentTag = "RegIntentService:onHandleIntent";
    private static final String registerGCMTag = "RegIntentService:registerGCM";
    private static final String sendRegistrationToServerTag = "RegIntentService:sendRegistrationToServer";
    private static final String subscribeToTopicTag = "RegIntentService:subscribeToTopic";
    private static final String unsubscribeToTopicTag = "RegIntentService:unsubscribeToTopic";


    private static final String[] TOPICS = {"global", "sensors"};


        public GCMIntentService() { super("RegIntentService"); }

        @Override
        protected void onHandleIntent(Intent intent) {
            String key = intent.getStringExtra(GCMConfig.KEY);
            switch (key) {
                case GCMConfig.SUBSCRIBE:
                    // subscribe to a topic
                    subscribeToTopic(intent.getStringExtra(GCMConfig.TOPIC));
                    break;
                case GCMConfig.UNSUBSCRIBE:
                    unsubscribeFromTopic(intent.getStringExtra(GCMConfig.TOPIC));
                    break;
                default:
                    registerGCM();
            }


            /*
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            try {
                // In the (unlikely) event that multiple refresh operations occur simultaneously,
                // ensure that they are processed sequentially.
                synchronized ("RegIntentService") {
                    // [START register_for_gcm]
                    // Initially this call goes out to the network to retrieve the token, subsequent calls
                    // are local.
                    // [START get_token]
                    InstanceID instanceID = InstanceID.getInstance(this);
                    String token = instanceID.getToken(Private.gcm_sender_id,
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    // [END get_token]
                    Log.i(onHandleIntentTag, "GCM Registration Token: " + token);

                    // TODO: Implement this method to send any registration to your app's servers.
                    sendRegistrationToServer(token);

                    // Subscribe to topic channels
                    subscribeTopics(token);

                    // You should store a boolean that indicates whether the generated token has been
                    // sent to your server. If the boolean is false, send the token to your server,
                    // otherwise your server should have already received the token.
                    sharedPreferences.edit().putBoolean(GCMPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                    // [END register_for_gcm]
                }
            } catch (Exception e) {
                Log.d(onHandleIntentTag, "Failed to complete token refresh", e);
                // If an exception happens while fetching the new token or updating our registration data
                // on a third-party server, this ensures that we'll attempt the update at a later time.
                sharedPreferences.edit().putBoolean(GCMPreferences.SENT_TOKEN_TO_SERVER, false).apply();
            }
            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent registrationComplete = new Intent(GCMPreferences.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
            */
        }


    private void registerGCM() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.e(registerGCMTag, "GCM Registration Token: " + token);

            // sending the registration id to our server
            sendRegistrationToServer(token);
            sharedPreferences.edit().putBoolean(GCMConfig.SENT_TOKEN_TO_SERVER, true).apply();
        } catch (Exception e) {
            Log.e(registerGCMTag, "Failed to complete token refresh", e);
            sharedPreferences.edit().putBoolean(GCMConfig.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(GCMConfig.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

        /**
         * Persist registration to third-party servers.
         *
         * Modify this method to associate the user's GCM registration token with any server-side account
         * maintained by your application.
         *
         * @param token The new token.
         */
        private void sendRegistrationToServer(final String token) {

            // checking for valid login session
            User user = Main.getInstance().getPrefManager().getUser();
            if (user == null) {
                // TODO
                // user not found, redirecting him to login screen
                return;
            }

            String endPoint = GCMConfig.USER.replace("_ID_", user.getId());
            Log.e(sendRegistrationToServerTag, "endpoint: " + endPoint);

            StringRequest strReq = new StringRequest(Request.Method.PUT,
                    endPoint, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.e(sendRegistrationToServerTag, "response: " + response);

                    try {
                        JSONObject obj = new JSONObject(response);

                        // check for error
                        if (obj.getBoolean("error") == false) {
                            // broadcasting token sent to server
                            Intent registrationComplete = new Intent(GCMConfig.SENT_TOKEN_TO_SERVER);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(registrationComplete);
                        } else {
                            Toast.makeText(getApplicationContext(), "Unable to send gcm registration id to our sever. " + obj.getJSONObject("error").getString("message"), Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Log.e(sendRegistrationToServerTag, "json parsing error: " + e.getMessage());
                        Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    Log.e(sendRegistrationToServerTag, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                    Toast.makeText(getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("gcm_registration_id", token);
                    Log.e(sendRegistrationToServerTag, "params: " + params.toString());
                    return params;
                }
            };

            //Adding request to request queue
            Main.getInstance().addToRequestQueue(strReq);
        }


    public static void subscribeToTopic(String topic) {
        GcmPubSub pubSub = GcmPubSub.getInstance(Main.getInstance().getApplicationContext());
        InstanceID instanceID = InstanceID.getInstance(Main.getInstance().getApplicationContext());
        String token = null;
        try {
            token = instanceID.getToken(Main.getInstance().getApplicationContext().getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            if (token != null) {
                pubSub.subscribe(token, "/topics/" + topic, null);
                Log.e(subscribeToTopicTag, "Subscribed to topic: " + topic);
            } else {
                Log.e(subscribeToTopicTag, "error: gcm registration id is null");
            }
        } catch (IOException e) {
            Log.e(subscribeToTopicTag, "Topic subscribe error. Topic: " + topic + ", error: " + e.getMessage());
            Toast.makeText(Main.getInstance().getApplicationContext(), "Topic subscribe error. Topic: " + topic + ", error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void unsubscribeFromTopic(String topic) {
        GcmPubSub pubSub = GcmPubSub.getInstance(getApplicationContext());
        InstanceID instanceID = InstanceID.getInstance(getApplicationContext());
        String token = null;
        try {
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            if (token != null) {
                pubSub.unsubscribe(token, "");
                Log.e(unsubscribeToTopicTag, "Unsubscribed from topic: " + topic);
            } else {
                Log.e(unsubscribeToTopicTag, "error: gcm registration id is null");
            }
        } catch (IOException e) {
            Log.e(unsubscribeToTopicTag, "Topic unsubscribe error. Topic: " + topic + ", error: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Topic subscribe error. Topic: " + topic + ", error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}