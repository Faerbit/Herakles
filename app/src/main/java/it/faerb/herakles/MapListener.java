/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package it.faerb.herakles;

import android.util.Log;

import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

import static it.faerb.herakles.Util.Config.DEFAULT_ZOOM_LEVEL;

class MapListener implements org.osmdroid.events.MapListener {

    private static final String TAG = "Herakles.MapListener";

    private MapFragment mapFragment;

    public MapListener(MapFragment mapFragment){
        this.mapFragment = mapFragment;
    }

    @Override
    public boolean onScroll(final ScrollEvent event) {
        Log.d(TAG, "onScroll");
        if (mapFragment.getMyLocation() == null) {
            return false;
        }
        final float centerLat = Math.round(
                event.getSource().getMapCenter().getLatitude() * 1000.0f) / 1000.0f;
        final float centerLong = Math.round(
                event.getSource().getMapCenter().getLongitude() * 1000.0f) / 1000.0f;
        final float myLocLat = Math.round(
                mapFragment.getMyLocation().getLatitude() * 1000.0f) / 1000.0f;
        final float myLocLong = Math.round(
                mapFragment.getMyLocation().getLongitude() * 1000.0f) / 1000.0f;
        if (centerLat != myLocLat && centerLong != myLocLong) {
            mapFragment.showZoomButton();
            return true;
        }
        return false;
    }

    @Override
    public boolean onZoom(final ZoomEvent event) {
        Log.d(TAG, "onZoom");
        // do not show button when the current location is unknown
        if (mapFragment.getMyLocation() == null) {
            return false;
        }
        if (event.getZoomLevel() != DEFAULT_ZOOM_LEVEL) {
            mapFragment.showZoomButton();
            return true;
        }
        return false;
    }
}
