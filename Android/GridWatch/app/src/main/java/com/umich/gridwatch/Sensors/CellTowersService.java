package com.umich.gridwatch.Sensors;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.umich.gridwatch.Utils.IntentConfig;

import java.util.List;

/**
 * Created by nklugman on 7/24/15.
 */
public class CellTowersService extends IntentService  {
    private final static String onHandleIntentTag = "cellTowersService:onHandleIntent";
    private final static String getSampleTag = "cellTowersService:getSampleTag";

    private static ResultReceiver mResultReceiver;
    private static TelephonyManager mTelephonyManager;

    public CellTowersService() {
        super("CellTowersService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(onHandleIntentTag, "hit");
        mResultReceiver = workIntent.getParcelableExtra(IntentConfig.RECEIVER_KEY);
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        getSample();
    }

    //This needs to be tested... it is not catching all TelephonyManager network types...
    //also, I don't have 3g or 2g phones to test on...
    public void getSample() {
        int type = mTelephonyManager.getNetworkType();
        Log.d(getSampleTag, "getCellIDInfo-->         NetworkType = " + type);
        int phoneType = mTelephonyManager.getPhoneType();
        Log.d(getSampleTag, "getCellIDInfo-->         phoneType = " + phoneType);
        String towers = "";
        if (type == TelephonyManager.NETWORK_TYPE_GPRS              // GSM
                || type == TelephonyManager.NETWORK_TYPE_EDGE
                || type == TelephonyManager.NETWORK_TYPE_HSDPA)
        {
            GsmCellLocation gsm = ((GsmCellLocation) mTelephonyManager.getCellLocation());
            if (gsm != null)
            {
                int lac = gsm.getLac();
                String mcc = mTelephonyManager.getNetworkOperator().substring(0, 3);
                String mnc = mTelephonyManager.getNetworkOperator().substring(3, 5);
                int cid = gsm.getCid();
                towers += "gsm:"+gsm.getCid() + ":" + lac + ",";
            }
        }else {
            if (type == TelephonyManager.NETWORK_TYPE_CDMA        // cdma
                    || type == TelephonyManager.NETWORK_TYPE_1xRTT
                    || type == TelephonyManager.NETWORK_TYPE_EVDO_0
                    || type == TelephonyManager.NETWORK_TYPE_EVDO_A) {
                CdmaCellLocation cdma = (CdmaCellLocation) mTelephonyManager.getCellLocation();
                if (cdma != null) {
                    int lac = cdma.getNetworkId();
                    String mcc = mTelephonyManager.getNetworkOperator().substring(0, 3);
                    String mnc = String.valueOf(cdma.getSystemId());
                    int cid = cdma.getBaseStationId();
                    towers += "cdma:" + cdma.getNetworkId() + ":" + lac + ",";
                }
            } else {
                if (type == TelephonyManager.NETWORK_TYPE_LTE) {
                    //this won't work until API 17
                    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                    if (currentapiVersion >= 17) {
                        List<CellInfo> a = mTelephonyManager.getAllCellInfo();
                        if (a != null) {
                            for (int i = 0; i < a.size(); i++) {
                                try {
                                    CellInfoLte r = (CellInfoLte) a.get(i);
                                    towers += "lte:" + r.getCellIdentity().toString() + ",";
                                } catch (ClassCastException e) {
                                }
                            }
                        }

                    } else {
                        towers += "network_type:" + String.valueOf(type) + ",";
                    }

                } else {
                    towers += "network_type:" + String.valueOf(type) + ",";
                }
            }
        }
        if (towers.length() > 0 ) {
            towers = towers.substring(0, towers.length() - 1);
        } else {
            towers = "can't get towers";
        }
        Log.d(getSampleTag + " towers", towers);
        Bundle bundle = new Bundle();
        bundle.putString(IntentConfig.INTENT_EXTRA_CELLPHONE_ID, towers);
        mResultReceiver.send(IntentConfig.CELL_INFO, bundle);
    }
}