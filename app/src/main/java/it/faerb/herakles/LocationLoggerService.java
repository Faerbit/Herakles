package it.faerb.herakles;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class LocationLoggerService extends Service implements LocationListener {

    final static String TAG = "Herakles.LocLogService";

    private final static int ONGOING_NOTIFICATION = 1;

    public LocationLoggerService() {
    }

    private Handler saveHandler;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Log.d(TAG, "onStartCommand");
        subscribeToLocationUpdates();
        startForeground(ONGOING_NOTIFICATION, createNotification());
        saveHandler = new Handler();
        saveHandler.postDelayed(save, Util.Constants.SAVE_INTERVAL);
        return START_STICKY;
    }

    private Notification createNotification() {
        String notificationText;
        notificationText = Util.formatDistance(LocationLog.getCurrentLocationLog().getDistance());
        notificationText += " ";
        notificationText += Util.formatDuration(LocationLog.getCurrentLocationLog().getDuration());
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(getApplicationInfo().icon)
                .setColor(getColor(R.color.colorPrimary))
                .setContentTitle(getString(R.string.app_name) + " " +
                        getString(R.string.notification_running))
                .setContentText(notificationText)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), 0))
                .build();
        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        saveHandler.removeCallbacksAndMessages(null);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    private Runnable save = new Runnable() {
        @Override
        public void run() {
            LocationLog.save(getApplicationContext());
            saveHandler.postDelayed(save, Util.Constants.SAVE_INTERVAL);
        }
    };


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
        Log.d(TAG, "onLocationChanged: adding location");
        LocationLog.addLocation(loc);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        notificationManager.notify(ONGOING_NOTIFICATION, createNotification());
    }

    public void onProviderEnabled(String s) {
    }

    public void onProviderDisabled(String s) {
    }

    public void onStatusChanged(String s, int i, Bundle b) {
    }

    private void subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates: begin");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Util.Constants.LOCATION_MIN_TIME, Util.Constants.LOCATION_MIN_DISTANCE, this);
        Log.d(TAG, "subscribeToLocationUpdates: requested");
    }

}
