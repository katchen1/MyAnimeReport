package com.example.myanimereport.utils;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.example.myanimereport.R;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

public class CustomMarkerView extends MarkerView implements IMarker {

    private TextView tvXContent;
    private TextView tvYContent;

    public CustomMarkerView (Context context, int layoutResource) {
        super(context, layoutResource);
        tvXContent = (TextView) findViewById(R.id.tvXContent);
        tvYContent = (TextView) findViewById(R.id.tvYContent);

    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;
            tvYContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, false));
        } else {
            tvXContent.setText(Utils.formatNumber(e.getX(), 0, false));
            tvYContent.setText("Animes Watched: " + Utils.formatNumber(e.getY(), 0, true));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight() - getHeight() / 4);
    }
}