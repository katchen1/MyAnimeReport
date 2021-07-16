package com.example.myanimereport.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.FragmentReportBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.utils.CustomMarkerView;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFragment extends Fragment {

    private final String TAG = "ReportFragment";
    private FragmentReportBinding binding;
    List<Entry> entries;

    Map<Integer, List<Entry>> yearToList;
    Map<String, List<Entry>> genreToList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        entries = new ArrayList<>();
        yearToList = new HashMap<>();
        genreToList = new HashMap<>();
        queryEntries(0);
    }

    /* Queries the entries 10 at a time. Skips the first skip items. */
    public void queryEntries(int skip) {
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(10); // Limit query to 10 items
        query.whereEqualTo(Entry.KEY_USER, ParseUser.getCurrentUser()); // Limit entries to current user's
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground((entriesFound, e) -> {
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting entries.", e);
                return;
            }
            entries.addAll(entriesFound);
            setCharts();
        });
    }

    /* Charts I want to include:
     * 1. Overview [Score Cards]
     *   1A. Total watched
     *   1B. Average rating
     *   1C. Watched this year (compared to last year)
     *   1D. Avg rating this year (compared to last year)
     * 2. Top 5
     * 3. Activity [Line Chart] - year vs. entry count
     * 4. Genres breakdown by year [Stacked Bar] - year vs. num anime in each genre
     * 5. Demographics [Pie]
     * 6. Genres [Pie]
     * 7. Genre Preference [Bar] - genre vs. average rating
     */
    private void setCharts() {
        // Set up maps
        for (Entry entry: entries) {
            Integer year = entry.getYearWatched();
            if (!yearToList.containsKey(year)) {
                yearToList.put(year, new ArrayList<>());
            }
            yearToList.get(year).add(entry);
        }

        setOverview();
        setChartActivity();
    }

    /* 1. Overview [Score Cards]
     *   1A. Total watched
     *   1B. Average rating
     *   1C. Watched this year (compared to last year)
     *   1D. Avg rating this year (compared to last year)
     */
    public void setOverview() {
        // Total animes watched
        int count = entries.size();
        binding.tvCount.setText(String.format(Locale.getDefault(), "%d", count));

        // Average rating
        double sumRating = 0;
        for (Entry entry: entries) sumRating += entry.getRating();
        double rating = sumRating / count;
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));

        // Animes watched this year and average rating this year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String recentCountText = "";
        String recentRatingText = "";
        if (yearToList.containsKey(currentYear)) {
            List<Entry> recentEntries = yearToList.get(currentYear);
            double sumRecentRating = 0;
            for (Entry entry: recentEntries) sumRecentRating += entry.getRating();
            recentCountText = recentEntries.size() + "";
            recentRatingText = sumRecentRating / recentEntries.size() + "";
        } else {
            recentCountText = "0";
            recentRatingText = "n/a";
        }
        binding.tvRecentCount.setText(recentCountText);
        binding.tvRecentRating.setText(recentRatingText);
    }

    /* 2. Activity (Line Chart) - year vs. entry count */
    public void setChartActivity() {
        // Create data points
        List<com.github.mikephil.charting.data.Entry> chartEntries = new ArrayList<>();
        List<Integer> years = new ArrayList<>(yearToList.keySet());
        Collections.sort(years);
        List<Integer> yValues = new ArrayList<>();
        for (Integer year = years.get(0); year <= years.get(years.size() - 1); year++) {
            int entryCount = yearToList.containsKey(year)? yearToList.get(year).size(): 0;
            chartEntries.add(new com.github.mikephil.charting.data.Entry(year, entryCount));
            yValues.add(entryCount);
        }

        // Turn the points into a data set
        chartEntries.sort((e1, e2) -> (Double.compare(e1.getX(), e2.getX()))); // Sort chronologically
        LineDataSet dataSet = new LineDataSet(chartEntries, "Animes Watched");
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.theme));
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), R.color.theme));
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.theme));
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(1.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.transparent));

        // Create a line chart
        LineData lineData = new LineData(dataSet);
        binding.chartActivity.setData(lineData);

        // Customize x-axis
        XAxis xAxis = binding.chartActivity.getXAxis();
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(chartEntries.get(0).getX());
        xAxis.setGridLineWidth(1f);
        xAxis.setGranularity(1f);

        // Customize y-axis
        binding.chartActivity.getAxisRight().setDrawLabels(false);
        binding.chartActivity.getAxisRight().setDrawGridLines(false);
        YAxis yAxis = binding.chartActivity.getAxisLeft();
        yAxis.setGridLineWidth(1f);
        yAxis.setLabelCount(Collections.max(yValues) + 2, true);
        yAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        yAxis.setAxisMinimum(0);
        yAxis.setAxisMaximum(Collections.max(yValues) + 1);

        // Customize overall
        binding.chartActivity.getDescription().setEnabled(false);
        binding.chartActivity.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.white));

        // Custom marker
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout);
        mv.setChartView(binding.chartActivity);
        binding.chartActivity.setMarker(mv);

        // Interaction
        binding.chartActivity.setTouchEnabled(true);
        binding.chartActivity.setDragEnabled(true);
        binding.chartActivity.setScaleEnabled(false);
        binding.chartActivity.setPinchZoom(true);
        binding.chartActivity.setHighlightPerTapEnabled(true);
        binding.chartActivity.setDoubleTapToZoomEnabled(false);

        // Refresh
        binding.chartActivity.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}