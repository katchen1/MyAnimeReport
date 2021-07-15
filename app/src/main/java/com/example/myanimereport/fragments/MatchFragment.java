package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.FragmentMatchBinding;
import com.example.myanimereport.models.Anime;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

public class MatchFragment extends Fragment {

    private FragmentMatchBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Present an anime for the user to accept or reject
        generateMatch();
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /* Generates the default anime for now. */
    public void generateMatch() {
        Anime anime = new Anime();
        Glide.with(this).load(anime.getBannerImage()).into(binding.ivImage);
        binding.tvTitle.setText(anime.getTitleEnglish());
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        binding.tvDescription.setText(anime.getDescription());

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