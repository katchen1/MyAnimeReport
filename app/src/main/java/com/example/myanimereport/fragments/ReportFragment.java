package com.example.myanimereport.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
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
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.utils.CustomMarkerView;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
public class ReportFragment extends Fragment {

    private final String TAG = "ReportFragment";
    private FragmentReportBinding binding;
    private List<Entry> entries;
    private Map<Integer, List<Entry>> yearToList; // Organize the entries by year
    private Map<String, List<Entry>> genreToList; // Organize the entries by genre

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /* When the report tab is clicked, regenerate the charts. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        entries = ParseApplication.entries;

        // Generate the maps
        yearToList = new HashMap<>();
        genreToList = new HashMap<>();
        for (Entry entry: entries) {
            // Year to entries map
            Integer year = entry.getYearWatched();
            if (!yearToList.containsKey(year)) yearToList.put(year, new ArrayList<>());
            yearToList.get(year).add(entry);

            // Genre to entries map
            List<String> genres = entry.getAnime().getGenres();
            for (String genre: genres) {
                if (!genreToList.containsKey(genre)) genreToList.put(genre, new ArrayList<>());
                genreToList.get(genre).add(entry);
            }

            // Generate the charts
            setOverview();
            setChartActivity();
            setChartGenre();
            setChartGenrePref();
        }
    }

    /* Overview [Score Cards] */
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

    /* Activity (Line Chart) - year vs. entry count */
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
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout, 0);
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
        binding.chartActivity.animateXY(2000,2000);
        binding.chartActivity.invalidate();
    }

    /* Genre [Pie] */
    public void setChartGenre() {
        // Create data points
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        for (String genre: genreToList.keySet()) {
            pieEntries.add(new PieEntry(genreToList.get(genre).size(),  genre));
        }

        // Create data set
        PieDataSet pieDataSet = new PieDataSet(pieEntries,"Genre");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieDataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.white));
        pieDataSet.setDrawValues(false);
        pieDataSet.setValueLineColor(ContextCompat.getColor(getContext(), R.color.white));

        // Custom marker
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout, 1);
        mv.setChartView(binding.chartGenre);
        binding.chartGenre.setMarker(mv);

        PieData pieData = new PieData(pieDataSet);
        binding.chartGenre.setData(pieData);
        Legend legend = binding.chartGenre.getLegend();
        legend.setTextSize(13);
        legend.setDrawInside(false);
        legend.setTextColor(getResources().getColor(R.color.white));
        legend.setWordWrapEnabled(true);
        binding.chartGenre.animateXY(2000,2000);
        binding.chartGenre.getLegend().setEnabled(false);
        binding.chartGenre.getDescription().setEnabled(false);
        binding.chartGenre.setHoleColor(ContextCompat.getColor(getContext(), R.color.dark_gray));
        binding.chartGenre.invalidate();
    }

    /* Genre Preference [Bar] - genre vs. average rating */
    private void setChartGenrePref() {
        binding.chartGenrePref.getDescription().setEnabled(false);

        // Create data set
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        int count = 0;
        ArrayList<String> xAxisLabels = new ArrayList<>();
        ArrayList<Float> yValues = new ArrayList<>();
        for (String genre: genreToList.keySet()) {
            // Calculate average rating
            float sumRating = 0;
            for (Entry entry: genreToList.get(genre)) {
                sumRating += entry.getRating();
            }
            float avgRating = sumRating / genreToList.get(genre).size();
            barEntries.add(new BarEntry(count, avgRating, genre));
            yValues.add(avgRating);
            count++;
            xAxisLabels.add(genre);
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Average Rating");
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.theme));
        dataSet.setDrawValues(false);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        BarData data = new BarData(dataSets);

        // Custom marker
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout, 2);
        mv.setChartView(binding.chartGenrePref);
        binding.chartGenrePref.setMarker(mv);

        data.setValueTextSize(12f);
        binding.chartGenrePref.setData(data);
        binding.chartGenrePref.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        binding.chartGenrePref.getXAxis().setDrawGridLines(false);
        binding.chartGenrePref.getAxisRight().setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        binding.chartGenrePref.getAxisLeft().setDrawLabels(false);
        binding.chartGenrePref.getAxisLeft().setDrawGridLines(false);

        YAxis yAxis = binding.chartGenrePref.getAxisRight();
        yAxis.setAxisMaximum(10);

        XAxis xAxis = binding.chartGenrePref.getXAxis();
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setLabelCount(genreToList.keySet().size());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        binding.chartGenrePref.animateXY(2000, 2000);
        binding.chartGenrePref.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}