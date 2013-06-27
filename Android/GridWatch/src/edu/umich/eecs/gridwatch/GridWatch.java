package edu.umich.eecs.gridwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class GridWatch extends Activity {
	TextView mChargerStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grid_watch);
		
		mChargerStatus = (TextView) findViewById(R.id.charger_status);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(null, ifilter);
		
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
			mChargerStatus.setText(R.string.power_ac);
		} else if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
			mChargerStatus.setText(R.string.power_usb);
		} else if (chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
			mChargerStatus.setText(R.string.power_wireless);
		} else {
			mChargerStatus.setText(R.string.power_none);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grid_watch, menu);
		return true;
	}

}
