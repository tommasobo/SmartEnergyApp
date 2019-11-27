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
    public static final double MaxAvgSpeedStill = 2.2f;
    public static final double TopAvgSpeedAllowed = 3.0f;
    public static final double MaxAccStill = 0.6;
    public static final double MaxSpeedStill = 4.0;
    public static final double StopSpeed = 0.0f;
    public static final double BONUS_PREVIOUS = 0.03;
    public static final String[] ListModes = {"Foot", "Train", "Bus", "Car", "Tram", "Bicycle", "E-Bike", "Motorcycle", "Still"};

    // Emission Logic
    public static final double[] CO2PerMode = {20.0, 31.375, 72.116, 120.0, 22.45, 10.0, 20.0, 72.0, 0.0};
    public static final double[] WattPerMode = {59.2, 272.0, 273.0, 455.8, 70.0, 32.2, 50.0, 270.0, 0.0};
    public static final double co2MultiplierDefaultCar = 1.55;
    public static final double co2MultiplierSmallCar = 1;
    public static final double co2MultiplierBigCar = 1.98;
    public static final double co2MultiplierElectricCar = 0.5;

    public static final double co2MultiplierMeatDietWalking = 11.5;
    public static final double co2MultiplierMeatDietBicylce = 11.5;
    public static final double co2MultiplierVeganDietWalking = 1.0;
    public static final double co2MultiplierVeganDietBicycle = 1.0;
    public static final double co2MultiplierAverageDietWalking = 5.0;
    public static final double co2MultiplierAverageDietBicycle = 5.0;
    public static final double co2MultiplierNoDietWalking = 0.0;
    public static final double co2MultiplierNoDietBicycle = 0.0;

    // UI
    public static final String[] listModesVerbose = {"on foot", "in a train", "on a bus", "in a car", "in a tram", "on a bicycle", "on a e-bike", "riding a bike", "not doing much"};
    public static final String TAG_FRAGMENT_HOME = "fragment_home";
    public static final String TAG_FRAGMENT_STATS = "fragment_stats";
    public static final String TAG_FRAGMENT_SETTINGS = "fragment_settings";
    public static final String TAG_FRAGMENT_ONBOARDING = "fragment_onboarding";
    public static final String[] TIMEFRAME_OPTIONS = {"Past Week", "This Month", "This Year"};
    public static final String[] MENU_OPTIONS = {"Time per Mode", "CO₂ per Mode", "Distance per Mode", "Energy per Mode"};
    public static final CharSequence[] PIE_GRAPH_DESCRIPTION = {"Minutes Per Transportation For Today",
            "CO₂ (g) Per Transportation For Today", "Distance (km) Per Transportation For Today", "Energy (Wh) Per Transportation For Today"};
    public static final String[] GRAPH_DESCRIPTION = {"Minutes Per Transportation Mode for", "CO₂ (g) Per Transportation Mode for", "Distance (km) per Mode for", "Energy (Wh) per Mode for"};
    public static final int[] MATERIAL_COLORS = {
            rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
            rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
            rgb("#D81B60")
    };
    public static final String PLANT_BASESD = "Plant Based";
    public static final String MEAT_BASED = "Meat Based";
    public static final String AVERAGE_DIET = "Average";
    public static final String IGNORE_DIET = "Ignore Diet";

    public static final String DEFAULT_CAR = "Average Car";
    public static final String SMALL_CAR = "Small Car";
    public static final String BIG_CAR = "Sport Car";
    public static final String ELECTRIC_CAR = "Electric Car";
    public static final int MINUTES_WITHOUT_GPS = 23; // Equals two minutes


    private static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }
}
