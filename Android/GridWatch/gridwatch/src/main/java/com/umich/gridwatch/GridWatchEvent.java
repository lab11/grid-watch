package com.umich.gridwatch;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.FloatMath;
import android.util.Log;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GridWatchEvent {

    private AudioRecord mRecorder = null;
    private final static int SAMPLE_FREQUENCY = 44100;
    private final static int BIT_RATE = 16;
    private final static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private final static int RECORDER_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int RECORDER_TIME = 10000; //ms
    private static String recordingFileName = null;
    private final static String recordingFileTmpName = "gw_tmp.raw";
    private final static String recordingFolder = "GW_recordings";
    private final static String recordingExtension = ".wav";

    private boolean debug = false;
    private boolean recording_on = false;


    private GridWatchEventType mEventType;
	private long mTimestamp;

    private String accelMagCSV = "";


	public boolean mMoved = false;
	private boolean mSixtyHz = false;

	private final static float ACCEL_MAG_THRESHOLD = 0.3f;
	private final static long ACCEL_DURATION_NANOSECONDS = 5000000000l;
	private long mAccelFirstTime = 0l;
	private float mAccelMagLast = 0.0f;
	//private float mAccelMag = 0.0f;

	private boolean mAccelFinished = false;
	private boolean mSixtyHzFinished = false;

    private Context mContext;


    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    FileWriter ascFW;

    public GridWatchEvent (GridWatchEventType eventType, Context context)  {
		mEventType = eventType;

        mContext = context;

		// Get the timestamp immediately after we determine
		// that an event happened.
		mTimestamp = System.currentTimeMillis();

        //spawn the audio thread if needed
        addMicrophoneSamples();
	}

    public String getRecordingFileName () {
        return recordingFileName;
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

            if (accelMagCSV.length() >= 1) {
                accelMagCSV += accelMagCSV.substring(0, accelMagCSV.length() - 1); //get rid of the last comma
            }
			return true;
		}

		// Compare this sample to the previous and see if the device
		// has moved.
		Float accelMagCurrent = FloatMath.sqrt(x*x + y*y + z*z);
		if (mAccelMagLast == 0.0f) {
			mAccelMagLast = accelMagCurrent;
			return false;
		}
		float delta = Math.abs(accelMagCurrent - mAccelMagLast);
		mAccelMagLast = accelMagCurrent;
		//mAccelMag = (mAccelMag * 0.9f) + delta;

        if (accelMagCurrent != null) {
            accelMagCSV += accelMagCurrent + ",";
        }

		if (delta > ACCEL_MAG_THRESHOLD) {
			// We detected movement, don't need any more samples
			mMoved = true;
			mAccelFinished = true;
			return true;
		}
		return false;
	}


	public void addMicrophoneSamples () {
        if (!needMicrophoneSamples()) return;

        if (debug) {
            return;
        }

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD){ // Disable mic on old API
            Thread audioThread = new Thread(new GridWatchAudioThread(this));
            audioThread.start();
        }

		mSixtyHz = true; //TODO hack
	}

	// Returns true if this event is ready to be sent to the server,
	// false if it needs more data
	public boolean readyForTransmission () {
        switch (mEventType) {
            case USR_PLUGGED:
                if (debug) {
                    return true;
                }

                if (!mSixtyHzFinished) {
                    Log.w("RECORDING", "not done with audio yet");
                    return false;
                }
                Log.d("GridWatchEvent", "USR Plugged Event");
                Log.w("RECORDING", "READY FOR TRANSMISSION");
                return true;
            case PLUGGED:
                if (debug) {
                    return true;
                }
                if (!mSixtyHzFinished) {
                    Log.w("RECORDING", "not done with audio yet");
                    return false;
                }
                Log.w("RECORDING", "READY FOR TRANSMISSION");
                return true;
            case WD:
                Log.d("GridWatchEvent", "WD Event");
                break;
            case UNPLUGGED:
                if (debug) {
                    return true;
                }
                if (!mAccelFinished) return false;
                if (!mSixtyHzFinished) {
                    Log.w("RECORDING", "not done with audio yet");
                    return false;
                }
                Log.d("GridWatchEvent", "Ready for Transmission");
                Log.w("RECORDING", "READY FOR TRANSMISSION");
                break;
            case USR_UNPLUGGED:
                if (debug) {
                    return true;
                }
                if (!mAccelFinished) return false;
                if (!mSixtyHzFinished) {
                    Log.w("RECORDING", "not done with audio yet");
                    return false;
                }
                Log.d("GridWatchEvent", "USR Unplugged Event");
                Log.w("RECORDING", "READY FOR TRANSMISSION");
                return true;
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
            nameValuePairs.add(new BasicNameValuePair("accel", accelMagCSV.replaceAll("null", "")));
			//nameValuePairs.add(new BasicNameValuePair("sixty_hz", String.valueOf(mSixtyHz)));
			break;
        case USR_UNPLUGGED:
            nameValuePairs.add(new BasicNameValuePair("moved", String.valueOf(mMoved)));
            nameValuePairs.add(new BasicNameValuePair("accel", accelMagCSV.replaceAll("null", "")));
            //nameValuePairs.add(new BasicNameValuePair("sixty_hz", String.valueOf(mSixtyHz)));
            break;
        case WD:
			break;
		case PLUGGED:
            break;
        case USR_PLUGGED:
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
        case USR_PLUGGED:
            return false;
		case WD:
			return false;
		case UNPLUGGED:
			return true;
        case USR_UNPLUGGED:
            return true;
		}
		return false;
	}

    // Based on the event type determine if we need the service
    // to hand us accelerometer samples.
    private boolean needMicrophoneSamples () {
        switch (mEventType) {
            case PLUGGED:
                return true;
            case USR_PLUGGED:
                return true;
            case WD:
                return false;
            case UNPLUGGED:
                return true;
            case USR_UNPLUGGED:
                return true;
        }
        return false;
    }

    // This thread handles getting audio data from the microphone
    class GridWatchAudioThread implements Runnable {
        GridWatchEvent mThisEvent;

        public GridWatchAudioThread (GridWatchEvent gwevent) {
            mThisEvent = gwevent;
        }

        @Override
        public void run() {

            // TODO should never be false... cut this off earlier. Hack
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
                int recBufferSize = AudioRecord.getMinBufferSize(SAMPLE_FREQUENCY,
                        RECORDER_CHANNELS,
                        RECORDER_ENCODING);

                mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_FREQUENCY,
                        RECORDER_CHANNELS,
                        RECORDER_ENCODING,
                        recBufferSize*2);


                //String mfolder = Environment.getExternalStorageDirectory().getPath() + "/Android/data/edu.umich.eecs.gridwatch/recordings";

                String tmpFilePath = "";
                if (android.os.Build.VERSION.SDK_INT>=19) {
                    ArrayList<String> arrMyMounts = new ArrayList<String>();
                    File[] possible_kitkat_mounts = mContext.getExternalFilesDirs(null);
                    for (int x = 0; x < possible_kitkat_mounts.length; x++) {
                        if (possible_kitkat_mounts[x] != null){
                            Log.d("RECORDING_MOUNTS", "possible_kitkat_mounts " + possible_kitkat_mounts[x].toString());
                            tmpFilePath = possible_kitkat_mounts[x].toString();
                        }
                    }
                } else {
                    // Set up the tmp file before WAV conversation
                    tmpFilePath = Environment.getExternalStorageDirectory().getPath();
                    Log.d("RECORDING PATH", tmpFilePath);
                }

                File fileFolder = new File(tmpFilePath, recordingFolder);
                if (!fileFolder.exists()) fileFolder.mkdirs();
                File tmpFile = new File(tmpFilePath, recordingFileTmpName);
                if (!tmpFile.exists()) tmpFile.delete();
                String tmpFileName = fileFolder.getAbsolutePath() + "/" + recordingFileTmpName;


                byte tmpData[] = new byte[recBufferSize];
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(tmpFileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // Get that recording going
                if (mRecorder != null) {
                    if (recording_on) {
                        mRecorder.startRecording();
                        Log.w("RECORDING", "Starting Recording");
                    } else {
                        RECORDER_TIME = 0;
                    }
                }

                // Take RECORDER_TIME worth of data
                int read = 0;
                if (os != null) {
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        long t = System.currentTimeMillis();
                        while (System.currentTimeMillis() - t <= RECORDER_TIME) {
                            read = mRecorder.read(tmpData, 0, recBufferSize);
                            try {
                                os.write(tmpData);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                Log.w("RECORDING", "Done Recording");


                // Stop recording
                mRecorder.stop();
                mRecorder.release();

                // Make a WAV file
                recordingFileName = fileFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + recordingExtension;
                Log.w("RECORDING_final_name", recordingFileName);
                Log.w("RECORDING_tmp_name", tmpFileName);

                // Convert RAW to WAV
                FileInputStream in = null;
                FileOutputStream out = null;
                long totalAudioLen = 0;
                long totalDataLen = totalAudioLen + 36;
                long longSampleRate = SAMPLE_FREQUENCY;
                int channels = 2;
                long byteRate = BIT_RATE * SAMPLE_FREQUENCY * channels/8;

                byte[] wavData = new byte[recBufferSize];

                try {
                    in = new FileInputStream(tmpFileName);
                    out = new FileOutputStream(recordingFileName);
                    totalAudioLen = in.getChannel().size();
                    totalDataLen = totalAudioLen + 36;

                    WriteWavHeader(out, totalAudioLen, totalDataLen,
                            longSampleRate, channels, byteRate);

                    while(in.read(wavData) != -1){
                        out.write(wavData);
                    }
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.w("RECORDING", "Done Transfering TMP to WAV");

                // Delete the tmp file
                if (tmpFile.exists()) tmpFile.delete();
                mThisEvent.mSixtyHzFinished = true;
            }
        }

    }

    private void WriteWavHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = BIT_RATE;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);

    }
}
