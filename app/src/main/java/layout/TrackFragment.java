package layout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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

import java.util.List;

import it.faerb.herakles.LocationLog;
import it.faerb.herakles.LocationLoggerService;
import it.faerb.herakles.MainActivity;
import it.faerb.herakles.R;

import it.faerb.herakles.Util;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TrackFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TrackFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackFragment extends Fragment implements GpsStatus.Listener, LocationListener {

    static final String TAG = "Herakles.TrackFragment";

    //private OnFragmentInteractionListener mListener;

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

        final Button startButton = (Button) view.findViewById(R.id.start_button);
        assert startButton != null;
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked start Button");
                getActivity().startService(new Intent(v.getContext(), LocationLoggerService.class));
                isRunning = true;
                refreshHandler.postDelayed(refresh, 1000);
            }
        });

        final Button stopButton = (Button) view.findViewById(R.id.stop_button);
        assert stopButton != null;
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked stop Button");
                getActivity().stopService(new Intent(v.getContext(), LocationLoggerService.class));
                refreshHandler.removeCallbacksAndMessages(null);
                isRunning = false;
                clearData();
            }
        });

        if (isRunning) {
            refreshHandler.postDelayed(refresh, 1000);
        }
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
        } else {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            assert imageView != null;
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);
        }

        startLocationUpdates();
        return view;
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
                TextView timeView = (TextView) getView().findViewById(R.id.text_view_time);
                assert timeView != null;
                timeView.setText(Util.formatSeconds(elapsedSeconds));

                for (int i = lastUpdateIndex; i < locList.size(); i++) {
                    lastUpdateDistance += locList.get(i).distanceTo(locList.get(i - 1));
                }
                lastUpdateIndex = locList.size();

                TextView distanceView = (TextView) getView().findViewById(R.id.text_view_distance);
                assert distanceView != null;
                distanceView.setText(Util.formatDistance(lastUpdateDistance));
            }

            TrackFragment.this.refreshHandler.postDelayed(refresh, 1000);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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
        TextView textView = (TextView) getView().findViewById(R.id.text_view_gps_error);
        assert textView != null;
        textView.setText(String.valueOf(loc.getAccuracy()) + " m");
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
