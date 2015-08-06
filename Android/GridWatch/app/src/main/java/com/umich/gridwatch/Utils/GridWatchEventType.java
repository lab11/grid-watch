package com.umich.gridwatch.Utils;

/**
 * Created by nklugman on 5/29/15.
 */
public enum GridWatchEventType {
    UNPLUGGED,
    PLUGGED,
    USR_PLUGGED,
    USR_UNPLUGGED,
    WD,
    GCM, //used to filter intents by high level event type
    GCM_FFT,
    GCM_ACCEL,
    GCM_MIC,
    GCM_GPS,
    GCM_WD, //triggers wd event type
    GCM_MAP_GET, //triggers download of global map data
    GCM_ASK, //triggers notification asking if experienced power outage
    GCM_ASK_RESPONSE, //holds response for the ask
    GCM_ALL,
    DELETE
}


