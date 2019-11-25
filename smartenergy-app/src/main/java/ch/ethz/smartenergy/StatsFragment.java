package ch.ethz.smartenergy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class StatsFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private String selectedGraphName = Constants.MENU_OPTIONS[1];
    private TextView dataTitle;
    private List<Entry> lineChartEntry;
    private LineData lineData;
    private List<Integer> colorEntries;
    private LineChart chart;
    private String selectedTimeFrame = Constants.TIMEFRAME_OPTIONS[1];

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        this.dataTitle = getView().findViewById(R.id.titleGraph);
        Spinner spinner = getView().findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.list_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition("This Month"));
        spinner.setOnItemSelectedListener(this);

        updateChart();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        this.selectedTimeFrame = parent.getItemAtPosition(pos).toString();
        updateChart();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private String getTitleGraph() {
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
            return "the past week";
        } else if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[1])) {
            return Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);
        } else {
            return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        }
    }

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

    private List<JSONObject> getJsonYear(JSONObject json) {
        List<JSONObject> listJson = new ArrayList<>();
        List<Integer> listMonth = new ArrayList<>();
        Iterator <?> keys = json.keys();

        // For every month of the current year
        //for (int i = 1; i <= 12; i++) {
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
        //}
        return listJson;
    }

    private List<JSONObject> getJsonPastWeek(JSONObject json) {
        List<JSONObject> listJson = new ArrayList<>();
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
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return listJson;
    }

    void updateChart() {
        View v = getView();
        this.chart = v.findViewById(R.id.chart);
        chart.setNoDataText("No data available for the selected graph.");

        JSONObject json = new JSONObject();

        if(!isFilePresent(getActivity(), "data.json")) {
            return;
        }

        try {
            json = new JSONObject(read(getActivity(), "data.json"));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

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

        this.setChartUI(listJson);
        chart.invalidate(); // refresh
    }

    private void setChartUI(List<JSONObject> listJson) {
        Calendar cal = Calendar.getInstance();
        TextView tvDesc = getView().findViewById(R.id.textDescription);
        String s = Constants.GRAPH_DESCRIPTION[Arrays.asList(Constants.MENU_OPTIONS).indexOf(this.selectedGraphName)] + " " + getTitleGraph();
        tvDesc.setText(s);
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
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return ("" + (int)value);
            }
        });
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getDescription().setText("");

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
            xAxis.setValueFormatter(new YearViewFormatter());
        } else {
            xAxis.setValueFormatter(new MonthViewFormatter());
        }
        boolean forceLabelCount = false;
        if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[0])) {
            chart.getXAxis().setLabelCount(7, false);
            chart.getXAxis().setAxisMinimum(getMinXAxisPerMonth(lineData.getXMin()));
            chart.getXAxis().setAxisMaximum(getMaxXAxisPerMonth(lineData.getXMax()));
        } else {
            if (lineData.getXMin() != lineData.getXMax()) {
                chart.getXAxis().setAxisMinimum(getMinXAxisPerMonth(lineData.getXMin()));
                chart.getXAxis().setAxisMaximum(getMaxXAxisPerMonth(lineData.getXMax()));
            }
            if (getLabelNumberForMonth(listJson.size()) > 3) {
                forceLabelCount = true;
            }
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                chart.getXAxis().setLabelCount(12, true);
            } else {
                chart.getXAxis().setLabelCount(getLabelNumberForMonth(listJson.size()), forceLabelCount);
            }
        }
        chart.getXAxis().setGranularityEnabled(true);
        chart.getXAxis().setCenterAxisLabels(false);
        chart.getXAxis().setAxisLineWidth(1.2f);
        chart.getXAxis().setDrawGridLines(false);
    }

    private void setDataGraph(List<JSONObject> listJson) {
        lineChartEntry = new ArrayList<>();
        colorEntries = new ArrayList<>();

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
                        this.getYearAvg(e, entries, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getInt("time") != 0) {
                            Date date = cal.getTime();
                            entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), e.getJSONObject(activity).getInt("time"), activity));
                        }
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    entries.add(new Entry(j, yearAvg.get(j - 1)));
                }
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
                    rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
                    rgb("#D81B60")
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

    private void getYearAvg(JSONObject e, List<Entry> entries, List<Integer> yearAvg, String activity) {
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
                        energyPerMode = energyPerMode * (Constants.WattPerMode[value]);
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

    private void updateCO2PerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
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
                        this.getYearAvg(e, entries, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") != 0) {
                            Date date = cal.getTime();
                            double gCO2 = e.getJSONObject(activity).getDouble("distance");
                            int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                            gCO2 = gCO2 * (Constants.CO2PerMode[value] / 1000);
                            if (activity.equals("Foot") || activity.equals("Car") || activity.equals("Bicycle")) {
                                gCO2 = addOptions(gCO2, activity);
                            }
                            if (gCO2 >= 1.0) {
                                entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (int)gCO2, activity));
                            }
                        }
                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            });

            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    entries.add(new Entry(j, yearAvg.get(j - 1)));
                }
            } else if (entries.isEmpty()) {
                i++;
                continue;
            }

            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
                    rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
                    rgb("#D81B60")
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

    private void updateDistancePerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
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
                        this.getYearAvg(e, entries, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") >= 10) {
                            Date date = cal.getTime();
                            entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (float)(e.getJSONObject(activity).getDouble("distance") / 1000), activity));
                        }
                    }

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    entries.add(new Entry(j, yearAvg.get(j - 1)));
                }
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
                    rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
                    rgb("#D81B60")
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

    private void updateEnergyPerMode(List<JSONObject> listJson) {
        int i = 0;
        for (String activity: Constants.ListModes) {
            if (activity.equals("Still")) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            List<Integer> yearAvg = new ArrayList<>();
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
                        this.getYearAvg(e, entries, yearAvg, activity);
                    } else {
                        if (e.getJSONObject(activity).getDouble("distance") != 0) {
                            double energyPerMode = e.getJSONObject(activity).getDouble("distance");
                            int value = Arrays.asList(Constants.ListModes).indexOf(activity);
                            energyPerMode = energyPerMode * (Constants.WattPerMode[value]);
                            if (energyPerMode >= 1.0) {
                                entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), (float)energyPerMode, activity));
                            }
                        }
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            });
            if (this.selectedTimeFrame.equals(Constants.TIMEFRAME_OPTIONS[2])) {
                for (int j = 1; j <=12; j++) {
                    entries.add(new Entry(j, yearAvg.get(j - 1)));
                }
            }
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
                    rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
                    rgb("#D81B60")
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

    private int getMinXAxisPerMonth(float minValueFloat) {
        int minValue = (int) minValueFloat;
        if (minValue <= 2) {
            return 1;
        }
        Calendar cal = Calendar.getInstance();
        int dayThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (dayThisMonth - minValue <= 0) {
            return minValue - 2;
        } else if (dayThisMonth - minValue == 1) {
            return minValue - 1;
        }
        return minValue;
    }

    private int getMaxXAxisPerMonth(float maxValueFloat) {
        int maxValue = (int) maxValueFloat;
        if (maxValue <= 2) {
            return 3;
        }
        Calendar cal = Calendar.getInstance();
        int dayThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (dayThisMonth - maxValue <= 1) {
            return dayThisMonth;
        }
        return maxValue;
    }

    private int rgb(String s) {
        int color = (int) Long.parseLong(s.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
    }


    private int getLabelNumberForMonth(int totalValue) {
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        if (totalValue <= 3) {
            return 3;
        } else if (totalValue < 10) {
            return totalValue;
        } else if (totalValue < 20) {
            return totalValue / 2;
        } else {
            return totalValue / 3;
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

    private boolean isFilePresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    void menuClick(int selectedViewGraph) {
        this.selectedGraphName = Constants.MENU_OPTIONS[selectedViewGraph];
        this.updateChart();
        this.dataTitle.setText(this.selectedGraphName);
    }
}
