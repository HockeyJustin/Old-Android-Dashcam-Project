package com.mcd.hkyj.dashcamshared;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Created by jones on 13/05/2015.
 * //get Your Current Location
 * THIS CLASS IS NOT CURRENTLY IN USE
 LocationManager locationManager=    (LocationManager)getSystemService(Context.LOCATION_SERVICE);
 MyCurrentLoctionListener locationListener = new MyCurrentLoctionListener();
 locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) locationListener);
 *
 */
public class CurrentLocationListener implements LocationListener {

    public double latitude;
    public double longitude;

    public String getLocationString(){
        return "ISO_6709_" + latitude +  "_" + longitude;
    }


    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
