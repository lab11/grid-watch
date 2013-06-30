package edu.umich.eecs.gridwatch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class GridWatch extends Activity implements SensorEventListener {
	TextView mChargerStatus;
	TextView mDockStatus;
	TextView mCurrentLocation;
	TextView mAccelStatus;
	
	LocationManager mLocationManager;
	
	SensorManager mSensorManager;
	Sensor mAccel;
	private float[][] mAccelHistory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grid_watch);
		
		mChargerStatus = (TextView) findViewById(R.id.charger_status);
		mDockStatus = (TextView) findViewById(R.id.dock_status);
		mCurrentLocation = (TextView) findViewById(R.id.current_location);
		mAccelStatus = (TextView) findViewById(R.id.accel_status);
		
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent intent = new Intent(this, GridWatchService.class);
		startService(intent);

		updateBattery();
		updateDock();
		updateLocation();
		updateAccel();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mSensorManager.unregisterListener(this);
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
		String provider = mLocationManager.getBestProvider(new Criteria(), false);
		if (provider == null) {
			mCurrentLocation.setText(R.string.loc_no_provider);
			return;
		}
		
		Location location = mLocationManager.getLastKnownLocation(provider);
		mCurrentLocation.setText("Provider: " + location.getProvider()
				+ "\nLat: " + location.getLatitude()
				+ "\nLon: " + location.getLongitude()
				+ "\nAccuracy: " + location.getAccuracy() + "m"
				+ "\nTime: " + location.getTime()
				);
	}
	
	private void updateAccel() {
		if (mAccel == null) {
			mAccelStatus.setText(R.string.accel_none);
			return;
		}
		
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}
	
	@Override
	public final void onSensorChanged(SensorEvent event) {
		String moved = getString(R.string.accel_static);
		
		if (mAccelHistory == null) {
			mAccelHistory = new float[10][3];
			
			for (int i=0; i<10; i++) {
				mAccelHistory[i][0] = event.values[0];
				mAccelHistory[i][1] = event.values[1];
				mAccelHistory[i][2] = event.values[2];
			}
		} else {
			int i = (int) ((event.timestamp / 500000000) % 10);
			mAccelHistory[i][0] = event.values[0];
			mAccelHistory[i][1] = event.values[1];
			mAccelHistory[i][2] = event.values[2];
			
			for (int j=0; j<10; j++) {
				if (Math.abs(mAccelHistory[j][0]-mAccelHistory[i][0]) > 2)
					moved = getString(R.string.accel_moved);
				if (Math.abs(mAccelHistory[j][1]-mAccelHistory[i][1]) > 2)
					moved = getString(R.string.accel_moved);
				if (Math.abs(mAccelHistory[j][2]-mAccelHistory[i][2]) > 2)
					moved = getString(R.string.accel_moved);
			}
		}
		
		mAccelStatus.setText(
				moved + "\n" +
				"x axis: " + event.values[0] + "\n" +
				"y axis: " + event.values[1] + "\n" +
				"z axis: " + event.values[2] + "\n" +
				"  time: " + (event.timestamp / 500000000) % 10 + "s"
		);
	}
}