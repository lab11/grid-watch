package com.umich.gridwatch.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.umich.gridwatch.GridWatchService;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent intent = new Intent(arg0, GridWatchService.class);
        arg0.startService(intent);        
	}
}
