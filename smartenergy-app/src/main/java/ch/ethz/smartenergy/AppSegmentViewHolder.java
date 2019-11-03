package ch.ethz.smartenergy;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import segmented_control.widget.custom.android.com.segmentedcontrol.item_row_column.SegmentViewHolder;

public class AppSegmentViewHolder extends SegmentViewHolder<String> {
    TextView textView;

    public AppSegmentViewHolder(@NonNull View sectionView) {
        super(sectionView);
        textView = (TextView) sectionView.findViewById(R.id.text_view);
    }

    @Override
    protected void onSegmentBind(String segmentData) {
        textView.setText(segmentData);
    }
}