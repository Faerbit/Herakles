package it.faerb.herakles;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class LocationLoggerService extends Service implements LocationListener {

    final static String TAG = "Herakles.LocLogService";

    private LocationManager locationManager;

    public LocationLoggerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Log.d(TAG, "onStartCommand");
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Herakles running")
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0))
                .setSmallIcon(getApplicationInfo().icon)
                .build();
        subscribeToLocationUpdates();
        startForeground(1, notification);
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

    }

    @Override
    public void onLocationChanged(Location loc) {
        LocationLog.addLocation(loc);
    }

    public void onProviderEnabled(String s) {
    }

    public void onProviderDisabled(String s) {
    }

    public void onStatusChanged(String s, int i, Bundle b) {
    }

    private void subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates: begin");
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Log.d(TAG, "subscribeToLocationUpdates: requested");
    }

}
