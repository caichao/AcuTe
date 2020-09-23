package com.example.acute2.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import com.example.acute2.play.SignalProc;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class AudioRecorder implements IAudioRecorder{

    //public static final int RECORDER_SAMPLE_RATE = 48000;
    //public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_OUT_STEREO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;//2个字节

    //这里是4800个采样点，左右两个声道
    public static final int BUFFER_BYTES_ELEMENTS = 4800*2;
    public static final int BUFFER_BYTES_PER_ELEMENT = RECORDER_AUDIO_ENCODING;
    public static final int RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_STEREO;


    public static final int RECORDER_STATE_FAILURE = -1;
    public static final int RECORDER_STATE_IDLE = 0;
    public static final int RECORDER_STATE_STARTING = 1;
    public static final int RECORDER_STATE_STOPPING = 2;
    public static final int RECORDER_STATE_BUSY = 3;


    private volatile int recorderState;

    private final Object recorderStateMonitor = new Object();

    private static final String TAG = "recorder";

    private RecordingCallback recordingCallback;

    private int samplingRate;

    private short[] rawData;
    private double T;

    public AudioRecorder(int samplingRate,int fmin,int fmax,double T){
        this.samplingRate = samplingRate;
        this.T = T;
        rawData = SignalProc.upChirp(samplingRate,fmin,fmax,T);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onRecordFailure() {
        recorderState = RECORDER_STATE_FAILURE;
        finishRecord();
    }

    @Override
    public void startRecord() {
        if (recorderState != RECORDER_STATE_IDLE) {
            return;
        }

        try {
            recorderState = RECORDER_STATE_STARTING;
            startRecordThread();
        } catch (FileNotFoundException e) {
            onRecordFailure();
            e.printStackTrace();
        }
    }

    private void startRecordThread() throws FileNotFoundException {
        Log.e(TAG,"record thread run");
        new Thread(new PriorityRunnable(Process.THREAD_PRIORITY_AUDIO) {

            private void onExit() {
                synchronized (recorderStateMonitor) {
                    recorderState = RECORDER_STATE_IDLE;
                    recorderStateMonitor.notifyAll();
                }
            }

            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void runImpl() {
                Log.e("record","into rumImpl");

                int bufferSizeInByte = Math.max(AudioRecord.getMinBufferSize(samplingRate, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING),(int)(samplingRate*T*2*2*2));
                Log.e(TAG,"bufferSize:" + bufferSizeInByte);

                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInByte);
                if(recorder.getState() == AudioRecord.STATE_UNINITIALIZED){
                    Log.e(AudioRecorder.class.getSimpleName(),"*******************************Initialize audio recorder error");
                    return;
                }else{
                    Log.d(AudioRecorder.class.getSimpleName(),"-------------------------------Initialize AudioRecord ok");
                }
                try {
                    if (recorderState == RECORDER_STATE_STARTING) {
                        recorderState = RECORDER_STATE_BUSY;
                    }

                    recorder.startRecording();
                    int readSizeInShort = (int)(samplingRate*T*2*2);//4800*2*2个shorts
                    short[] recordBuffer = new short[readSizeInShort];
                    do {
                        /**************************** 读数据 ******************************/

                        int shortsRead = recorder.read(recordBuffer, 0, readSizeInShort);
                        Log.d(TAG,"shortread"+shortsRead);
                        if (shortsRead > 0) {
                            /********************* 回调音频数据 *************************/
                            //根据源数据和左声道数据获得偏移量对信号进行同步
                            int offset = getOffset(recordBuffer);
                            recordingCallback.onDataReady(recordBuffer, offset);
                        } else {
                            onRecordFailure();
                        }

                    } while (recorderState == RECORDER_STATE_BUSY);
                } finally {
                    recorder.release();
                }
                onExit();
            }
        }).start();
    }

    @Override
    public void finishRecord() {
        int recorderStateLocal = recorderState;
        if (recorderStateLocal != RECORDER_STATE_IDLE) {
            synchronized (recorderStateMonitor) {
                recorderStateLocal = recorderState;
                if (recorderStateLocal == RECORDER_STATE_STARTING
                        || recorderStateLocal == RECORDER_STATE_BUSY) {

                    recorderStateLocal = recorderState = RECORDER_STATE_STOPPING;
                }

                do {
                    try {
                        if (recorderStateLocal != RECORDER_STATE_IDLE) {
                            recorderStateMonitor.wait();
                        }
                    } catch (InterruptedException ignore) {
                        /* Nothing to do */
                    }
                    recorderStateLocal = recorderState;
                } while (recorderStateLocal == RECORDER_STATE_STOPPING);
            }
        }
    }

    @Override
    public boolean isRecording() {
        return recorderState != RECORDER_STATE_IDLE;
    }

    public interface RecordingCallback {
        void onDataReady(short[] data, int offset);
    }

    public void registerCallback(RecordingCallback callback){
        this.recordingCallback = callback;
    }

    private int getOffset(short[] data){

        int offset = 0;
        int maxCof = Integer.MIN_VALUE;
        short[] left = new short[data.length/2];
        for(int i=0;i<data.length/2;i++){
            left[i] = data[2*i];
        }
        //i指的是偏移量
        for(int i=0;i<left.length;i++){

            int cof = 0;

            for(int j=0;j<rawData.length;j++){

                if(j+i>=left.length){
                    cof += rawData[j] * left[(j+i)%left.length];
                }else {
                    cof += rawData[j] * left[j + i];
                }
            }

            if(cof>maxCof){
                maxCof = cof;
                offset = i;
            }
        }

        return offset;
    }

}
