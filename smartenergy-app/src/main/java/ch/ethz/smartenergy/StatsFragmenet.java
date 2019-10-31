package ch.ethz.smartenergy;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;


public class StatsFragmenet extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        //updateChart();


    }

    /*private void updateChart() {
        View v = getView();
        LineChart chart = (LineChart) v.findViewById(R.id.chart);


        JSONObject json = new JSONObject();

        try {
            json = new JSONObject(read(getActivity(), "data.json"));
        } catch (JSONException err) {
            Log.d("Error", err.toString());
        }

        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy");
        JSONObject todayData = new JSONObject();

        try {
            todayData = json.getJSONObject(date.format(Calendar.getInstance().getTime()));
            todayData = json.getJSONObject(date.format(Calendar.getInstance().get));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        todayData.getJSONObject()

        int i = 0;
        for (String activity : Constants.ListModes) {
            try {
                if (todayData.getInt(activity) != 0) {
                    pieChartEntries.add(new PieEntry(todayData.getInt(activity), activity));
                    colors.add(material_colors[i]);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }

        List<Entry> entries = new ArrayList<>();
        for (YourData data : dataObjects) {
            // turn your data into Entry objects
            entries.add(new Entry(data.getValueX(), data.getValueY()));

        }

        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(...);
        dataSet.setValueTextColor(...); // styling, ...
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }*/

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
