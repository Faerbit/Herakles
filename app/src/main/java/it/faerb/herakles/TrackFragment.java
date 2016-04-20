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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class TrackFragment extends Fragment implements GpsStatus.Listener, LocationListener {

    static final String TAG = "Herakles.TrackFragment";

    public TrackFragment() {
        // Required empty public constructor
    }

    public static TrackFragment newInstance() {
        return new TrackFragment();
    }


    private Handler refreshHandler = new Handler();
    private static Boolean isRunning = false;
    private LocationManager locationManager;
    private MyLocationNewOverlay locationOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_track, container, false);
        MapView map = (MapView) view.findViewById(R.id.map);
        assert map != null;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        final IMapController mapController = map.getController();
        mapController.setZoom(15);
        locationOverlay = new MyLocationNewOverlay(getContext(), map);
        map.getOverlays().add(locationOverlay);
        locationOverlay.enableFollowLocation();

        final Button startStopButton = (Button) view.findViewById(R.id.button_start_stop);
        assert startStopButton != null;
        if (isRunning) {
            startStopButton.setText(R.string.button_label_stop);
        }
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStopButtonClick(view);
            }
        });

        final Button newButton = (Button) view.findViewById(R.id.button_new);
        assert newButton != null;
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newButtonClick();
            }
        });

        refreshHandler.post(refresh);
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
            startLocationUpdates();
        } else {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);
        }
        return view;
    }

    private void newButtonClick() {
        LocationLog.save(getContext());
        clearData();
    }

    private void startStopButtonClick(View view) {
        if (!isRunning) {
            Log.d(TAG, "Clicked start Button");
            getActivity().startService(new Intent(getContext(), LocationLoggerService.class));
            isRunning = true;
            ((Button) view).setText(getText(R.string.button_label_stop));
        }
        else {
            Log.d(TAG, "Clicked stop Button");
            getActivity().stopService(new Intent(getContext(), LocationLoggerService.class));
            isRunning = false;
            LocationLog.save(getContext());
            ((Button) view).setText(getText(R.string.button_label_start));
        }
    }


    private void clearData() {
        LocationLog.clear();
    }


    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            TextView timeView = (TextView) getView().findViewById(R.id.text_view_time);
            assert timeView != null;
            if (isRunning) {
                timeView.setText(Util.formatDuration(LocationLog.getElapsedSeconds()));
            }
            else {
                timeView.setText(Util.formatDuration(LocationLog
                        .getCurrentLocationLog().getDuration()));
            }

            TextView distanceView = (TextView) getView().findViewById(R.id.text_view_distance);
            assert distanceView != null;
            distanceView.setText(Util.formatDistance(LocationLog
                    .getCurrentLocationLog().getDistance()));

            TrackFragment.this.refreshHandler.postDelayed(refresh, Util.Constants.REFRESH_INTERVAL);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Util.Constants.LOCATION_MIN_TIME, Util.Constants.LOCATION_MIN_DISTANCE, this);
        locationManager.addGpsStatusListener(this);
        locationOverlay.enableMyLocation();
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
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
                ImageView imageView = (ImageView) getView().findViewById(R.id.image_view_gps);
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
        TextView textView = (TextView) getView().findViewById(R.id.text_view_satellites);
        assert textView != null;
        textView.setText(satellitesInFix + "/" + totalSatellites);
    }

    @Override
    public void onLocationChanged(Location loc) {
        // handle weird null pointer exception
        View view = getView();
        if (view != null) {
            TextView textView = (TextView) view.findViewById(R.id.text_view_gps_error);
            if (textView != null) {
                textView.setText(String.valueOf(loc.getAccuracy()) + " m");
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle b) {}

    @Override
    public void onProviderEnabled(String string) {
        Log.d(TAG, String.format("onProviderEnabled: Provider : %s", string));
        if (LocationManager.GPS_PROVIDER.equals(string)) {
            ImageView imageView = (ImageView) getView().findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
        }
        startLocationUpdates();
    }

    @Override
    public void onProviderDisabled(String string) {
        if (LocationManager.GPS_PROVIDER.equals(string)) {
            ImageView imageView = (ImageView) getView().findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);

            TextView textView = (TextView) getView().findViewById(R.id.text_view_satellites);
            assert textView != null;
            textView.setText("0/0");

            textView = (TextView) getView().findViewById(R.id.text_view_gps_error);
            assert textView != null;
            textView.setText("0 m");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (TrackFragment.isRunning) {
            refreshHandler.post(refresh);
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
    }
}
