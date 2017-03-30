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
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;

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

import static it.faerb.herakles.LocationLog.getCurrentLocationLog;
import static it.faerb.herakles.Util.Config.DEFAULT_ZOOM_LEVEL;
import static it.faerb.herakles.Util.Config.LOCATION_MIN_TIME;
import static it.faerb.herakles.Util.Config.MAP_EVENT_AGGREGATION_DURATION;
import static it.faerb.herakles.Util.Config.MAX_CONCURRENT_GEOPOINTS;
import static it.faerb.herakles.Util.Config.OPTIMIZE_INTERVAL;

public class MapFragment extends Fragment {

    private static final String TAG = "Herakles.MapFragment";

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param live Wether or not this map should receive new locations
     *
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance(boolean live) {
        MapFragment fragment = new MapFragment();

        Bundle args = new Bundle();
        args.putBoolean("live", live);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            live = bundle.getBoolean("live");
        }
    }

    private boolean live;
    private MyLocationNewOverlay locationOverlay;
    private Polyline polylineOverlay;
    private ArrayList<GeoPoint> polylinePoints = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
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

        if (live) {
            // set center to last known location
            Location lastKnown = getLastKnownLocation();
            if (lastKnown != null) {
                mapController.setCenter(new GeoPoint(lastKnown));
            }
        }

        polylineOverlay = new Polyline();
        polylineOverlay.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        map.getOverlays().add(polylineOverlay);

        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), map);
        map.getOverlays().add(locationOverlay);
        locationOverlay.enableFollowLocation();


        // setup buttons
        final ImageButton zoomToMeButton = (ImageButton) view.findViewById(R.id.button_zoom);
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
                hideZoomButton();
            }
        });
        zoomToMeButton.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void optimizePoints() {
        List<Location> list = getCurrentLocationLog().getLocations();
        // compute how many points are acceptable until next optimization
        int ACCEPTABLE_POINT_COUNT = 0;
        if (live) {
            ACCEPTABLE_POINT_COUNT = MAX_CONCURRENT_GEOPOINTS -
                    (OPTIMIZE_INTERVAL / LOCATION_MIN_TIME);
        }
        else {
            ACCEPTABLE_POINT_COUNT = MAX_CONCURRENT_GEOPOINTS;
        }
        final int SKIP_POINTS = (int) Math.ceil((float)list.size()/ACCEPTABLE_POINT_COUNT);
        ArrayList<GeoPoint> newPoints = new ArrayList<>();
        for(int i = 0; i<list.size(); i+=SKIP_POINTS) {
            newPoints.add(new GeoPoint(list.get(i)));
        }
        Log.d(TAG, String.format("optimized from: %d to: %d", list.size(), newPoints.size()));
        polylinePoints = newPoints;
        polylineOverlay.setPoints(polylinePoints);
    }


    public GeoPoint getMyLocation() {
        if (locationOverlay != null) {
            return locationOverlay.getMyLocation();
        }
        return null;
    }

    // prevent multiple button animations
    private boolean zoomToMeButtonVisible = false;

    void hideZoomButton() {
        if (zoomToMeButtonVisible == false) {
            return;
        }
        zoomToMeButtonVisible = false;
        final ImageButton zoomToMeButton =
                (ImageButton) getView().findViewById(R.id.button_zoom);
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

    void showZoomButton() {
        if (zoomToMeButtonVisible == true) {
            return;
        }
        zoomToMeButtonVisible = true;
        final ImageButton zoomToMeButton =
                (ImageButton) getView().findViewById(R.id.button_zoom);
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

    public void enableMyLocation() {
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    public void disableMyLocation() {
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }
    }

    public void update() {
        // update polyline
        for (Location loc : LocationLog.getNewLocations()) {
            polylinePoints.add(new GeoPoint(loc));
        }
        polylineOverlay.setPoints(polylinePoints);
    }
}
