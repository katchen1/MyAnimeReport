package com.example.myanimereport.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaAllQuery;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.databinding.FragmentMatchBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.ParseApplication;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MatchFragment extends Fragment {

    private FragmentMatchBinding binding;
    private List<Anime> allAnime;
    private Anime currentAnime; // Anime that's currently shown

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allAnime = new ArrayList<>();

        // Present an anime for the user to accept or reject
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);

        // Get all available mediaIds
        ParseApplication.apolloClient.query(new MediaAllQuery(1)).enqueue(
            new ApolloCall.Callback<MediaAllQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaAllQuery.Data> response) {
                    for (MediaAllQuery.Medium m: response.getData().Page().media()) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        allAnime.add(anime);
                    }
                    ParseApplication.currentActivity.runOnUiThread(() -> {
                        generateMatch();
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /* Generates the default anime for now. */
    public void generateMatch() {
        Random rand = new Random();
        Anime anime = allAnime.get(rand.nextInt(allAnime.size()));
        currentAnime = anime;
        Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        binding.tvTitle.setText(anime.getTitleEnglish());
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        binding.tvDescription.setText(Html.fromHtml(anime.getDescription()));
        binding.cvAnime.setStrokeColor(anime.getColor());

        // Handle values that may be null - hide the views
        if (anime.getSeasonYear() == null) binding.llYear.setVisibility(View.GONE);
        else {
            binding.llYear.setVisibility(View.VISIBLE);
            binding.tvYear.setText(String.format(Locale.getDefault(), "%d", anime.getSeasonYear()));
        }
        if (anime.getEpisodes() == null) binding.llEpisodes.setVisibility(View.GONE);
        else {
            binding.llEpisodes.setVisibility(View.VISIBLE);
            binding.tvEpisodes.setText(String.format(Locale.getDefault(), "%d Episodes", anime.getEpisodes()));
        }
        if (anime.getAverageScore() == null) binding.llRating.setVisibility(View.GONE);
        else {
            binding.llRating.setVisibility(View.VISIBLE);
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        }

        // Scroll down a bit so that the poster doesn't take up the whole card view
        binding.nestedScrollView.post(() -> {
            int targetPosition = binding.appBar.getHeight() * 2;
            binding.nestedScrollView.smoothScrollTo(0, targetPosition);
        });

        // Fill in genres chip group
        ChipGroup cgGenres = binding.cgGenres;
        cgGenres.removeAllViews();
        for (String genre: anime.getGenres()) {
            Chip chip = new Chip(requireContext());
            chip.setText(genre);
            chip.setChipBackgroundColorResource(R.color.white);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray));
            chip.setEnabled(false);
            cgGenres.addView(chip);
        }
    }

    /* Adds the anime to the user's backlog and generates a new anime. */
    private void accept(View view) {
        BacklogItem item = new BacklogItem();
        item.setMediaId(currentAnime.getMediaId());
        item.setUser(ParseUser.getCurrentUser());
        item.saveInBackground(e -> {
            if (e == null) {
                // Pass back the entry so it can be inserted in the recycler view
                Toast.makeText(getContext(), "Added to backlog.", Toast.LENGTH_SHORT).show();
                item.setAnime(currentAnime);
                ParseApplication.backlogItems.add(item);
                generateMatch();
            } else {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Generates a new anime. */
    private void reject(View view) {
        Toast.makeText(requireContext(), "Rejected", Toast.LENGTH_SHORT).show();
        generateMatch();
    }
}