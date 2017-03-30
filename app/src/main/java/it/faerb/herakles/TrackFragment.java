/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static it.faerb.herakles.Util.Config.OPTIMIZE_INTERVAL;


public class TrackFragment extends Fragment implements GpsStatus.Listener, LocationListener {

    static final String TAG = "Herakles.TrackFragment";

    public TrackFragment() {
        // Required empty public constructor
    }

    public static TrackFragment newInstance() {
        return new TrackFragment();
    }


    private Handler refreshHandler = new Handler();
    private Handler optimizeHandler = new Handler();
    private static Boolean isRunning = false;
    private LocationManager locationManager;
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_track, container, false);

        // setup map
        mapFragment = MapFragment.newInstance(true);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.frame_layout_map_live, mapFragment).commit();
        optimizeHandler.post(optimizePoints);

        // setup buttons
        final Button startStopButton = (Button) view.findViewById(R.id.button_start_stop);
        updateStartStopButtonText(startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStopButtonClick();
            }
        });

        final Button newButton = (Button) view.findViewById(R.id.button_new);
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newButtonClick();
            }
        });

        // handle gps status display
        refreshHandler.post(refresh);
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
            startLocationUpdates();
        } else {
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view_gps);
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);
        }
        return view;
    }

    private void newButtonClick() {
        if (isRunning) {
            Intent clearIntent = new Intent(getContext(), LocationLoggerService.class);
            clearIntent.putExtra("clear", true);
            getActivity().startService(clearIntent);
            getActivity().stopService(new Intent(getContext(), LocationLoggerService.class));
            isRunning = false;
        }
        updateStartStopButtonText();
        refreshImmediately();
    }

    private void updateStartStopButtonText() {
        Button button = (Button) getView().findViewById(R.id.button_start_stop);
        updateStartStopButtonText(button);
    }

    private void updateStartStopButtonText(Button button) {
        if (isRunning) {
            button.setText(getText(R.string.button_label_stop));
        }
        else {
            button.setText(getText(R.string.button_label_start));
        }
    }

    private void startStopButtonClick() {
        if (!isRunning) {
            Log.d(TAG, "Clicked start Button");
            getActivity().startService(new Intent(getContext(), LocationLoggerService.class));
            optimizeHandler.postDelayed(optimizePoints, OPTIMIZE_INTERVAL);
            isRunning = true;
        }
        else {
            Log.d(TAG, "Clicked stop Button");
            getActivity().stopService(new Intent(getContext(), LocationLoggerService.class));
            isRunning = false;
        }
        updateStartStopButtonText();
        refreshImmediately();
    }

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            // update time
            TextView timeView = (TextView) getView().findViewById(R.id.text_view_time);
            if (isRunning) {
                timeView.setText(Util.formatDuration(LocationLog.getElapsedSeconds()));
            }
            else {
                timeView.setText(Util.formatDuration(LocationLog
                        .getCurrentLocationLog().getDuration()));
            }

            // update distance
            TextView distanceView = (TextView) getView().findViewById(R.id.text_view_distance);
            distanceView.setText(Util.formatDistance(LocationLog
                    .getCurrentLocationLog().getDistance()));

            // update mapFragment
            if (isRunning) {
                mapFragment.update();
            }

            // schedule next refresh
            TrackFragment.this.refreshHandler.postDelayed(refresh, Util.Config.REFRESH_INTERVAL);
        }
    };

    private void refreshImmediately() {
        refreshHandler.removeCallbacksAndMessages(null);
        refreshHandler.post(refresh);
    }

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
                Util.Config.LOCATION_MIN_TIME, Util.Config.LOCATION_MIN_DISTANCE, this);
        locationManager.addGpsStatusListener(this);
        mapFragment.enableMyLocation();
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
        mapFragment.disableMyLocation();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateGpsInfo();
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                ImageView imageView = (ImageView) getView().findViewById(R.id.image_view_gps);
                imageView.setImageResource(R.drawable.ic_gps_fixed_24dp);
                break;
        }
    }

    private void updateGpsInfo() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        int totalSatellites = 0;
        int satellitesInFix = 0;
        for (GpsSatellite satellite: locationManager.getGpsStatus(null).getSatellites()) {
            if(satellite.usedInFix()) {
                satellitesInFix++;
            }
            totalSatellites++;
        }
        TextView textView = (TextView) getView().findViewById(R.id.text_view_satellites);
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
            imageView.setImageResource(R.drawable.ic_location_searching_24dp);
        }
        startLocationUpdates();
    }

    @Override
    public void onProviderDisabled(String string) {
        if (LocationManager.GPS_PROVIDER.equals(string)) {
            ImageView imageView = (ImageView) getView().findViewById(R.id.image_view_gps);
            imageView.setImageResource(R.drawable.ic_location_disabled_24dp);

            TextView textView = (TextView) getView().findViewById(R.id.text_view_satellites);
            textView.setText("0/0");

            textView = (TextView) getView().findViewById(R.id.text_view_gps_error);
            textView.setText("0 m");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (TrackFragment.isRunning) {
            refreshHandler.post(refresh);
            optimizeHandler.post(optimizePoints);
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
        optimizeHandler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
    }



    private final Runnable optimizePoints = new Runnable() {
        @Override
        public void run() {
            new PointOptimizer().execute(mapFragment);
        }
    };

    public class PointOptimizer extends AsyncTask<MapFragment, Void, Void> {

        @Override
        protected Void doInBackground(MapFragment... fragments) {
            fragments[0].optimizePoints();
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            if (TrackFragment.isRunning()) {
                optimizeHandler.postDelayed(optimizePoints, OPTIMIZE_INTERVAL);
            }
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
