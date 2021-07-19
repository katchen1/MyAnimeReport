package com.example.myanimereport.fragments;

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
import com.example.myanimereport.databinding.FragmentMatchBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.ParseApplication;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MatchFragment extends Fragment {

    private FragmentMatchBinding binding;
    private List<Anime> allAnime;

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
        Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        binding.tvTitle.setText(anime.getTitleEnglish());
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        binding.tvDescription.setText(Html.fromHtml(anime.getDescription()));
        binding.cvAnime.setStrokeColor(anime.getColor());

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
        Toast.makeText(requireContext(), "Added to backlog", Toast.LENGTH_SHORT).show();
        generateMatch();
    }

    /* Generates a new anime. */
    private void reject(View view) {
        Toast.makeText(requireContext(), "Rejected", Toast.LENGTH_SHORT).show();
        generateMatch();
    }
}