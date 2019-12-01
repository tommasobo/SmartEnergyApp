package ch.ethz.smartenergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private String selectedGraphName = Constants.MENU_OPTIONS[1];
    private TextView dataTitle;
    private LineData lineData;
    private LineChart chart;
    private String selectedTimeFrame = Constants.TIMEFRAME_OPTIONS[0];
    private List<Integer> lastWeekItems;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        this.dataTitle = Objects.requireNonNull(getView()).findViewById(R.id.titleGraph);
        Spinner spinner = getView().findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(Objects.requireNonNull(getContext()),
                R.array.list_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition("Past Month"));
        spinner.setOnItemSelectedListener(this);

        updateChart();
    }

    /**
     * Returns the title of the graph based on the time frame selected
     */
    private String getTitleGraph() {
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
            return "the past week";
        } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
            return Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);
        } else {
            return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        }
    }

    /**
     * Returns json objects which includes data in the current month.
     *
     * @param  json json data
     */
    private List<JSONObject> getJsonMonth(JSONObject json) {
        List<JSONObject> listJson = new ArrayList<>();
        Iterator <?> keys = json.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                if (json.get(key) instanceof JSONObject) {
                    JSONObject tempJson = json.getJSONObject(key);
                    String[] out = key.split("-");

                    Date date = new Date();
                    DateTime datetime = new DateTime(date);
                    if(out[1].equals(datetime.toString("MM"))) {
                        listJson.add(tempJson);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return listJson;
    }

    /**
     * Returns json objects which includes data in the current year.
     *
     * @param  json json data
     */
    private List<JSONObject> getJsonYear(JSONObject json) {
        List<JSONObject> listJson = new ArrayList<>();
        Iterator <?> keys = json.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                if (json.get(key) instanceof JSONObject) {
                    JSONObject tempJson = json.getJSONObject(key);
                    String[] out = key.split("-");

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(calendar.getTime());

                    if (out[2].equals(String.valueOf(calendar.get(Calendar.YEAR)))) {
                        listJson.add(tempJson);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return listJson;
    }

    /**
     * Returns json objects which includes data in the past week
     *
     * @param  json json data
     */
    private List<JSONObject> getJsonPastWeek(JSONObject json) {
        List<JSONObject> listJson = new ArrayList<>();
        this.lastWeekItems = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Iterator <?> keys = json.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                try {
                    if (json.get(key) instanceof JSONObject) {
                        JSONObject tempJson = json.getJSONObject(key);
                        String[] out = key.split("-");

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(calendar.getTime());
                        calendar.add(Calendar.DAY_OF_YEAR, - i);

                        // Fix problem with days starting with 0
                        if (out[0].charAt(0) == '0') {
                            out[0] = out[0].substring(1);
                        }
                        if (out[0].equals(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH))) &&
                            out[1].equals(String.valueOf(calendar.get(Calendar.MONTH) + 1)) &&
                            out[2].equals(String.valueOf(calendar.get(Calendar.YEAR)))) {
                            listJson.add(tempJson);
                            this.lastWeekItems.add(Math.abs(i-6));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return listJson;
    }

    /**
     * Updates the main chart
     */
    void updateChart() {
        View v = getView();
        if (v != null) {
            this.chart = v.findViewById(R.id.chart);
        }
        chart.setNoDataText("No data available for the selected graph.");

        JSONObject json = new JSONObject();

        if(!Utility.isFilePresent(Objects.requireNonNull(getActivity()))) {
            return;
        }

        try {
            json = new JSONObject(Objects.requireNonNull(Utility.read(getActivity())));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

        TextView tvDesc = getView().findViewById(R.id.textDescription);
        String s = Constants.GRAPH_DESCRIPTION[Arrays.asList(Constants.MENU_OPTIONS).indexOf(this.selectedGraphName)] + " " + getTitleGraph();
        tvDesc.setText(s);
        List<JSONObject> listJson = null;
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
            listJson = getJsonPastWeek(json);
        } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])){
            listJson = getJsonMonth(json);
        } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])){
            listJson = getJsonYear(json);
        }

        chart.clear();
        chart.invalidate();

        this.lineData = new LineData();
        this.setDataGraph(listJson);
        if (lineData.getDataSetCount() == 0) {
            this.chart.clear();
            this.chart.invalidate();
            return;
        }
        this.setChartUI();
        chart.invalidate(); // refresh
    }

    /**
     * Sets the Chart UI
     */
    private void setChartUI() {
        chart.setData(lineData);
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

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setAxisLineWidth(1.2f);
        chart.getAxisLeft().setGridLineWidth(0.6f);
        if (Constants.MENU_OPTIONS[2].equals(this.selectedGraphName)) {
            chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    if (value <= 4) {
                        return ("" + String.format("%.1f", value));
                    } else {
                        return ("" + (int) value);
                    }
                }
            });
        } else {
            chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return ("" + (int)value);
                }
            });
        }

        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getDescription().setText("");

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
            xAxis.setValueFormatter(new YearViewFormatter());
        } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])){
            xAxis.setValueFormatter(new WeekViewFormatter());
        } else {
            xAxis.setValueFormatter(new MonthViewFormatter());
        }
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
            chart.getXAxis().setAxisMinimum(0);
            chart.getXAxis().setAxisMaximum(6);
            chart.getXAxis().setLabelCount(7, true);
        } else {
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
                chart.getXAxis().setAxisMinimum(1);
                int maxMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                chart.getXAxis().setAxisMaximum(maxMonth);
                chart.getXAxis().setLabelCount(7, true);
            }

            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                chart.getXAxis().setAxisMinimum(1);
                chart.getXAxis().setAxisMaximum(12);
                chart.getXAxis().setLabelCount(12, true);
            }
        }
        chart.getXAxis().setGranularityEnabled(true);
        chart.getXAxis().setCenterAxisLabels(false);
        chart.getXAxis().setSpaceMax(0.0f);
        chart.getXAxis().setSpaceMin(0.0f);
        chart.getXAxis().setAxisLineWidth(1.2f);
        chart.getXAxis().setDrawGridLines(false);
    }

    /**
     * Set the data for the graph based on the graph selected
     *
     * @param  listJson list of json data
     */
    private void setDataGraph(List<JSONObject> listJson) {

        if (selectedGraphName.equals(Constants.MENU_OPTIONS[0])) {
            this.updateDataTime(listJson);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[1])) {
            this.updateCO2PerMode(listJson);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[2])) {
            this.updateDistancePerMode(listJson);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[3])){
            this.updateEnergyPerMode(listJson);
        }
    }

    /**
     * Updates the graph showing time of utilization for each mode
     *
     * @param listJson JSON with the data to be used for the graph
     *
     */
    private void updateDataTime(List<JSONObject> listJson) {
        int i = 0;

        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Integer> yearAvg = new ArrayList<>();
            for (int j = 0; j < 12; j++) {
                yearAvg.add(j, 0);
            }
            List<Entry> entries = new ArrayList<>();
            AtomicInteger runCount = new AtomicInteger(0);
            listJson.forEach(e -> {
                try {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                    if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                        this.getYearAvg(e, yearAvg, activity);
                    } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])){
                        if (e.getJSONObject(activity).getInt("time") != 0) {
                            cal.getTime();
                            entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), e.getJSONObject(activity).getInt("time"), activity));
                        }
                    } else {
                        if (e.getJSONObject(activity).getInt("time") != 0) {
                            cal.getTime();
                            entries.add(new Entry(this.lastWeekItems.get(runCount.get()), e.getJSONObject(activity).getInt("time"), activity));
                        }
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                runCount.incrementAndGet();
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    if (yearAvg.get(j - 1) > 0) {
                        entries.add(new Entry(j, yearAvg.get(j - 1)));
                    }
                }
            }
            if (entries.isEmpty()) {
                i++;
                continue;
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    Utility.rgb("#2ecc71"), Utility.rgb("#f1c40f"), Utility.rgb("#e74c3c"), Utility.rgb("#3498db"),
                    Utility.rgb("#795548"), Utility.rgb("#607D8B"), Utility.rgb("#E040FB"), Utility.rgb("#00BFA5"),
                    Utility.rgb("#D81B60")
            };
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            dataSet.setColor(material_colors[i]);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(material_colors[i]);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(0);
            lineData.addDataSet(dataSet);
            i++;
        }
    }

    /**
     * Get the Avg for the year
     *
     * @param e current json object selected
     * @param yearAvg list of data per year
     * @param activity current activity
     *
     */
    private void getYearAvg(JSONObject e, List<Integer> yearAvg, String activity) {
        if (this.selectedGraphName.equals(Constants.MENU_OPTIONS[0])) {
            try {
                if (e.getJSONObject(activity).getInt("time") != 0) {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                        cal.setTime(cal.getTime());
                        yearAvg.set(cal.get(Calendar.MONTH), yearAvg.get(cal.get(Calendar.MONTH)) + e.getJSONObject(activity).getInt("time"));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        } else if (this.selectedGraphName.equals(Constants.MENU_OPTIONS[1])) {
            try {
                if (e.getJSONObject(activity).getDouble("distance") != 0) {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                        cal.setTime(cal.getTime());
                        double gCO2 = e.getJSONObject(activity).getDouble("distance");
                        int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                        gCO2 = gCO2 * (Constants.CO2PerMode[value] / 1000);
                        if (activity.equals("Foot") || activity.equals("Car") || activity.equals("Bicycle")) {
                            gCO2 = addOptions(gCO2, activity);
                        }
                        yearAvg.set(cal.get(Calendar.MONTH), yearAvg.get(cal.get(Calendar.MONTH)) + (int)gCO2);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        } else if (this.selectedGraphName.equals(Constants.MENU_OPTIONS[2])) {
            try {
                if (e.getJSONObject(activity).getDouble("distance") != 0) {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                        cal.setTime(cal.getTime());
                        yearAvg.set(cal.get(Calendar.MONTH), yearAvg.get(cal.get(Calendar.MONTH)) + (int)((int)e.getJSONObject(activity).getDouble("distance") / 1000f));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                if (e.getJSONObject(activity).getDouble("distance") != 0) {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                        double energyPerMode = e.getJSONObject(activity).getDouble("distance");
                        int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                        energyPerMode = energyPerMode * (Constants.WattPerMode[value]) / 1000;
                        if (activity.equals("Car")) {
                            energyPerMode = addOptionsEnergy(energyPerMode, activity);
                        }
                        cal.setTime(cal.getTime());
                        yearAvg.set(cal.get(Calendar.MONTH), yearAvg.get(cal.get(Calendar.MONTH)) + (int)energyPerMode);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * Updates the graph showing CO2 for each mode
     *
     * @param listJson JSON with the data to be used for the graph
     *
     */
    private void updateCO2PerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
            AtomicInteger runCount = new AtomicInteger(0);
            for (int j = 0; j < 12; j++) {
                yearAvg.add(j, 0);
            }
            listJson.forEach(e -> {
                try {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }

                    if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                        this.getYearAvg(e, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") != 0) {
                            cal.getTime();
                            double gCO2 = e.getJSONObject(activity).getDouble("distance");
                            int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                            gCO2 = gCO2 * (Constants.CO2PerMode[value] / 1000);
                            if (activity.equals("Foot") || activity.equals("Car") || activity.equals("Bicycle")) {
                                gCO2 = addOptions(gCO2, activity);
                            }
                            if (gCO2 >= 1.0 && this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
                                entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (int)gCO2, activity));
                            } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
                                entries.add(new Entry(this.lastWeekItems.get(runCount.get()), (int)gCO2, activity));
                            }
                        }
                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                runCount.incrementAndGet();
            });

            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    if (yearAvg.get(j - 1) > 0) {
                        entries.add(new Entry(j, yearAvg.get(j - 1)));
                    }
                }
            }
            if (entries.isEmpty()) {
                i++;
                continue;
            }

            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    Utility.rgb("#2ecc71"), Utility.rgb("#f1c40f"), Utility.rgb("#e74c3c"), Utility.rgb("#3498db"),
                    Utility.rgb("#795548"), Utility.rgb("#607D8B"), Utility.rgb("#E040FB"), Utility.rgb("#00BFA5"),
                    Utility.rgb("#D81B60")
            };
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setColor(material_colors[i]);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(material_colors[i]);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(0);
            lineData.addDataSet(dataSet);
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getView()).getContext());

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
     * Updates the graph showing distance in Km of utilization for each mode
     *
     * @param listJson JSON with the data to be used for the graph
     *
     */
    private void updateDistancePerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
            AtomicInteger runCount = new AtomicInteger(0);
            for (int j = 0; j < 12; j++) {
                yearAvg.add(j, 0);
            }
            listJson.forEach(e -> {
                try {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }

                    if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                        this.getYearAvg(e, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") >= 10 && this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
                            cal.getTime();
                            if (e.getJSONObject(activity).getDouble("distance") >= 10) {
                                entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (float)(e.getJSONObject(activity).getDouble("distance") / 1000), activity));
                            }

                        } else if (e.getJSONObject(activity).getDouble("distance") >= 10 && this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
                            cal.getTime();
                            if (e.getJSONObject(activity).getDouble("distance") >= 10) {
                                entries.add(new Entry(this.lastWeekItems.get(runCount.get()), (float)(e.getJSONObject(activity).getDouble("distance") / 1000), activity));
                            }

                        }
                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                runCount.incrementAndGet();
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    if (yearAvg.get(j - 1) > 0) {
                        entries.add(new Entry(j, yearAvg.get(j - 1)));
                    }
                }
            }
            if (entries.isEmpty()) {
                i++;
                continue;
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    Utility.rgb("#2ecc71"), Utility.rgb("#f1c40f"), Utility.rgb("#e74c3c"), Utility.rgb("#3498db"),
                    Utility.rgb("#795548"), Utility.rgb("#607D8B"), Utility.rgb("#E040FB"), Utility.rgb("#00BFA5"),
                    Utility.rgb("#D81B60")
            };
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setColor(material_colors[i]);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(material_colors[i]);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(0);
            lineData.addDataSet(dataSet);
            i++;
        }
    }

    /**
     * Updates the graph showing energy in Wh of utilization for each mode
     *
     * @param listJson JSON with the data to be used for the graph
     *
     */
    private void updateEnergyPerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
            AtomicInteger runCount = new AtomicInteger(0);
            for (int j = 0; j < 12; j++) {
                yearAvg.add(j, 0);
            }
            listJson.forEach(e -> {
                try {
                    String test = e.getString("date");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                    try {
                        cal.setTime(Objects.requireNonNull(sdf.parse(test)));// all done
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }

                    if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                        this.getYearAvg(e, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") != 0 && this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
                            double energyPerMode = e.getJSONObject(activity).getDouble("distance");
                            int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                            energyPerMode = energyPerMode * (Constants.WattPerMode[value]) / 1000;
                            if (activity.equals("Car")) {
                                energyPerMode = addOptionsEnergy(energyPerMode, activity);
                            }
                            if (energyPerMode >= 1.0) {
                                entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (float)energyPerMode, activity));
                            }
                        } else if (e.getJSONObject(activity).getDouble("distance") != 0 && this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
                            double energyPerMode = e.getJSONObject(activity).getDouble("distance");
                            int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                            energyPerMode = energyPerMode * (Constants.WattPerMode[value]) / 1000;
                            if (activity.equals("Car")) {
                                energyPerMode = addOptions(energyPerMode, activity);
                            }
                            if (energyPerMode >= 1.0) {
                                entries.add(new Entry(this.lastWeekItems.get(runCount.get()), (float)energyPerMode, activity));
                            }
                        }
                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                runCount.incrementAndGet();
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    if (yearAvg.get(j - 1) > 0) {
                        entries.add(new Entry(j, yearAvg.get(j - 1)));
                    }
                }
            }
            if (entries.isEmpty()) {
                i++;
                continue;
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    Utility.rgb("#2ecc71"), Utility.rgb("#f1c40f"), Utility.rgb("#e74c3c"), Utility.rgb("#3498db"),
                    Utility.rgb("#795548"), Utility.rgb("#607D8B"), Utility.rgb("#E040FB"), Utility.rgb("#00BFA5"),
                    Utility.rgb("#D81B60")
            };
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setColor(material_colors[i]);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(material_colors[i]);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(0);
            lineData.addDataSet(dataSet);
            i++;
        }
    }

    /**
     * Updates the GUI when menu button is clicked
     *
     * @param selectedViewGraph selected view
     *
     */
    void menuClick(int selectedViewGraph) {
        this.selectedGraphName = Constants.MENU_OPTIONS[selectedViewGraph];
        this.updateChart();
        this.dataTitle.setText(this.selectedGraphName);
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        this.selectedTimeFrame = parent.getItemAtPosition(pos).toString();
        updateChart();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}
