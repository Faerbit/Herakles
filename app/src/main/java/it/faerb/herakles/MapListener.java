package it.faerb.herakles;

import android.util.Log;

import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

class MapListener implements org.osmdroid.events.MapListener {

    private static final String TAG = "Herakles.MapListener";

    private TrackFragment trackFragment;

    public MapListener(TrackFragment trackFragment){
        this.trackFragment = trackFragment;
    }

    @Override
    public boolean onScroll(final ScrollEvent event) {
        Log.d(TAG, "onScroll");
        final float centerLat = Math.round(
                event.getSource().getMapCenter().getLatitude() * 1000.0f) / 1000.0f;
        final float centerLong = Math.round(
                event.getSource().getMapCenter().getLongitude() * 1000.0f) / 1000.0f;
        final float myLocLat = Math.round(
                trackFragment.getMyLocation().getLatitude() * 1000.0f) / 1000.0f;
        final float myLocLong = Math.round(
                trackFragment.getMyLocation().getLongitude() * 1000.0f) / 1000.0f;
        if (centerLat != myLocLat && centerLong != myLocLong) {
            trackFragment.showZoomToMeButton();
        }
        return false;
    }

    @Override
    public boolean onZoom(final ZoomEvent event) {
        return false;
    }
}
