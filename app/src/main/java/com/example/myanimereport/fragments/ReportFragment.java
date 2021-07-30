package com.example.myanimereport.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.ActivityMainBinding;
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
import com.github.mikephil.charting.renderer.PieChartRenderer;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

        // Button listeners
        binding.btnMenu.setOnClickListener(this::openNavDrawer);
        binding.btnShare.setOnClickListener(this::shareOnClick);
    }

    /* Allows user to share their report to social media. */
    private void shareOnClick(View view) {
        NestedScrollView scrollView = binding.scrollView;
        Bitmap screenshot = getScreenshot(scrollView,
                scrollView.getChildAt(0).getHeight(),
                scrollView.getChildAt(0).getWidth());
        saveScreenshot(screenshot, "report");
    }

    /* Returns a screenshot bitmap of a view. */
    private Bitmap getScreenshot(View view, int height, int width) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(ContextCompat.getColor(requireContext(), R.color.dark_gray));
        view.draw(canvas);
        return bitmap;
    }

    /* Saves the screenshot. */
    public void saveScreenshot(Bitmap bm, String fileName){
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getFilesDir();
        File file = new File(directory,"report.png");
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            shareImage(file);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Shares report as an image. */
    private void shareImage(File file){
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getApplicationContext().getPackageName() + ".provider", file);
        System.out.println("URI: " + uri.toString());
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Share Screenshot"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No App Available", Toast.LENGTH_SHORT).show();
        }
    }

    /* Opens the navigation drawer. */
    private void openNavDrawer(View view) {
        ActivityMainBinding binding = MainActivity.binding;
        binding.btnLayout.setVisibility(View.GONE);
        binding.btnSort.setVisibility(View.GONE);
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortTitle.setVisibility(View.GONE);
        binding.btnSortWatchDate.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.btnDeleteAllEntries.setVisibility(View.GONE);
        binding.btnDeleteBacklog.setVisibility(View.GONE);
        binding.btnFilter.setVisibility(View.GONE);
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    /* When the report tab is clicked, regenerate the charts. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        entries = ParseApplication.entries;

        // Check if user has data
        if (entries.isEmpty()) {
            binding.scrollView.setVisibility(View.INVISIBLE);
            binding.rlMessage.setVisibility(View.VISIBLE);
            binding.tvCreate.setOnClickListener(this::createOnClick);
            return;
        } else {
            binding.scrollView.setVisibility(View.VISIBLE);
            binding.rlMessage.setVisibility(View.INVISIBLE);
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
            if (entry.getAnime() != null) {
                List<String> genres = entry.getAnime().getGenres();
                for (String genre : genres) {
                    if (!genreToList.containsKey(genre)) genreToList.put(genre, new ArrayList<>());
                    genreToList.get(genre).add(entry);
                }
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
        customizeLineDataSet(dataSet);
        chart.setData(new LineData(dataSet));

        // Customize the chart
        int maxY = Collections.max(entryCounts) + 1;
        customizeChart(chart, 0);
        customizeXAxis(chart.getXAxis(), chartEntries.get(0).getX(), -1, -1, false, true);
        customizeYAxisMain(chart.getAxisLeft(), 0, maxY, maxY + 1);
        customizeYAxisSecondary(chart.getAxisRight());
        chart.setScaleEnabled(false);
        chart.highlightValue(null);
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
        pieEntries.sort((e1, e2) -> Float.compare(e2.getY(), e1.getY())); // Descending order

        // Create data set
        PieDataSet dataSet = new PieDataSet(pieEntries,"Genre");
        customizePieDataSet(dataSet);
        chart.setData(new PieData(dataSet));

        // Customize the chart
        customizeChart(chart, 1);
        chart.getLegend().setEnabled(false);
        chart.setHoleColor(darkGray);
        chart.highlightValue(null);
        refreshChart(chart);
    }

    /* Genre Preference [Bar] - genre vs. average rating */
    private void setChartGenrePref() {
        BarChart chart = binding.chartGenrePref;

        // Create data points for average genre ratings
        ArrayList<Pair<String, Float>> pairs = new ArrayList<>();
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> genres = new ArrayList<>();
        int xValue = 0;
        for (String genre: genreToList.keySet()) {
            pairs.add(new Pair<>(genre, (float) getAverageRating(genreToList.get(genre))));
        }
        pairs.sort((p1, p2) -> Float.compare(p1.second, p2.second));
        for (Pair<String, Float> p: pairs) {
            genres.add(p.first);
            barEntries.add(new BarEntry(xValue++, p.second, p.first));
        }

        // Create data set
        BarDataSet dataSet = new BarDataSet(barEntries, "Average Rating");
        customizeBarDataSet(dataSet, null, theme, null);
        chart.setData(new BarData(Collections.singletonList(dataSet)));

        // Customize the chart
        customizeChart(chart, 2);
        customizeXAxis(chart.getXAxis(), -1, -1, genreToList.keySet().size(), false, false);
        customizeYAxisMain(chart.getAxisRight(), -1, -1, -1);
        customizeYAxisSecondary(chart.getAxisLeft());
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(genres));
        chart.setScaleEnabled(false);
        chart.highlightValue(null);
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
                        if (entry.getAnime() != null && entry.getAnime().getGenres().contains(genre)) {
                            genreCount++;
                        }
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
        customizeBarDataSet(dataSet, genreToList.keySet().toArray(new String[0]), -1, colors);
        chart.setData(new BarData(Collections.singletonList(dataSet)));

        // Customize the chart
        customizeChart(chart, 3);
        customizeXAxis(chart.getXAxis(), -1, -1, -1, false, false);
        customizeYAxisMain(chart.getAxisLeft(), 0, -1, -1);
        customizeYAxisSecondary(chart.getAxisRight());
        chart.setScaleEnabled(false);
        chart.highlightValue(null);
        chart.setFitBars(true);
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

    /* General styling of the x-axis. Range between min and max (auto range if min/max = -1). */
    public void customizeXAxis(XAxis xAxis, float min, float max, int labelCount, boolean force, boolean gridLines) {
        xAxis.setTextColor(white);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGridLineWidth(1f);
        xAxis.setGranularity(1f);
        if (min != -1) xAxis.setAxisMinimum(min);
        if (max != -1) xAxis.setAxisMaximum(max);
        if (labelCount != -1) xAxis.setLabelCount(labelCount, force);
        xAxis.setDrawGridLines(gridLines);
    }

    /* General styling of the main y-axis. Range between min and max (auto range if min/max = -1). */
    public void customizeYAxisMain(YAxis yAxis, float min, float max, int labelCount) {
        yAxis.setGridLineWidth(1f);
        yAxis.setTextColor(white);
        if (min != -1) yAxis.setAxisMinimum(min);
        if (max != -1) yAxis.setAxisMaximum(max);
        if (labelCount != -1) yAxis.setLabelCount(labelCount, true);
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

    /* General styling of a line dataset. */
    public void customizeLineDataSet(LineDataSet dataSet) {
        dataSet.setColor(theme);
        dataSet.setCircleColor(theme);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(1.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setHighLightColor(theme);
        dataSet.setDrawHighlightIndicators(false);
        dataSet.setDrawValues(false);
    }

    /* General styling of a pie dataset. */
    public void customizePieDataSet(PieDataSet dataSet) {
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setDrawValues(false);
        dataSet.setValueLineColor(white);
    }

    /* General styling of a bar dataset. */
    public void customizeBarDataSet(BarDataSet dataSet, String[] stackLabels, int color, List<Integer> colors) {
        if (stackLabels != null) dataSet.setStackLabels(stackLabels);
        if (color != -1) dataSet.setColor(color);
        if (colors != null) dataSet.setColors(colors);
        dataSet.setDrawValues(false);
    }

    /* Creates an entry from the home fragment. */
    public void createOnClick(View view) {
        Intent i = new Intent(getContext(), EntryActivity.class);
        FragmentManager manager = getFragmentManager();
        if (manager == null) return;
        Fragment homeFragment = manager.findFragmentByTag("home");
        if (homeFragment == null) return;
        homeFragment.startActivityForResult(i, HomeFragment.NEW_ENTRY_REQUEST_CODE);
        manager.beginTransaction().hide(this).show(homeFragment).commit();
        MainActivity.binding.navView.setSelectedItemId(R.id.navigation_home);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}