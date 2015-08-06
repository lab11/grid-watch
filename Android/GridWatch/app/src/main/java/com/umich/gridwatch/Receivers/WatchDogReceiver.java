package com.umich.gridwatch.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.umich.gridwatch.GridWatchService;


public class WatchDogReceiver extends BroadcastReceiver {
	private final static String INTENT_EXTRA_EVENT_MANUAL_WD = "event_manual_wd";
	private final static String INTENT_MANUAL_KEY = "manual_state";
	private static String noteTag = "note";

	
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, GridWatchService.class);
		service.putExtra(INTENT_MANUAL_KEY, INTENT_EXTRA_EVENT_MANUAL_WD);
        context.startService(service);
        Log.w(noteTag, "Called context.startService from AlarmReceiver.onReceive");
    }
}