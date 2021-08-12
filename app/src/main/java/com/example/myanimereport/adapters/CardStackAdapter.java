package com.example.myanimereport.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ItemSwipeCardBinding;
import com.example.myanimereport.models.Anime;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;
import java.util.Locale;

public class CardStackAdapter extends RecyclerView.Adapter<CardStackAdapter.ViewHolder> {

    private final Context context;
    private final List<Anime> animes;

    /* Constructor takes the context and the list of animes in the recycler view. */
    public CardStackAdapter(Context context, List<Anime> animes) {
        this.context = context;
        this.animes = animes;
    }

    /* Creates a view holder for the anime card. */
    @NonNull
    @Override
    public CardStackAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardStackAdapter.ViewHolder(ItemSwipeCardBinding.inflate(LayoutInflater.from(context)));
    }

    /* Binds the anime at the passed in position to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull CardStackAdapter.ViewHolder holder, int position) {
        holder.bind(animes.get(position));
    }

    /* Returns the number of items in the list. */
    @Override
    public int getItemCount() {
        return animes.size();
    }

    /* Defines the view holder for an anime card. */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSwipeCardBinding binding;

        /* Constructor takes in a binding for the anime card's view. */
        public ViewHolder(ItemSwipeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Scroll down a bit so that the poster doesn't take up the whole card view
            binding.nestedScrollView.post(() -> {
                int targetPosition = binding.ivImage.getHeight() / 4;
                binding.nestedScrollView.smoothScrollTo(0, targetPosition);
            });
        }

        /* Binds the anime's data to the view's components (with null checking). */
        public void bind(Anime anime) {
            if (anime == null) return;
            if (anime.getCoverImage() != null) Glide.with(context).load(anime.getCoverImage()).into(binding.ivImage);
            if (anime.getTitleEnglish() != null) binding.tvTitle.setText(anime.getTitleEnglish());
            if (anime.getAverageScore() != null) binding.tvRating.setText(String.format(Locale.getDefault(),
                    "%.1f", anime.getAverageScore()));
            if (anime.getDescription() != null) binding.tvDescription.setText(Html.fromHtml(anime.getDescription()));
            if (anime.getColor() != null) binding.cvAnime.setStrokeColor(anime.getColor());

            // Handle values that may be null - hide the views
            if (anime.getSeasonYear() == null) binding.llYear.setVisibility(View.GONE);
            else  binding.tvYear.setText(String.format(Locale.getDefault(), "%d", anime.getSeasonYear()));
            if (anime.getEpisodes() == null) binding.llEpisodes.setVisibility(View.GONE);
            else binding.tvEpisodes.setText(String.format(Locale.getDefault(), "%d Episodes", anime.getEpisodes()));
            Double rating = anime.getAverageScore();
            if (rating == null || rating == -1.0) binding.llRating.setVisibility(View.GONE);
            else binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
            if (anime.getSiteUrl() == null) binding.llLink.setVisibility(View.GONE);
            else binding.tvLink.setText(anime.getSiteUrl());

            // Fill in genres chip group
            if (anime.getGenres() != null) {
                ChipGroup cgGenres = binding.cgGenres;
                cgGenres.removeAllViews();
                for (String genre : anime.getGenres()) {
                    Chip chip = new Chip(context);
                    chip.setText(genre);
                    chip.setChipBackgroundColorResource(R.color.white);
                    chip.setTextColor(ContextCompat.getColor(context, R.color.dark_gray));
                    chip.setEnabled(false);
                    cgGenres.addView(chip);
                }
            }
        }
    }
}