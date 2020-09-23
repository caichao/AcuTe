package com.example.acute2.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;

/**
 * LocationListener
 */
public class GPSLocation implements LocationListener {

    private GPSLocationListener mGpsLocationListener;

    public GPSLocation(GPSLocationListener gpsLocationListener) {
        this.mGpsLocationListener = gpsLocationListener;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mGpsLocationListener.UpdateLocation(location);
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        mGpsLocationListener.UpdateStatus(provider, status, extras);
        switch (status) {
            case LocationProvider.AVAILABLE:
                mGpsLocationListener.UpdateGPSProviderStatus(GPSProviderStatus.GPS_AVAILABLE);
                break;
            case LocationProvider.OUT_OF_SERVICE:
                mGpsLocationListener.UpdateGPSProviderStatus(GPSProviderStatus.GPS_OUT_OF_SERVICE);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                mGpsLocationListener.UpdateGPSProviderStatus(GPSProviderStatus.GPS_TEMPORARILY_UNAVAILABLE);
                break;
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        mGpsLocationListener.UpdateGPSProviderStatus(GPSProviderStatus.GPS_ENABLED);
    }

    @Override
    public void onProviderDisabled(String provider) {
        mGpsLocationListener.UpdateGPSProviderStatus(GPSProviderStatus.GPS_DISABLED);
    }
}