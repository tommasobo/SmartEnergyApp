package ch.ethz.smartenergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.xizzhu.simpletooltip.ToolTip;
import com.github.xizzhu.simpletooltip.ToolTipView;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        getView().findViewById(R.id.button_info_diet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolTip toolTip = new ToolTip.Builder()
                        .withText("Choose diet to count emissions \n for walking and bicycle. \n If ignored they will count as zero.")
                        .withTextSize(40)
                        .withPadding(15,15,15,15)
                        .withBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorDarkGrey))
                        .build();
                ToolTipView toolTipView = new ToolTipView.Builder(getContext())
                        .withAnchor(v)
                        .withToolTip(toolTip)
                        .withGravity(Gravity.END)
                        .build();
                toolTipView.show();
            }
        });

        getView().findViewById(R.id.button_info_car).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolTip toolTip = new ToolTip.Builder()
                        .withText("Choose the car you own \n to make calculations \n more precise")
                        .withTextSize(40)
                        .withPadding(15,15,15,15)
                        .withBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorDarkGrey))
                        .build();
                ToolTipView toolTipView = new ToolTipView.Builder(getContext())
                        .withAnchor(v)
                        .withToolTip(toolTip)
                        .withGravity(Gravity.END)
                        .build();
                toolTipView.show();
            }
        });


        RadioGroup rgDiet = (RadioGroup) getView().findViewById(R.id.radio_group_diet);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int dietId = preferences.getInt("dietId", -1);
        if(dietId != -1)
        {
            rgDiet.check(dietId);
        }else{
            rgDiet.check(R.id.radio_diet_ignore);
        }

        rgDiet.setOnCheckedChangeListener((group, checkedId) -> {
            // checkedId is the RadioButton selected
            RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);

            boolean isChecked = checkedRadioButton.isChecked();
            // If the radiobutton that has changed in check state is now checked...
            if (isChecked)
            {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("diet", checkedRadioButton.getText().toString());
                editor.putInt("dietId", checkedId);
                editor.apply();
            }
            MainActivity main = (MainActivity) getActivity();
            HomeFragment hF = (HomeFragment) main.getHomeFragment();
            hF.updateChart(true);
            StatsFragment sF = (StatsFragment) main.getStatsFragment();
            sF.updateChart();

        });

        RadioGroup rgCar = (RadioGroup) getView().findViewById(R.id.radio_group_car);


        int carId = preferences.getInt("carId", -1);
        if(carId != -1)
        {
            rgCar.check(carId);
        }else{
            rgCar.check(R.id.radio_car_default);
        }

        rgCar.setOnCheckedChangeListener((group, checkedId) -> {
            // checkedId is the RadioButton selected
            RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);

            boolean isChecked = checkedRadioButton.isChecked();
            // If the radiobutton that has changed in check state is now checked...
            if (isChecked)
            {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("car", checkedRadioButton.getText().toString());
                editor.putInt("carId", checkedId);
                editor.apply();
            }

            MainActivity main = (MainActivity) getActivity();
            HomeFragment hF = (HomeFragment) main.getHomeFragment();
            hF.updateChart(true);
            StatsFragment sF = (StatsFragment) main.getStatsFragment();
            sF.updateChart();
        });




        Button buttonOpenSlider = (Button) getView().findViewById(R.id.buttonOpenSlider);
        buttonOpenSlider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity main = (MainActivity) getActivity();
                main.changeViewToOnboarding();
            }
        });



    }


}
