package edu.umich.eecs.gridwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootListener extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent intent = new Intent(arg0, GridWatchService.class);
        arg0.startService(intent);
        Log.i("GridWatchBootListener", "started");
	}

}
