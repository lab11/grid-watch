package edu.umich.eecs.gridwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class GridWatch extends Activity {
	TextView mChargerStatus;
	TextView mDockStatus;
	TextView mCurrentLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grid_watch);
		
		mChargerStatus = (TextView) findViewById(R.id.charger_status);
		mDockStatus = (TextView) findViewById(R.id.dock_status);
		mCurrentLocation = (TextView) findViewById(R.id.current_location);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		updateBattery();
		updateDock();
		updateLocation();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grid_watch, menu);
		return true;
	}

	private void updateBattery() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(null, ifilter);
		if (batteryStatus == null) {
			mChargerStatus.setText(R.string.power_err);
			return;
		}
		
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
	
	private void updateDock() {
		mDockStatus.setText(R.string.dock_unsupported);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
			Intent dockStatus = registerReceiver(null, ifilter);
			if (dockStatus == null) {
				mDockStatus.setText(R.string.dock_err);
				return;
			}
			
			int dockState = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
			
			boolean isCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
			boolean isDesk =
					dockState == Intent.EXTRA_DOCK_STATE_DESK ||
					dockState == Intent.EXTRA_DOCK_STATE_HE_DESK ||
					dockState == Intent.EXTRA_DOCK_STATE_LE_DESK;
			
			if (isCar) {
				mDockStatus.setText(R.string.dock_car);
			} else if (isDesk) {
				mDockStatus.setText(R.string.dock_desk);
			} else {
				mDockStatus.setText(R.string.dock_none);
			}
		}
	}
	
	private void updateLocation() {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		String provider = locationManager.getBestProvider(new Criteria(), false);
		if (provider == null) {
			mCurrentLocation.setText(R.string.loc_no_provider);
			return;
		}
		
		Location location = locationManager.getLastKnownLocation(provider);
		mCurrentLocation.setText("Provider: " + location.getProvider()
				+ "\nLat: " + location.getLatitude()
				+ "\nLon: " + location.getLongitude()
				+ "\nAccuracy: " + location.getAccuracy() + "m"
				+ "\nTime: " + location.getTime()
				);
	}
	
	public void forceLocationUpdate(View view) {
		updateLocation();
	}
}