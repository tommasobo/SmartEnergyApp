package ch.ethz.smartenergy;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private List<CircleImageView> listIcons;
    private TextSwitcher textSwitcher;
    private TextView dataTitle;
    private PieChart chart;
    private Button button;
    private List<PieEntry> pieChartEntries;
    private List<Integer> colorEntries;
    private String selectedGraphName = Constants.MENU_OPTIONS[1];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        View v = getView();

        dataTitle = Objects.requireNonNull(getView()).findViewById(R.id.data_title);
        textSwitcher = getView().findViewById(R.id.textSwitcher);
        textSwitcher.setInAnimation(getView().getContext(), android.R.anim.slide_in_left);
        textSwitcher.setOutAnimation(getView().getContext(), android.R.anim.slide_out_right);
        textSwitcher.setCurrentText("not collecting any data");
        button = Objects.requireNonNull(v).findViewById(R.id.button_start);

        this.listIcons = new ArrayList<>();
        for (int i = 0; i < Constants.ListModes.length; i++) {
            int resID = getResources().getIdentifier("iconMode" + i, "id", Objects.requireNonNull(getActivity()).getPackageName());
            CircleImageView img = getView().findViewById(resID);
            img.setAlpha(0.30f);
            img.setBorderWidth(3);
            img.setBorderColor(Constants.MATERIAL_COLORS[i]);
            this.listIcons.add(img);
        }

        chart = v.findViewById(R.id.chart_graph);
        DisplayMetrics displayMetrics = Objects.requireNonNull(getContext()).getResources().getDisplayMetrics();

        float heightPercentage = 0.493f;
        float widthPercentage = 0.762f;
        if (displayMetrics.heightPixels <= 1280) {
            heightPercentage = 0.433f;
        }
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (dpHeight * heightPercentage), getResources().getDisplayMetrics());
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (dpWidth * widthPercentage), getResources().getDisplayMetrics());
        this.getView().findViewById(R.id.constraintLayout_outChart).getLayoutParams().height = height + 5;
        this.chart.getLayoutParams().width = width;
        this.chart.getLayoutParams().height = height;

        if ((dpHeight == 732.0 && dpWidth == 360.0) || (dpHeight == 592.0 && dpWidth == 360.0)) {
            this.listIcons.forEach(icon -> {
                icon.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, getResources().getDisplayMetrics());
                icon.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, getResources().getDisplayMetrics());
            });
        }

        if (dpHeight == 712.0 && dpWidth == 360.0) {
            this.listIcons.forEach(icon -> {
                icon.getLayoutParams().width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                icon.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
            });
        }

        this.chart.setNoDataText("No Data Available for Today");
        this.updateChart(true);
    }


    /**
     * Updates the main chart
     *
     * @param needsAnimation indicating if the graph needs an animation when it is getting updated
     *
     */
    void updateChart(boolean needsAnimation) {

        if (!Utility.isFilePresent(Objects.requireNonNull(getActivity()))) {
            return;
        }
        JSONObject json = new JSONObject();

        try {
            json = new JSONObject(Objects.requireNonNull(Utility.read(getActivity())));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

        TimeZone tz = TimeZone.getDefault();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
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

    /**
     * Updated the graph based on the selected menu option
     *
     * @param todayData JSON with the data to be used for the graph
     *
     */
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

    /**
     * Updates the graph showing today's time utilization for each mode
     *
     * @param todayData JSON with the data to be used for the graph
     *
     */
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

    /**
     * Updates the graph showing today's CO2 utilization for each mode
     *
     * @param todayData JSON with the data to be used for the graph
     *
     */
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

    /**
     * Select the right value for CO2 based on the options chosen by the user
     *
     * @param gPerCO2 current grams of CO2
     * @param activity selected activity
     *
     */
    private double addOptions(double gPerCO2, String activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getView()).getContext());

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

    /**
     * Select the right value for Wh based on the options chosen by the user
     *
     * @param energy current wh
     * @param activity selected activity
     *
     */
    private double addOptionsEnergy(double energy, String activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getView().getContext());

        if (activity.equals("Car")) {
            String dietName = preferences.getString("car", "Ignore Diet");
            switch (dietName) {
                case Constants.DEFAULT_CAR:
                    energy *= Constants.energyMultiplierDefaultCar;
                    break;
                case Constants.SMALL_CAR:
                    energy *= Constants.energyMultiplierSmallCar;
                    break;
                case Constants.BIG_CAR:
                    energy *= Constants.energyMultiplierBigCar;
                    break;
                case Constants.ELECTRIC_CAR:
                    energy *= Constants.energyMultiplierElectricCar;
                    break;
            }
            return energy;
        }

        return 1.0;
    }

    /**
     * Updates the graph showing today's distance in KM for each mode
     *
     * @param todayData JSON with the data to be used for the graph
     *
     */
    private void updateDistancePerMode(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
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

    /**
     * Updates the graph showing today's Wh utilization for each mode
     *
     * @param todayData JSON with the data to be used for the graph
     *
     */
    private void updateEnergyPerMode(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            try {
                if (todayData.getJSONObject(activity).getDouble("distance") != 0.0) {
                    double energyPerMode = todayData.getJSONObject(activity).getDouble("distance");
                    energyPerMode = energyPerMode * (Constants.WattPerMode[i]) / 1000;
                    if (activity.equals("Car")) {
                        energyPerMode = addOptionsEnergy(energyPerMode, activity);
                    }
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

    /**
     * Set the GUI of the graph
     *
     * @param needsAnimation If the graph needs an animation when updating
     *
     */
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
        set.setValueLinePart1OffsetPercentage(100f); /* When valuePosition is OutsideSlice, indicates offset as percentage out of the slice size */
        set.setValueLinePart1Length(0.45f); /* When valuePosition is OutsideSlice, indicates length of first half of the line */
        set.setValueLinePart2Length(0.48f); /* When valuePosition is OutsideSlice, indicates length of second half of the line */

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

    /**
     * Updates the GUI when the user starts scanning for data
     *
     */
    void startScanning() {
        if (this.chart != null && this.chart.getData() == null) {
            this.chart.setNoDataText("Collecting Data, please wait...");
            this.chart.setNoDataTextColor(Utility.rgb("#1da554"));
            this.chart.invalidate();
        }
        this.button.setText(R.string.start_scanning_on_click);
        this.button.getBackground().setColorFilter(Utility.rgb("#c9e7ca"), PorterDuff.Mode.MULTIPLY);
    }

    /**
     * Updates the GUI when the user changes graph
     *
     * @param selectedViewGraph selected graph by the user
     *
     */
    void menuClick(int selectedViewGraph) {
        this.selectedGraphName = Constants.MENU_OPTIONS[selectedViewGraph];
        this.updateChart(true);
        this.dataTitle.setText(this.selectedGraphName);
    }

    private double getPercentage(Integer value, Integer total) {
        return (value / (double) total) * 100;
    }

    /**
     * Updated the Icons on the homepage based on how likely each mode is
     *
     * @param mostPresentWindow map with the predictions for each mode
     * @param distanceLastMinute
     *
     */
    void updateIcons(Map<String, Integer> mostPresentWindow, float[] accuracy, int latestWiFiNumber, int oldWifi, int commonWifi, double avgSpeed, boolean gpsOn, int blueNumbers, double meanAcc, float[] predictions, float points, double distanceLastMinute) {

        TextView t = Objects.requireNonNull(getView()).findViewById(R.id.accuracyText);
        String s = "GPS on: " + gpsOn + " GPS Accuracy: " + Arrays.toString(accuracy) + " Wifi(OldNewCommon): " + oldWifi + " " + latestWiFiNumber + " " + commonWifi + " Avg.Speed " + String.format("%.3f", avgSpeed) + " Blue : " + blueNumbers + " Mean Acc: " +  String.format("%.4f", meanAcc) + " Points: " + points + " Dis: " + String.format("%.4f", distanceLastMinute);
        t.setText(s);

        for (String activity : Constants.ListModes) {
            if (!mostPresentWindow.containsKey(activity)) {
                int index = Arrays.asList(Constants.ListModes).indexOf(activity);
                CircleImageView img = listIcons.get(index);
                img.setAlpha(0.30f);
                img.setBorderWidth(3);
            }
        }
        for (Map.Entry<String, Integer> entry : mostPresentWindow.entrySet()) {
            int index = Arrays.asList(Constants.ListModes).indexOf(entry.getKey());
            CircleImageView img = listIcons.get(index);
            if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 33.3f &&
                    getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) > 15f) {
                img.setAlpha(0.45f);
                img.setBorderWidth(4);
            } else if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 66.6f) {
                img.setAlpha(0.75f);
                img.setBorderWidth(5);
            } else {
                img.setAlpha(1f);
                img.setBorderWidth(7);
            }

            if (Collections.max(mostPresentWindow.entrySet(), Map.Entry.comparingByValue()).getKey().equals(entry.getKey())) {
                img.setAlpha(1f);
                img.setBorderWidth(7);
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