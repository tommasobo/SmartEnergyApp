package ch.ethz.smartenergy;

import android.graphics.Color;

public class Constants {

    // Scanning Logic
    public static final String DataBroadcastActionName = "DATA";
    public static final String WindowBroadcastActionName = "WINDOW";
    public static final String WindowBroadcastExtraName = "WindowData";
    public static final String WifiScanExtraName = "WifiScan";
    public static final String BluetoothScanExtraName = "BluetoothScan";
    public static final String LocationScanExtraName = "LocationScan";
    public static final String AccReadingExtraName = "AccelerometerReading";
    public static final String GyroReadingExtraName = "GyroscopeReading";
    public static final String MagnReadingExtraName = "MagneticFieldReading";

    // General Logic
    public static final double MaxSpeedStill = 2.5;
    public static final double MaxAccStill = 0.6;
    public static double MaxAvgSpeedStill = 6;
    public static final String[] ListModes = {"Foot", "Train", "Bus", "Car", "Tram", "Bicycle", "E-Bike", "Motorcycle", "Still"};

    // Emission Logic
    public static final double[] CO2PerMode = {1.0, 34.0, 44.0, 98.0, 20.0, 1.0, 1.0, 72.0, 1.0};
    public static final double[] WattPerMode = {3.0, 7.0, 34.0, 58.0, 40.0, 34.0, 55.0, 101.0, 1.0};
    public static final double co2MultiplierDefaultCar = 1;
    public static final double co2MultiplierSmallCar = 0.8;
    public static final double co2MultiplierBigCar = 1.8;
    public static final double co2MultiplierElectricCar = 0.5;

    public static final double co2MultiplierMeatDietWalking = 1.5;
    public static final double co2MultiplierMeatDietBicylce = 1.5;
    public static final double co2MultiplierVeganDietWalking = 0.5;
    public static final double co2MultiplierVeganDietBicycle = 0.5;
    public static final double co2MultiplierAverageDietWalking = 1.0;
    public static final double co2MultiplierAverageDietBicycle = 1.0;
    public static final double co2MultiplierNoDietWalking = 0.0;
    public static final double co2MultiplierNoDietBicycle = 0.0;


    // UI
    public static final String[] listModesVerbose = {"on foot", "in a train", "on a bus", "in a car", "in a tram", "on a bicycle", "on a e-bike", "riding a bike", "not doing much"};
    public static final String TAG_FRAGMENT_HOME = "fragment_home";
    public static final String TAG_FRAGMENT_STATS = "fragment_stats";
    public static final String TAG_FRAGMENT_SETTINGS = "fragment_settings";
    public static final String[] MENU_OPTIONS = {"Time per Mode", "CO₂ per Mode", "Distance per Mode", "Energy per Mode"};
    public static final CharSequence[] PIE_GRAPH_DESCRIPTION = {"Minutes Per Transportation For Today",
            "CO₂ Per Transportation For Today", "Distance (m) Per Transportation For Today", "Energy (Wh) Per Transportation For Today"};
    public static final String[] GRAPH_DESCRIPTION = {"Minutes Per Transportation Mode for", "CO₂ (g) Per Transportation Mode for", "Distance (m) per Mode", "Energy (Wh) per Mode"};
    public static final int[] MATERIAL_COLORS = {
            rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
            rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
            rgb("#D81B60")
    };
    public static final String PLANT_BASESD = "Plant Based";
    public static final String MEAT_BASED = "Meat Based";
    public static final String AVERAGE_DIET = "Average";
    public static final String IGNORE_DIET = "Ignore Diet";

    public static final String DEFAULT_CAR = "Default Car";
    public static final String SMALL_CAR = "Small Car";
    public static final String BIG_CAR = "Sport Car";
    public static final String ELECTRIC_CAR = "Electric Car";


    private static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }
}
