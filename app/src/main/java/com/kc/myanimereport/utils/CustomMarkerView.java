package com.kc.myanimereport.utils;

import android.content.Context;
import android.text.Html;
import android.widget.TextView;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.kc.myanimereport.R;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import java.util.Locale;
import java.util.Set;

public class CustomMarkerView extends MarkerView implements IMarker {

    private final TextView tvXContent;
    private final TextView tvYContent;
    private final int mode;

    public CustomMarkerView (Context context, int layoutResource, int mode) {
        super(context, layoutResource);
        tvXContent = findViewById(R.id.tvXContent);
        tvYContent = findViewById(R.id.tvYContent);
        this.mode = mode;
    }

    /* Called everytime the MarkerView is redrawn, can be used to update the UI.
     * Mode = 0: for the Activity line chart
     * Mode = 1: for the Genre pie chart
     * Mode = 2: for the Genre Preference horizontal bar chart
     * Mode = 3: for the Genre Breakdown by Year stacked bar chart
     */
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // Set label (X)
        if (mode == 0) tvXContent.setText(Utils.formatNumber(e.getX(), 0, false));
        else if (mode == 1) tvXContent.setText(((PieEntry) e).getLabel());
        else if (mode == 2) tvXContent.setText(e.getData().toString());
        else if (mode == 3) tvXContent.setText(Utils.formatNumber(e.getX(), 0, false));

        // Set value (Y)
        String yText = "";
        if (mode == 0 || mode == 1) {
            yText = "Animes Watched: " + String.format(Locale.getDefault(), "<b>%.0f</b>", e.getY());
        } else if (mode == 2) {
            yText = "Average Rating: " + String.format(Locale.getDefault(), "<b>%.2f</b>", e.getY());
        } else if (mode == 3) { // Show count for each genre
            tvYContent.setSingleLine(false);
            int i = 0;
            for (String genre: (Set<String>) e.getData()) {
                int genreCount = (int) ((BarEntry) e).getYVals()[i++];
                if (genreCount > 0) yText += genre + ": <b>" + genreCount + "</b><br>";
            }
            if (yText.length() > 0) yText = yText.substring(0, yText.length() - 4); // Removes the last newline
        }
        tvYContent.setText(Html.fromHtml(yText, Html.FROM_HTML_MODE_LEGACY));
        super.refreshContent(e, highlight);
    }

    /* Shifts the marker view so that it is centered horizontally and slightly above the entry. */
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2.0f), -getHeight() - (getHeight() / 4.0f));
    }
}