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
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.util.FVec;
import ch.ethz.smartenergy.model.LocationScan;
import ch.ethz.smartenergy.model.ScanResult;
import ch.ethz.smartenergy.model.SensorReading;
import ch.ethz.smartenergy.service.DataCollectionService;
import ch.ethz.smartenergy.service.SensorScanPeriod;


public class MainActivity extends AppCompatActivity {

    private int internalCycle = 0;
    private int selectedViewGraph = 0;

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int PERMISSION_ALL = 4242;
    private int locationRequestCount = 0;

    private Predictor predictor;
    private Interpreter tflite_model;

    private List<Double> train_mean;
    private List<Double> train_std;

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment statsFragment = new StatsFragment();
    private final Fragment settingsFragment = new SettingsFragment();
    private Fragment currentFragment = homeFragment;

    private Map<String, Integer> mostPresentWindow;
    private Map<String, Integer> mostPresentPersistent;

    private float[] predictionsNN;
    private float[] predictions;

    public float[] getPredictionsNN() {
        return predictionsNN;
    }
    public float[] getPredictions() {
        return predictions;
    }

    private double distance;

    private String accuracy = "";
    private boolean isNotStill = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.mostPresentWindow = new HashMap<>();
        this.mostPresentPersistent = new HashMap<>();
        for (String mode: Constants.ListModes) {
            this.mostPresentPersistent.put(mode, 0);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_view);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        try {
            // load pretrained predictor
            InputStream model = getResources().openRawResource(R.raw.xgboost);
            predictor = new Predictor(model);

            // load pretrained nn predictor
            load_assets();


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        registerReceiver();


        this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, settingsFragment, Constants.TAG_FRAGMENT_HOME).hide(settingsFragment).commit();
        this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, statsFragment, Constants.TAG_FRAGMENT_STATS).hide(statsFragment).commit();
        this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,homeFragment, Constants.TAG_FRAGMENT_SETTINGS).commit();

    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {

                FragmentManager fragmentManager = getSupportFragmentManager();
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        fragmentManager.beginTransaction().hide(currentFragment).
                                show(homeFragment).commit();
                        currentFragment = homeFragment;
                        return true;

                    case R.id.nav_stats:
                        fragmentManager.beginTransaction().hide(currentFragment).
                                show(statsFragment).commit();
                        currentFragment = statsFragment;
                        return true;

                    case R.id.nav_settings:
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
            return 0.0;
        }

        this.accuracy = "";
        locationScans.forEach(e->this.accuracy = this.accuracy + (int)e.getAccuracy() + " ");

        locationScans.removeIf(location -> location.getAccuracy() >= 30);

        if (locationScans.isEmpty()) {
            return 0.0;
        }

        double lon1 = locationScans.get(0).getLongitude();
        double lon2 = locationScans.get(locationScans.size() - 1).getLongitude();
        double lat1 = locationScans.get(0).getLatitude();
        double lat2 = locationScans.get(locationScans.size() - 1).getLatitude();

        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);

        return (double) result[0];
    }

    private void updateJSON(String key, ArrayList<LocationScan> locationScans, double distance) {

        boolean isFilePresent = isFilePresent(this, "data.json");
        if(isFilePresent) {
            String jsonString = read(this, "data.json");
            //do the json parsing here and do the rest of functionality of app
        } else {
            boolean isFileCreated = create(this, "data.json", "{}");
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
                    double d = activity.getJSONObject(key).getDouble("distance");
                    activity.getJSONObject(key).put("distance", d + distance);
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

        create(this,"data.json", json.toString());

    }

    private void updateAAAA(ArrayList<LocationScan> locationScans) {

        if (locationScans == null || locationScans.isEmpty()) {
            return;
        }

        double lon1 = locationScans.get(0).getLongitude();
        double lon2 = locationScans.get(locationScans.size() - 1).getLongitude();
        double lat1 = locationScans.get(0).getLatitude();
        double lat2 = locationScans.get(locationScans.size() - 1).getLatitude();


        String ret = "";

        try {
            InputStream inputStream = this.openFileInput("aaaa.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }




        try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("aaaa.txt", Context.MODE_PRIVATE));
                outputStreamWriter.write(ret + "\n");
                outputStreamWriter.write(lat1 + " " + lon1 + " " + lat2 + " " + lon2 + "\n");
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getExtras();
            if (data == null) return;

            if (data.containsKey(Constants.WindowBroadcastExtraName)) {
                ScanResult scan = (ScanResult) data.getSerializable(Constants.WindowBroadcastExtraName);
                if (scan != null) {
                    double meanMagnitude = calculateMeanMagnitude(scan.getAccReadings());
                    double maxSpeed = calculateMaxSpead(scan.getLocationScans());
                    double avgSpeed = calculateAvgSpeed(scan.getLocationScans());

                    System.out.println("\n\nAvg Speed: " + avgSpeed + " MaxSpeed: " + maxSpeed + "\n\n");

                    boolean isStill = isStill(meanMagnitude, avgSpeed, maxSpeed, avgAccX(scan.getAccReadings()),
                            avgAccY(scan.getAccReadings()), avgAccZ(scan.getAccReadings()), getLatitude(scan.getLocationScans()),
                            getLongitude(scan.getLocationScans()));
                    float[] predictionsXGBoost = predict(meanMagnitude, maxSpeed);

                    predict_NN(scan.getAccReadings());

                    updateData(isStill, predictionsXGBoost, scan.getLocationScans());

                    HomeFragment homeFragment = (HomeFragment) MainActivity.this.homeFragment;
                    if (homeFragment != null) {
                        homeFragment.showResult();
                        homeFragment.appendResult();
                    }

                    MainActivity.this.internalCycle++;
                }
            }

        }

    };

    private void updateData(boolean isStill, float[] predictionsXGBoost, ArrayList<LocationScan> locationScans) {
        List<Float> listPredictions = new ArrayList<Float>();
        for (float prediction : predictions) {
            listPredictions.add(prediction);
        }

        int indexMaxMode = listPredictions.indexOf(listPredictions.stream().max(Float::compare).get());

        if (isStill) {
            this.mostPresentWindow.put(Constants.ListModes[8], this.mostPresentWindow.getOrDefault(Constants.ListModes[8], 0) + 1);
        } else {
            this.mostPresentWindow.put(Constants.ListModes[indexMaxMode], this.mostPresentWindow.getOrDefault(Constants.ListModes[indexMaxMode], 0) + 1);
        }

        distance += calculateDistance(locationScans);
        HomeFragment homeFragment = (HomeFragment) MainActivity.this.homeFragment;
        homeFragment.updateIcons(this.mostPresentWindow, this.accuracy);

        if (this.internalCycle == 11) {

            String key = Collections.max(this.mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey();
            if (key.equals("Still")) {
                isNotStill = false;
            } else {
                isNotStill = true;
            }

            updateJSON(key, locationScans, distance);
            //updateAAAA(locationScans);

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

    private void load_assets() {
        try {
            MappedByteBuffer tfliteModel = loadModelFile(MainActivity.this, "example_nn.tflite");
            Interpreter.Options options = new Interpreter.Options();
            this.tflite_model = new Interpreter(tfliteModel, options);


            train_mean = new ArrayList<>();
            train_std = new ArrayList<>();

            try {
                InputStream is = getAssets().open("train_mean.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    train_mean.add(Double.valueOf(line));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                InputStream is = getAssets().open("train_std.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    train_std.add(Double.valueOf(line));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void predict_NN(List<SensorReading> acc_readings) {
        List<Double> magnitudes = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        for (SensorReading reading : acc_readings) {
            double sumOfPows = reading.getValueOnXAxis() * reading.getValueOnXAxis() +
                    reading.getValueOnYAxis() * reading.getValueOnYAxis() +
                    reading.getValueOnZAxis() * reading.getValueOnZAxis();

            magnitudes.add(sumOfPows);
            times.add(reading.getReadingTime().getTime());
        }

        // Do nearest neighbor interpolation
        List<Double> magnitudes_interpolated = new ArrayList<>();
        double abs_sum = 0;
        if (times.isEmpty()) {
            return;
        }
        long windowStart = times.get(0);
        for (int i = 0; i < 5000; i += 20) {
            long timeNode = windowStart + i;
            long last_diff = Math.abs(windowStart - timeNode);
            for (int j = 0; j < times.size(); j++) {
                long current_diff = Math.abs(times.get(j) - timeNode);
                if (current_diff <= last_diff) {
                    last_diff = current_diff;
                    if (j + 1 == times.size()) {
                        double value = magnitudes.get(j);
                        magnitudes_interpolated.add(value);
                        abs_sum += Math.abs(value);
                    }
                } else {
                    double value = magnitudes.get(j);
                    magnitudes_interpolated.add(value);
                    abs_sum += Math.abs(value);
                    break;
                }

            }
        }

        List<Double> magnitudes_normalized = new ArrayList<>();

        // Scale with l1 norm
        for (int i = 0; i < magnitudes_interpolated.size(); i++)
            magnitudes_normalized.add(magnitudes_interpolated.get(i) / abs_sum);


        // TODO: Scale to 0 mean and unit variance


        float[][] inputs = new float[250][1];
        for (int i = 0; i < magnitudes_normalized.size(); i++) {
            inputs[i][0] = (magnitudes_normalized.get(i).floatValue() - train_mean.get(i).floatValue()) / train_std.get(i).floatValue();
        }
        Log.d("INPUTS: ", magnitudes_normalized.toString());
        float[][] outputs = new float[1][8];

        this.tflite_model.run(inputs, outputs);


        float[] predictions = new float[outputs[0].length];

        for (int i = 0; i < outputs[0].length; i++) {
            predictions[i] = outputs[0][i];
        }

        this.predictionsNN = predictions;

    }

    private float[] predict(double meanMagnitude, double maxSpeed) {
        // build features vector
        double[] features = {meanMagnitude, maxSpeed};
        FVec features_vector = FVec.Transformer.fromArray(features, false);

        //predict
        float[] predictions = predictor.predict(features_vector);

        this.predictions = predictions;
        return predictions;
    }

    private double convertToKmPerHour(double metersPerSecond) {
        return ((metersPerSecond*3600)/1000);
    }

    private boolean isStill(double meanMagnitude, double avgSpeed, double maxSpeed, double avgAccX, double avgAccY, double avgAccZ, double latitude, double longitude) {
        // Calculate if standing still
        double avgSpeedInKm = convertToKmPerHour(avgSpeed);

        return avgSpeedInKm <= Constants.MaxAvgSpeedStill && maxSpeed <= Constants.MaxSpeedStill;
    }

    private double calculateMaxSpead(ArrayList<LocationScan> locationScans) {
        double maxSpeed = 0;
        for (LocationScan locationScan : locationScans) {
            if (locationScan.getSpeed() > maxSpeed) {
                maxSpeed = locationScan.getSpeed();
            }
        }

        return maxSpeed;
    }

    private double calculateAvgSpeed(ArrayList<LocationScan> locationScans) {
        return locationScans.stream().mapToDouble(val -> val.getSpeed()).average().orElse(0.0);
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

    private double calculateMeanMagnitude(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        double sumOfMagnitudes = 0;

        for (SensorReading reading : accReadings) {
            double sumOfPows = reading.getValueOnXAxis() * reading.getValueOnXAxis() +
                    reading.getValueOnYAxis() * reading.getValueOnYAxis() +
                    reading.getValueOnZAxis() * reading.getValueOnZAxis();

            System.out.println( "\n\n" + reading.getValueOnYAxis() +  "\n\n");
            sumOfMagnitudes += Math.sqrt(sumOfPows);
        }



        return sumOfMagnitudes / accReadings.size();
    }

    private MappedByteBuffer loadModelFile(Context context, String fileName) throws IOException {

        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("example_nn.tflite");
        FileInputStream inputStream = fileDescriptor.createInputStream();
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

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
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_ALL);
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

    public void rightClickTopBar(View view) {
        getNextElement(1);
        StatsFragment sF = (StatsFragment) statsFragment;
        HomeFragment hF = (HomeFragment) homeFragment;
        //sF.menuClick(this.selectedViewGraph);
        hF.menuClick(this.selectedViewGraph);
    }

    public void leftClickTopBar(View view) {
        getNextElement(-1);
        StatsFragment sF = (StatsFragment) statsFragment;
        HomeFragment hF = (HomeFragment) homeFragment;
        //sF.menuClick(this.selectedViewGraph);
        hF.menuClick(this.selectedViewGraph);
    }

    private void getNextElement(int indexToAdd) {
        if ((this.selectedViewGraph == 2) && (indexToAdd == 1)) {
            this.selectedViewGraph = 0;
            return;
        }
        if ((this.selectedViewGraph == 0) && (indexToAdd == -1)) {
            this.selectedViewGraph = 2;
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
        } catch (FileNotFoundException fileNotFound) {
            return null;
        } catch (IOException ioException) {
            return null;
        }
    }

    private boolean create(Context context, String fileName, String jsonString){
        try {
            FileOutputStream fos = context.openFileOutput(fileName,Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (FileNotFoundException fileNotFound) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

    }

    public boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }



}


