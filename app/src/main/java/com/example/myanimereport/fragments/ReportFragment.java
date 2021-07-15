package com.example.myanimereport.fragments;

import android.graphics.Color;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {

    private final String TAG = "ReportFragment";
    private FragmentReportBinding binding;
    List<Entry> entries;

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
        queryEntries(0);
    }

    /* Queries the entries 10 at a time. Skips the first skip items. */
    public void queryEntries(int skip) {
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(10); // Limit query to 10 items
        query.whereEqualTo(Entry.KEY_USER, ParseUser.getCurrentUser()); // Limit entries to current user's
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground(this::queryEntriesCallback);
    }

    private void queryEntriesCallback(List<Entry> entriesFound, ParseException e) {
        // Check for errors
        if (e != null) {
            Log.e(TAG, "Error when getting entries.", e);
            return;
        }
        entries.addAll(entriesFound);

        /* Charts I want to include:
         * 1. Overview [Score Cards]
         *   1A. Total watched - count
         *   1B. Average rating - averageRating
         *   1C. Watched this year (compared to last year) - yearToList.get(this year).size()
         *   1D. Avg rating this year (compared to last year) - for loop over yearToList.get(thisYear);
         * 2. Activity [Line Chart] - year vs. entry count
         * 3. Genres breakdown by year [Stacked Bar] - year vs. num anime in each genre
         * 4. Demographics [Pie]
         * 5. Genres [Pie]
         * 6. Genre Preference [Bar] - genre vs. average rating
         */

        // Collect data
        int count = 0;
        Double sumRating = 0.0;
        Map<Integer, List<Entry>> yearToList = new HashMap<>();
        Map<String, Integer> genreToCount = new HashMap<>();
        for (Entry entry: entries) {
            //entry.setAnime();
            count++;
            sumRating += entry.getRating();
            if (!yearToList.containsKey(entry.getYearWatched())) {
                yearToList.put(entry.getYearWatched(), new ArrayList<>());
            }
            yearToList.get(entry.getYearWatched()).add(entry);
        }
        double averageRating = sumRating / count;


        // 2. Activity (Line Chart) - year vs. entry count
        List<com.github.mikephil.charting.data.Entry> chartEntries = new ArrayList<>();
        for (Integer year: yearToList.keySet()) {
            int entryCount = yearToList.get(year).size();
            System.out.println("test: " + year + " " + entryCount);
            chartEntries.add(new com.github.mikephil.charting.data.Entry(year, entryCount));
        }

        chartEntries.sort((e1, e2) -> (Double.compare(e1.getX(), e2.getX())));
        System.out.println("total count: " + count);
        System.out.println("avg rating: " + averageRating);

        LineDataSet dataSet = new LineDataSet(chartEntries, "Num Watched"); // add entries to dataset
        dataSet.setColor(ContextCompat.getColor(getContext(), R.color.white));
        dataSet.setValueTextColor(ContextCompat.getColor(getContext(), R.color.white));
        dataSet.setValueTextSize(16);


        LineData lineData = new LineData(dataSet);
        binding.chart.setData(lineData);
        binding.chart.getLegend().setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        binding.chart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        binding.chart.getXAxis().setTextSize(16);
        binding.chart.getXAxis().setLabelCount(10, true);
        binding.chart.getXAxis().setAxisMaximum(2021);
        binding.chart.getXAxis().setAxisMinimum(2012);
        binding.chart.invalidate(); // refresh
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}