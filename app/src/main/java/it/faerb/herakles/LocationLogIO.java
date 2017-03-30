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
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static it.faerb.herakles.LocationLog.getCurrentLocationLog;

class LocationLogIO implements ILocationLog {

    private static final String TAG = "Herakles.LocationLogIO";

    // To prevent typos
    private final static class FieldNames {
        public static final String TIME = "time";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ALTITUDE = "altitude";
        public static final String SPEED = "speed";
        public static final String ACCURACY = "accuracy";
    }

    private static class LocationSerializer implements JsonSerializer<Location> {

        @Override
        public JsonElement serialize(Location src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            long time = src.getTime();
            jsonObject.addProperty(FieldNames.TIME, time);

            double latitude = src.getLatitude();
            jsonObject.addProperty(FieldNames.LATITUDE, latitude);

            double longitude = src.getLongitude();
            jsonObject.addProperty(FieldNames.LONGITUDE, longitude);

            double altitude = src.getAltitude();
            jsonObject.addProperty(FieldNames.ALTITUDE, altitude);

            float speed = src.getSpeed();
            jsonObject.addProperty(FieldNames.SPEED, speed);

            float accuracy = src.getAccuracy();
            jsonObject.addProperty(FieldNames.ACCURACY, accuracy);

            return jsonObject;
        }
    }

    private static class LocationDeserializer implements JsonDeserializer {
        @Override
        public Location deserialize(JsonElement json, Type type,
                                    JsonDeserializationContext context) throws JsonParseException {
            Location location = new Location("gps");

            JsonObject jsonObject = json.getAsJsonObject();

            long time = jsonObject.get(FieldNames.TIME).getAsLong();
            location.setTime(time);

            double latitude = jsonObject.get(FieldNames.LATITUDE).getAsDouble();
            location.setLatitude(latitude);

            double longitude = jsonObject.get(FieldNames.LONGITUDE).getAsDouble();
            location.setLongitude(longitude);

            double altitude = jsonObject.get(FieldNames.ALTITUDE).getAsDouble();
            location.setAltitude(altitude);

            float speed = jsonObject.get(FieldNames.SPEED).getAsFloat();
            location.setSpeed(speed);

            float accuracy = jsonObject.get(FieldNames.ACCURACY).getAsFloat();
            location.setAccuracy(accuracy);

            return location;
        }
    }

    private static class OnlyMetaDataStrategy implements ExclusionStrategy {

        public OnlyMetaDataStrategy() {}

        @Override
        public boolean shouldSkipClass(Class cls) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes attr) {
            return attr.getAnnotation(MetaData.class) == null;
        }
    }

    private static class OnlyDataStrategy implements ExclusionStrategy {

        public OnlyDataStrategy() {}

        @Override
        public boolean shouldSkipClass(Class cls) {
            return false;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes attr) {
            return attr.getAnnotation(Data.class) == null;
        }
    }

    public static void deleteFile(Context context, int position) {
        Log.d(TAG, String.format("deleteFile: position: %d", position));
        File file = getDataFile(getFiles(context).get(position));
        Log.d(TAG, String.format("deleteFile: name: %s", file.getName()));
        file.delete();
        file = getFiles(context).get(position);
        Log.d(TAG, String.format("deleteFile: name: %s", file.getName()));
        file.delete();
        refreshFileCache(context);
    }

    public static synchronized void save(Context context) {
        String filename = getFilename();
        if (filename == "") {
            return;
        }
        Log.d(TAG, String.format("save: file %s", filename));
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Location.class, new LocationSerializer())
                    .setExclusionStrategies(new OnlyDataStrategy())
                    .create();
            outputStream.write(gson.toJson(getCurrentLocationLog()).getBytes());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        filename = getMetaFilename();
        Log.d(TAG, String.format("save: file %s", filename));
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            Gson gson = new GsonBuilder()
                    .setExclusionStrategies(new OnlyMetaDataStrategy())
                    .create();
            outputStream.write(gson.toJson(getCurrentLocationLog()).getBytes());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        refreshFileCache(context);
    }

    private static String getFilename() {
        if (getCurrentLocationLog().containsData()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS");
            String ret = dateFormat.format(new Date(getCurrentLocationLog().getFirstLocTime()));
            ret += ".loclog";
            return ret;
        }
        else {
            return "";
        }
    }

    private static String getMetaFilename() {
        String ret = getFilename();
        if (ret != "") {
            return ret + ".meta";
        }
        else {
            return "";
        }
    }

    public static int getFilesCount(Context context) {
        int ret = getFiles(context).size();
        //Log.d(TAG, String.format("getFilesCount: %d", ret));
        return ret;
    }

    private static ArrayList<File> fileListCache = null;

    private static void refreshFileCache(Context context) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".loclog.meta");
            }
        };
        fileListCache = new ArrayList<>(Arrays.asList(context.getFilesDir().listFiles(filter)));
        Collections.sort(fileListCache, Collections.<File>reverseOrder());
    }

    private static ArrayList<File> getFiles(Context context) {
        if (fileListCache == null) {
            refreshFileCache(context);
        }
        return fileListCache;
    }

    private static File getDataFile(File file) {
        String filename = file.getAbsolutePath();
        filename = filename.substring(0, filename.length() - 5);
        return new File(filename);
    }

    public static LocationLog loadFiles(Context context, int position) {
        try {
            FileInputStream inputStream = new FileInputStream(getFiles(context).get(position));
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(streamReader);
            StringBuffer metaContent = new StringBuffer();

            String buffer = bufferedReader.readLine();
            while (buffer != null) {
                metaContent.append(buffer);
                buffer = bufferedReader.readLine();
            }

            inputStream = new FileInputStream(getDataFile(getFiles(context).get(position)));
            streamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(streamReader);
            StringBuffer dataContent = new StringBuffer();

            buffer = bufferedReader.readLine();
            while (buffer != null) {
                dataContent.append(buffer);
                buffer = bufferedReader.readLine();
            }

            // merge contents
            StringBuffer content = metaContent.deleteCharAt(metaContent.length() - 1);
            content.append(",");
            content.append(dataContent.substring(1, dataContent.length()));

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Location.class, new LocationDeserializer())
                    .create();
            return gson.fromJson(content.toString(), LocationLog.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static LocationLog loadMetaFile(Context context, int id) {
        try {
            FileInputStream inputStream = new FileInputStream(getFiles(context).get(id));
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(streamReader);
            StringBuffer content = new StringBuffer();

            String buffer = bufferedReader.readLine();
            while (buffer != null) {
                content.append(buffer);
                buffer = bufferedReader.readLine();
            }

            Gson gson = new Gson();
            return gson.fromJson(content.toString(), LocationLog.class);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized ArrayList<LocationLog> loadMetaFiles(Context context, int start,
                                                                    int end) {
        //Log.d(TAG, String.format("loadFiles: start: %d end: %d", start, end));
        end = Math.min(end, getFilesCount(context));
        ArrayList<LocationLog> ret = new ArrayList<>();
        if (end < start) {
            return ret;
        }
        for (int i = start; i<end; i++) {
            ret.add(loadMetaFile(context, i));
        }
        return ret;
    }
}
