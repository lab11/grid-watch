package com.umich.gridwatch.Sensors;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.umich.gridwatch.Utils.IntentConfig;
import com.umich.gridwatch.Utils.SensorConfig;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SpectralPeakProcessor;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;

/**
 * Created by nklugman on 6/4/15.
 */
public class FFTService  extends IntentService {
    private String onHandleIntentTag = "FFTService:onHandleIntent";
    private String doFFTTag = "FFTService:doFFT";

    private static ResultReceiver resultReceiver;
    private static long firstTime = 0;
    private static final long TIME_THRES = SensorConfig.FFT_SAMPLE_TIME_MS;


    private final int sampleRate = 22050;
    private final int fftsize = 32768/2;
    private final int overlap = fftsize/2;

    private int sixtyCnt = 0;
    private int oneTwentyCnt = 0;
    private int twoFortyCnt = 0;
    private int FFT_cnt = 0;

    private AudioDispatcher dispatcher;

    public FFTService() {
        super("FFTService");
    }


    @Override
    protected void onHandleIntent(Intent workIntent) {
        resultReceiver = workIntent.getParcelableExtra(IntentConfig.RECEIVER_KEY);
        //fileName = workIntent.getStringExtra(IntentConfig.MESSAGE_KEY); //the filename is passed as a parameter
        //Log.d(onHandleIntentTag+":fileName", fileName);
        firstTime = System.currentTimeMillis();
        this.doFFT();
    }

    private void doFFT() {
        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, fftsize, overlap);
            AudioProcessor lpfilter = new LowPassFS(130, 22050);
            dispatcher.addAudioProcessor(lpfilter);
            //AudioProcessor filter = new BandPass(60, 120, 22050);
            //dispatcher.addAudioProcessor(filter);
            final SpectralPeakProcessor spectralPeakFollower = new SpectralPeakProcessor(fftsize, overlap, sampleRate);
            dispatcher.addAudioProcessor(spectralPeakFollower);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public void processingFinished() {

                }

                @Override
                public boolean process(AudioEvent audioEvent) {
                    Float f = new Float(0.2);
                    float[] noiseFloor = SpectralPeakProcessor.calculateNoiseFloor(spectralPeakFollower.getMagnitudes(), 10, f);
                    List localMaxima = SpectralPeakProcessor.findLocalMaxima(spectralPeakFollower.getMagnitudes(), noiseFloor);
                    List<SpectralPeakProcessor.SpectralPeak> list = SpectralPeakProcessor.findPeaks(spectralPeakFollower.getMagnitudes(), spectralPeakFollower.getFrequencyEstimates(), localMaxima, 5, 20);
                    for (int i = 0; i < list.size(); i++) {
                        SpectralPeakProcessor.SpectralPeak cur = list.get(i);
                        //Log.w("HZ: ", String.valueOf(i) + " : " + String.valueOf(cur.getFrequencyInHertz()));
                        if (cur.getFrequencyInHertz() > 55 && cur.getFrequencyInHertz() < 65) {
                            Log.w("HZ", "HIT 60");
                            sixtyCnt++;
                        }
                        if (cur.getFrequencyInHertz() > 115 && cur.getFrequencyInHertz() < 125) {
                            Log.w("HZ", "HIT 120");
                            oneTwentyCnt++;
                        }
                        if (cur.getFrequencyInHertz() > 235 && cur.getFrequencyInHertz() < 245) {
                            Log.w("HZ", "HIT 240");
                            twoFortyCnt++;
                        }
                    }
                    return true;
                }
            });
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
            final Future handler = executor.submit(dispatcher);
            executor.schedule(new Runnable(){
                public void run(){
                    dispatcher.stop();
                    int total = sixtyCnt + oneTwentyCnt + twoFortyCnt;
                    Bundle bundle = new Bundle();
                    bundle.putString(IntentConfig.FFT_CNT, String.valueOf(total));
                    if (total > SensorConfig.NUM_FFT_HIT_CNT) {
                        bundle.putString(IntentConfig.RESULT_KEY, IntentConfig.RESULT_PASSED);
                    } else {
                        bundle.putString(IntentConfig.RESULT_KEY, IntentConfig.RESULT_FAILED);
                    }
                    resultReceiver.send(IntentConfig.FFT, bundle);
                    handler.cancel(true);

                }
            }, 10000, TimeUnit.MILLISECONDS);





        } catch (IllegalStateException e) {
            //Just punt as a failure... there should be a way to catch this better
            //problem arises with more than one audiodispatcher... we should also
            //look into caching the audiodispatcher result... could build a semiphore
            Bundle bundle = new Bundle();
            bundle.putString(IntentConfig.FFT_CNT, String.valueOf(-1));
            bundle.putString(IntentConfig.RESULT_KEY, IntentConfig.RESULT_FAILED);
            resultReceiver.send(IntentConfig.FFT, bundle);
        }
    }
}

