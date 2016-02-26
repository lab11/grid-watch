package com.umich.gridwatch.GCM;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;
import com.umich.gridwatch.Chat.activity.ChatRoomActivity;
import com.umich.gridwatch.Chat.helper.NotificationUtils;
import com.umich.gridwatch.Chat.model.Message;
import com.umich.gridwatch.Chat.model.User;
import com.umich.gridwatch.GridWatchService;
import com.umich.gridwatch.HomeActivity;
import com.umich.gridwatch.Main;
import com.umich.gridwatch.Utils.IntentConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nklugman on 7/10/15.
 */
public class GCMListenerService extends GcmListenerService {

    private static final String onMessageReceivedTag = "GcmListenerS:onMessageReceived";
    private static final String processChatRoomPushTag = "GcmListenerS:processChatRoomPush";
    private static final String processUserMessageTag = "GcmListenerS:processUserMessageTag";

    private NotificationUtils notificationUtils;

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

        String title = data.getString("title");
        Boolean isBackground = Boolean.valueOf(data.getString("is_background"));
        String flag = data.getString("flag");
        String data_str = data.getString("data");
        Log.d(onMessageReceivedTag, "From: " + from);
        Log.d(onMessageReceivedTag, "title: " + title);
        Log.d(onMessageReceivedTag, "isBackground: " + isBackground);
        Log.d(onMessageReceivedTag, "flag: " + flag);
        Log.d(onMessageReceivedTag, "data: " + data_str);

        if (flag == null)
            return;

        if(Main.getInstance().getPrefManager().getUser() == null){
            // user is not logged in, skipping push notification
            Log.e(onMessageReceivedTag, "user is not logged in, skipping push notification");
            return;
        }

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
            startService(intent);
        } else {
            switch (Integer.parseInt(flag)) {
                case GCMConfig.PUSH_TYPE_CHATROOM:
                    // push notification belongs to a chat room
                    processChatRoomPush(title, isBackground, data_str);
                    break;
                case GCMConfig.PUSH_TYPE_USER:
                    // push notification is specific to user
                    processUserMessage(title, isBackground, data_str);
                    break;
                case GCMConfig.PUSH_TYPE_MAP_UPDATE:
                    intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_TYPE, IntentConfig.INTENT_EXTRA_EVENT_GCM_MAP_UPDATE);
                    intent.putExtra(IntentConfig.INTENT_MANUAL_KEY, IntentConfig.INTENT_EXTRA_EVENT_GCM_MAP_UPDATE);
                    intent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_MAP_POINT_KEY, data_str);
                    startService(intent);
            }
        }
    }




    /**
     * Processing chat room push message
     * this message will be broadcasts to all the activities registered
     * */
    private void processChatRoomPush(String title, boolean isBackground, String data) {
        if (!isBackground) {

            try {
                JSONObject datObj = new JSONObject(data);

                String chatRoomId = datObj.getString("chat_room_id");

                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();
                message.setMessage(mObj.getString("message"));
                message.setId(mObj.getString("message_id"));
                message.setCreatedAt(mObj.getString("created_at"));

                JSONObject uObj = datObj.getJSONObject("user");

                // skip the message if the message belongs to same user as
                // the user would be having the same message when he was sending
                // but it might differs in your scenario
                if (uObj.getString("user_id").equals(Main.getInstance().getPrefManager().getUser().getId())) {
                    Log.e(processChatRoomPushTag, "Skipping the push message as it belongs to same user");
                    return;
                }

                User user = new User();
                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);

                // verifying whether the app is in background or foreground
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {

                    // app is in foreground, broadcast the push message
                    Intent pushNotification = new Intent(GCMConfig.PUSH_NOTIFICATION);
                    pushNotification.putExtra("type", GCMConfig.PUSH_TYPE_CHATROOM);
                    pushNotification.putExtra("message", message);
                    pushNotification.putExtra("chat_room_id", chatRoomId);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                    // play notification sound
                    NotificationUtils notificationUtils = new NotificationUtils();
                    notificationUtils.playNotificationSound();
                } else {

                    // app is in background. show the message in notification try
                    Intent resultIntent = new Intent(getApplicationContext(), ChatRoomActivity.class);
                    resultIntent.putExtra("chat_room_id", chatRoomId);
                    showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                }

            } catch (JSONException e) {
                Log.e(processChatRoomPushTag, "json parsing error: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            // the push notification is silent, may be other operations needed
            // like inserting it in to SQLite
        }
    }

    /**
     * Processing user specific push message
     * It will be displayed with / without image in push notification tray
     * */
    private void processUserMessage(String title, boolean isBackground, String data) {
        if (!isBackground) {

            try {
                JSONObject datObj = new JSONObject(data);

                String imageUrl = datObj.getString("image");

                JSONObject mObj = datObj.getJSONObject("message");
                Message message = new Message();
                message.setMessage(mObj.getString("message"));
                message.setId(mObj.getString("message_id"));
                message.setCreatedAt(mObj.getString("created_at"));

                JSONObject uObj = datObj.getJSONObject("user");
                User user = new User();
                user.setId(uObj.getString("user_id"));
                user.setEmail(uObj.getString("email"));
                user.setName(uObj.getString("name"));
                message.setUser(user);

                // verifying whether the app is in background or foreground
                if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {

                    // app is in foreground, broadcast the push message
                    Intent pushNotification = new Intent(GCMConfig.PUSH_NOTIFICATION);
                    pushNotification.putExtra("type", GCMConfig.PUSH_TYPE_USER);
                    pushNotification.putExtra("message", message);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

                    // play notification sound
                    NotificationUtils notificationUtils = new NotificationUtils();
                    notificationUtils.playNotificationSound();
                } else {

                    // app is in background. show the message in notification try
                    Intent resultIntent = new Intent(getApplicationContext(), HomeActivity.class);

                    // check for push notification image attachment
                    if (TextUtils.isEmpty(imageUrl)) {
                        showNotificationMessage(getApplicationContext(), title, user.getName() + " : " + message.getMessage(), message.getCreatedAt(), resultIntent);
                    } else {
                        // push notification contains image
                        // show it with the image
                        showNotificationMessageWithBigImage(getApplicationContext(), title, message.getMessage(), message.getCreatedAt(), resultIntent, imageUrl);
                    }
                }
            } catch (JSONException e) {
                Log.e(processUserMessageTag, "json parsing error: " + e.getMessage());
                Toast.makeText(getApplicationContext(), "Json parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            // the push notification is silent, may be other operations needed
            // like inserting it in to SQLite
        }
    }

    /**
     * Showing notification with text only
     * */
    private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
    }

    /**
     * Showing notification with text and image
     * */
    private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
    }


}
