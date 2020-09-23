package com.example.acute2.play;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;


public class AudioPlayer implements IAudioPlayer{

    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int PLAY_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    private AudioTrack audioTrack;
    private int bufferSizeInBytes;

    private volatile PlayStatus status = PlayStatus.PLAY_NO_READY;

    private short[] chirp;
    private int fs;
    private double T;
    private int fmin;
    private int fmax;

    public AudioPlayer(int fs,double T,int fmin,int fmax){
        this.fs = fs;
        Log.d("player","fs:" + fs);
        this.T = T;
        this.fmin = fmin;
        this.fmax = fmax;
        //headChirp = SignalProc.upChirpForPlay(fs,fmin,fmax,0.01);
        chirp = SignalProc.upChirp(fs,fmin,fmax,T);//0.1s
        init();
    }

    private void init(){
        bufferSizeInBytes = AudioTrack.getMinBufferSize(fs,PLAY_CHANNELS,AUDIO_FORMAT);
        //bufferSizeInBytes = bufferSizeInBytes * 2;
        Log.d("player","buffer size:" + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0){
            throw new IllegalStateException("AudioTrack is not available " + bufferSizeInBytes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            audioTrack = new AudioTrack.Builder()
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(fs)
                        .setChannelMask(PLAY_CHANNELS)
                        .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }else {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,fs,PLAY_CHANNELS,
                    AUDIO_FORMAT,bufferSizeInBytes, AudioTrack.MODE_STREAM);
        }
        status = PlayStatus.PLAY_READY;
    }

    @Override
    public void startPlay() {
        if (status == PlayStatus.PLAY_NO_READY || audioTrack == null){
            throw new IllegalStateException("no init");
        }
        if (status == PlayStatus.PLAY_START){
            throw new IllegalStateException("player has started");
        }
        Log.d("player","===start===");
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                playAudioData();
            }
        }).start();
        status = PlayStatus.PLAY_START;
    }


    private void playAudioData(){
        short[] zeroData = new short[(int)(T*fs)];
        audioTrack.play();

        while (status == PlayStatus.PLAY_START && audioTrack!=null){
            //chirp signal last 1s
            audioTrack.write(chirp,0,chirp.length);

            //blank signal last 1s
            audioTrack.write(zeroData,0,zeroData.length);
        }

    }

    @Override
    public void finishPlay() {
        if (status != PlayStatus.PLAY_START){
            throw new IllegalStateException("no yet playing");
        }else {
            status = PlayStatus.PLAY_STOP;
            audioTrack.stop();
            if (audioTrack != null){
                audioTrack.release();
                audioTrack = null;
            }
            status = PlayStatus.PLAY_NO_READY;
        }
    }

    @Override
    public boolean isPlaying() {
        return status == PlayStatus.PLAY_START;
    }
}
