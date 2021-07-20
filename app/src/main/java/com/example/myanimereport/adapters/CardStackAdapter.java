package com.example.myanimereport.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.BacklogItemDetailsActivity;
import com.example.myanimereport.databinding.ItemBacklogBinding;
import com.example.myanimereport.databinding.ItemSwipeCardBinding;
import com.example.myanimereport.fragments.BacklogFragment;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.parceler.Parcels;

import java.util.List;
import java.util.Locale;

public class CardStackAdapter extends RecyclerView.Adapter<CardStackAdapter.ViewHolder> {

    private final Context context;
    private final List<Anime> animes;

    /* Constructor takes the context and the list of backlog items in the recycler view. */
    public CardStackAdapter(Context context, List<Anime> animes) {
        this.context = context;
        this.animes = animes;
    }

    /* Creates a view holder for the item. */
    @NonNull
    @Override
    public CardStackAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CardStackAdapter.ViewHolder(ItemSwipeCardBinding.inflate(LayoutInflater.from(context)));
    }

    /* Binds the item at the passed in position to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull CardStackAdapter.ViewHolder holder, int position) {
        Anime anime = animes.get(position);
        holder.bind(anime);
    }

    /* Returns the number of items in the list. */
    @Override
    public int getItemCount() {
        return animes.size();
    }

    /* Defines the view holder for an item. */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSwipeCardBinding binding;

        /* Constructor takes in a binding for the item's view. */
        public ViewHolder(ItemSwipeCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Scroll down a bit so that the poster doesn't take up the whole card view
            binding.nestedScrollView.post(() -> {
                int targetPosition = binding.ivImage.getHeight() / 4;
                binding.nestedScrollView.smoothScrollTo(0, targetPosition);
            });
        }

        /* Binds the item's data to the view's components. */
        public void bind(Anime anime) {
            Glide.with(context).load(anime.getCoverImage()).into(binding.ivImage);
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

            // Fill in genres chip group
            ChipGroup cgGenres = binding.cgGenres;
            cgGenres.removeAllViews();
            for (String genre: anime.getGenres()) {
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
