package com.umich.gridwatch.ReportTypes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Sensors.AccelerometerService;
import com.umich.gridwatch.Sensors.CellTowersService;
import com.umich.gridwatch.Sensors.FFTService;
import com.umich.gridwatch.Sensors.MicrophoneService;
import com.umich.gridwatch.Sensors.SSIDService;
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
    private boolean mSSIDsEnded = false;
    private boolean mCellInfoEnded = false;

    private boolean mGatheredExtrasFinished = false;
    private boolean mAccelStarted = false;
    private boolean mMicrophoneStarted = false;
    private boolean mSixtyHzStarted = false;
    private boolean mGatheredExtrasStarted = false;
    private boolean mAskDialogStarted = false;
    private boolean mSSIDsStarted = false;
    private boolean mCellInfoStarted = false;

    public boolean mHasBeenLogged = false; //hack for now

    private int cur_index;

    private Context mContext;
    private long mTimestamp;

    private static WorkEventResultReceiver m_resultReceiver;
    private static Intent intent;

    private boolean mGCMType = false;
    private boolean mGCMAskResult;

    private static String mResultMsg = "";
    private static String mFFTMsg = "";
    private static String mFFTType = "";
    private static String mSSIDs = "";
    private static String mCellInfo = "";
    private static boolean mFailed = false;

    private boolean needAccelerometerSamples () {
        switch (mEventType) {
            case INSTALL:
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
            case INSTALL:
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
            case INSTALL:
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
            case INSTALL:
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
    public boolean needSSIDs() {
        Log.d("NEED SSIDS", "HIT");
        switch (mEventType) {
            case GCM_ACCEL:
            case GCM_ASK:
            case INSTALL:
            case GCM_ALL:
                return true;
        }
        if (!SensorConfig.SSIDs_ON) {
            return false;
        }
        switch (mEventType) {
            case UNPLUGGED:
                return true;
        }
        return false;
    }
    public boolean needCellInfo() {
        switch (mEventType) {
            case GCM_ACCEL:
            case GCM_ASK:
            case INSTALL:
            case GCM_ALL:
                return true;
        }
        if (!SensorConfig.CELL_INFO_ON) {
            return false;
        }
        switch (mEventType) {
            case UNPLUGGED:
            case USR_UNPLUGGED:
                return true;
        }
        return false;
    }
    public boolean needOfflineLogging() {
        return true;
    }


    public String getTimeStampMS() {
        return String.valueOf(mTimestamp);
    }

    public GridWatchEvent (GridWatchEventType eventType, Context context)  {
        mEventType = eventType;
        mContext = context;
        mTimestamp = System.currentTimeMillis();

        //Gets Results Back From Sensors
        m_resultReceiver = new WorkEventResultReceiver(null);
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
        return mResultMsg;
    }

    public String getFFTMessage () { return mFFTMsg; }
    public String getmFFTType () { return mFFTType; }

    public String getSSIDMessage() {return mSSIDs; }

    public String getCellInfoMessage() {return mCellInfo;}

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
        if (needSSIDs() && !mSSIDsEnded) {
            if (!mSSIDsStarted) {
                Log.d(readyForTransmissionTag, "starting SSIDs");
                startSSIDs();
            } else {
                Log.d(readyForTransmissionTag, "not done with SSIDs yet");
            }
        }
        if (needCellInfo() && !mCellInfoEnded) {
            if (!mCellInfoStarted) {
                Log.d(readyForTransmissionTag, "starting Cell Info");
                startCellInfo();
            } else {
                Log.d(readyForTransmissionTag, "not done with Cell Info yet");
            }
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
    private void start60Hz() {
        mSixtyHzStarted = true;
        launch_service(FFTService.class, null);
    }

    private void startSSIDs() {
        mSSIDsStarted = true;
        launch_service(SSIDService.class, null);
    }

    private void startCellInfo() {
        mCellInfoEnded = true;
        launch_service(CellTowersService.class, null);
    }

    private void launch_service(Class a, String msg) {
        intent = new Intent(mContext, a);
        intent.putExtra(IntentConfig.RECEIVER_KEY, m_resultReceiver);
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
                    mResultMsg = "ACCELERATION";
                }
                mAccelFinished = true;
            }
            else if(resultCode == IntentConfig.MICROPHONE){
                Log.d(onReceiveResultTag, "MICROPHONE");
                if (!resultData.getString(IntentConfig.RESULT_KEY).equals(IntentConfig.RESULT_PASSED)) {
                    mFailed = true;
                    mResultMsg = "MICROPHONE";
                }
                mMicrophoneFinished = true;
            }
            else if(resultCode == IntentConfig.FFT){
                Log.d(onReceiveResultTag, "FFT");
                if (!resultData.getString(IntentConfig.RESULT_KEY).equals(IntentConfig.RESULT_PASSED)) {
                    mFailed = true;
                    mResultMsg = "FFT";
                }
                Log.d(onReceiveResultTag, "SIXTYHZ: " + String.valueOf(mSixtyHzFinished));
                mFFTMsg = resultData.getString(IntentConfig.FFT_CNT);
                mFFTType = resultData.getString(IntentConfig.FFT_TYPE);
                mSixtyHzFinished = true;
            }
            else if(resultCode == IntentConfig.ASK_DIALOG) {
                Log.d(onReceiveResultTag, "ASK");
                mAskDialogEnded = true;
            }
            else if (resultCode == IntentConfig.SSIDs) {
                Log.d(onReceiveResultTag, "SSIDs");
                mSSIDs = resultData.getString(IntentConfig.MESSAGE_KEY);
                mSSIDsEnded = true;
            }
            else if (resultCode == IntentConfig.CELL_INFO) {
                Log.d(onReceiveResultTag, "CELL_INFO");
                mCellInfo = resultData.getString(IntentConfig.MESSAGE_KEY);
                mCellInfoEnded = true;
            }
            else {
                Log.w(onReceiveResultTag, String.valueOf(resultCode));
            }
        }
    }
}
