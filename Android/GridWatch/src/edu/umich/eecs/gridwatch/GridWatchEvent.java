package edu.umich.eecs.gridwatch;

import android.util.FloatMath;

public class GridWatchEvent {

	private GridWatchEventType mEventType;
	private long mTimestamp;
	private boolean mMoved = false;

	private final static float ACCEL_MAG_THRESHOLD = 3.0f;
	private final static long ACCEL_DURATION_NANOSECONDS = 5000000000l;
	private long mAccelFirstTime = 0l;
	private float mAccelMagLast = 0.0f;
	private float mAccelMag = 0.0f;
	private boolean mAccelFinished = false;

	public GridWatchEvent (GridWatchEventType eventType) {
		mEventType = eventType;

		// Get the timestamp immediately after we determine
		// that an event happened.
		mTimestamp = System.currentTimeMillis();
	}

	// Call this with an accelerometer sample.
	// Returns true if we no longer need accelerometer samples.
	public boolean addAccelerometerSample (long timeNano, float x, float y, float z) {

		// If we have already detected movement we don't need any more samples.
		if (mMoved) return true;

		// If we don't need samples for this event type, tell the service
		// that we don't need any more samples.
		if (!needAccelerometerSamples()) return true;

		// Check if this sample is in the range of accelerometer samples
		// we are checking.
		if (mAccelFirstTime == 0) {
			mAccelFirstTime = timeNano;
		} else if (timeNano - mAccelFirstTime > ACCEL_DURATION_NANOSECONDS) {
			// It has been too long since our first sample we don't
			// need to continue checking any longer
			mAccelFinished = true;
			return true;
		}

		// Compare this sample to the previous and see if the device
		// has moved.
		float accelMagCurrent = FloatMath.sqrt(x*x + y*y + z*z);
		float delta = accelMagCurrent - mAccelMagLast;
		mAccelMagLast = accelMagCurrent;
		mAccelMag = (mAccelMag * 0.9f) + delta;

		if (mAccelMag > ACCEL_MAG_THRESHOLD) {
			// We detected movement, don't need any more samples
			mMoved = true;
			mAccelFinished = true;
			return true;
		}
		return false;

	}

	// Returns true if this event is ready to be sent to the server,
	// false if it needs more data
	public boolean readyForTransmission () {
		switch (mEventType) {
		case PLUGGED:
			return true;
		case UNPLUGGED:
			if (!mAccelFinished) return false;
			break;
		}
		return true;
	}

	public String getEventType () {
		return String.valueOf(mEventType).toLowerCase();
	}

	public long getTimestampMilli () {
		return mTimestamp;
	}

	// Based on the event type determine if we need the service
	// to hand us accelerometer samples.
	private boolean needAccelerometerSamples () {
		switch (mEventType) {
		case PLUGGED:
			return false;
		case UNPLUGGED:
			return true;
		}
		return false;
	}


}
