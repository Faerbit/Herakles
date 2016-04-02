package it.faerb.herakles;

class Util {

    public static String formatSeconds(int pSeconds) {
        int hours = pSeconds / 3600;
        int minutes = pSeconds / 60 % 60;
        int seconds = pSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
