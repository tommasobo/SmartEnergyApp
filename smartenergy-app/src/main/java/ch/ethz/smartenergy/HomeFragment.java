package ch.ethz.smartenergy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;


public class HomeFragment extends Fragment {

    private MainActivity mainActivity;
    private List<CircleImageView> listIcons;

    private TextSwitcher textSwitcher;
    private TextView dataTitle;
    private PieChart chart;
    private Button button;

    private List<PieEntry> pieChartEntries;
    private List<Integer> colorEntries;
    private String selectedGraphName = Constants.MENU_OPTIONS[0];


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        this.mainActivity = (MainActivity) this.getActivity();
        View v = getView();

        dataTitle = getView().findViewById(R.id.data_title);
        textSwitcher = getView().findViewById(R.id.textSwitcher);
        textSwitcher.setInAnimation(getView().getContext(), android.R.anim.slide_in_left);
        textSwitcher.setOutAnimation(getView().getContext(), android.R.anim.slide_out_right);
        textSwitcher.setCurrentText("not collecting any data");
        button = v.findViewById(R.id.button_start);

        this.listIcons = new ArrayList<>();
        for (int i = 0; i < Constants.ListModes.length; i++) {
            int resID = getResources().getIdentifier("iconMode" + i, "id", getActivity().getPackageName());
            CircleImageView img = getView().findViewById(resID);
            img.setAlpha(0.33f);
            img.setBorderWidth(3);
            img.setBorderColor(Constants.MATERIAL_COLORS[i]);
            this.listIcons.add(img);
        }

        chart = v.findViewById(R.id.chart_graph);
        this.chart.setNoDataText("No Data Available for Today");
        this.updateChart(true);
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

    void updateChart(boolean needsAnimation) {

        if (!isFilePresent(getActivity(), "data.json")) {
            return;
        }
        JSONObject json = new JSONObject();

        try {
            json = new JSONObject(read(getActivity(), "data.json"));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

        TimeZone tz = TimeZone.getDefault();
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
        JSONObject todayData = new JSONObject();
        try {
            todayData = json.getJSONObject(date.format(Calendar.getInstance(tz).getTime()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.setDataGraph(todayData);
        if (pieChartEntries.size() == 0) {
            this.chart.clear();
            this.chart.invalidate();
            return;
        }

        this.setChartUI(needsAnimation);
    }

    private void setDataGraph(JSONObject todayData) {

        pieChartEntries = new ArrayList<>();
        colorEntries = new ArrayList<>();

        if (selectedGraphName.equals(Constants.MENU_OPTIONS[0])) {
            this.updateDataTime(todayData);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[1])) {
            this.updateCO2PerMode(todayData);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[2])) {
            this.updateDistancePerMode(todayData);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[3])){
            this.updateEnergyPerMode(todayData);
        }
    }

    private void updateDataTime(JSONObject todayData) {
        int i = 0;

        for (String activity : Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            try {
                if (todayData.getJSONObject(activity).getInt("time") != 0) {
                    pieChartEntries.add(new PieEntry(todayData.getJSONObject(activity).getInt("time"), activity));
                    colorEntries.add(Constants.MATERIAL_COLORS[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void updateCO2PerMode(JSONObject todayData) {

        int i = 0;
        for (String activity : Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            try {
                if (todayData.getJSONObject(activity).getDouble("distance") != 0.0) {
                    double gPerCO2 = todayData.getJSONObject(activity).getDouble("distance");
                    gPerCO2 = gPerCO2 * (Constants.CO2PerMode[i] / 1000);
                    if (activity.equals("Foot") || activity.equals("Car") || activity.equals("Bicycle")) {
                        gPerCO2 = addOptions(gPerCO2, activity);
                    }
                    if (gPerCO2 >= 1.0) {
                        pieChartEntries.add(new PieEntry((int) gPerCO2, activity));
                        colorEntries.add(Constants.MATERIAL_COLORS[i]);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private double addOptions(double gPerCO2, String activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());

        if (activity.equals("Foot")) {
            String dietName = preferences.getString("diet", "Ignore Diet");
            switch (dietName) {
                case Constants.IGNORE_DIET:
                    gPerCO2 *= Constants.co2MultiplierNoDietWalking;
                    break;
                case Constants.AVERAGE_DIET:
                    gPerCO2 *= Constants.co2MultiplierAverageDietWalking;
                    break;
                case Constants.MEAT_BASED:
                    gPerCO2 *= Constants.co2MultiplierMeatDietWalking;
                    break;
                case Constants.PLANT_BASESD:
                    gPerCO2 *= Constants.co2MultiplierVeganDietWalking;
                    break;
            }
            return gPerCO2;
        }

        if (activity.equals("Bicycle")) {
            String dietName = preferences.getString("diet", "Ignore Diet");
            switch (dietName) {
                case Constants.IGNORE_DIET:
                    gPerCO2 *= Constants.co2MultiplierNoDietBicycle;
                    break;
                case Constants.AVERAGE_DIET:
                    gPerCO2 *= Constants.co2MultiplierAverageDietBicycle;
                    break;
                case Constants.MEAT_BASED:
                    gPerCO2 *= Constants.co2MultiplierMeatDietBicylce;
                    break;
                case Constants.PLANT_BASESD:
                    gPerCO2 *= Constants.co2MultiplierVeganDietBicycle;
                    break;
            }
            return gPerCO2;
        }

        if (activity.equals("Car")) {
            String dietName = preferences.getString("car", "Ignore Diet");
            switch (dietName) {
                case Constants.DEFAULT_CAR:
                    gPerCO2 *= Constants.co2MultiplierDefaultCar;
                    break;
                case Constants.SMALL_CAR:
                    gPerCO2 *= Constants.co2MultiplierSmallCar;
                    break;
                case Constants.BIG_CAR:
                    gPerCO2 *= Constants.co2MultiplierBigCar;
                    break;
                case Constants.ELECTRIC_CAR:
                    gPerCO2 *= Constants.co2MultiplierElectricCar;
                    break;
            }
            return gPerCO2;
        }

        return 1.0;
    }

    private void updateDistancePerMode(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
            /*if (activity.equals("Still")) {
                continue;
            }*/
            try {
                if (todayData.getJSONObject(activity).getDouble("distance") >= 10.0) {
                    pieChartEntries.add(new PieEntry((float)(todayData.getJSONObject(activity).getDouble("distance") / 1000), activity));
                    colorEntries.add(Constants.MATERIAL_COLORS[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void updateEnergyPerMode(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            try {
                if (todayData.getJSONObject(activity).getDouble("distance") != 0.0) {
                    double energyPerMode = todayData.getJSONObject(activity).getDouble("distance");
                    energyPerMode = energyPerMode * (Constants.WattPerMode[i]);
                    if (energyPerMode >= 1.0) {
                        pieChartEntries.add(new PieEntry((int) energyPerMode, activity));
                        colorEntries.add(Constants.MATERIAL_COLORS[i]);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void setChartUI(boolean needsAnimation) {

        // Outside values
        Legend l = this.chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDirection(Legend.LegendDirection.LEFT_TO_RIGHT);
        l.setWordWrapEnabled(true);
        l.setDrawInside(true);

        PieDataSet set = new PieDataSet(pieChartEntries, "");

        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setValueLinePart1OffsetPercentage(100f); /** When valuePosition is OutsideSlice, indicates offset as percentage out of the slice size */
        set.setValueLinePart1Length(0.45f); /** When valuePosition is OutsideSlice, indicates length of first half of the line */
        set.setValueLinePart2Length(0.48f); /** When valuePosition is OutsideSlice, indicates length of second half of the line */

        this.chart.setExtraOffsets(20.f, 7.f, 20, 7f); // Ofsets of the view chart to prevent outside values being cropped /** Sets extra offsets (around the chart view) to be appended to the auto-calculated offsets.*/
        this.chart.setClickable(false);
        this.chart.setHighlightPerTapEnabled(false);
        set.setColors(colorEntries);

        PieData data = new PieData(set);
        data.setDrawValues(true);
        data.setValueTextColor(Color.BLACK);
        int colorBlack = Color.parseColor("#000000");
        chart.setEntryLabelColor(colorBlack);
        chart.setDrawEntryLabels(false);
        this.chart.setData(data);
        this.chart.setHoleRadius(50);
        data.setValueTextSize(11f);

        this.chart.getDescription().setEnabled(false);
        this.chart.getLegend().setEnabled(false);

        if (this.selectedGraphName.equals(Constants.MENU_OPTIONS[2])) {
            chart.getData().setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {

                    return String.format(Locale.ENGLISH, "%.2f", value);
                }
            });
        } else {
            chart.getData().setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return ("" + (int) value);
                }
            });
        }
        int index = Arrays.asList(Constants.MENU_OPTIONS).indexOf(this.selectedGraphName);
        this.chart.setCenterText(Constants.PIE_GRAPH_DESCRIPTION[index]);
        this.chart.setCenterTextRadiusPercent(90f);
        if (needsAnimation) {
            this.chart.animateXY(800, 800);
            this.chart.invalidate();
        }
    }

    private boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    private static int rgb(String hex) {
        int color = (int) Long.parseLong(hex.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }

    void startScanning() {
        if (this.chart != null && this.chart.getData() == null) {
            this.chart.setNoDataText("Collecting Data, please wait...");
            this.chart.setNoDataTextColor(rgb("#1da554"));
            this.chart.invalidate();
        }
        this.button.setText(R.string.start_scanning_on_click);
    }

    void menuClick(int selectedViewGraph) {
        this.selectedGraphName = Constants.MENU_OPTIONS[selectedViewGraph];
        this.updateChart(true);
        this.dataTitle.setText(this.selectedGraphName);
    }

    private double getPercentage(Integer value, Integer total) {
        return (value / (double) total) * 100;
    }

    void updateIcons(Map<String, Integer> mostPresentWindow, String accuracy, int latestWiFiNumber, int oldWifi, int avgSpeed, boolean gpsOn, int blueNumbers, double meanAcc, float[] predictions) {

        TextView t = getView().findViewById(R.id.accuracyText);
        String s = "GPS on: " + gpsOn + " GPS Accuracy: " + accuracy + " Wifi(OldVsNew): " + latestWiFiNumber + " " + oldWifi + " Avg.Speed " + avgSpeed + " Blue : " + blueNumbers + " Mean Acc: " +  String.format(Locale.ENGLISH, "%.4f", meanAcc) + " Pred: " + Arrays.toString(predictions);
        t.setText(s);

        for (String activity : Constants.ListModes) {
            if (!mostPresentWindow.containsKey(activity)) {
                int index = Arrays.asList(Constants.ListModes).indexOf(activity);
                CircleImageView img = listIcons.get(index);
                img.setAlpha(0.33f);
                img.setBorderWidth(3);
            }
        }
        for (Map.Entry<String, Integer> entry : mostPresentWindow.entrySet()) {
            int index = Arrays.asList(Constants.ListModes).indexOf(entry.getKey());
            CircleImageView img = listIcons.get(index);
            if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 33.3f &&
                    getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) > 15f) {
                img.setAlpha(0.50f);
                img.setBorderWidth(5);
            } else if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 66.6f) {
                img.setAlpha(0.75f);
                img.setBorderWidth(7);
            } else {
                img.setAlpha(1f);
                img.setBorderWidth(9);
            }

            if (Collections.max(mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey().equals(entry.getKey())) {
                img.setAlpha(1f);
                img.setBorderWidth(9);
            }
        }

        String mostLikely = Collections.max(mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey();
        TextView tv = (TextView) this.textSwitcher.getCurrentView();
        boolean change = true;
        int index = Arrays.asList(Constants.ListModes).indexOf(mostLikely);
        if (tv.getText().toString().length()>0) {
            if (tv.getText().toString().equals(Constants.listModesVerbose[index])) {
                change = false;
            }
        }
        if (change) {
            this.textSwitcher.setText(Constants.listModesVerbose[index]);
            TextView tv2 = (TextView) this.textSwitcher.getCurrentView();
            tv2.setGravity(Gravity.CENTER_HORIZONTAL);
            tv2.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }
}