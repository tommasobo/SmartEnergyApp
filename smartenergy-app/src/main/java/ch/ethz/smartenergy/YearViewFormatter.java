package ch.ethz.smartenergy;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DateFormatSymbols;
import java.util.Locale;


public class YearViewFormatter extends ValueFormatter {

    @Override
    public String getAxisLabel(float value, AxisBase axis) {

        DateFormatSymbols symbols = new DateFormatSymbols(Locale.ENGLISH);
        return symbols.getShortMonths()[(int)value - 1];

    }
}
