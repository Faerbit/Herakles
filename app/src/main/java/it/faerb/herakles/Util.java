package it.faerb.herakles;

class Util {

    public static String formatSeconds(int pSeconds) {
        int hours = pSeconds / 3600;
        int minutes = pSeconds / 60 % 60;
        int seconds = pSeconds % 60;

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
