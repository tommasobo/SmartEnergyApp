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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
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

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int PERMISSION_ALL = 4242;
    private int locationRequestCount = 0;

    private Predictor predictor;
    private Interpreter tflite_model;

    private List<Double> train_mean;
    private List<Double> train_std;

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

        //I added this if statement to keep the selected fragment when rotating the device
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment(), "Home").commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                String selected = null;

                switch (item.getItemId()) {
                    case R.id.nav_home:
                        selectedFragment = new HomeFragment();
                        selected = "Home";
                        break;
                    case R.id.nav_stats:
                        selectedFragment = new StatsFragmenet();
                        selected = "Stats";
                        break;
                    case R.id.nav_settings:
                        selectedFragment = new SettingsFragment();
                        selected = "Settings";
                        break;
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                        selectedFragment, selected).commit();

                List<Fragment> allFragments = getSupportFragmentManager().getFragments();
                allFragments.forEach(e -> System.out.println("TAG: " + e.getTag()));

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

    private void updateJSON(String key) {

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
                    this.mostPresentPersistent.put(mode, activity.getInt(mode));

                }
                Integer temp = this.mostPresentPersistent.get(key);
                if (temp != null) {
                    this.mostPresentPersistent.put(key, temp + 1);
                }
            }catch (JSONException err){
                Log.d("Error", err.toString());
            }
        }

        try {
            activity.put("date", Calendar.getInstance().getTime());
            for (String mode: Constants.ListModes) {
                activity.put(mode, this.mostPresentPersistent.get(mode));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            json.put(dateOnly.format(cal.getTime()), activity);
        }catch (JSONException err){
            Log.d("Error", err.toString());
            exists = true;
        }

        create(this,"data.json", json.toString());

    }

    private void writeActivity(String act) {

        try (FileWriter file = new FileWriter(this.getFilesDir().getAbsolutePath() + "/" + "storage.txt", true)) {

            file.write(Calendar.getInstance().getTime() + "," + act + "\n");
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
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

                    updateData(isStill, predictionsXGBoost);

                    //updateChart(isStill, predictionsXGBoost);

                    predict_NN(scan.getAccReadings());

                    MainActivity.this.internalCycle++;
                }
            }

        }

    };

    private void updateData(boolean isStill, float[] predictionsXGBoost) {
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

        if (this.internalCycle == 2) {
            String key = Collections.max(this.mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey();
            writeActivity(key);

            updateJSON(key);

            JSONObject json = new JSONObject();

            try {
                json = new JSONObject(read(this, "data.json"));
            } catch (JSONException err) {
                Log.d("Error", err.toString());
            }

            SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
            JSONObject todayData = new JSONObject();
            try {
                todayData = json.getJSONObject(date.format(Calendar.getInstance().getTime()));
            } catch (JSONException e) {
                e.printStackTrace();
            }


            List<Fragment> allFragments = getSupportFragmentManager().getFragments();
            allFragments.forEach(e -> System.out.println("TAG: " + e.getTag()));

            HomeFragment frag = ((HomeFragment) getSupportFragmentManager().findFragmentByTag("Home"));

            frag.updateChart(todayData);

            this.internalCycle = 0;
            this.mostPresentWindow = new HashMap<>();
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

        //appendResult(predictions);
    }

    private float[] predict(double meanMagnitude, double maxSpeed) {
        // build features vector
        double[] features = {meanMagnitude, maxSpeed};
        FVec features_vector = FVec.Transformer.fromArray(features, false);

        //predict
        float[] predictions = predictor.predict(features_vector);
        //showResult(predictions);

        this.predictions = predictions;
        return predictions;
    }

    private double convertToKmPerHour(double metersPerSecond) {
        return ((metersPerSecond*3600)/1000);
    }

    private boolean isStill(double meanMagnitude, double avgSpeed, double maxSpeed, double avgAccX, double avgAccY, double avgAccZ, double latitude, double longitude) {
        // Calculate if standing still
        double speedInKm = convertToKmPerHour(avgSpeed);

        if (speedInKm <= Constants.MaxSpeedStill &&  meanMagnitude <= Constants.MaxAccStill) {
            return true;
        }
        return false;
    }

    /*private void updateChart(boolean isStill, float[] predictions) {

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

        if (this.internalCycle == 2) {
            String key = Collections.max(this.mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey();
            writeActivity(key);

            updateJSON(key);

            JSONObject json = new JSONObject();

            try {
                json = new JSONObject(read(this, "data.json"));
            } catch (JSONException err) {
                Log.d("Error", err.toString());
            }

            SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
            JSONObject todayData = new JSONObject();
            try {
                todayData = json.getJSONObject(date.format(Calendar.getInstance().getTime()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            List<PieEntry> pieChartEntries = new ArrayList<>();

            for (String activity : Constants.ListModes) {
                try {
                    if (todayData.getInt(activity) != 0) {
                        pieChartEntries.add(new PieEntry(todayData.getInt(activity), activity));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Outside values
            Legend l = this.chart.getLegend();
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDirection(Legend.LegendDirection.LEFT_TO_RIGHT);
            l.setWordWrapEnabled(true);
            l.setDrawInside(false);

            PieDataSet set = new PieDataSet(pieChartEntries,"");

            set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            set.setValueLinePart1OffsetPercentage(100f); /** When valuePosition is OutsideSlice, indicates offset as percentage out of the slice size */
            //set.setValueLinePart1Length(0.6f); /** When valuePosition is OutsideSlice, indicates length of first half of the line */
            //set.setValueLinePart2Length(0.6f); /** When valuePosition is OutsideSlice, indicates length of second half of the line */

            //this.chart.setExtraOffsets(0.f, 5.f, 0.f, 5.f); // Ofsets of the view chart to prevent outside values being cropped /** Sets extra offsets (around the chart view) to be appended to the auto-calculated offsets.*/
            /*set.setColors(ColorTemplate.COLORFUL_COLORS);
            PieData data = new PieData(set);
            this.chart.setData(data);
            data.setValueTextSize(10f);


            this.chart.getDescription().setEnabled(false);
            this.chart.animateXY(1200, 1200);

            this.chart.invalidate();

            this.internalCycle = 0;
            this.mostPresentWindow = new HashMap<>();
        }

    }*/


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

        accReadings.stream().forEach(val -> Math.abs(val.getValueOnXAxis()));
        return accReadings.stream().mapToDouble(val -> val.getValueOnXAxis()).average().orElse(0.0);
    }

    private double avgAccY(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        accReadings.stream().forEach(val -> Math.abs(val.getValueOnYAxis()));
        return accReadings.stream().mapToDouble(val -> val.getValueOnYAxis()).average().orElse(0.0);
    }

    private double avgAccZ(ArrayList<SensorReading> accReadings) {
        if (accReadings.size() == 0) return 0;

        accReadings.stream().forEach(val -> Math.abs(val.getValueOnZAxis()));
        return accReadings.stream().mapToDouble(val -> val.getValueOnZAxis()).average().orElse(0.0);
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
        view.setEnabled(false);
        Intent serviceIntent = new Intent(this, DataCollectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(serviceIntent);
        } else {
            this.startService(serviceIntent);
        }
        boolean isFilePresent = isFilePresent(this, "storage.json");
        if(isFilePresent) {
            String jsonString = read(this, "storage.json");
            //do the json parsing here and do the rest of functionality of app
        } else {
            boolean isFileCreated = create(this, "storage.json", "{}");
            if(isFileCreated) {
                //proceed with storing the first todo  or show ui
            } else {
                //show error or try again.
            }
        }
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


