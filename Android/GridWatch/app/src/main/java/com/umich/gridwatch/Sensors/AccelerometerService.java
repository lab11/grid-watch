package com.umich.gridwatch.Sensors;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

/**
 * Created by nklugman on 5/29/15.
 */
public class AccelerometerService extends IntentService implements SensorEventListener {
    private String onHandleIntentTag = "accelerometerService:onHandleIntent";
    private String onSensorChangedTag = "accelerometerService:onSensorChanged";

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private static long lastUpdate = 0;
    private static long firstTime = 0;
    private static float last_x, last_y, last_z;
    private static ResultReceiver resultReceiver;

    private static final long TIME_THRES = SensorConfig.ACCEL_SAMPLE_TIME_MS;
    private static final int SHAKE_THRESHOLD = SensorConfig.ACCEL_SHAKE_THRESHOLD;
    private static final int SAMPLE_WINDOW_SIZE = SensorConfig.ACCEL_SAMPLE_WINDOW_SIZE;

    public AccelerometerService() {
        super("AcceleromterService");
    }

    //TODO add in privacy settings here
    @Override
    protected void onHandleIntent(Intent workIntent) {
        firstTime = System.currentTimeMillis();
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        resultReceiver = workIntent.getParcelableExtra(IntentConfig.RECEIVER_KEY);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > SAMPLE_WINDOW_SIZE) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                   Log.w(onSensorChangedTag, "THRESHOLD HIT. ACCEL FAILED.");
                   senSensorManager.unregisterListener(this, senAccelerometer);
                   Bundle bundle = new Bundle();
                   bundle.putString(IntentConfig.RESULT_KEY, IntentConfig.RESULT_FAILED);
                   resultReceiver.send(IntentConfig.ACCELEROMETER, bundle);
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }

            if ((curTime - firstTime) > TIME_THRES) {
                Log.d(onSensorChangedTag, "ACCEL PASSED");
                senSensorManager.unregisterListener(this, senAccelerometer);
                Bundle bundle = new Bundle();
                bundle.putString(IntentConfig.RESULT_KEY, IntentConfig.RESULT_PASSED);
                resultReceiver.send(IntentConfig.ACCELEROMETER, bundle);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
