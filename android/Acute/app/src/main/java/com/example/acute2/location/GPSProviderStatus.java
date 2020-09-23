package com.example.acute2.location;

/**
 * GPS status
 * Created by lizhenya on 2016/9/12.
 */
public class GPSProviderStatus {
    //Users manually turn on GPS
    public static final int GPS_ENABLED = 0;
    //Users manually turn off GPS
    public static final int GPS_DISABLED = 1;
    //The service has stopped and will not change for a short time
    public static final int GPS_OUT_OF_SERVICE = 2;
    //The service is temporarily stopped and will resume in a short time
    public static final int GPS_TEMPORARILY_UNAVAILABLE = 3;
    //The service is normal and effective
    public static final int GPS_AVAILABLE = 4;
}
