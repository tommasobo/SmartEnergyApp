package ch.ethz.smartenergy;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StatsFragmenet extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        updateChart();


    }

    private void updateChart() {
        View v = getView();
        LineChart chart = v.findViewById(R.id.chart);


        JSONObject json = new JSONObject();

        if(!isFilePresent(getActivity(), "data.json")) {
            return;
        }

        try {
            json = new JSONObject(read(getActivity(), "data.json"));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

        List<JSONObject> listJson = new ArrayList<>();
        JSONObject finalJson = json;
        Iterator <?> keys = finalJson.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {
                if (finalJson.get(key) instanceof JSONObject) {
                    JSONObject tempJson = finalJson.getJSONObject(key);
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

        LineData lineData = new LineData();
        int i = 0;
        for (String activity: Constants.ListModes) {
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
                    if (e.getInt(activity) != 0) {
                        entries.add(new Entry(cal.getTime().toInstant().
                                atZone(ZoneId.systemDefault()).toLocalDate().
                                getDayOfMonth(), e.getInt(activity), activity));
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            });
            LineDataSet dataSet = new LineDataSet(entries, activity); // add entries to dataset
            final int[] material_colors = {
                    rgb("#2ecc71"), rgb("#f1c40f"), rgb("#e74c3c"), rgb("#3498db"),
                    rgb("#795548"), rgb("#607D8B"), rgb("#E040FB"), rgb("#00BFA5"),
                    rgb("#D81B60")
            };
            dataSet.setColor(material_colors[i]);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleColor(material_colors[i]);
            dataSet.setLineWidth(2f);
            dataSet.setValueTextSize(0);
            lineData.addDataSet(dataSet);
            i++;
        }

        ArrayList<String> xVals = new ArrayList<>();
        for (int j = 0; j <= new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth() ; j++) {
            xVals.add(j + " " + (new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)));
        }

        Description desc = new Description();
        desc.setText("Minutes Per Transportation Mode");
        chart.setDescription(desc);
        chart.setData(lineData);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getXAxis().setAxisLineWidth(1.2f);
        chart.getAxisLeft().setAxisLineWidth(1.2f);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisLeft().setGridLineWidth(0.4f);
        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xVals));
        chart.invalidate(); // refresh
    }

    private int rgb(String s) {
        int color = (int) Long.parseLong(s.replace("#", ""), 16);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        return Color.rgb(r, g, b);
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
}
