package com.example.myanimereport.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myanimereport.activities.AnimeDetailsActivity;
import com.example.myanimereport.activities.BacklogItemDetailsActivity;
import com.example.myanimereport.activities.EntryDetailsActivity;
import com.example.myanimereport.databinding.ItemBacklogBinding;
import com.example.myanimereport.fragments.BacklogFragment;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;

import org.parceler.Parcels;

import java.util.List;
import java.util.Locale;

public class BacklogItemsAdapter extends RecyclerView.Adapter<BacklogItemsAdapter.ViewHolder> {

    private final Fragment fragment;
    private final Context context;
    private final List<BacklogItem> items;

    /* Constructor takes the context and the list of backlog items in the recycler view. */
    public BacklogItemsAdapter(Fragment fragment, List<BacklogItem> items) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.items = items;
    }

    /* Creates a view holder for the item. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemBacklogBinding.inflate(LayoutInflater.from(context)));
    }

    /* Binds the item at the passed in position to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BacklogItem item = items.get(position);
        holder.bind(item);
    }

    /* Returns the number of items in the list. */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /* Defines the view holder for an item. */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ItemBacklogBinding binding;

        /* Constructor takes in a binding for the item's view. */
        public ViewHolder(ItemBacklogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
        }

        /* Binds the item's data to the view's components. */
        public void bind(BacklogItem item) {
            Anime anime = item.getAnime();
            binding.tvTitle.setText(anime.getTitleEnglish());
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        }

        /* When the backlog item is clicked, expand it to show the anime details. */
        @Override
        public void onClick(View v) {
            // Check if anime data has been set
            BacklogItem item = items.get(getAdapterPosition());
            if (item.getAnime() == null) {
                item.setAnime();
                return;
            }

            // Navigate to the anime details activity
            Intent intent = new Intent(context, BacklogItemDetailsActivity.class);
            intent.putExtra("item", item); // Pass in the entry
            intent.putExtra("position", getAdapterPosition()); // Pass in its position in the list
            intent.putExtra("anime", Parcels.wrap(item.getAnime())); // Pass in the entry's anime
            fragment.startActivityForResult(intent, BacklogFragment.VIEW_BACKLOG_ITEM_REQUEST_CODE);

        }
    }
}