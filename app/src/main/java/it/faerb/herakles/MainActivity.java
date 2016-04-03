package it.faerb.herakles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "Herakles.MainActivity";

    // START PERMISSION CHECK
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        String message = getResources().getString(R.string.app_name) + " "
                + getResources().getString(R.string.permission_request_intro);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            message += "\n" + getResources().getString(R.string.permission_request_location);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            message += "\n" + getResources().getString(R.string.permission_request_storage);
        }
        if (!permissions.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } // else: We already have permissions, so handle as normal
    }

    private Handler refreshHandler = new Handler();
    private static Boolean isRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }
        MapView map = (MapView) findViewById(R.id.map);
        assert map != null;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        final IMapController mapController = map.getController();
        mapController.setZoom(15);
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(this, map);
        map.getOverlays().add(locationOverlay);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        final Button startButton = (Button) findViewById(R.id.start_button);
        assert startButton != null;
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked start Button");
                startService(new Intent(v.getContext(), LocationLoggerService.class));
                MainActivity.isRunning = true;
                refreshHandler.postDelayed(refresh, 1000);
            }
        });

        final Button stopButton = (Button) findViewById(R.id.stop_button);
        assert stopButton !=null;
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked stop Button");
                stopService(new Intent(v.getContext(), LocationLoggerService.class));
                refreshHandler.removeCallbacksAndMessages(null);
                MainActivity.isRunning = false;
                clearData();
            }
        });
        if (MainActivity.isRunning) {
            refreshHandler.postDelayed(refresh, 1000);
        }
    }

    private void clearData() {
        lastUpdateDistance = 0;
        lastUpdateIndex = 1;
        LocationLog.clear();
    }

    private int lastUpdateDistance = 0;
    private int lastUpdateIndex = 1;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            List<Location> locList = LocationLog.getLocationLog();
            Log.d(TAG, "run: locList length:" + locList.size());

            if (locList.size() > 1) {
                long startTime = locList.get(0).getElapsedRealtimeNanos();
                long elapsedNanos = SystemClock.elapsedRealtimeNanos() - startTime;
                int elapsedSeconds = (int) (elapsedNanos / 1e9);
                TextView timeView = (TextView) findViewById(R.id.text_view_time);
                assert timeView != null;
                timeView.setText(Util.formatSeconds(elapsedSeconds));

                for(int i = lastUpdateIndex; i<locList.size(); i++) {
                    lastUpdateDistance += locList.get(i).distanceTo(locList.get(i-1));
                }
                lastUpdateIndex = locList.size();

                TextView distanceView = (TextView) findViewById(R.id.text_view_distance);
                assert distanceView != null;
                distanceView.setText(lastUpdateDistance + " m");
            }

            MainActivity.this.refreshHandler.postDelayed(refresh, 1000);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
                if (location && storage) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.permission_response_granted),
                            Toast.LENGTH_SHORT).show();
                } else if (location) {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_storage),
                            Toast.LENGTH_LONG).show();
                } else if (storage) {
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_location),
                            Toast.LENGTH_LONG).show();
                } else { // !location && !storage case
                    // Permission Denied
                    Toast.makeText(this,
                            getResources().getString(R.string.permission_response_storage) +
                            "\n" + getResources().getString(R.string.permission_response_location),
                            Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MainActivity.isRunning) {
            refreshHandler.postDelayed(refresh, 1000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
    }
}
