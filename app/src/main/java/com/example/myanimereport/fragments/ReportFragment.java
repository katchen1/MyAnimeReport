package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myanimereport.R;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.FragmentReportBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.utils.CustomMarkerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
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
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* Charts included:
 * 1. Overview [Score Cards]
 *   1A. Total watched
 *   1B. Average rating
 *   1C. Watched this year (compared to last year)
 *   1D. Avg rating this year (compared to last year)
 * 2. Activity [Line Chart] - year vs. entry count
 * 3. Genres [Pie]
 * 4. Genre Preference [Bar] - genre vs. average rating
 * 5. Genres breakdown by year [Stacked Bar] - year vs. num anime in each genre
 * 6. Top 5 anime by user's rating
 */
public class ReportFragment extends Fragment {

    private FragmentReportBinding binding;
    private List<Entry> entries; // Entries to be analyzed
    private Map<Integer, List<Entry>> yearToList; // Organize the entries by year
    private Map<String, List<Entry>> genreToList; // Organize the entries by genre
    private int white, red, green, theme, darkGray; // Colors used by the report
    private Integer minYear, maxYear; // For charts with year on the x-axis

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            // Set colors
            white = ContextCompat.getColor(getContext(), R.color.white);
            red = ContextCompat.getColor(getContext(), R.color.red);
            green = ContextCompat.getColor(getContext(), R.color.green);
            theme = ContextCompat.getColor(getContext(), R.color.theme);
            darkGray = ContextCompat.getColor(getContext(), R.color.dark_gray);
        }
    }

    /* When the report tab is clicked, regenerate the charts. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        entries = ParseApplication.entries;

        if (entries.isEmpty()) {
            // Do something to indicate no data
            return;
        }

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
        }

        // Find min/max years
        List<Integer> years = new ArrayList<>(yearToList.keySet());
        Collections.sort(years);
        minYear = years.get(0);
        maxYear = years.get(years.size() - 1);

        // Generate the charts
        setOverview();
        setChartActivity();
        setChartGenre();
        setChartGenrePref();
        setGenreBreakdownByYear();
        setTop5();
    }

    /* Overview [Score Cards] */
    public void setOverview() {
        // Total animes watched
        binding.tvCount.setText(String.format(Locale.getDefault(), "%d", entries.size()));

        // Average rating
        double sumRating = 0;
        for (Entry entry: entries) sumRating += entry.getRating();
        double rating = sumRating / entries.size();
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", rating));

        // Animes watched this year + average rating this year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int prevYear = currentYear - 1;

        // Default values
        binding.tvRecentCount.setText("0");
        binding.tvRecentRating.setText("n/a");
        binding.tvCountCompare.setText("\u2195 n/a");
        binding.tvRatingCompare.setText("\u2195 n/a");
        binding.tvCountCompare.setTextColor(white);
        binding.tvRatingCompare.setTextColor(white);

        // Set current year info
        List<Entry> recentEntries = new ArrayList<>();
        if (yearToList.containsKey(currentYear)) {
            recentEntries = yearToList.get(currentYear);
            binding.tvRecentCount.setText(String.format(Locale.US, "%d", recentEntries.size()));
            binding.tvRecentRating.setText(String.format(Locale.US, "%.1f", getAverageRating(recentEntries)));
        }

        // Set comparisons to previous year
        if (yearToList.containsKey(prevYear)) {
            List<Entry> prevEntries = yearToList.get(prevYear);
            setComparisonText(prevEntries.size(), recentEntries.size(), binding.tvCountCompare);
            if (!binding.tvRecentRating.getText().equals("n/a")) {
                setComparisonText(getAverageRating(prevEntries),
                        Double.parseDouble(binding.tvRecentRating.getText().toString()),
                        binding.tvRatingCompare);
            }
        }
    }

    /* Activity (Line Chart) - year vs. entry count */
    public void setChartActivity() {
        LineChart chart = binding.chartActivity;

        // Create data points
        List<com.github.mikephil.charting.data.Entry> chartEntries = new ArrayList<>();
        List<Integer> entryCounts = new ArrayList<>();
        for (int year = minYear; year <= maxYear; year++) {
            int entryCount = yearToList.containsKey(year)? yearToList.get(year).size(): 0;
            chartEntries.add(new com.github.mikephil.charting.data.Entry(year, entryCount));
            entryCounts.add(entryCount);
        }

        // Turn the points into a data set (sorted chronologically)
        chartEntries.sort((e1, e2) -> (Double.compare(e1.getX(), e2.getX())));
        LineDataSet dataSet = new LineDataSet(chartEntries, "Animes Watched");
        dataSet.setColor(theme);
        dataSet.setCircleColor(theme);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(1.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setHighLightColor(theme);
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setDrawValues(false);
        chart.setData(new LineData(dataSet));

        // Customize the chart
        customizeChart(chart, 0);
        customizeXAxis(chart.getXAxis());
        customizeYAxisMain(chart.getAxisLeft());
        customizeYAxisSecondary(chart.getAxisRight());
        chart.getXAxis().setAxisMinimum(chartEntries.get(0).getX());
        chart.getAxisLeft().setLabelCount(Collections.max(entryCounts) + 2, true);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisLeft().setAxisMaximum(Collections.max(entryCounts) + 1);
        chart.setScaleEnabled(false);
        refreshChart(chart);
    }

    /* Genre [Pie] */
    public void setChartGenre() {
        PieChart chart = binding.chartGenre;

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
        pieDataSet.setDrawValues(false);
        pieDataSet.setValueLineColor(white);
        chart.setData(new PieData(pieDataSet));

        // Customize the chart
        customizeChart(chart, 1);
        chart.getLegend().setEnabled(false);
        chart.setHoleColor(darkGray);
        refreshChart(chart);
    }

    /* Genre Preference [Bar] - genre vs. average rating */
    private void setChartGenrePref() {
        BarChart chart = binding.chartGenrePref;

        // Create data points for average genre ratings
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        int xValue = 0;
        for (String genre: genreToList.keySet()) {
            double avgRating = getAverageRating(genreToList.get(genre));
            barEntries.add(new BarEntry(xValue++, (float) avgRating, genre));
        }

        // Create data set
        BarDataSet dataSet = new BarDataSet(barEntries, "Average Rating");
        dataSet.setColor(theme);
        dataSet.setDrawValues(false);
        chart.setData(new BarData(Arrays.asList(dataSet)));

        // Customize the chart
        customizeChart(chart, 2);
        customizeXAxis(chart.getXAxis());
        customizeYAxisMain(chart.getAxisRight());
        customizeYAxisSecondary(chart.getAxisLeft());
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(genreToList.keySet()));
        chart.getXAxis().setLabelCount(genreToList.keySet().size());
        chart.setScaleEnabled(false);
        chart.setFitBars(true);
        refreshChart(chart);
    }

    /* Genres breakdown by year [Stacked Bar] - year vs. num anime in each genre */
    public void setGenreBreakdownByYear() {
        BarChart chart = binding.chartGenreBreakdown;

        // Create a bar entry for each year, with each entry having multiple y values
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (int year = minYear; year <= maxYear; year++) {
            float[] genreCounts = new float[genreToList.keySet().size()];
            int i = 0;
            for (String genre: genreToList.keySet()) {
                int genreCount = 0;
                if (yearToList.containsKey(year)) {
                    for (Entry entry: yearToList.get(year)) {
                        if (entry.getAnime().getGenres().contains(genre)) genreCount++;
                    }
                }
                genreCounts[i++] = genreCount;
            }
            barEntries.add(new BarEntry(year, genreCounts, genreToList.keySet()));
        }

        // Create a dataset
        BarDataSet dataSet = new BarDataSet(barEntries, "Animes Watched");
        List<Integer> colors = new ArrayList<>();
        int i = 0;
        for (String ignored : genreToList.keySet()) colors.add(ColorTemplate.MATERIAL_COLORS[i++%4]);
        dataSet.setColors(colors);
        dataSet.setStackLabels(genreToList.keySet().toArray(new String[0]));
        dataSet.setDrawValues(false);
        BarData data = new BarData(Arrays.asList(dataSet));
        data.setValueTextColor(white);
        chart.setData(data);

        // Customize the chart
        customizeChart(chart, 3);
        customizeXAxis(chart.getXAxis());
        customizeYAxisMain(chart.getAxisLeft());
        customizeYAxisSecondary(chart.getAxisRight());
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.setFitBars(true);
        chart.setScaleEnabled(false);
        refreshChart(chart);
    }

    /* The 5 animes with the highest rating. */
    private void setTop5() {
        // Sort by rating (descending) and get the top 5
        List<Entry> sortedEntries = new ArrayList<>(entries);
        sortedEntries.sort((e1, e2) -> Double.compare(e2.getRating(), e1.getRating()));
        if (sortedEntries.size() > 5) sortedEntries = sortedEntries.subList(0, 5);

        // Set up adapter and layout of recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        EntriesAdapter adapter = new EntriesAdapter(this, sortedEntries, false);
        binding.rvTop5.setLayoutManager(layoutManager);
        binding.rvTop5.setAdapter(adapter);
    }

    /* Calculates the average rating of a list of entries. */
    public double getAverageRating(List<Entry> entries) {
        double sumRating = 0;
        for (Entry entry : entries) sumRating += entry.getRating();
        return sumRating / entries.size();
    }

    /* Calculates and sets the percent difference between two numbers. */
    public void setComparisonText(double prev, double curr, TextView tvCompare) {
        double diff = (curr - prev) / prev;
        if (diff > 0) {
            tvCompare.setText(String.format(Locale.getDefault(), "\u2191 %.1f%%", diff * 100));
            tvCompare.setTextColor(green);
        } else if (diff == 0) {
            tvCompare.setText(String.format(Locale.getDefault(), "\u2195 %.1f%%", diff * 100));
            tvCompare.setTextColor(white);
        } else if (diff < 0) {
            tvCompare.setText(String.format(Locale.getDefault(), "\u2193 %.1f%%", diff * 100));
            tvCompare.setTextColor(red);
        }
    }

    /* Refreshes a chart. */
    public void refreshChart(Chart chart) {
        chart.animateXY(2000,2000);
        chart.invalidate();
    }

    /* General styling of the x-axis. */
    public void customizeXAxis(XAxis xAxis) {
        xAxis.setTextColor(white);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGridLineWidth(1f);
        xAxis.setGranularity(1f);
    }

    /* General styling of the main y-axis. */
    public void customizeYAxisMain(YAxis yAxis) {
        yAxis.setGridLineWidth(1f);
        yAxis.setTextColor(white);
    }

    /* General styling of the secondary y-axis. */
    public void customizeYAxisSecondary(YAxis yAxis) {
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);
    }

    /* General styling of a chart. Mode is for different ways of drawing the marker view. */
    public void customizeChart(Chart chart, int mode) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(white);
        chart.getLegend().setWordWrapEnabled(true);
        chart.getLegend().setYOffset(3f);
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view_layout, mode);
        mv.setChartView(chart);
        chart.setMarker(mv);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}