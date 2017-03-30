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
import android.location.Criteria;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import static it.faerb.herakles.Util.Config.DEFAULT_ZOOM_LEVEL;
import static it.faerb.herakles.Util.Config.LOCATION_MIN_TIME;
import static it.faerb.herakles.Util.Config.MAP_EVENT_AGGREGATION_DURATION;
import static it.faerb.herakles.Util.Config.MAX_CONCURRENT_GEOPOINTS;
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
    private MyLocationNewOverlay locationOverlay;
    private Polyline polylineOverlay;
    private ArrayList<GeoPoint> polylinePoints = new ArrayList<>();

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
        MapView map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setMultiTouchControls(true);
        MapListener mapListener = new MapListener(this);
        DelayedMapListener delayedMapListener = new DelayedMapListener(mapListener,
                MAP_EVENT_AGGREGATION_DURATION);
        map.setMapListener(delayedMapListener);
        final IMapController mapController = map.getController();
        mapController.setZoom(DEFAULT_ZOOM_LEVEL);

        // set center to last known location
        Location lastKnown = getLastKnownLocation();
        if (lastKnown != null) {
            mapController.setCenter(new GeoPoint(lastKnown));
        }

        polylineOverlay = new Polyline();
        polylineOverlay.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        optimizeHandler.post(optimizePoints);
        map.getOverlays().add(polylineOverlay);

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), map);
        map.getOverlays().add(locationOverlay);
        locationOverlay.enableFollowLocation();


        // setup buttons
        final ImageButton zoomToMeButton = (ImageButton) view.findViewById(R.id.button_zoom_to_me);
        zoomToMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GeoPoint loc = locationOverlay.getMyLocation();
                if (loc != null) {
                    mapController.animateTo(loc);
                }
                mapController.zoomTo(DEFAULT_ZOOM_LEVEL);
                locationOverlay.enableFollowLocation();
                Log.d(TAG, "clicked zoom to me button");
                hideZoomToMeButton();
            }
        });
        zoomToMeButton.setVisibility(View.GONE);

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

            // update polyline
            if (isRunning) {
                for (Location loc : LocationLog.getNewLocations()) {
                    polylinePoints.add(new GeoPoint(loc));
                }
                polylineOverlay.setPoints(polylinePoints);
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
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        return lm.getLastKnownLocation(lm.getBestProvider(crit, true));
    }

    // prevent multiple button animations
    private boolean zoomToMeButtonVisible = false;

    void hideZoomToMeButton() {
        if (zoomToMeButtonVisible == false) {
            return;
        }
        zoomToMeButtonVisible = false;
        final ImageButton zoomToMeButton =
                (ImageButton) getView().findViewById(R.id.button_zoom_to_me);
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(1000);
        animation.setRepeatCount(0);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                zoomToMeButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        zoomToMeButton.startAnimation(animation);
        Log.d(TAG, "started hide zoom button animation");
    }

    void showZoomToMeButton() {
        if (zoomToMeButtonVisible == true) {
            return;
        }
        zoomToMeButtonVisible = true;
        final ImageButton zoomToMeButton =
                (ImageButton) getView().findViewById(R.id.button_zoom_to_me);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        animation.setRepeatCount(0);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
               zoomToMeButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        zoomToMeButton.startAnimation(animation);
        Log.d(TAG, "started show zoom button animation");
    }

    GeoPoint getMyLocation() {
        if (locationOverlay != null) {
            return locationOverlay.getMyLocation();
        }
        return null;
    }

    private final Runnable optimizePoints = new Runnable() {
        @Override
        public void run() {
            new PointOptimizer().execute(LocationLog.getCurrentLocationLog().getLocations());
        }
    };

    private class PointOptimizer extends AsyncTask<List<Location>, Void, ArrayList<GeoPoint>> {

        @Override
        protected ArrayList<GeoPoint> doInBackground(List<Location>... lists) {
            List<Location> list = lists[0];
            // compute how many points are acceptable until next optimization
            final int ACCEPTABLE_POINT_COUNT = MAX_CONCURRENT_GEOPOINTS -
                    (OPTIMIZE_INTERVAL/LOCATION_MIN_TIME);
            final int SKIP_POINTS = (int) Math.ceil((float)list.size()/ACCEPTABLE_POINT_COUNT);
            ArrayList<GeoPoint> ret = new ArrayList<>();
            for(int i = 0; i<list.size(); i+=SKIP_POINTS) {
                ret.add(new GeoPoint(list.get(i)));
            }
            Log.d(TAG, String.format("optimized from: %d to: %d", list.size(), ret.size()));
            return ret;
        }

        @Override
        protected void onPostExecute(ArrayList<GeoPoint> list) {
            polylinePoints = list;
            polylineOverlay.setPoints(polylinePoints);
            optimizeHandler.postDelayed(optimizePoints, OPTIMIZE_INTERVAL);
        }
    }
}
