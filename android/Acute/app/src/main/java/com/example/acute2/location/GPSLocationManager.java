package com.example.acute2.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;

/**
 * 类描述：GPS定位的管理类
 */
public class GPSLocationManager {
    private static final String GPS_LOCATION_NAME = android.location.LocationManager.GPS_PROVIDER;
    private static GPSLocationManager gpsLocationManager;
    private static Object objLock = new Object();
    private boolean isGpsEnabled;
    private static String mLocateType;
    private WeakReference<Activity> mContext;
    private LocationManager locationManager;
    private GPSLocation mGPSLocation;
    private boolean isOPenGps;
    private long mMinTime;
    private float mMinDistance;

    private GPSLocationManager(Activity context) {
        initData(context);
    }

    private void initData(Activity context) {
        this.mContext = new WeakReference<>(context);
        if (mContext.get() != null) {
            locationManager = (LocationManager) (mContext.get().getSystemService(Context.LOCATION_SERVICE));
        }
        //type:internet
        mLocateType = locationManager.NETWORK_PROVIDER;

        isOPenGps = false;

        mMinTime = 1000;

        mMinDistance = 0;
    }

    public static GPSLocationManager getInstances(Activity context) {
        if (gpsLocationManager == null) {
            synchronized (objLock) {
                if (gpsLocationManager == null) {
                    gpsLocationManager = new GPSLocationManager(context);
                }
            }
        }
        return gpsLocationManager;
    }

    /**
     * @param minTime
     */
    public void setScanSpan(long minTime) {
        this.mMinTime = minTime;
    }

    /**
     * @param minDistance
     */
    public void setMinDistance(float minDistance) {
        this.mMinDistance = minDistance;
    }

    /**
     * @param gpsLocationListener
     */
    public void start(GPSLocationListener gpsLocationListener) {
        this.start(gpsLocationListener, isOPenGps);
    }

    /**
     * @param gpsLocationListener
     * @param isOpenGps
     */
    public void start(GPSLocationListener gpsLocationListener, boolean isOpenGps) {
        this.isOPenGps = isOpenGps;
        if (mContext.get() == null) {
            return;
        }
        mGPSLocation = new GPSLocation(gpsLocationListener);
        isGpsEnabled = locationManager.isProviderEnabled(GPS_LOCATION_NAME);
        if (!isGpsEnabled && isOPenGps) {
            openGPS();
            return;
        }

        if (ActivityCompat.checkSelfPermission(mContext.get(),Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (mContext.get(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location lastKnownLocation = locationManager.getLastKnownLocation(mLocateType);
        mGPSLocation.onLocationChanged(lastKnownLocation);

        locationManager.requestLocationUpdates(mLocateType, mMinTime, mMinDistance, mGPSLocation);
    }

    public void openGPS() {
        Toast.makeText(mContext.get(), "please open gps", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT > 15) {
            Intent intent = new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mContext.get().startActivityForResult(intent, 0);
        }
    }


    public void stop() {
        if (mContext.get() != null) {
            if (ActivityCompat.checkSelfPermission(mContext.get(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext.get(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(mGPSLocation);
        }
    }
}
