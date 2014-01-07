package edu.umich.eecs.gridwatch;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.util.FloatMath;

public class GridWatchEvent {

	private GridWatchEventType mEventType;
	private long mTimestamp;

	private boolean mMoved = false;
	private boolean mSixtyHz = false;

	private final static float ACCEL_MAG_THRESHOLD = 0.3f;
	private final static long ACCEL_DURATION_NANOSECONDS = 5000000000l;
	private long mAccelFirstTime = 0l;
	private float mAccelMagLast = 0.0f;
	//private float mAccelMag = 0.0f;
	private boolean mAccelFinished = false;

	private boolean mSixtyHzFinished = false;

	SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);



	FileWriter ascFW;

	public GridWatchEvent (GridWatchEventType eventType) {
		mEventType = eventType;

		// Get the timestamp immediately after we determine
		// that an event happened.
		mTimestamp = System.currentTimeMillis();


/*
		File root = Environment.getExternalStorageDirectory();
		String now = mDateFormat.format(new Date());
//		File rawFile = new File(root, "microphone_" + now + ".s16");
		File asciiFile = new File(root, "microphone_" + now + ".dat");

		//FileOutputStream rawFW;

		try {
			//rawFW = new FileOutputStream(rawFile.getAbsolutePath());
			ascFW = new FileWriter(asciiFile.getAbsolutePath());

			ascFW.write("; Sample Rate 44100\n");
			ascFW.write("; Channels 1\n");
		} catch (IOException e){}*/
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
		if (mAccelMagLast == 0.0f) {
			mAccelMagLast = accelMagCurrent;
			return false;
		}
		float delta = Math.abs(accelMagCurrent - mAccelMagLast);
		mAccelMagLast = accelMagCurrent;
		//mAccelMag = (mAccelMag * 0.9f) + delta;

		if (delta > ACCEL_MAG_THRESHOLD) {
			// We detected movement, don't need any more samples
			mMoved = true;
			mAccelFinished = true;
			return true;
		}
		return false;

	}

	public boolean addMicrophoneSamples (short[] buffer, int len) {
/*
		try {
			for (int i=0; i<len; i++) {
				double index = (1.0 / 44100.0) * i;
				double val = buffer[i] / (double) Short.MAX_VALUE;
				//rawFW.write(buffer[i]);
				ascFW.write(String.valueOf(index) + " " + String.valueOf(val) + "\n");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		mSixtyHz = true;
		mSixtyHzFinished = true;

		return true;

	}

	// Returns true if this event is ready to be sent to the server,
	// false if it needs more data
	public boolean readyForTransmission () {
		switch (mEventType) {
		case PLUGGED:
			return true;
		case UNPLUGGED:
			if (!mAccelFinished) return false;
			if (!mSixtyHzFinished) return false;
			break;
		}
		return true;
	}

	public String getEventType () {
		return String.valueOf(mEventType).toLowerCase(Locale.US);
	}

	public long getTimestampMilli () {
		return mTimestamp;
	}

	// Get extra values we should send to the server, for example
	// whether or not the device moved when it was unplugged
	public List<NameValuePair> getNameValuePairs () {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(15);

		switch (mEventType) {
		case UNPLUGGED:
			nameValuePairs.add(new BasicNameValuePair("moved", String.valueOf(mMoved)));
			nameValuePairs.add(new BasicNameValuePair("sixty_hz", String.valueOf(mSixtyHz)));
			break;
		case PLUGGED:
			break;
		}

		return nameValuePairs;
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
