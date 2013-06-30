package edu.umich.eecs.gridwatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class GridWatchService extends Service implements SensorEventListener {
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
				onPowerConnected();
			else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
				onPowerDisconnected();
			else if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT))
				onDockEvent(intent);
			else
				Log.d("GridWatchService", "Unknown intent: " + intent.getAction());
		}
	};
	
	private SensorManager mSensorManager;
	private Sensor mAccel;
	private long mAccelFirstTime;
	private float[][] mAccelHistory;
	
	private boolean mDockCar = false;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// Service does not allow binding
		return null;
	}

	@Override
	public void onCreate() {
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
		ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		ifilter.addAction(Intent.ACTION_DOCK_EVENT);
		this.registerReceiver(mBroadcastReceiver, ifilter);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		Log.d("GridWatchService", "service started");
		Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onDestroy() {
		this.unregisterReceiver(mBroadcastReceiver);
		Log.d("GridWatchService", "service destroyed");
		Toast.makeText(this, "service destroyed", Toast.LENGTH_SHORT).show();
	}
	
	private void onPowerConnected() {
		Log.d("GridWatchService", "onPowerConnected called");
		mSensorManager.unregisterListener(this);
		mAccelHistory = null;
	}
	
	private void onPowerDisconnected() {
		Log.d("GridWatchService", "onPowerDisconnected called");
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	private void onDockEvent(Intent intent) {
		int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
		mDockCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		Log.d("GridWatchService", "mDockCar set to " + mDockCar);
	}
	
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// We don't really care about sensor accuracy that much; ignore
	}
	
	@Override
	public final void onSensorChanged(SensorEvent event) {
		boolean moved = false;
		
		if (mAccelHistory == null) {
			Log.d("GridWatchService", "first sensor event");
			mAccelHistory = new float[10][3];
			mAccelFirstTime = event.timestamp;
			
			for (int i=0; i<10; i++) {
				mAccelHistory[i][0] = event.values[0];
				mAccelHistory[i][1] = event.values[1];
				mAccelHistory[i][2] = event.values[2];
			}
		} else {
			int i = (int) (((event.timestamp-mAccelFirstTime) / 500000000) % 10);
			Log.d("GridWatchService", "sensor event i=" + i);
			mAccelHistory[i][0] = event.values[0];
			mAccelHistory[i][1] = event.values[1];
			mAccelHistory[i][2] = event.values[2];
			
			for (int j=0; j<10; j++) {
				if (Math.abs(mAccelHistory[j][0]-mAccelHistory[i][0]) > 2)
					moved = true;
				if (Math.abs(mAccelHistory[j][1]-mAccelHistory[i][1]) > 2)
					moved = true;
				if (Math.abs(mAccelHistory[j][2]-mAccelHistory[i][2]) > 2)
					moved = true;
			}
			
			if (moved) {
				// Once we've determined we've moved we can bail on this unplug event
				mSensorManager.unregisterListener(this);
				mAccelHistory = null;
				Log.d("GridWatchService", "sample " + i + " found movement");
				Toast.makeText(this, "Sample " + i + " found movement", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (i == 9) {
				// Got 5 seconds worth of data, that's enough
				mSensorManager.unregisterListener(this);
				mAccelHistory = null;
				
				if (!moved) {
					Log.d("GridWatchService", "no movement found, should trigger");
					Toast.makeText(this, "Should trigger", Toast.LENGTH_SHORT).show();
				} else {
					Log.d("GridWatchService", "now how'd we get here?");
				}
			}
		}

	}
}
