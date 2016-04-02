package it.faerb.herakles;

import android.location.Location;

import java.util.ArrayList;


/* This is the data storage for the current sports activity of the user*/
public class LocationLog {
    private static class LocationLogHolder {
        static final LocationLog log = new LocationLog();
    }

    public static LocationLog getInstance() {
        return LocationLogHolder.log;
    }

    ArrayList<Location> locationLog;

    private LocationLog() {
        locationLog = new ArrayList<>();
    }

    public static ArrayList<Location> getLocationLog() {
        return getInstance().locationLog;
    }

    public static void addLocation(Location loc) {
        getInstance().locationLog.add(loc);
    }
}
