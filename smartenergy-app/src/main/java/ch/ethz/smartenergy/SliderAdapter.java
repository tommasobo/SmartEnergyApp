package ch.ethz.smartenergy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;

    public SliderAdapter(Context context){
        this.context = context;
    }

    public String[] slide_headings = {
            "Welcome to Smart Energy",
            "Visualize your impact",
            "Personalize your experience"
    };

    public String[] slide_paragraphs = {
            "With our application you can monitor your impact on the environment through transportation.",
            "Look at the different graphs to have a clear picture of your energy usage and see how your behaviour changes over time. ",
            "You can go to settings to personalize your experience. We can provide more accurate results if you can give us some information about your habits."
    };




    @Override
    public int getCount() {
        return slide_headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (RelativeLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);

        TextView slideHeading = (TextView) view.findViewById(R.id.slideHeading);
        TextView slideParagraph = (TextView) view.findViewById(R.id.slideParagraph);

        slideHeading.setText(slide_headings[position]);
        slideParagraph.setText(slide_paragraphs[position]);
        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        container.removeView((RelativeLayout)object);

    }
}
