package ch.ethz.smartenergy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    public int[] slide_images = {
            R.drawable.lamp,
            R.drawable.deep_learning,
            R.drawable.barchart,
            R.drawable.ecologic
    };

    public String[] slide_headings = {
            "Welcome to Smart Energy",
            "How it works",
            "Visualize your impact",
            "Personalize your experience"
    };

    public String[] slide_paragraphs = {
            "Our App uses Machine Learning to detect automatically what mean of transport you are using and automatically calculates your carbon and energy footprint.\nMoreover we offer a sleek and clean User Interface where you can check your data in graphs.",
            "You don't need to start and stop a trip, just start collecting the data when you feel like it. We will automatically take care of it. Simply close the app when you are done. \n" +
                    "Please note that we only register and save meaningful trips, that means that walking inside the house with many stops in between will most likely not be registered while walking for more time without interruption will.",
            "Look at the different graphs to have a clear picture of your daily carbon and energy footprint  and see how your behaviour changes over time. There are also other interesting stats like distance and time.\n" +
                    "The icons on the home page light up based on how likely they are being used based on our predictions.",
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

        ImageView slideImage = (ImageView) view.findViewById(R.id.slideImage);
        TextView slideHeading = (TextView) view.findViewById(R.id.slideHeading);
        TextView slideParagraph = (TextView) view.findViewById(R.id.slideParagraph);

        slideImage.setImageResource(slide_images[position]);
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
