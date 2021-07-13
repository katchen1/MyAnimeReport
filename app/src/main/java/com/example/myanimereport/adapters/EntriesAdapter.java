package com.example.myanimereport.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaDetailsByIdQuery;
import com.example.myanimereport.activities.EntryDetailsActivity;
import com.example.myanimereport.databinding.ItemEntryBinding;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import java.util.List;
import java.util.Locale;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.ViewHolder> {

    private final String TAG = "EntriesAdapter";
    private final Fragment fragment;
    private final Context context;
    private final List<Entry> entries;

    /* Constructor takes the fragment and the list of entries in the recycler view. */
    public EntriesAdapter(Fragment fragment, List<Entry> entries) {
        this.fragment = fragment;
        this.context = fragment.getContext();
        this.entries = entries;
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
            // Query the anime's information by its mediaId
            Integer mediaId = entry.getMediaId();
            ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(mediaId)).enqueue(
                new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                    @Override
                    public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                        // View editing needs to happen in the main thread, not the background thread
                        ParseApplication.currentActivity.runOnUiThread(() -> {
                            Anime anime = new Anime(response);
                            Glide.with(context).load(anime.getBannerImage()).into(binding.ivImage);
                            binding.tvTitle.setText(anime.getTitleEnglish());
                            binding.tvYearWatched.setText(String.format(Locale.getDefault(), "%d", entry.getYearWatched()));
                            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
                        });
                    }

                    @Override
                    public void onFailure(@NonNull ApolloException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            );
        }

        /* When the entry card is clicked, expand it to show its full information. */
        @Override
        public void onClick(View v) {
            System.out.println("clicked");
            Intent intent = new Intent(context, EntryDetailsActivity.class);
            intent.putExtra("entry", entries.get(getAdapterPosition())); // Pass in the entry
            intent.putExtra("position", getAdapterPosition()); // Pass in its position in the list
            fragment.startActivityForResult(intent, HomeFragment.VIEW_ENTRY_REQUEST_CODE);
        }
    }
}