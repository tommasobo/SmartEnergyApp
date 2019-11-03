package ch.ethz.smartenergy;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Calendar;
import java.util.Locale;


public class MonthViewFormatter extends ValueFormatter {

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        Calendar cal = Calendar.getInstance();

        // Handle Case when
        if (value == 0) {
            cal.add(Calendar.MONTH, -1);
            String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            return (cal.getActualMaximum(Calendar.DAY_OF_MONTH) + " " + month);
        }

        String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        return ((int) value + " " + month);
    }
}
