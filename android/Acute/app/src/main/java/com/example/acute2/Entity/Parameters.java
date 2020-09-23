package com.example.acute2.Entity;

/**
 * Entity of parameters
 */
public class Parameters {
    private float d ;
    private float duration;
    private int fmin;
    private int fmax;
    private int sampleRate;
    private int fftLength;
    private String ip;
    private String port;
    private double resolution;
    private double temperatureForCalibration;

    public double getTemperatureForCalibration() {
        return temperatureForCalibration;
    }

    public float getD() {
        return d;
    }
    public double getResolution(){
        return (double)(sampleRate/fftLength);
    }
    public float getDuration() {
        return duration;
    }

    public int getFmin() {
        return fmin;
    }

    public int getFmax() {
        return fmax;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getFftLength() {
        return fftLength;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public void setD(float d) {
        this.d = d;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setFmin(int fmin) {
        this.fmin = fmin;
    }

    public void setFmax(int fmax) {
        this.fmax = fmax;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setFftLength(int fftLength) {
        this.fftLength = fftLength;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setTemperatureForCalibration(double temperatureForCalibration) {
        this.temperatureForCalibration = temperatureForCalibration;
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "d=" + d +
                ", duration=" + duration +
                ", fmin=" + fmin +
                ", fmax=" + fmax +
                ", sampleRate=" + sampleRate +
                ", fftLength=" + fftLength +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", resolution=" + resolution +
                ", temperatureForCalibration=" + temperatureForCalibration +
                '}';
    }
}
