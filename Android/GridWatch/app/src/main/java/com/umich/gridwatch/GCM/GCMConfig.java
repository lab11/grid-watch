package com.umich.gridwatch.GCM;

/**
 * Created by nklugman on 6/25/15.
 */
public class GCMConfig {

    public static final String APP_SERVER_URL = "http://192.168.2.4:9000/gcm/gcm.php?shareRegId=true";
    public static final String GOOGLE_PROJ_ID = "837715578074";
    public static final String MSG_KEY = "m";

    // flag to identify whether to show single line
    // or multi line test push notification tray
    public static boolean appendNotificationMessages = true;

    // global topic to receive app wide push notifications
    public static final String TOPIC_GLOBAL = "global";
    public static final String TOPIC_MAPS = "maps";

    // broadcast receiver intent filters
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String PUSH_NOTIFICATION = "pushNotification";

    // type of push messages
    public static final int PUSH_TYPE_CHATROOM = 1;
    public static final int PUSH_TYPE_USER = 2;
    public static final int PUSH_TYPE_MAP_UPDATE = 3;


    // id to handle the notification in the notification try
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;

    public static final String BASE_URL = "http://ec2-54-175-208-137.compute-1.amazonaws.com/gcm_chat/v1";
    public static final String LOGIN = BASE_URL + "/user/login";
    public static final String CREATE_CATEGORY = BASE_URL + "/chat_rooms";
    public static final String USER = BASE_URL + "/user/_ID_";
    public static final String CHAT_ROOMS = BASE_URL + "/chat_rooms";
    public static final String CHAT_THREAD = BASE_URL + "/chat_rooms/_ID_";
    public static final String CHAT_ROOM_MESSAGE = BASE_URL + "/chat_rooms/_ID_/message";

    public static final String MAP_UPDATE_REQ = BASE_URL + "/get_alerts";

    public static final String KEY = "key";
    public static final String TOPIC = "topic";
    public static final String SUBSCRIBE = "subscribe";
    public static final String UNSUBSCRIBE = "unsubscribe";


}
