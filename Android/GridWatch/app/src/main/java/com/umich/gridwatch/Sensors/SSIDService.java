package com.umich.gridwatch.Sensors;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Utils.IntentConfig;

import java.util.List;

/**
 * Created by nklugman on 7/24/15.
 */
public class SSIDService extends IntentService {
    private String onHandleIntentTag = "SSIDService:onHandleIntent";
    private String getSampleTag = "SSIDService:getSample";

    private static ResultReceiver mResultReceiver;

    private static WifiManager mWifi;

    public SSIDService() {
        super("SSIDService");
    }

    //TODO add in privacy settings here
    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(onHandleIntentTag, "hit");
        mResultReceiver = workIntent.getParcelableExtra(IntentConfig.RECEIVER_KEY);
        mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        getSample();
    }

    protected void getSample() {
        Log.d(getSampleTag, "hit");
        String ssids = "";
        if (mWifi.isWifiEnabled() != false) {
            List<ScanResult> results = mWifi.getScanResults();
            Log.d("ssids", results.toString());
            for (ScanResult result : results) {
                ssids += result.SSID + ":";
            }
        } else {
            Log.d(getSampleTag, "wifi not enabled");
        }
        if (ssids.length() > 0) {
            ssids = ssids.substring(0, ssids.length() - 1);
        } else {
            ssids = "none";
        }
        Bundle bundle = new Bundle();
        bundle.putString(IntentConfig.MESSAGE_KEY, ssids);
        mResultReceiver.send(IntentConfig.SSIDs, bundle);
    }
}