package ch.ethz.smartenergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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
        Spinner dietSpinner = getView().findViewById(R.id.spinner_settings_diet);
        ArrayAdapter<CharSequence> dietAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.list_diet, android.R.layout.simple_spinner_item);
        dietAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dietSpinner.setAdapter(dietAdapter);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String diet = preferences.getString("diet", null);
        if(diet != null)
        {
            dietSpinner.setSelection(dietAdapter.getPosition(diet));
        }else{
            dietSpinner.setSelection(dietAdapter.getPosition("Default"));
        }


        Spinner carSpinner = getView().findViewById(R.id.spinner_settings_car);
        ArrayAdapter<CharSequence> carAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.list_car, android.R.layout.simple_spinner_item);
        carAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carSpinner.setAdapter(carAdapter);
        String car = preferences.getString("car", null);
        if(car != null)
        {
            carSpinner.setSelection(carAdapter.getPosition(car));
        }else{
            carSpinner.setSelection(carAdapter.getPosition("Default"));
        }


        dietSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("diet", dietSpinner.getSelectedItem().toString());
                editor.apply();

                System.out.println("SIX CONSOOOOLES");
                System.out.println(dietSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });

        carSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("car", carSpinner.getSelectedItem().toString());
                editor.apply();

                System.out.println("SIX CONSOOOOLES");
                System.out.println(carSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });



    }


}
