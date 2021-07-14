package com.example.myanimereport.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaDetailsByIdQuery;
import com.example.myanimereport.databinding.ActivityEntryDetailsBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import java.text.DateFormatSymbols;
import java.util.Locale;

public class EntryDetailsActivity extends AppCompatActivity {

    private final String TAG = "EntryDetailsActivity";
    private ActivityEntryDetailsBinding binding;
    private Entry entry; // The entry whose information is being shown
    private Integer position; // The position of the entry in the adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Get the passed in data
        entry = getIntent().getParcelableExtra("entry");
        position = getIntent().getIntExtra("position", -1);

        // Show the entry's information
        String month = (new DateFormatSymbols().getMonths()[entry.getMonthWatched() - 1]);
        binding.tvMonthWatched.setText(month);
        binding.tvYearWatched.setText(String.format(Locale.getDefault(), "%d", entry.getYearWatched()));
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
        binding.tvNote.setText(entry.getNote());

        // Make a query for the anime's cover image and title
        Integer mediaId = entry.getMediaId();
        ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(mediaId)).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                    // View editing needs to happen in the main thread, not the background thread
                    ParseApplication.currentActivity.runOnUiThread(() -> {
                        Anime anime = new Anime(response);
                        Glide.with(EntryDetailsActivity.this).load(anime.getCoverImage()).into(binding.ivImage);
                        binding.tvTitle.setText(anime.getTitleEnglish());
                        binding.cvAnime.setStrokeColor(anime.getColor());
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        );
    }
}