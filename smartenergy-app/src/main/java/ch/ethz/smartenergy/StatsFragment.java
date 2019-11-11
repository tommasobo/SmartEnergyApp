package ch.ethz.smartenergy;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StatsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        Spinner spinner = getView().findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.list_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition("Current Month"));

        updateChart();


    }

    void updateChart() {
        View v = getView();
        LineChart chart = v.findViewById(R.id.chart);
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

        chart.clear();
        chart.invalidate();
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
                    if (e.getJSONObject(activity).getInt("time") != 0) {
                        Date date = cal.getTime();
                        entries.add(new Entry(cal.get(Calendar.DAY_OF_MONTH), e.getJSONObject(activity).getInt("time"), activity));
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

        Calendar cal = Calendar.getInstance();
        String selectedFilterText = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        chart.getDescription().setText("Minutes Per Transportation Mode for " + selectedFilterText);
        DisplayMetrics ds = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(ds);
        int width = ds.widthPixels;
        int height = ds.heightPixels;
        chart.getDescription().setTextSize(12f);
        chart.setData(lineData);
        chart.getLineData().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ("" + (int)value);
            }
        });

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setAxisLineWidth(1.2f);
        chart.getAxisLeft().setGridLineWidth(0.7f);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return ("" + (int)value);
            }
        });
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisLeft().setAxisMinimum(0);
        //chart.getData().setValueTextColor(Color.BLACK);
        //chart.getData().setValueTextSize(10);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new MonthViewFormatter());
        boolean forceLabelCount = false;
        if (lineData.getXMin() != lineData.getXMax()) {
            chart.getXAxis().setAxisMinimum(getMinXAxisPerMonth(lineData.getXMin()));
            chart.getXAxis().setAxisMaximum(getMaxXAxisPerMonth(lineData.getXMax()));
        }
        if (getLabelNumberForMonth(listJson.size()) > 3) {
            forceLabelCount = true;
        }
        chart.getXAxis().setLabelCount(getLabelNumberForMonth(listJson.size()), forceLabelCount);
        chart.getXAxis().setGranularityEnabled(true);
        chart.getXAxis().setCenterAxisLabels(false);
        chart.getXAxis().setAxisLineWidth(1.2f);
        chart.getXAxis().setDrawGridLines(false);

        chart.invalidate(); // refresh
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
}
