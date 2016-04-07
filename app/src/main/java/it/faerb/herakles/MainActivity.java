package it.faerb.herakles;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GpsStatus.Listener, LocationListener {

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
    private LocationManager locationManager;
    private MyLocationNewOverlay locationOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String[] titles = getResources().getStringArray(R.array.navigation_menu);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert  drawerLayout != null;
        ListView navDrawer = (ListView) findViewById(R.id.nav_drawer);
        assert navDrawer != null;
        navDrawer.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, titles));
        navDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, String.format("onItemClick: %d", position));
            }
        });
        navDrawer.setItemChecked(0, true);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this,                  // host Activity
                drawerLayout,         // DrawerLayout object
                toolbar,
                R.string.nav_open,  // "open drawer" description for accessibility
                R.string.nav_close  //"close drawer" description for accessibiliy
        );
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        MapView map = (MapView) findViewById(R.id.map);
        assert map != null;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        final IMapController mapController = map.getController();
        mapController.setZoom(15);
        locationOverlay = new MyLocationNewOverlay(this, map);
        map.getOverlays().add(locationOverlay);
        locationOverlay.enableFollowLocation();

        final Button startButton = (Button) findViewById(R.id.start_button);
        assert startButton != null;
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked start Button");
                startService(new Intent(v.getContext(), LocationLoggerService.class));
                isRunning = true;
                refreshHandler.postDelayed(refresh, 1000);
            }
        });

        final Button stopButton = (Button) findViewById(R.id.stop_button);
        assert stopButton != null;
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked stop Button");
                stopService(new Intent(v.getContext(), LocationLoggerService.class));
                refreshHandler.removeCallbacksAndMessages(null);
                isRunning = false;
                clearData();
            }
        });

        if (isRunning) {
            refreshHandler.postDelayed(refresh, 1000);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ImageView imageView = (ImageView) findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
        } else {
            ImageView imageView = (ImageView) findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);
        }

        startLocationUpdates();
    }

    private void clearData() {
        lastUpdateDistance = 0;
        lastUpdateIndex = 1;
        LocationLog.clear();
    }

    private float lastUpdateDistance = 0;
    private int lastUpdateIndex = 1;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            List<Location> locList = LocationLog.getLocationLog();

            if (locList.size() > 1) {
                long startTime = locList.get(0).getElapsedRealtimeNanos();
                long elapsedNanos = SystemClock.elapsedRealtimeNanos() - startTime;
                int elapsedSeconds = (int) (elapsedNanos / 1e9);
                TextView timeView = (TextView) findViewById(R.id.text_view_time);
                assert timeView != null;
                timeView.setText(Util.formatSeconds(elapsedSeconds));

                for (int i = lastUpdateIndex; i < locList.size(); i++) {
                    lastUpdateDistance += locList.get(i).distanceTo(locList.get(i - 1));
                }
                lastUpdateIndex = locList.size();

                TextView distanceView = (TextView) findViewById(R.id.text_view_distance);
                assert distanceView != null;
                distanceView.setText(Util.formatDistance(lastUpdateDistance));
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
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.addGpsStatusListener(this);
        locationOverlay.enableMyLocation();
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
        locationManager.removeGpsStatusListener(this);
        locationOverlay.disableMyLocation();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateGpsInfo();
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                ImageView imageView = (ImageView) findViewById(R.id.image_view_gps);
                assert imageView != null;
                imageView.setImageResource(R.drawable.ic_gps_fixed_24dp);
        }
    }

    private void updateGpsInfo() {
        int totalSatellites = 0;
        int satellitesInFix = 0;
        for (GpsSatellite satellite: locationManager.getGpsStatus(null).getSatellites()) {
            if(satellite.usedInFix()) {
                satellitesInFix++;
            }
            totalSatellites++;
        }
        TextView textView = (TextView) findViewById(R.id.text_view_satellites);
        assert textView != null;
        textView.setText(satellitesInFix + "/" + totalSatellites);
    }

    @Override
    public void onLocationChanged(Location loc) {
        TextView textView = (TextView) findViewById(R.id.text_view_gps_error);
        assert textView != null;
        textView.setText(String.valueOf(loc.getAccuracy()) + " m");
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle b) {}

    @Override
    public void onProviderEnabled(String string) {
        if (LocationManager.GPS_PROVIDER.equals(string)) {
            ImageView imageView = (ImageView) findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
        }
    }

    @Override
    public void onProviderDisabled(String string) {
        if (LocationManager.GPS_PROVIDER.equals(string)) {
            ImageView imageView = (ImageView) findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);

            TextView textView = (TextView) findViewById(R.id.text_view_satellites);
            assert textView != null;
            textView.setText("0/0");

            textView = (TextView) findViewById(R.id.text_view_gps_error);
            assert textView != null;
            textView.setText("0 m");
        }
    }
}
