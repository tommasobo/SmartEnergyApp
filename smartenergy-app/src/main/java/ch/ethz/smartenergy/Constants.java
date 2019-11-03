package ch.ethz.smartenergy;

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

    public static final double MaxSpeedStill = 5;
    public static final double MaxAccStill = 0.6;

    public static final String[] ListModes = {"Foot", "Train", "Bus", "Car", "Tram", "Bicycle", "E-Bike", "Motorcycle", "Still"};

    public static final String TAG_FRAGMENT_HOME = "fragment_home";
    public static final String TAG_FRAGMENT_STATS = "fragment_stats";
    public static final String TAG_FRAGMENT_SETTINGS = "fragment_settings";
}
