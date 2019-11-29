package ch.ethz.smartenergy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.util.FVec;
import ch.ethz.smartenergy.model.BluetoothScan;
import ch.ethz.smartenergy.model.LocationScan;
import ch.ethz.smartenergy.model.ScanResult;
import ch.ethz.smartenergy.model.SensorReading;
import ch.ethz.smartenergy.service.DataCollectionService;
import ch.ethz.smartenergy.service.SensorScanPeriod;


public class MainActivity extends AppCompatActivity {

    private int internalCycle = 1;
    private int selectedViewGraph = 1;

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int PERMISSION_ALL = 4242;
    private int locationRequestCount = 0;

    private Predictor predictor_with_gps;
    private Predictor predictor_without_gps;

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment statsFragment = new StatsFragment();
    private final Fragment settingsFragment = new SettingsFragment();
    private final Fragment onboardingFragment = new OnboardingFragment();
    private Fragment previousFragment = homeFragment;
    private Fragment currentFragment;

    private Map<String, Integer> mostPresentWindow;
    private Map<String, Integer> mostPresentPersistent;

    private float[] predictions;
    private int countReset = 0;
    private double distance;
    private double accuracy;
    private int latestWiFiNumber = 0;
    private int oldWiFiNumber = -1;
    private int commonWiFi = 0;
    private List<String> latestWifiNames = null;
    private List<String> oldWifiNames = null;
    private Location latestKnownLocation = null;
    private double avgSpeedIcon;
    private int lastGPSUpdate = 0;
    private boolean gpsOn = false;
    private List<Integer> lastKnownModes = new ArrayList<>();
    private List<Boolean> lastGPSStatus = new ArrayList<>();
    private int blueNumbers = 0; // temp
    private int previousBlueNumbers = 0;
    private List<Integer> previousModes = new ArrayList<>();
    private double previousAccuracy = -1.0;
    private double meanAcc;
    private float points;

    private boolean scanning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        this.mostPresentWindow = new HashMap<>();
        this.mostPresentPersistent = new HashMap<>();
        for (String mode: Constants.ListModes) {
            this.mostPresentPersistent.put(mode, 0);
        }

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_view);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        try {
            // load pretrained predictor_with_gps
            InputStream model = getResources().openRawResource(R.raw.xgboost_final_with_gps);
            predictor_with_gps = new Predictor(model);
            InputStream model_no_gps = getResources().openRawResource(R.raw.xgboost_final_no_gps);
            predictor_without_gps = new Predictor(model_no_gps);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        registerReceiver();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstTime = preferences.getBoolean("first_time", true );
        if(firstTime){
            currentFragment = onboardingFragment;
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, onboardingFragment, Constants.TAG_FRAGMENT_ONBOARDING).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, settingsFragment, Constants.TAG_FRAGMENT_SETTINGS).hide(settingsFragment).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, statsFragment, Constants.TAG_FRAGMENT_STATS).hide(statsFragment).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,homeFragment, Constants.TAG_FRAGMENT_HOME).hide(homeFragment).commit();
        }else {
            currentFragment = homeFragment;

            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, onboardingFragment, Constants.TAG_FRAGMENT_ONBOARDING).hide(onboardingFragment).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, settingsFragment, Constants.TAG_FRAGMENT_SETTINGS).hide(settingsFragment).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, statsFragment, Constants.TAG_FRAGMENT_STATS).hide(statsFragment).commit();
            this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, homeFragment, Constants.TAG_FRAGMENT_HOME).commit();

        }
    }

    public void changeViewToPrevious() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().hide(currentFragment).
                show(this.previousFragment).commit();
        currentFragment = this.previousFragment;
    }

    public void changeViewToOnboarding() {
        previousFragment = currentFragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().hide(currentFragment).
                show(onboardingFragment).commit();
        currentFragment = onboardingFragment;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {

                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        previousFragment = currentFragment;
                        fragmentManager.beginTransaction().hide(currentFragment).
                                show(homeFragment).commit();
                        currentFragment = homeFragment;
                        return true;

                    case R.id.nav_stats:
                        previousFragment = currentFragment;
                        fragmentManager.beginTransaction().hide(currentFragment).
                                show(statsFragment).commit();
                        currentFragment = statsFragment;
                        return true;

                    case R.id.nav_settings:
                        previousFragment = currentFragment;
                        fragmentManager.beginTransaction().hide(currentFragment).
                                show(settingsFragment).commit();
                        currentFragment = settingsFragment;
                        return true;
                }

                return true;
            };


    @Override
    protected void onStart() {
        super.onStart();
        askPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private double calculateDistance(ArrayList<LocationScan> locationScans) {

        if (locationScans == null || locationScans.isEmpty()) {
            this.lastGPSUpdate++;
            return 0.0;
        }

        //this.accuracy = "";
        //locationScans.forEach(e->this.accuracy = this.accuracy + (int)e.getAccuracy() + " ");

        // Relax rules if we are missing GPS for a long time
        if (this.lastGPSUpdate >= Constants.MINUTES_WITHOUT_GPS) {
            locationScans.removeIf(location -> location.getAccuracy() >= 1000);
        } else {
            locationScans.removeIf(location -> location.getAccuracy() >= 30);
        }

        if (locationScans.isEmpty()) {
            this.lastGPSUpdate++;
            return 0.0;
        }

        this.lastGPSUpdate = 0;

        double lon1 = locationScans.get(0).getLongitude();
        double lon2 = locationScans.get(locationScans.size() - 1).getLongitude();
        double lat1 = locationScans.get(0).getLatitude();
        double lat2 = locationScans.get(locationScans.size() - 1).getLatitude();

        if (this.latestKnownLocation == null) {
            this.latestKnownLocation = new Location("");
            this.latestKnownLocation.setLongitude(lon1);
            this.latestKnownLocation.setLatitude(lat1);
        }

        if (this.latestKnownLocation.getLatitude() == lat2 && lon2 == this.latestKnownLocation.getLongitude()) {
            this.lastGPSUpdate++;
            return 0.0;
        }

        float[] result = new float[1];
        Location.distanceBetween(this.latestKnownLocation.getLatitude(), this.latestKnownLocation.getLongitude(), lat2, lon2, result);

        this.latestKnownLocation = new Location("");
        this.latestKnownLocation.setLongitude(locationScans.get(locationScans.size() - 1).getLongitude());
        this.latestKnownLocation.setLatitude(locationScans.get(locationScans.size() - 1).getLatitude());

        return (double) result[0];
    }

    private void updateJSON(String key, ArrayList<LocationScan> locationScans, double distance) {

        boolean isFilePresent = isFilePresent(this, "data.json");
        if(isFilePresent) {
            String jsonString = read(this, "data.json");
            //do the json parsing here and do the rest of functionality of app
        } else {
            boolean isFileCreated = create(this, "{}");
            if(isFileCreated) {
                //proceed with storing the first
            } else {
                //show error or try again.
            }
        }

        JSONObject json = new JSONObject();

        try {
            json = new JSONObject(read(this, "data.json"));
        } catch (JSONException err){
            Log.d("Error", err.toString());
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateOnly = new SimpleDateFormat("dd-MM-yyyy");

        boolean exists = true;

        JSONObject activity = new JSONObject();
        try {
            activity = json.getJSONObject(dateOnly.format(cal.getTime()));
        }catch (JSONException err){
            Log.d("Error", err.toString());
            exists = false;
        }

        if(exists){
            try {
                for (String mode: Constants.ListModes) {
                    this.mostPresentPersistent.put(mode, activity.getJSONObject(mode).getInt("time"));
                    activity.getJSONObject(mode).put("time", this.mostPresentPersistent.get(mode));
                }
                Integer temp = this.mostPresentPersistent.get(key);
                if (temp != null) {
                    this.mostPresentPersistent.put(key, temp + 1);
                    activity.getJSONObject(key).put("time", this.mostPresentPersistent.get(key));

                    if (this.lastGPSStatus.get(this.lastGPSStatus.size() - 1) && this.lastGPSStatus.size() == 4) {
                        if (!this.lastGPSStatus.get(0) && !this.lastGPSStatus.get(1) && !this.lastGPSStatus.get(2)) {
                            String key_update_gps = findMostRecentModeWithGPS();
                            double d = activity.getJSONObject(key_update_gps).getDouble("distance");
                            activity.getJSONObject(key_update_gps).put("distance", d + distance);
                        }
                    } else {
                        double d = activity.getJSONObject(key).getDouble("distance");
                        activity.getJSONObject(key).put("distance", d + distance);
                    }

                }
            }catch (JSONException err){
                Log.d("Error", err.toString());
            }
        } else {
            try {
                activity.put("date", Calendar.getInstance().getTime());
                for (String mode : Constants.ListModes) {
                    this.mostPresentPersistent.put(mode, 0);
                    JSONObject data = new JSONObject();
                    data.put("time", this.mostPresentPersistent.get(mode));
                    data.put("distance", 0.0);
                    activity.put(mode, data);
                }
                Integer temp = this.mostPresentPersistent.get(key);
                if (temp != null) {
                    this.mostPresentPersistent.put(key, temp + 1);
                    activity.getJSONObject(key).put("time", this.mostPresentPersistent.get(key));
                    activity.getJSONObject(key).put("distance", distance);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            json.put(dateOnly.format(cal.getTime()), activity);
        }catch (JSONException err){
            Log.d("Error", err.toString());
            exists = true;
        }

        create(this, json.toString());

    }

    private String findMostRecentModeWithGPS() {
        Map<Integer, Integer> mapModes = new HashMap<>();

        for (Integer modeIndex : this.lastKnownModes) {
            mapModes.put(modeIndex, mapModes.getOrDefault(modeIndex, 0) + 1);
        }

        Integer key = Collections.max(mapModes.entrySet(), Map.Entry.comparingByValue()).getKey();
        return Constants.ListModes[key];
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        if (data == null) return;

        if (data.containsKey(Constants.WindowBroadcastExtraName)) {
            ScanResult scan = (ScanResult) data.getSerializable(Constants.WindowBroadcastExtraName);
            if (scan != null) {

                double meanMagnitude = calculateMeanMagnitude(scan.getAccReadings(), true);
//                double minAcc = calculateMinAcc(scan.getAccReadings());
//                double maxAcc = calculateMaxAcc(scan.getAccReadings());
                double maxSpeed = calculateMaxSpead(scan.getLocationScans());
                double avgSpeed = calculateAvgSpeed(scan.getLocationScans());
                double minSpeed = calculateMinSpead(scan.getLocationScans());
                double accuracyGPS = calculateAccuracy(scan.getLocationScans());
//                double gyroAvg = calculateAvgGyro(scan.getGyroReadings());
//                double gyroMin = calculateMinGyro(scan.getGyroReadings());
//                double gyroMax = calculateMaxGyro(scan.getGyroReadings());

                if (MainActivity.this.oldWiFiNumber != -1) {
                    MainActivity.this.oldWiFiNumber = MainActivity.this.latestWiFiNumber;
                    MainActivity.this.oldWifiNames = new ArrayList<>();
                    if (MainActivity.this.latestWifiNames != null) {
                        MainActivity.this.oldWifiNames = new ArrayList<>(MainActivity.this.latestWifiNames);
                    }
                }

                MainActivity.this.latestWifiNames = new ArrayList<>();
                if (scan.getWifiScans().size() >= 1) {
                    MainActivity.this.latestWiFiNumber = scan.getWifiScans().get(0).getDiscoveredDevices().size();
                    scan.getWifiScans().get(0).getDiscoveredDevices().forEach(e ->
                            MainActivity.this.latestWifiNames.add(e.getSsid()));
                } else {
                    MainActivity.this.latestWiFiNumber = 0;
                }

                if (MainActivity.this.oldWiFiNumber == -1) {
                    MainActivity.this.oldWiFiNumber = MainActivity.this.latestWiFiNumber;
                    MainActivity.this.oldWifiNames = new ArrayList<>();
                    if (MainActivity.this.latestWifiNames != null) {
                        MainActivity.this.oldWifiNames = new ArrayList<>(MainActivity.this.latestWifiNames);
                        MainActivity.this.commonWiFi = (int) MainActivity.this.latestWifiNames.stream().filter(MainActivity.this.oldWifiNames::contains).count();
                    }
                }

                boolean isStill = isStill(meanMagnitude, avgSpeed, maxSpeed, avgAccX(scan.getAccReadings()),
                        avgAccY(scan.getAccReadings()), avgAccZ(scan.getAccReadings()), getLatitude(scan.getLocationScans()),
                        getLongitude(scan.getLocationScans()));

                int bluetoothNumber = getBluetoothNumbers(scan.getBluetoothScans());
                MainActivity.this.blueNumbers = bluetoothNumber;

                float[] predictionsXGBoost = predict(
                        meanMagnitude, avgAccX(scan.getAccReadings()), avgAccY(scan.getAccReadings()), avgAccZ(scan.getAccReadings()),
                        maxSpeed, minSpeed, avgSpeed, accuracyGPS,
                        bluetoothNumber,
                        calculateMeanMagnitude(scan.getGyroReadings(), false), avgAccX(scan.getGyroReadings()), avgAccY(scan.getGyroReadings()), avgAccZ(scan.getGyroReadings()),
                        calculateMeanMagnitude(scan.getMagnReadings(), false), avgAccX(scan.getMagnReadings()), avgAccY(scan.getMagnReadings()), avgAccZ(scan.getMagnReadings()));

                updateData(isStill, predictionsXGBoost, scan.getLocationScans());

                MainActivity.this.internalCycle++;
            }
        }
        }
    };

    private double calculateAccuracy(ArrayList<LocationScan> locationScans) {
        double accuracy = locationScans.stream().mapToDouble(LocationScan::getAccuracy).average().orElse(-1.0);
        if (accuracy == -1.0) {
            if (this.previousAccuracy != -1.0) {
                accuracy = this.previousAccuracy;
            } else {
                accuracy = 0.0;
            }
        }
        this.previousAccuracy = accuracy;
        this.accuracy = accuracy;
        return accuracy;
    }

    private int getBluetoothNumbers(ArrayList<BluetoothScan> bluetoothScans) {
        if (bluetoothScans == null) {
            return 0;
        }

        if (bluetoothScans.isEmpty() && countReset <= 3) {
            countReset++;
            return this.previousBlueNumbers;
        } else if (bluetoothScans.isEmpty()){
            this.previousBlueNumbers = 0;
            countReset = 0;
        }

        List<String> listBluetooth = new ArrayList<>();
        bluetoothScans.forEach(e -> e.getDiscoveredDevices().forEach(device -> {
            listBluetooth.add(device.getMac());
        }));

        this.previousBlueNumbers = (int)listBluetooth.stream().distinct().count();
        return (int)listBluetooth.stream().distinct().count();
    }

    private double calculateMinAcc(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;
        List<Double> listMagnitudes = new ArrayList<>();

        for (SensorReading reading : accReadings) {
            double sumOfPows = reading.getValueOnXAxis() * reading.getValueOnXAxis() +
                    reading.getValueOnYAxis() * reading.getValueOnYAxis() +
                    reading.getValueOnZAxis() * reading.getValueOnZAxis();

            System.out.println( "\n\n" + reading.getValueOnYAxis() +  "\n\n");
            listMagnitudes.add(Math.sqrt(sumOfPows));
        }

        return listMagnitudes.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    private double calculateMaxAcc(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;
        List<Double> listMagnitudes = new ArrayList<>();

        for (SensorReading reading : accReadings) {
            double sumOfPows = reading.getValueOnXAxis() * reading.getValueOnXAxis() +
                    reading.getValueOnYAxis() * reading.getValueOnYAxis() +
                    reading.getValueOnZAxis() * reading.getValueOnZAxis();

            System.out.println( "\n\n" + reading.getValueOnYAxis() +  "\n\n");
            listMagnitudes.add(Math.sqrt(sumOfPows));
        }

        return listMagnitudes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private double calculateMinSpead(ArrayList<LocationScan> locationScans) {
        double minSpeed = 9999;
        for (LocationScan locationScan : locationScans) {
            if (locationScan.getSpeed() < minSpeed && locationScan.getAccuracy() <= 50) {
                minSpeed = locationScan.getSpeed();
            }
        }

        if (minSpeed == 9999) {
            return 0.0;
        }
        return minSpeed;
    }

    private void updateData(boolean isStill, float[] predictionsXGBoost, ArrayList<LocationScan> locationScans) {
        List<Float> listPredictions = new ArrayList<>();
        ArrayList<LocationScan> locationScans2 = new ArrayList<>(locationScans);

        for (Integer index : this.previousModes) {
            predictions[index] += Constants.BONUS_PREVIOUS;
        }

        for (float prediction : predictions) {
            listPredictions.add(prediction);
        }

        int indexMaxMode = listPredictions.indexOf(listPredictions.stream().max(Float::compare).get());

        if (this.previousModes.size() == 4) {
            this.previousModes.clear();
        }
        this.previousModes.add(this.previousModes.size(), indexMaxMode);

        if (isStill) {
            this.mostPresentWindow.put(Constants.ListModes[8], this.mostPresentWindow.getOrDefault(Constants.ListModes[8], 0) + 1);
        } else {
            this.mostPresentWindow.put(Constants.ListModes[indexMaxMode], this.mostPresentWindow.getOrDefault(Constants.ListModes[indexMaxMode], 0) + 1);
        }

        distance += calculateDistance(locationScans);
        isGPSOn(locationScans2);
        if (this.lastGPSStatus.size() <= 4) {
            this.lastGPSStatus.add(this.gpsOn);
        } else {
            this.lastGPSStatus.remove(0);
            this.lastGPSStatus.add(this.gpsOn);
        }

        HomeFragment homeFragment = (HomeFragment) MainActivity.this.homeFragment;
        homeFragment.updateIcons(this.mostPresentWindow, this.accuracy, this.latestWiFiNumber, this.commonWiFi, convertToKmPerHour(this.avgSpeedIcon), this.gpsOn, this.blueNumbers, this.meanAcc, this.predictions, this.points);

        if (this.internalCycle == 10) {

            String key = Collections.max(this.mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey();
            int key_number = Arrays.asList(Constants.ListModes).indexOf(key);
            boolean isNotStill;
            isNotStill = !key.equals("Still");

            if (this.lastKnownModes.size() <= 4) {
                this.lastKnownModes.add(key_number);
            } else {
                this.lastKnownModes.remove(0);
                this.lastKnownModes.add(key_number);
            }

            updateJSON(key, locationScans, distance);

            // Update Home and Stats Fragment
            homeFragment = (HomeFragment) MainActivity.this.homeFragment;
            homeFragment.updateChart(isNotStill);
            StatsFragment statsFragment = (StatsFragment) MainActivity.this.statsFragment;
            statsFragment.updateChart();

            this.internalCycle = 0;
            this.mostPresentWindow = new HashMap<>();

            distance = 0;
        }

    }

    private void isGPSOn(ArrayList<LocationScan> locationScans) {
        if (locationScans == null || this.accuracy == -1.0) {
            this.gpsOn = false;
            return;
        }

        this.gpsOn = false;
        locationScans.forEach(e -> {
            if (e.getAccuracy() < 250) {
                this.gpsOn = true;
            }
        });
        if (this.gpsOn || this.accuracy < 250) {
            this.gpsOn = true;
            return;
        }

        if (locationScans.isEmpty()) {
            this.gpsOn = false;
            return;
        }

        this.lastGPSUpdate = 0;
        double lon2 = locationScans.get(locationScans.size() - 1).getLongitude();
        double lat2 = locationScans.get(locationScans.size() - 1).getLatitude();

        if (this.latestKnownLocation != null) {
            if (this.latestKnownLocation.getLatitude() == lat2 && lon2 == this.latestKnownLocation.getLongitude()) {
                this.gpsOn = false;
            }
        }
    }

    private float[] predict(double meanMagnitude, double avgAccX, double avgAccY, double avgAccZ,
                            double maxSpeed, double minSpeed, double avgSpeed, double accuracyGPS,
                            int bluetoothNumbers,
                            double magnitudeGyro, double avgGyroX, double avgGyroY, double avgGyroZ,
                            double magnitudeMagn, double avgMagnX, double avgMagnY, double avgMagnZ) {


        float[] predictions;
        if (this.gpsOn) {
            // build features vector
            double[] features = {meanMagnitude, avgAccY, accuracyGPS,
                    bluetoothNumbers, magnitudeGyro,
                    magnitudeMagn, avgMagnX, avgMagnY, avgMagnZ,
                    maxSpeed, minSpeed};
            FVec features_vector = FVec.Transformer.fromArray(features, false);
            //predict
            predictions = predictor_with_gps.predict(features_vector);
        } else {
            // build features vector
            double[] features = {meanMagnitude, avgAccX, avgAccY, avgAccZ,
                    bluetoothNumbers, magnitudeGyro,
                    magnitudeMagn, avgMagnX, avgMagnY, avgMagnZ};
            FVec features_vector = FVec.Transformer.fromArray(features, false);
            //predict
            predictions = predictor_without_gps.predict(features_vector);
            predictions[1] += Constants.BONUS_TRAIN_NO_GPS;
        }

        this.predictions = predictions;
        return predictions;
    }

    private double convertToKmPerHour(double metersPerSecond) {
        return ((metersPerSecond*3600)/1000);
    }

    private boolean isStill(double meanMagnitude, double avgSpeed, double maxSpeed, double avgAccX, double avgAccY, double avgAccZ, double latitude, double longitude) {
        float points = 0.0f;

        // Case where no GPS detected
        if (!this.gpsOn) {
            if (meanMagnitude <= 0.40) {
                points += 0.80f;
            } else if (meanMagnitude <= 0.46 && meanMagnitude > 0.40) {
                points += 0.50f;
            }

            if (this.latestWiFiNumber >= 1) {
                float percent = Math.abs(1f - ((float)this.commonWiFi / (float)this.latestWiFiNumber));
                if (this.latestWiFiNumber == this.commonWiFi) {
                    points += 0.40f;
                } else if (this.latestWiFiNumber >= 3){
                    if (percent <= 0.10f) {
                        points += 0.30f;
                    }
                }
            } else {
                points += 0.30f;
            }

            this.points = points;
            return points >= 0.79f;
        }

        // Wifi numbers
        /*if (this.latestWiFiNumber >= 1) {
            float percent = Math.abs(1f - ((float)this.commonWiFi / (float)this.latestWiFiNumber));
            if (this.latestWiFiNumber == this.commonWiFi) {
                points += 0.70f;
            } else {
                if (percent <= 0.10f) {
                    points += 0.50f;
                }
                if (percent <= 0.25f && percent > 0.10f) {
                    points += 0.30f;
                }
            }
        } else {
            points += 0.30f;
        }*/

        if (meanMagnitude <= 0.20) {
            points += 0.50f;
        } else if (meanMagnitude <= 0.45 && meanMagnitude > 0.20) {
            points += 0.30f;
        }

        if (convertToKmPerHour(avgSpeed) <= Constants.MaxAvgSpeedStill && convertToKmPerHour(avgSpeed) > Constants.StopSpeed) {
            points += 0.30f;
        }

        if (convertToKmPerHour(avgSpeed) <= Constants.TopAvgSpeedAllowed && convertToKmPerHour(avgSpeed) > Constants.MaxAvgSpeedStill) {
            points += 0.20f;
        }

        if (convertToKmPerHour(avgSpeed) <= Constants.StopSpeed) {
            points += 0.70f;
        }

        if (convertToKmPerHour(maxSpeed) <= Constants.MaxSpeedStill) {
            points += 0.30f;
        }

        this.points = points;
        return points >= 0.79f;
    }

    private double calculateMaxSpead(ArrayList<LocationScan> locationScans) {
        double maxSpeed = 0;
        for (LocationScan locationScan : locationScans) {
            if (locationScan.getAccuracy() <= 50) {
                if (locationScan.getSpeed() > maxSpeed) {
                    maxSpeed = locationScan.getSpeed();
                }
            }
        }
        return maxSpeed;
    }

    private double calculateAvgSpeed(ArrayList<LocationScan> locationScans) {
        this.avgSpeedIcon = locationScans.stream().filter(e->e.getAccuracy()<=50).mapToDouble(LocationScan::getSpeed).average().orElse(0.0);
        return locationScans.stream().filter(e->e.getAccuracy()<=50).mapToDouble(LocationScan::getSpeed).average().orElse(0.0);
    }

    private double getLatitude(ArrayList<LocationScan> locationScans) {
        if (locationScans != null && !locationScans.isEmpty()) {
            return locationScans.get(locationScans.size() - 1).getLatitude();
        } else {
            return 0.0;
        }
    }

    private double getLongitude(ArrayList<LocationScan> locationScans) {
        if (locationScans != null && !locationScans.isEmpty()) {
            return locationScans.get(locationScans.size() - 1).getLongitude();
        } else {
            return 0.0;
        }
    }

    private double avgAccX(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        accReadings.forEach(val -> Math.abs(val.getValueOnXAxis()));
        return accReadings.stream().mapToDouble(SensorReading::getValueOnXAxis).average().orElse(0.0);
    }

    private double avgAccY(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        accReadings.forEach(val -> Math.abs(val.getValueOnYAxis()));
        return accReadings.stream().mapToDouble(SensorReading::getValueOnYAxis).average().orElse(0.0);
    }

    private double avgAccZ(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        accReadings.forEach(val -> Math.abs(val.getValueOnZAxis()));
        return accReadings.stream().mapToDouble(SensorReading::getValueOnZAxis).average().orElse(0.0);
    }

    private double calculateMeanMagnitude(ArrayList<SensorReading> accReadings, boolean isAcc) {
        if (accReadings.size() == 0) {
            if (isAcc) this.meanAcc = 0.0; // Remove this
            return 0;
        }

        double sumOfMagnitudes = 0;

        for (SensorReading reading : accReadings) {
            double sumOfPows = reading.getValueOnXAxis() * reading.getValueOnXAxis() +
                    reading.getValueOnYAxis() * reading.getValueOnYAxis() +
                    reading.getValueOnZAxis() * reading.getValueOnZAxis();

            System.out.println( "\n\n" + reading.getValueOnYAxis() +  "\n\n");
            sumOfMagnitudes += Math.sqrt(sumOfPows);
        }

        if (isAcc) this.meanAcc = sumOfMagnitudes / accReadings.size(); // Remove this
        return sumOfMagnitudes / accReadings.size();
    }

    /**
     * Register broadcast receiver
     */
    private void registerReceiver() {
        IntentFilter it = new IntentFilter();
        it.addAction(Constants.WindowBroadcastActionName);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, it);
    }

    /**
     * Ask for fine location permissions
     */
    private void askPermissions() {

        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        ActivityCompat.requestPermissions(MainActivity.this, permissions.toArray(new String[0]), PERMISSION_ALL);
        locationRequestCount = 0;
        locationSettingsRequest();
    }

    /**
     * Build location settings request
     */
    private void locationSettingsRequest() {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(SensorScanPeriod.GPS_SENSOR_PERIOD);
        //Highest possible accuracy => high battery usage
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //We can handle fast location updates, this limit is to not stress
        mLocationRequest.setFastestInterval(100);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    /**
     * Handle activity result from location settings request
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == Activity.RESULT_CANCELED) {
            locationRequestCount++;
            if (locationRequestCount >= 3) {
                showLocationsRequiredGoodbyeDialog();
            } else {
                showLocationsRequiredDialog();
            }
        }
    }

    /**
     * Show dialog that we really need the location
     */
    private void showLocationsRequiredDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.locations_required_title));
        alertDialog.setMessage(getString(R.string.locations_required_text));
        alertDialog.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                locationSettingsRequest();
            }
        });
        alertDialog.create().show();
    }

    /**
     * Location permission not given. Close application
     */
    private void showLocationsRequiredGoodbyeDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.locations_required_title));
        alertDialog.setMessage(getString(R.string.locations_required_goodbye));
        alertDialog.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        alertDialog.create().show();
    }

    /**
     * Start collecting data on button click
     *
     * @param view
     */
    public void startScanning(View view) {
        if(!scanning) {

            // Update Home Fragment
            HomeFragment hF = (HomeFragment) homeFragment;
            hF.startScanning();
            view.setEnabled(false);
            Intent serviceIntent = new Intent(this, DataCollectionService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(serviceIntent);
            } else {
                this.startService(serviceIntent);
            }
        }
        scanning = true;
    }

    public boolean isScanning(){
        return scanning;
    }

    public void rightClickTopBar(View view) {
        getNextElement(1);
        StatsFragment sF = (StatsFragment) statsFragment;
        HomeFragment hF = (HomeFragment) homeFragment;
        sF.menuClick(this.selectedViewGraph);
        hF.menuClick(this.selectedViewGraph);
    }

    public void leftClickTopBar(View view) {
        getNextElement(-1);
        StatsFragment sF = (StatsFragment) statsFragment;
        HomeFragment hF = (HomeFragment) homeFragment;
        sF.menuClick(this.selectedViewGraph);
        hF.menuClick(this.selectedViewGraph);
    }

    private void getNextElement(int indexToAdd) {
        if ((this.selectedViewGraph == Constants.MENU_OPTIONS.length - 1) && (indexToAdd == 1)) {
            this.selectedViewGraph = 0;
            return;
        }
        if ((this.selectedViewGraph == 0) && (indexToAdd == -1)) {
            this.selectedViewGraph = Constants.MENU_OPTIONS.length - 1;
            return;
        }
        this.selectedViewGraph += indexToAdd;
    }


    private String read(Context context, String fileName) {
        try {
            FileInputStream fis = context.openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException fileNotFound) {
            return null;
        }
    }

    private boolean create(Context context, String jsonString){
        try {
            FileOutputStream fos = context.openFileOutput("data.json",Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (IOException fileNotFound) {
            return false;
        }

    }

    public boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    public Fragment getHomeFragment() {
        return homeFragment;
    }

    public Fragment getStatsFragment() {
        return statsFragment;
    }

}


