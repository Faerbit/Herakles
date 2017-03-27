/*
 * Herakles - Sports Activity Tracking App for Android
 * Copyright (c) 2017 Fabian Klemp
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package it.faerb.herakles;

public class Util {

    public static class Config {
        public static final float LOCATION_MIN_DISTANCE = 0;
        // Time values are in milliseconds
        public static final int LOCATION_MIN_TIME = 2000;
        public static final int SAVE_INTERVAL = 3 * 60 * 1000;
        public static final int OPTIMIZE_INTERVAL = 60 * 1000;
        public static final int REFRESH_INTERVAL = 1000;
        public static final int DEFAULT_ZOOM_LEVEL = 16;
        public static final int MAX_CONCURRENT_GEOPOINTS = 350;
    }

    public static String formatDuration(long pSeconds) {
        long hours = pSeconds / 3600;
        long minutes = pSeconds / 60 % 60;
        long seconds = pSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatDistance(float distance) {
        if (distance > 1000) {
            distance /= 1000;
            return String.format("%4.2f km", distance);
        }
        else {
            return String.format("%3.0f m", distance);
        }
    }
}
