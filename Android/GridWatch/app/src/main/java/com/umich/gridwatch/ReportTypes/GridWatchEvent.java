package com.umich.gridwatch.ReportTypes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Sensors.AccelerometerService;
import com.umich.gridwatch.Sensors.FFTService;
import com.umich.gridwatch.Sensors.MicrophoneService;
import com.umich.gridwatch.Utils.GridWatchEventType;
import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

import java.util.Locale;

/**
 * Created by nklugman on 5/29/15.
 */
public class GridWatchEvent {
    private String readyForTransmissionTag = "gridWatchEvent:readyForTransmission";
    private String onReceiveResultTag = "gridWatchEvent:onReceiveResult";

    private GridWatchEventType mEventType;
    private boolean mAccelFinished = false;
    private boolean mSixtyHzFinished = false;
    private boolean mMicrophoneFinished = false;
    private boolean mAskDialogEnded = false;

    private boolean mGatheredExtrasFinished = false;
    private boolean mAccelStarted = false;
    private boolean mMicrophoneStarted = false;
    private boolean mSixtyHzStarted = false;
    private boolean mGatheredExtrasStarted = false;
    private boolean mAskDialogStarted = false;

    private int cur_index;

    private Context mContext;
    private long mTimestamp;

    private static WorkEventResultReceiver resultReceiver;
    private static Intent intent;

    private boolean mGCMType = false;
    private boolean mGCMAskResult;

    private boolean mLocalFFT;

    private static String recordingFileName = "";

    private static String mResult_Msg = "";
    private static String mFFT_Msg = "";


    private static boolean mFailed = false;

    private boolean needAccelerometerSamples () {
        switch (mEventType) {
            case GCM_ACCEL:
            case GCM_ASK:
            case GCM_ALL:
                return true;
        }
        if (!SensorConfig.ACCEL_ON) {
            return false;
        }
        switch (mEventType) {
            case UNPLUGGED:
                return true;
        }
        return false;
    }
    private boolean needMicrophoneSamples () {
        switch (mEventType) {
            case GCM_MIC:
            //case GCM_ASK:
            case GCM_ALL:
                return true;
        }
        if (!SensorConfig.MICROPHONE_ON) {
            return false;
        }
        switch (mEventType) {
            case PLUGGED:
            case USR_PLUGGED:
            case UNPLUGGED:
            case USR_UNPLUGGED:
                return true;
        }
        return false;
    }
    public boolean needFFT() {
        switch (mEventType) {
            case GCM_FFT:
            case GCM_ASK:
            case GCM_ALL:
                return true;
        }
        if (!SensorConfig.FFT_ON) {
            return false;
        }
        switch (mEventType) {
            case PLUGGED:
            case USR_PLUGGED:
            case UNPLUGGED:
            case USR_UNPLUGGED:
                return true;
        }
        return false;
    }
    private boolean needGPS() {
        switch (mEventType){
            case GCM_GPS:
            case GCM_ASK:
            case GCM_ALL:
                return true;
        }
        return false;
    }
    private boolean needAskDialogResponse() {
        switch (mEventType){
            case GCM_ASK:
                return true;
        }
        return false;
    }


    public String getTimeStampMS() {
        return String.valueOf(mTimestamp);
    }

    public GridWatchEvent (GridWatchEventType eventType, Context context)  {
        mEventType = eventType;
        mContext = context;
        mTimestamp = System.currentTimeMillis();

        //Gets Results Back From Sensors
        resultReceiver = new WorkEventResultReceiver(null);
    }

    public void setGCMAskResult(boolean result) {
        Log.d("setGCMAskResult", "hit");
        mGCMAskResult = result;
        mAskDialogEnded = true;
    }
    public boolean getGCMAskResult() { return mGCMAskResult; }
    public void setGCMType(boolean type) { mGCMType = type; }

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    public String getTime() {
        return String.valueOf(mTimestamp);
    }

    public String getFailureMessage() {
        return mResult_Msg;
    }

    public String getFFTMessage () { return mFFT_Msg; }

    public boolean didFail() {
        return mFailed;
    }

    public boolean readyForTransmission(int i) {
        cur_index = i; //used to coordinate ask events
        if (mFailed) return true;
        if (needAccelerometerSamples() && !mAccelFinished) {
            if (!mAccelStarted) {
                Log.d(readyForTransmissionTag, "starting accel");
                startAccelerometer();
            } else {
                Log.d(readyForTransmissionTag, "not done with accel yet");
            }
            return false;
        }
        if (needAskDialogResponse() && !mAskDialogEnded) {
            if (!mAskDialogStarted) {
                Log.d(readyForTransmissionTag, "launching dialog");
                startAskDialog();
            }
            else {
                Log.d(readyForTransmissionTag, "not done with ask dialog yet");
            }
            return false;
        }
        if (needMicrophoneSamples() && !mMicrophoneFinished) {
            if (!mMicrophoneStarted) {
                Log.d(readyForTransmissionTag, "starting audio recording");
                startAudioRecording();
            } else {
                Log.d(readyForTransmissionTag, "not done with audio recording yet");
            }
            return false;
        }
        if (needFFT() && !mSixtyHzFinished) {
            if (!mSixtyHzStarted) {
                Log.d(readyForTransmissionTag, "starting FFT");
                start60Hz();
            }  else {
                Log.d(readyForTransmissionTag, "not done with FFT yet");
            }
            return false;
        }
        if (!mGatheredExtrasFinished) {
            if (!mGatheredExtrasStarted) {
                Log.d(readyForTransmissionTag, "starting gathering extras");
                startGatheringExtras();
            } else {
                Log.d(readyForTransmissionTag, "not done gathering extras");
            }
            return false;
        }
        Log.d(readyForTransmissionTag, "Event Ready");
        return true;
    }

    public String getEventType () {
        return String.valueOf(mEventType).toLowerCase(Locale.US);
    }

    private void startAskDialog() {
        //the ask dialog is handled at the homeactivity for now... this is in an attempt to keep all dialogs on UI activity
        mAskDialogStarted = true;
        Intent aIntent = new Intent(IntentConfig.INTENT_NAME);
        aIntent.putExtra(IntentConfig.INTENT_TO_HOME, IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK);
        aIntent.putExtra(IntentConfig.INTENT_EXTRA_EVENT_GCM_ASK_INDEX, cur_index);
        aIntent.setPackage("com.umich.gridwatch");
        mContext.sendBroadcast(aIntent);
        Log.w("PROCESSING EVENTS", "GCM_ASK");
    }

    private void startAccelerometer() {
        mAccelStarted = true;
        launch_service(AccelerometerService.class, null);
    }

    public void startAudioRecording() {
        mMicrophoneStarted = true;
        launch_service(MicrophoneService.class, null);
    }

    //TODO public function for testing... restrict later
    //TODO add in parameterizable FFT...
    public void start60Hz() {
        mSixtyHzStarted = true;
        launch_service(FFTService.class, null);
    }


    private void launch_service(Class a, String msg) {
        intent = new Intent(mContext, a);
        intent.putExtra(IntentConfig.RECEIVER_KEY, resultReceiver);
        if (msg != null) {
            intent.putExtra(IntentConfig.MESSAGE_KEY, msg);
        }
        mContext.startService(intent);
    }

    //TODO not yet implemented... this is the hook for things like WIFI SSID and such and such
    private void startGatheringExtras() {
        mGatheredExtrasStarted = true;
        mGatheredExtrasFinished = true;
    }

    class WorkEventResultReceiver extends ResultReceiver
    {
        public WorkEventResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == IntentConfig.ACCELEROMETER){
                Log.d(onReceiveResultTag, "ACCEL");
                if (!resultData.getString(IntentConfig.RESULT_KEY).equals(IntentConfig.RESULT_PASSED)) {
                    mFailed = true;
                    mResult_Msg = "ACCELERATION";
                }
                mAccelFinished = true;
            }
            else if(resultCode == IntentConfig.MICROPHONE){
                Log.d(onReceiveResultTag, "MICROPHONE");
                if (!resultData.getString(IntentConfig.RESULT_KEY).equals(IntentConfig.RESULT_PASSED)) {
                    mFailed = true;
                    mResult_Msg = "MICROPHONE";
                }
                recordingFileName = resultData.getString(IntentConfig.MESSAGE_KEY);
                mMicrophoneFinished = true;
            }
            else if(resultCode == IntentConfig.FFT){
                Log.d(onReceiveResultTag, "FFT");
                if (!resultData.getString(IntentConfig.RESULT_KEY).equals(IntentConfig.RESULT_PASSED)) {
                    mFailed = true;
                    mResult_Msg = "FFT";
                }
                Log.d(onReceiveResultTag, "SIXTYHZ: " + String.valueOf(mSixtyHzFinished));
                mFFT_Msg = resultData.getString(IntentConfig.FFT_CNT);
                mSixtyHzFinished = true;
            }
            else if(resultCode == IntentConfig.ASK_DIALOG) {
                Log.d(onReceiveResultTag, "ASK");
                mAskDialogEnded = true;
            }
            else{
                Log.w(onReceiveResultTag, String.valueOf(resultCode));
            }
        }
    }
}
