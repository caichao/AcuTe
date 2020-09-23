package com.example.acute2.utils;

import android.util.Log;

import com.example.acute2.Entity.Parameters;

import java.util.Arrays;

public class HandleUtil {

    /**
     * find the peek of fft result
     * @param fftRes
     * @param isCalibrating
     * @param parameters
     * @return a double array
     */
    public static double[] findPeek(double[] fftRes, boolean isCalibrating, Parameters parameters) {

        double resolution = parameters.getResolution();

        double[] res = new double[3];

        int start = 0;
        int end = 0;

        if(!isCalibrating){
            start =  (int)(TransferUtil.temperatureToFreq(40,parameters)/resolution) - 1;
            end = (int)(TransferUtil.temperatureToFreq(0,parameters)/resolution) + 1;
        }else if(isCalibrating){
            start = (int)(TransferUtil.dToFreq(0.10,parameters)/resolution) - 1;
            end = (int)(TransferUtil.dToFreq(0.20,parameters)/resolution) + 1;
        }

        double peekMag = Math.sqrt(fftRes[2*start]*fftRes[2*start]+fftRes[2*start+1]*fftRes[2*start+1]);
        double peekFreq = start*resolution;

        int index = start;

        for(int i=start+1;i<=end;i++){
            double mag = Math.sqrt(fftRes[2*i]*fftRes[2*i]+fftRes[2*i+1]*fftRes[2*i+1]);
            if(mag > peekMag){
                peekMag = mag;
                index = i;
            }
        }

        peekFreq = index*resolution;
        res[0] = peekFreq;
        res[1] = peekMag;
        res[2] = index;

        return res;
    }

    /**
     * mix left channel and right channel of record
     * @param left
     * @param right
     * @return
     */
    public static int[] mixData(short[] left,short[] right) {

        int[] mixedData = new int[left.length];
        for (int i = 0; i < left.length; i++) {
            mixedData[i] = left[i]*right[i];
        }
        return mixedData;
    }

}
