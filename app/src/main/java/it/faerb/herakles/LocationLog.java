package it.faerb.herakles;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LocationLog {

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

    private List<Location> locationLog;
    private float distance = 0.0f;
    private long duration = 0;

    private LocationLog() {
        locationLog = Collections.synchronizedList(new ArrayList<Location>());
    }

    public static synchronized void addLocation(Location loc) {
        getCurrentLocationLog().locationLog.add(loc);
        if (getCurrentLocationLog().locationLog.size() > 1) {
            Location lastLoc = getCurrentLocationLog().locationLog
                    .get(getCurrentLocationLog().locationLog.size() - 2);
            getCurrentLocationLog().distance +=  loc.distanceTo(lastLoc);
            getCurrentLocationLog().duration += (loc.getTime() - lastLoc.getTime()) / 1e3;
        }
    }

    public static synchronized void replaceCurrentLocationLog(Context context, int id) {
        Log.d(TAG, String.format("replaceCurrentLocationLog: before locs: %d", getCurrentLocationLog().locationLog.size()));
        currentLocationLog = loadFile(context, id);
        Log.d(TAG, String.format("replaceCurrentLocationLog: after locs: %d", getCurrentLocationLog().locationLog.size()));
    }

    public float getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public long getBegin() {
        if (locationLog.size() == 0) {
            return 0;
        }
        else {
            return locationLog.get(0).getTime();
        }
    }

    public static synchronized void clear() {
        getCurrentLocationLog().locationLog.clear();
        getCurrentLocationLog().distance = 0.0f;
        getCurrentLocationLog().duration = 0;
    }

    public static void deleteFile(Context context, int position) {
        Log.d(TAG, String.format("deleteFile: %d", position));
        File file = getFiles(context).get(position);
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
            Gson gson = new Gson();
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
                return filename.endsWith(".loclog");
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

    private static LocationLog loadFile(Context context, int id) {
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

    public static synchronized ArrayList<LocationLog> loadFiles(Context context, int start, int end) {
        end = Math.min(end, getFilesCount(context));
        ArrayList<LocationLog> ret = new ArrayList<>();
        for (int i = start; i<end; i++) {
            ret.add(loadFile(context, i));
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
