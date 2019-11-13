package ch.ethz.smartenergy;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
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
import java.util.Map;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;


public class HomeFragment extends Fragment {

    private MainActivity mainActivity;

    private TextView probabilityOnFoot;
    private TextView probabilityTrain;
    private TextView probabilityTramway;
    private TextView probabilityBus;
    private TextView probabilityCar;
    private TextView probabilityBicycle;
    private TextView probabilityEbike;
    private TextView probabilityMotorcycle;
    private TextView modeBeingUsed;

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
        /*probabilityOnFoot = v.findViewById(R.id.text_predicted_foot);
        probabilityTrain = v.findViewById(R.id.text_predicted_train);
        probabilityTramway = v.findViewById(R.id.text_predicted_tramway);
        probabilityBus = v.findViewById(R.id.text_predicted_bus);
        probabilityCar = v.findViewById(R.id.text_predicted_car);
        probabilityBicycle = v.findViewById(R.id.text_predicted_bicycle);
        probabilityEbike = v.findViewById(R.id.text_predicted_ebike);
        probabilityMotorcycle = v.findViewById(R.id.text_predicted_motorcycle);*/
        button = v.findViewById(R.id.button_start);

        /*probabilityOnFoot.setText(getString(R.string.on_foot, 0.00));
        probabilityTrain.setText(getString(R.string.train, 0.00));
        probabilityBus.setText(getString(R.string.bus, 0.00));
        probabilityCar.setText(getString(R.string.car, 0.00));
        probabilityTramway.setText(getString(R.string.tramway, 0.00));
        probabilityBicycle.setText(getString(R.string.bicycle, 0.00));
        probabilityEbike.setText(getString(R.string.ebike, 0.00));
        probabilityMotorcycle.setText(getString(R.string.motorcycle, 0.00));*/
        this.listIcons = new ArrayList<>();
        for (int i = 0; i < Constants.ListModes.length; i++) {
            int resID = getResources().getIdentifier("iconMode" + i, "id", getActivity().getPackageName());
            CircleImageView img = getView().findViewById(resID);
            img.setAlpha(0.33f);
            img.setBorderColor(Constants.MATERIAL_COLORS[i]);
            this.listIcons.add(img);
        }

        chart = v.findViewById(R.id.chart_graph);
        this.chart.setNoDataText("No Data Available for Today");
        this.updateChart();
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

    void updateChart() {

        // Temporary
        if (this.mainActivity.getPredictions() != null) {
            showResult();
        }
        if (this.mainActivity.getPredictionsNN() != null) {
            appendResult();
        }

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

        this.setChartUI();
    }

    private void setDataGraph(JSONObject todayData) {

        pieChartEntries = new ArrayList<>();
        colorEntries = new ArrayList<>();

        if (selectedGraphName.equals(Constants.MENU_OPTIONS[0])) {
            this.updateDataTime(todayData);
        } else if (selectedGraphName.equals(Constants.MENU_OPTIONS[1])) {
            this.updateCO2PerMode(todayData);
        } else {
            //this.updateAverageCO2(todayData);
            updateAverageCO2(todayData);
        }
            }

    private void updateDataTime(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
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
            try {
                if (todayData.getJSONObject(activity).getInt("distance") != 0) {
                    double gPerCO2 = todayData.getJSONObject(activity).getDouble("distance");
                    gPerCO2 *= Constants.CO2PerMode[i];
                    pieChartEntries.add(new PieEntry((int) gPerCO2, activity));
                    colorEntries.add(Constants.MATERIAL_COLORS[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void updateAverageCO2(JSONObject todayData) {
        int i = 0;
        for (String activity : Constants.ListModes) {
            try {
                if (todayData.getJSONObject(activity).getDouble("distance") != 0.0) {
                    pieChartEntries.add(new PieEntry((int)(todayData.getJSONObject(activity).getDouble("distance")), activity));
                    colorEntries.add(Constants.MATERIAL_COLORS[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void setChartUI() {

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
        this.chart.setData(data);
        this.chart.setHoleRadius(50);
        data.setValueTextSize(11f);

        this.chart.getDescription().setEnabled(false);
        this.chart.animateXY(1000, 1000);
        this.chart.getLegend().setEnabled(false);
        chart.getData().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ("" + (int)value);
            }
        });
        int index = Arrays.asList(Constants.MENU_OPTIONS).indexOf(this.selectedGraphName);
        this.chart.setCenterText(Constants.PIE_GRAPH_DESCRIPTION[index]);
        this.chart.setCenterTextRadiusPercent(90f);
        this.chart.invalidate();
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

    void appendResult() {

        float[] predictions = this.mainActivity.getPredictionsNN();
        Log.d("PROBABILITIES: ", Arrays.toString(predictions));

        /*probabilityOnFoot.append(String.format("%s %.2f %s", " vs", predictions[0] * 100, " %"));
        probabilityTrain.append(String.format("%s %.2f %s", " vs", predictions[1] * 100, " %"));
        probabilityBus.append(String.format("%s %.2f %s", " vs", predictions[2] * 100, " %"));
        probabilityCar.append(String.format("%s %.2f %s", " vs", predictions[3] * 100, " %"));
        probabilityTramway.append(String.format("%s %.2f %s", " vs", predictions[4] * 100, " %"));
        probabilityBicycle.append(String.format("%s %.2f %s", " vs", predictions[5] * 100, " %"));
        probabilityEbike.append(String.format("%s %.2f %s", " vs", predictions[6] * 100, " %"));
        probabilityMotorcycle.append(String.format("%s %.2f %s", " vs", predictions[7] * 100, " %"));*/
    }

    void showResult() {
        float[] predictions = this.mainActivity.getPredictions();
        /*probabilityOnFoot.setText(getString(R.string.on_foot, predictions[0] * 100));
        probabilityTrain.setText(getString(R.string.train, predictions[1] * 100));
        probabilityBus.setText(getString(R.string.bus, predictions[2] * 100));
        probabilityCar.setText(getString(R.string.car, predictions[3] * 100));
        probabilityTramway.setText(getString(R.string.tramway, predictions[4] * 100));
        probabilityBicycle.setText(getString(R.string.bicycle, predictions[5] * 100));
        probabilityEbike.setText(getString(R.string.ebike, predictions[6] * 100));
        probabilityMotorcycle.setText(getString(R.string.motorcycle, predictions[7] * 100));*/
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
        this.updateChart();
        this.dataTitle.setText(this.selectedGraphName);
    }

    private double getPercentage(Integer value, Integer total) {
        return (value / (double) total) * 100;
    }

    void updateIcons(Map<String, Integer> mostPresentWindow) {

        for (String activity : Constants.ListModes) {
            if (!mostPresentWindow.containsKey(activity)) {
                int index = Arrays.asList(Constants.ListModes).indexOf(activity);
                ImageView img = listIcons.get(index);
                img.setAlpha(0.33f);
            }
        }
        for (Map.Entry<String, Integer> entry : mostPresentWindow.entrySet()) {
            int index = Arrays.asList(Constants.ListModes).indexOf(entry.getKey());
            ImageView img = listIcons.get(index);
            if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 33.3f &&
                    getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) > 15f) {
                img.setAlpha(0.65f);
            } else if (getPercentage(entry.getValue(), mostPresentWindow.values().stream().reduce(0, Integer::sum)) < 66.6f) {
                img.setAlpha(0.85f);
            } else {
                img.setAlpha(1f);
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



