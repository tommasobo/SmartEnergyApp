package ch.ethz.smartenergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        TextView tv = (TextView) getView().findViewById(R.id.textViewInfo);
        tv.setMovementMethod(new ScrollingMovementMethod());

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
                SharedPreferences preferences1 = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences1.edit();
                editor.putString("diet", checkedRadioButton.getText().toString());
                editor.putInt("dietId", checkedId);
                editor.apply();
            }
            MainActivity main = (MainActivity) getActivity();
            HomeFragment hF = (HomeFragment) main.getHomeFragment();
            hF.updateChart(true);

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
                SharedPreferences preferences12 = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences12.edit();
                editor.putString("car", checkedRadioButton.getText().toString());
                editor.putInt("carId", checkedId);
                editor.apply();
            }

            MainActivity main = (MainActivity) getActivity();
            HomeFragment hF = (HomeFragment) main.getHomeFragment();
            hF.updateChart(true);
        });



    }


}
