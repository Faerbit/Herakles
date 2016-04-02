package it.faerb.herakles;

import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/* This is the data storage for the current sports activity of the user*/
public class LocationLog {
    private static class LocationLogHolder {
        static final LocationLog log = new LocationLog();
    }

    public static LocationLog getInstance() {
        return LocationLogHolder.log;
    }

    private List<Location> locationLog;

    private LocationLog() {
        locationLog = Collections.synchronizedList(new ArrayList<Location>());
    }

    public static List<Location> getLocationLog() {
        return getInstance().locationLog;
    }

    public static void addLocation(Location loc) {
        getInstance().locationLog.add(loc);
    }

    public static void clear() {
        getInstance().locationLog.clear();
    }
}
