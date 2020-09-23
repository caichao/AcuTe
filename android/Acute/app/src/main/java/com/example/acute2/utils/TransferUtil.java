package com.example.acute2.utils;

/**
 * a util for calculating via the relationship between sound velocity and temperature
 */

import com.example.acute2.Entity.Parameters;

public class TransferUtil {
    /**
     * Calculate frequency
     * @param dForRange
     * @param parameters
     * @return
     */
    public static double dToFreq(double dForRange, Parameters parameters){
        double freq = 0;
        double alpha = 403;
        double B = parameters.getFmax()-parameters.getFmin();
        freq = Math.sqrt((dForRange*dForRange*B*B)/(alpha*(parameters.getTemperatureForCalibration()+273.15)*parameters.getDuration()*parameters.getDuration()));
        return freq;
    }

    /**
     *get d via frequency
     * @param freq
     * @param parameters
     * @return
     */
    public static double freqTod(double freq, Parameters parameters){
        double dCalibrated = 0;
        double alpha = 403;
        double B = parameters.getFmax()-parameters.getFmin();
        dCalibrated = Math.sqrt(alpha*(parameters.getTemperatureForCalibration()+273.15)*freq*freq*parameters.getDuration()*parameters.getDuration()/(B*B));
        return dCalibrated;
    }

    /**
     *get temperature via temperature
     * @param temperature
     * @return
     */
    public static double temperatureToFreq(double temperature,Parameters parameters){

        temperature = temperature + 273.15;
        double freq = 0;
        double alpha = 403;
        double B = parameters.getFmax()-parameters.getFmin();
        freq = Math.sqrt((parameters.getD()*parameters.getD()*B*B)/(alpha*temperature*parameters.getDuration()*parameters.getDuration()));
        return freq;
    }

    /**
     *get frequency via temperature
     * @param freq
     * @return
     */
    public static double freqToTemperature(double freq, Parameters parameters){
        double temperature = 0;
        double B = parameters.getFmax() - parameters.getFmin();
        double alpha = 403;
        temperature = (parameters.getD()*parameters.getD()*B*B)/(alpha*freq*freq*parameters.getDuration()*parameters.getDuration()) - 273.15;
        return temperature;
    }
}
