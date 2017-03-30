/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package it.faerb.herakles;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationLog implements  ILocationLog{

    private static final String TAG = "Herakles.LocationLog";

    private static LocationLog currentLocationLog = null;

    public static LocationLog getCurrentLocationLog() {
        if (currentLocationLog == null) {
            synchronized (LocationLog.class) {
                return currentLocationLog = new LocationLog();
            }
        }
        return currentLocationLog;
    }

    private LocationLog() {
        locationLog = Collections.synchronizedList(new ArrayList<Location>());
    }

    @Data private List<Location> locationLog;
    @MetaData private float distance = 0.0f;
    @MetaData private long duration = 0;
    @MetaData private long begin = 0;

    public static synchronized void addLocation(Location loc) {
        getCurrentLocationLog().locationLog.add(loc);
        if (getCurrentLocationLog().locationLog.size() == 1) {
            getCurrentLocationLog().begin = loc.getTime();
        }
        if (getCurrentLocationLog().locationLog.size() > 1) {
            Location lastLoc = getCurrentLocationLog().locationLog
                    .get(getCurrentLocationLog().locationLog.size() - 2);
            getCurrentLocationLog().distance +=  loc.distanceTo(lastLoc);
            getCurrentLocationLog().duration += (loc.getTime() - lastLoc.getTime()) / 1e3;
        }
    }

    public static synchronized void replaceCurrentLocationLog(Context context, int id) {
        Log.d(TAG, String.format("replaceCurrentLocationLog: before locs: %d",
                getCurrentLocationLog().locationLog.size()));
        currentLocationLog = LocationLogIO.loadFiles(context, id);
        Log.d(TAG, String.format("replaceCurrentLocationLog: after locs: %d",
                getCurrentLocationLog().locationLog.size()));
    }

    public float getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public long getBegin() {
        return begin;
    }

    public long getFirstLocTime() {
        return locationLog.get(0).getTime();
    }

    public boolean containsData() {
        return locationLog.size() > 0;
    }

    private int locationsReturnedIndex = 0;

    public static synchronized List<Location> getNewLocations() {
        List<Location> ret = getCurrentLocationLog().locationLog.subList(
                getCurrentLocationLog().locationsReturnedIndex,
                getCurrentLocationLog().locationLog.size());
        getCurrentLocationLog().locationsReturnedIndex = getCurrentLocationLog().locationLog.size();
        return ret;
    }

    public static synchronized List<Location> getLocations() {
        getCurrentLocationLog().locationsReturnedIndex = getCurrentLocationLog().locationLog.size();
        return getCurrentLocationLog().locationLog.subList(0,
                getCurrentLocationLog().locationLog.size());
    }

    public static synchronized void clear() {
        getCurrentLocationLog().locationLog.clear();
        getCurrentLocationLog().distance = 0.0f;
        getCurrentLocationLog().duration = 0;
    }

    public static long getElapsedSeconds() {
        if (getCurrentLocationLog().locationLog.size() > 0) {
            long startTime = getCurrentLocationLog().locationLog.get(0).getElapsedRealtimeNanos();
            long elapsedNanos = SystemClock.elapsedRealtimeNanos() - startTime;
            long elapsedSeconds = (long) (elapsedNanos / 1e9);
            return elapsedSeconds;
        }
        else {
            return 0;
        }
    }
}
