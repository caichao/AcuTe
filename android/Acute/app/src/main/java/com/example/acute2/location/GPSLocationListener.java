package com.example.acute2.location;

import android.location.Location;
import android.os.Bundle;


public interface GPSLocationListener {
    /**
     * Called when the location information changes
     *
     * @param location
     */
    void UpdateLocation(Location location);

    /**
     * 方法描述：Called when the source type changes
     *
     * @param provider provider
     * @param status   provider status
     * @param extras
     */
    void UpdateStatus(String provider, int status, Bundle extras);

    /**
     * Called when GPS status changes
     * @param gpsStatus
     */
    void UpdateGPSProviderStatus(int gpsStatus);
}
