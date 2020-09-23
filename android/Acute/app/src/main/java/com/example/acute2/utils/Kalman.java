package com.example.acute2.utils;

import java.util.List;

/**
 * a util for kalman filtering
 */
public class Kalman {

    private double freqP = 1;
    private double freqX = -60;

    public void setFreqP(double freqP) {
        this.freqP = freqP;
    }

    public void setFreqX(double freqX) {
        this.freqX = freqX;
    }

    public void kalmanFilter(List<Double> list, double Q, double R){
        double cur = freqX;
        freqP = freqP+Q;
        double kg = freqP / (freqP + R);//kalman gain
        cur = cur + kg*(list.get(list.size()-1) - cur);
        freqP = (1-kg)*freqP;
        freqX = cur;
        list.set(list.size()-1,cur);
    }
}
