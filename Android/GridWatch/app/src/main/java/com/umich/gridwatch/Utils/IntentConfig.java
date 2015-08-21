package com.umich.gridwatch.Utils;

/**
 * Created by nklugman on 6/4/15.
 */
public class IntentConfig {

    //CONSTANTS FOR INTENT PASSING / GETTING FROM GRIDWATCH EVENT
    public final static String RESULT_KEY = "RESULT";
    public final static String RESULT_PASSED = "PASSED";
    public final static String RESULT_FAILED = "FAILED";
    public final static String MESSAGE_KEY = "MSG";
    public final static String RECEIVER_KEY = "RECEIVER";
    public final static int ACCELEROMETER = 1001;
    public final static int MICROPHONE = 1002;
    public final static int GPS = 1003;
    public final static int FFT = 1004;
    public final static int ASK_DIALOG = 1005;
    public final static int CELL_INFO = 1006;
    public final static int SSIDs = 1007;

    //ADDITIONAL SENSOR SPECIFIC INTENTS
    public final static String FFT_CNT = "FFT_CNT";
    public final static String FFT_TYPE = "FFT_TYPE";
    public final static String INTENT_EXTRA_CELLPHONE_ID = "cell_id";

    //CONSTANTS FOR INTENTS THAT GENERATE A NEW EVENT
    public final static String INTENT_NAME = "GridWatch-update-event";
    public final static String INTENT_EXTRA_EVENT_TYPE = "event_type";
    public final static String INTENT_EXTRA_EVENT_INFO = "event_info";
    public final static String INTENT_EXTRA_EVENT_TIME = "event_time";
    public final static String INTENT_EXTRA_EVENT_MANUAL_ON = "event_manual_on";
    public final static String INTENT_EXTRA_EVENT_MANUAL_OFF = "event_manual_off";
    public final static String INTENT_EXTRA_EVENT_MANUAL_WD = "event_manual_wd";
    public final static String INTENT_MANUAL_KEY = "manual_state";

    public final static String INTENT_EXTRA_EVENT_GCM_ALL = "GCM_ALL";
    public final static String INTENT_EXTRA_EVENT_GCM_FFT = "GCM_FFT";
    public final static String INTENT_EXTRA_EVENT_GCM_GPS = "GCM_GPS";
    public final static String INTENT_EXTRA_EVENT_GCM_ACCEL = "GCM_ACCEL";
    public final static String INTENT_EXTRA_EVENT_GCM_MIC = "GCM_MIC";
    public final static String INTENT_EXTRA_EVENT_GCM_TYPE = "event_gcm_type";
    public final static String INTENT_EXTRA_EVENT_GCM_ASK = "GCM_ASK";
    public final static String INTENT_EXTRA_EVENT_GCM_WD = "GCM_WD";
    public final static String INTENT_EXTRA_EVENT_GCM_ASK_RESULT = "gcm_ask_result";
    public final static String INTENT_EXTRA_EVENT_GCM_ASK_INDEX = "gcm_ask_index";
    public final static String INTENT_EXTRA_EVENT_GCM_MAP = "GCM_MAP";


    //TO BE USED FOR ALL UI BEING AT THE HOME ACTIVITY
    public final static String INTENT_TO_HOME = "home_passback";

    //FOR DATA DELETION
    public final static String INTENT_DELETE_KEY = "delete_key";
    public final static String INTENT_DELETE = "delete";
    public final static String INTENT_DELETE_MSG = "delete_msg";



}
