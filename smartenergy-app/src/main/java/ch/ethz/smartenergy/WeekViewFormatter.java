package ch.ethz.smartenergy;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Calendar;
import java.util.Locale;


public class WeekViewFormatter extends ValueFormatter {

    @Override
    public String getAxisLabel(float value, AxisBase axis) {

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE,  - 6 + (int)value);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);
        return (currentDay + " " + month);
    }
}

