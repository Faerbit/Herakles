package it.faerb.herakles;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
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
import java.util.List;

public class LocationLog {

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

    private @interface MetaData {
        // Field tag only annotation
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

    private @interface Data {
        // Field tag only annotation
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

    @Data private List<Location> locationLog;
    @MetaData private float distance = 0.0f;
    @MetaData private long duration = 0;
    @MetaData private long begin = 0;

    private LocationLog() {
        locationLog = Collections.synchronizedList(new ArrayList<Location>());
    }

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
        Log.d(TAG, String.format("replaceCurrentLocationLog: before locs: %d", getCurrentLocationLog().locationLog.size()));
        currentLocationLog = loadFiles(context, id);
        Log.d(TAG, String.format("replaceCurrentLocationLog: after locs: %d", getCurrentLocationLog().locationLog.size()));
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

    public static synchronized void clear() {
        getCurrentLocationLog().locationLog.clear();
        getCurrentLocationLog().distance = 0.0f;
        getCurrentLocationLog().duration = 0;
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
        String filename = getCurrentLocationLog().getFilename();
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
        filename = getCurrentLocationLog().getMetaFilename();
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

    private String getFilename() {
        if (locationLog.size() > 0) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSSS");
            String ret = dateFormat.format(new Date(locationLog.get(0).getTime()));
            ret += ".loclog";
            return ret;
        }
        else {
            return "";
        }
    }

    private String getMetaFilename() {
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

    private static LocationLog loadFiles(Context context, int position) {
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
