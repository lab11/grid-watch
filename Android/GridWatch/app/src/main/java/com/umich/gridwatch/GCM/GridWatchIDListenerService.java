package com.umich.gridwatch.GCM;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by nklugman on 7/10/15.
 */
public class GridWatchIDListenerService  extends InstanceIDListenerService {
    private static final String TAG = GridWatchIDListenerService.class.getSimpleName();

    /**
     * Called if InstanceID token is updated.
     * May occur if the security of the previous token had been compromised
     * and is initiated by the InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        Log.e(TAG, "onTokenRefresh");

        // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        startService(new Intent(this, GCMIntentService.class));
    }
}