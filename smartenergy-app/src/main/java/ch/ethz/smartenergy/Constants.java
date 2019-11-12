package ch.ethz.smartenergy;

import android.graphics.Color;

public class Constants {

    public static final String DataBroadcastActionName = "DATA";
    public static final String WindowBroadcastActionName = "WINDOW";
    public static final String WindowBroadcastExtraName = "WindowData";
    public static final String WifiScanExtraName = "WifiScan";
    public static final String BluetoothScanExtraName = "BluetoothScan";
    public static final String LocationScanExtraName = "LocationScan";
    public static final String AccReadingExtraName = "AccelerometerReading";
    public static final String GyroReadingExtraName = "GyroscopeReading";
    public static final String MagnReadingExtraName = "MagneticFieldReading";

    // Logic
    public static final double MaxSpeedStill = 5;
    public static final double MaxAccStill = 0.6;
    public static final String[] ListModes = {"Foot", "Train", "Bus", "Car", "Tram", "Bicycle", "E-Bike", "Motorcycle", "Still"};
    public static final int[] CO2PerMode = {1, 34, 44, 98, 20, 1, 1, 72, 1};



    // UI
    public static final String TAG_FRAGMENT_HOME = "fragment_home";
    public static final String TAG_FRAGMENT_STATS = "fragment_stats";
    public static final String TAG_FRAGMENT_SETTINGS = "fragment_settings";
    public static final String[] MENU_OPTIONS = {"Time per Mode", "CO₂ per Mode", "Average CO₂"};
    public static final CharSequence[] PIE_GRAPH_DESCRIPTION = {"Minutes Per Transportation Mode For Today",
            "CO₂ Per Transportation Mode For Today", "CO₂ Emitted Compared to Average For Today"};
    public static final int[] MATERIAL_COLORS = {
            rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
            rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
            rgb("#D81B60")
    };


    private static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }
}
