package com.example.myanimereport.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaDetailsByIdQuery;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.EntryDetailsActivity;
import com.example.myanimereport.databinding.ItemEntryBinding;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import org.parceler.Parcels;
import java.util.List;
import java.util.Locale;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.ViewHolder> {

    private final String TAG = "EntriesAdapter";
    private final Fragment fragment;
    private final Context context;
    private List<Entry> entries;
    private final boolean editable;
    private boolean gridView;

    /* Constructor takes the fragment and the list of entries in the recycler view. */
    public EntriesAdapter(Fragment fragment, List<Entry> entries, boolean editable) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.entries = entries;
        this.editable = editable;
        this.gridView = true;
    }

    /* Creates a view holder for the entry. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemEntryBinding.inflate(LayoutInflater.from(fragment.getContext())));
    }

    /* Binds the entry at the passed in position to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entry entry = entries.get(position);
        holder.bind(entry);
    }

    /* Returns the number of entries in the list. */
    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setGridView(boolean gridView) {
        this.gridView = gridView;
    }

    public void updateEntries(List<Entry> updatedEntries){
        entries.clear();
        entries.addAll(updatedEntries);
        notifyDataSetChanged();
    }

    /* Defines the view holder for a entry. */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ItemEntryBinding binding;

        /* Constructor takes in a binding for the entry's view. */
        public ViewHolder(ItemEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
        }

        /* Binds the entry's data to the view's components. */
        public void bind(Entry entry) {
            // Set data unrelated to the anime
            if (entry == null) return;
            binding.tvYearWatched.setText(String.format(Locale.getDefault(), "%d", entry.getYearWatched()));
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));

            // Use pre-queried anime to prevent excessive network requests
            if (entry.getAnime() != null) {
                loadAnimeData(entry.getAnime());
                return;
            }

            // Query the anime's information by its mediaId
            Integer mediaId = entry.getMediaId();
            ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(mediaId)).enqueue(
                new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                    @Override
                    public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                        // View editing needs to happen in the main thread, not the background thread
                        ParseApplication.currentActivity.runOnUiThread(() -> {
                            Anime anime = new Anime(response);
                            entry.setAnime(anime);
                            loadAnimeData(anime);
                        });
                    }

                    @Override
                    public void onFailure(@NonNull ApolloException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            );
        }

        /* Binds the entry's anime-relevant data to the view's components. */
        public void loadAnimeData(Anime anime) {
            if (gridView) {
                binding.ivImageTop.setVisibility(View.VISIBLE);
                binding.ivImageStart.setVisibility(View.GONE);
                Glide.with(context).load(anime.getBannerImage()).placeholder(R.drawable.logo).into(binding.ivImageTop);
            } else {
                binding.ivImageTop.setVisibility(View.GONE);
                binding.ivImageStart.setVisibility(View.VISIBLE);
                Glide.with(context).load(anime.getCoverImage()).placeholder(R.drawable.logo).into(binding.ivImageStart);
            }
            binding.tvTitle.setText(anime.getTitleEnglish());
        }

        /* When the entry card is clicked, expand it to show its full information. */
        @Override
        public void onClick(View v) {
            // Check if anime data has been set
            Entry entry = entries.get(getAdapterPosition());
            if (entry.getAnime() == null) {
                entry.setAnime();
                return;
            }

            // Navigate to the entry details activity
            Intent intent = new Intent(context, EntryDetailsActivity.class);
            intent.putExtra("entry", entry); // Pass in the entry
            intent.putExtra("position", getAdapterPosition()); // Pass in its position in the list
            intent.putExtra("allPosition", ParseApplication.entries.indexOf(entry));
            intent.putExtra("anime", Parcels.wrap(entry.getAnime())); // Pass in the entry's anime
            intent.putExtra("editable", editable);

            // Animate the transition
            Activity activity = ParseApplication.currentActivity;
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(activity, binding.cvEntry, "card");
            fragment.startActivityForResult(intent, HomeFragment.VIEW_ENTRY_REQUEST_CODE, options.toBundle());
        }
    }
}