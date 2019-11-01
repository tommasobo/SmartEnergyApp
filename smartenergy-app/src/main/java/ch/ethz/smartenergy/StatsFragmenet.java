package ch.ethz.smartenergy;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


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

        System.out.println(listJson);
        List<Entry> entries = new ArrayList<>();

        listJson.forEach(e-> {
            try {
                entries.add(new Entry());
                entries.add(new Entry(2, e.getInt("Still")));
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        });


        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
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
}
