package com.umich.gridwatch.Chat.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.umich.gridwatch.Chat.model.ChatRoom;
import com.umich.gridwatch.Chat.model.User;
import com.umich.gridwatch.Utils.SensorConfig;

public class ChatPreferenceManager {

    private String TAG = ChatPreferenceManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    SharedPreferences.Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "chat";

    // All Shared Preferences Keys
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_COUNTRY = "user_country";
    private static final String KEY_USER_CITY = "user_city";
    private static final String KEY_USER_STATE = "user_state";

    private static final String KEY_CHATROOM_ID = "chatroom_id";
    private static final String KEY_CHATROOM_SUBSCRIBED = "chatroom_subscribed";

    private static final String KEY_NOTIFICATIONS = "notifications";

    // Constructor
    public ChatPreferenceManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }


    public void storeUser(User user) {
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.commit();

        Log.e(TAG, "User is stored in shared preferences. " + user.getName() + ", " + user.getEmail());
    }

    public void storeChatRoom(ChatRoom chatRoom) {
        editor.putString(KEY_CHATROOM_ID, chatRoom.getId());
        editor.putBoolean(KEY_CHATROOM_SUBSCRIBED, chatRoom.getSubscribed());
        editor.commit();
        Log.e(TAG, "Chatroom is stored in shared preferences. " + chatRoom.getName() + ", " + chatRoom.getSubscribed());
    }

    public boolean getChatroomSubscribed() {
        if (pref.getString(KEY_CHATROOM_ID, null) != null) {
            return pref.getBoolean(KEY_CHATROOM_SUBSCRIBED, false);
        }
        return false;
    }

    public void storeConsent(boolean a) {
        editor.putBoolean(SensorConfig.consent, a);
        editor.commit();
    }

    public User getUser() {
        if (pref.getString(KEY_USER_ID, null) != null) {
            String id, name, email,country,city,lat,lng,state;
            id = pref.getString(KEY_USER_ID, null);
            name = pref.getString(KEY_USER_NAME, null);
            email = pref.getString(KEY_USER_EMAIL, null);
            country = pref.getString(KEY_USER_COUNTRY,null);
            city = pref.getString(KEY_USER_CITY,null);
            state = pref.getString(KEY_USER_STATE,null);

            User user = new User(id, name, email,country,city,state);
            return user;
        }
        return null;
    }

    public void addNotification(String notification) {

        // get old notifications
        String oldNotifications = getNotifications();

        if (oldNotifications != null) {
            oldNotifications += "|" + notification;
        } else {
            oldNotifications = notification;
        }

        editor.putString(KEY_NOTIFICATIONS, oldNotifications);
        editor.commit();
    }

    public String getNotifications() {
        return pref.getString(KEY_NOTIFICATIONS, null);
    }

    public void logout() {
        editor.remove(KEY_USER_ID);
        editor.commit();
    }

    public void clear() {
        editor.clear();
        editor.commit();
    }
}
