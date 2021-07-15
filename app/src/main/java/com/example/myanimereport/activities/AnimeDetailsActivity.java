package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ActivityAnimeDetailsBinding;
import com.example.myanimereport.databinding.ActivityEntryDetailsBinding;
import com.example.myanimereport.models.Anime;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.parceler.Parcels;

import java.util.Locale;

public class AnimeDetailsActivity extends AppCompatActivity {

    ActivityAnimeDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnimeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Fill in anime's info
        Anime anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));
        Glide.with(this).load(anime.getBannerImage()).into(binding.ivBanner);
        Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        binding.tvTitle.setText(anime.getTitleEnglish());
        binding.tvYear.setText(String.format(Locale.getDefault(), "%d", anime.getSeasonYear()));
        binding.tvEpisodes.setText(String.format(Locale.getDefault(), "%d Episodes", anime.getEpisodes()));
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        binding.tvDescription.setText(anime.getDescription());
        binding.cvAnime.setStrokeColor(anime.getColor());

        binding.nestedScrollView.post(() -> {
            int targetPosition = binding.ivBanner.getHeight() - binding.appBar.getHeight();
            binding.nestedScrollView.smoothScrollTo(0, targetPosition);
        });

        //binding.nestedScrollView.scrollTo(0, binding.tvDescription.getBottom());

        // Fill in genres chip group
        ChipGroup cgGenres = binding.cgGenres;
        cgGenres.removeAllViews();
        for (String genre: anime.getGenres()) {
            Chip chip = new Chip(this);
            chip.setText(genre);
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
            chip.setEnabled(false);
            cgGenres.addView(chip);
        }
    }
}