package ch.ethz.smartenergy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import org.w3c.dom.Text;

public class OnboardingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    private ViewPager slideViewPager;
    private LinearLayout dotLayout;

    private TextView[] dots;

    private SliderAdapter sliderAdapter;

    private Button nextButton;
    private Button backButton;

    private int currentPage;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        slideViewPager = (ViewPager) getView().findViewById(R.id.slideViewPager);
        dotLayout = (LinearLayout) getView().findViewById(R.id.dotNavigation);

        nextButton = (Button) getView().findViewById(R.id.nextButton);
        backButton = (Button) getView().findViewById(R.id.previousButton);

        sliderAdapter = new SliderAdapter(getContext());

        slideViewPager.setAdapter(sliderAdapter);

        addDotsNavigation(0);

        slideViewPager.addOnPageChangeListener(viewListener);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(currentPage == 3){

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("first_time", false);
                    editor.apply();

                    MainActivity main = (MainActivity) getActivity();
                    main.changeViewToPrevious();

                    currentPage = -1;

                }

                slideViewPager.setCurrentItem(currentPage + 1);

            }
        });

        backButton.setOnClickListener(v -> slideViewPager.setCurrentItem(currentPage - 1));

    }

    private void addDotsNavigation(int position){

        dots = new TextView[4];
        dotLayout.removeAllViews();

        for(int i = 0; i < dots.length; i++){
            dots[i] = new TextView(getContext());
            dots[i].setText(Html.fromHtml("&#8226", HtmlCompat.FROM_HTML_MODE_LEGACY));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getContext().getColor(R.color.colorWhiteSoft));

            dotLayout.addView(dots[i]);
        }

        if(dots.length > 0){
            dots[position].setTextColor(getContext().getColor(R.color.colorWhite));
        }

    }

    private ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {


        }

        @Override
        public void onPageSelected(int position) {

            addDotsNavigation(position);

            currentPage = position;

            if(currentPage == 0){
                nextButton.setEnabled(true);
                backButton.setEnabled(false);
                backButton.setVisibility(View.INVISIBLE);

                nextButton.setText("Next");
                backButton.setText("");
            } else if (position == dots.length - 1) {
                nextButton.setEnabled(true);
                backButton.setEnabled(true);
                backButton.setVisibility(View.VISIBLE);

                nextButton.setText("Finish");
                backButton.setText("Back");
            } else {
                nextButton.setEnabled(true);
                backButton.setEnabled(true);
                backButton.setVisibility(View.VISIBLE);

                nextButton.setText("Next");
                backButton.setText("Back");
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
