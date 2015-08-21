package com.umich.gridwatch.Utils;

/**
 * Created by nklugman on 6/3/15.
 */
public class SensorConfig {

    //CONFIGURE THE ACCELEROMETER
    public final static int ACCEL_SAMPLE_TIME_MS = 5000;
    public final static int ACCEL_SHAKE_THRESHOLD = 100;
    public final static int ACCEL_SAMPLE_WINDOW_SIZE = 100;
    public final static Boolean ACCEL_ON = true;

    //CONFIGURE THE AUDIO RECORDING
    public final static int MICROPHONE_SAMPLE_TIME_MS = 10000;
    public final static int MICROPHONE_SAMPLE_FREQUENCY = 44100;
    public final static byte MICROPHONE_BIT_RATE = 16;
    public final static String recordingFileTmpName = "gw_tmp.raw";
    public final static String recordingFolder = "GW_recordings";
    public final static String recordingExtension = ".wav";
    public final static Boolean MICROPHONE_ON = false;

    //CONFIGURE THE GPS
    public final static int TWO_MINUTES = 1000 * 60 * 2;
    public final static int GPS_CURRENT_THRESH = TWO_MINUTES;
    public final static int ACCURACY_CHANGED_SIGNIFICANCE = 200;
    public final static String GPS_FINE = "0";
    public final static String GPS_HIGH = "1";

    //CONFIGURE THE FFT
    public final static Boolean LOCAL_FFT_BOOL = true;
    public final static int CENTER_FREQ = 60;
    public final static int NOTCH_SIZE = 5; // 5 hz +- center freq
    public final static Boolean FIRST_HARMONIC = false;
    public final static int NUM_FFT_HIT_CNT = 5;
    public final static int FFT_SAMPLE_TIME_MS = 5000;
    public final static Boolean FFT_ON = true;

    public final static String yes = "2";
    public final static String no = "1";
    public final static String always = "0";

    public final static Boolean debug = false;

    public final static String consent = "consent_agreement";
    public final static String wifi_or_network = "wifi_or_network";
    public final static String make_data_public = "make_data_public";

    public final static Boolean CELL_INFO_ON = true;
    public final static Boolean SSIDs_ON = true;

}
