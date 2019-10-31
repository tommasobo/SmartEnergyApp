package ch.ethz.smartenergy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.smartenergy.model.ScanResult;


public class HomeFragment extends Fragment {

    private MainActivity mainActivity;

    private TextView sensorData;
    private TextView probabilityStandingStil;
    private TextView probabilityOnFoot;
    private TextView probabilityTrain;
    private TextView probabilityTramway;
    private TextView probabilityBus;
    private TextView probabilityCar;
    private TextView probabilityBicycle;
    private TextView probabilityEbike;
    private TextView probabilityMotorcycle;
    private PieChart chart;


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


        sensorData = v.findViewById(R.id.text_sensor);
        probabilityStandingStil = v.findViewById(R.id.text_predicted_still);
        probabilityOnFoot = v.findViewById(R.id.text_predicted_foot);
        probabilityTrain = v.findViewById(R.id.text_predicted_train);
        probabilityTramway = v.findViewById(R.id.text_predicted_tramway);
        probabilityBus = v.findViewById(R.id.text_predicted_bus);
        probabilityCar = v.findViewById(R.id.text_predicted_car);
        probabilityBicycle = v.findViewById(R.id.text_predicted_bicycle);
        probabilityEbike = v.findViewById(R.id.text_predicted_ebike);
        probabilityMotorcycle = v.findViewById(R.id.text_predicted_motorcycle);

        sensorData.setText(getString(R.string.sensors, 0.00));
        probabilityStandingStil.setText(getString(R.string.still, 0.00));
        probabilityOnFoot.setText(getString(R.string.on_foot, 0.00));
        probabilityTrain.setText(getString(R.string.train, 0.00));
        probabilityBus.setText(getString(R.string.bus, 0.00));
        probabilityCar.setText(getString(R.string.car, 0.00));
        probabilityTramway.setText(getString(R.string.tramway, 0.00));
        probabilityBicycle.setText(getString(R.string.bicycle, 0.00));
        probabilityEbike.setText(getString(R.string.ebike, 0.00));
        probabilityMotorcycle.setText(getString(R.string.motorcycle, 0.00));
        chart = v.findViewById(R.id.chart_graph);
    }


    public void updateChart(JSONObject todayData) {

        appendResult();
        showResult();

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

        PieDataSet set = new PieDataSet(pieChartEntries, "");

        set.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setValueLinePart1OffsetPercentage(100f); /** When valuePosition is OutsideSlice, indicates offset as percentage out of the slice size */
        set.setValueLinePart1Length(0.6f); /** When valuePosition is OutsideSlice, indicates length of first half of the line */
        set.setValueLinePart2Length(0.6f); /** When valuePosition is OutsideSlice, indicates length of second half of the line */

        this.chart.setExtraOffsets(0.f, 5.f, 0.f, 5.f); // Ofsets of the view chart to prevent outside values being cropped /** Sets extra offsets (around the chart view) to be appended to the auto-calculated offsets.*/
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData data = new PieData(set);
        this.chart.setData(data);
        data.setValueTextSize(10f);


        this.chart.getDescription().setEnabled(false);
        this.chart.animateXY(1200, 1200);

        this.chart.invalidate();

    }

    private void writeActivity(String act) {

        try (FileWriter file = new FileWriter(getActivity().getFilesDir().getAbsolutePath() + "/" + "storage.txt", true)) {

            file.write(Calendar.getInstance().getTime() + "," + act + "\n");
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void appendResult() {

        float[] predictions = this.mainActivity.getPredictionsNN();
        Log.d("PROBABILITIES: ", Arrays.toString(predictions));

        probabilityOnFoot.append(String.format("%s %.2f %s", " vs", predictions[0] * 100, " %"));
        probabilityTrain.append(String.format("%s %.2f %s", " vs", predictions[1] * 100, " %"));
        probabilityBus.append(String.format("%s %.2f %s", " vs", predictions[2] * 100, " %"));
        probabilityCar.append(String.format("%s %.2f %s", " vs", predictions[3] * 100, " %"));
        probabilityTramway.append(String.format("%s %.2f %s", " vs", predictions[4] * 100, " %"));
        probabilityBicycle.append(String.format("%s %.2f %s", " vs", predictions[5] * 100, " %"));
        probabilityEbike.append(String.format("%s %.2f %s", " vs", predictions[6] * 100, " %"));
        probabilityMotorcycle.append(String.format("%s %.2f %s", " vs", predictions[7] * 100, " %"));

    }


    private void showResult() {

        float[] predictions = this.mainActivity.getPredictions();
        probabilityOnFoot.setText(getString(R.string.on_foot, predictions[0] * 100));
        probabilityTrain.setText(getString(R.string.train, predictions[1] * 100));
        probabilityBus.setText(getString(R.string.bus, predictions[2] * 100));
        probabilityCar.setText(getString(R.string.car, predictions[3] * 100));
        probabilityTramway.setText(getString(R.string.tramway, predictions[4] * 100));
        probabilityBicycle.setText(getString(R.string.bicycle, predictions[5] * 100));
        probabilityEbike.setText(getString(R.string.ebike, predictions[6] * 100));
        probabilityMotorcycle.setText(getString(R.string.motorcycle, predictions[7] * 100));
    }


}



