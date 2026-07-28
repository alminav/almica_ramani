package com.almica.gpssatstatus;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocListener implements LocationListener {
    private final boolean mIsGps;
    private final InfaLocListener infaLocListener;

    public LocListener(boolean isGps, InfaLocListener infaLocListener) {
        mIsGps = isGps;
        this.infaLocListener = infaLocListener;
    }

    @Deprecated
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){
    }

    @Override
    public void onLocationChanged(Location location) {
        // GPS location time tracking removed as it was unused and had side-effects
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (infaLocListener != null)
            infaLocListener.setTimer();
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mIsGps && infaLocListener != null) {
            infaLocListener.clearGpsData();
        }
        if (infaLocListener != null)
            infaLocListener.setTimer();
    }

    interface InfaLocListener {

        void clearGpsData();

        void setTimer();
    }
}
