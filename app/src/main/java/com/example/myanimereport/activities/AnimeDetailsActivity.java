package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.bumptech.glide.Glide;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ActivityAnimeDetailsBinding;
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

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Change status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this ,R.color.dark_gray));

        // Fill in anime's info
        Anime anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));
        if (anime == null) finish();
        if (anime.getBannerImage() != null) Glide.with(this).load(anime.getBannerImage()).into(binding.ivBanner);
        if (anime.getCoverImage() != null) Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        if (anime.getTitleEnglish() != null) binding.tvTitle.setText(anime.getTitleEnglish());
        if (anime.getDescription() != null) binding.tvDescription.setText(Html.fromHtml(anime.getDescription()));
        if (anime.getColor() != null) binding.cvAnime.setStrokeColor(anime.getColor());

        // Handle values that may be null - hide the views
        if (anime.getSeasonYear() == null) binding.llYear.setVisibility(View.GONE);
        else binding.tvYear.setText(String.format(Locale.getDefault(), "%d", anime.getSeasonYear()));
        if (anime.getEpisodes() == null) binding.llEpisodes.setVisibility(View.GONE);
        else binding.tvEpisodes.setText(String.format(Locale.getDefault(), "%d Episodes", anime.getEpisodes()));
        Double rating = anime.getAverageScore();
        if (rating == null || rating == -1.0) binding.llRating.setVisibility(View.GONE);
        else binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));

        // Scroll down a bit so that the poster doesn't take up the whole card view
        binding.nestedScrollView.post(() -> {
            int targetPosition = binding.ivBanner.getHeight() - binding.appBar.getHeight();
            binding.nestedScrollView.smoothScrollTo(0, targetPosition);
        });

        // Fill in genres chip group
        if (anime.getGenres() != null) {
            ChipGroup cgGenres = binding.cgGenres;
            cgGenres.removeAllViews();
            for (String genre : anime.getGenres()) {
                Chip chip = new Chip(this);
                chip.setText(genre);
                chip.setChipBackgroundColorResource(R.color.white);
                chip.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));
                chip.setEnabled(false);
                cgGenres.addView(chip);
            }
        }
    }
}