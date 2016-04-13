package it.faerb.herakles;

import java.util.ArrayList;

public class LocationLog {

    private String title = "asdf";
    private String subtitle = "subtitle";

    public LocationLog() {

    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public static ArrayList<LocationLog> getLocationLogs() {
        ArrayList<LocationLog> ret = new ArrayList<>();
        ret.add(new LocationLog());
        ret.add(new LocationLog());
        ret.add(new LocationLog());
        ret.add(new LocationLog());
        return ret;
    }
}
