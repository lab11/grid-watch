package com.umich.gridwatch.ReportTypes;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.umich.gridwatch.Utils.GridWatchEventType;

import java.util.Locale;

/**
 * Created by nklugman on 5/29/15.
 */
public class GridWatchDeleteEvent {
    private String readyForTransmissionTag = "gridWatchDeleteEvent:readyForTransmission";
    private String onReceiveResultTag = "gridWatchDeleteEvent:onReceiveResult";

    private GridWatchEventType mEventType;

    private Context mContext;
    private long mTimestamp;
    private static String mResult_Msg = "";
    private String mHouse;

    private static Intent intent;

    private boolean mGCMType = false;
    private boolean mGCMAskResult;


    private static boolean mFailed = false;

    public GridWatchDeleteEvent(GridWatchEventType eventType, Context context, String msg)  {
        mEventType = eventType;
        mContext = context;
        mTimestamp = System.currentTimeMillis();
        mResult_Msg = msg;
        mHouse = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);


    }

    public String getTimeString() {
        return String.valueOf(mTimestamp);
    }

    public long getTime() {
        return mTimestamp;
    }

    public String getHouse() {
        return mHouse;
    }

    public String getMessage() {
        return mResult_Msg;
    }

    public boolean didFail() {
        return mFailed;
    }

    public boolean readyForTransmission(int i) {
        if (mFailed) return true;
        Log.d(readyForTransmissionTag, "Event Ready");
        return true;
    }

    public String getEventType () {
        return String.valueOf(mEventType).toLowerCase(Locale.US);
    }

}
