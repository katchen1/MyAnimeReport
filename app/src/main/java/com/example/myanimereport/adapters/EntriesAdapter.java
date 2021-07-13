package com.example.myanimereport.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaDetailsByIdQuery;
import com.example.myanimereport.databinding.ItemEntryBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import java.util.List;
import java.util.Locale;

public class EntriesAdapter extends RecyclerView.Adapter<EntriesAdapter.ViewHolder> {

    private final String TAG = "EntriesAdapter";
    private final Context context;
    private final List<Entry> entries;

    /* Constructor takes the context and the list of entries in the recycler view. */
    public EntriesAdapter(Context context, List<Entry> entries) {
        this.context = context;
        this.entries = entries;
    }

    /* Creates a view holder for the entry. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemEntryBinding.inflate(LayoutInflater.from(context)));
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
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemEntryBinding binding;

        /* Constructor takes in a binding for the entry's view. */
        public ViewHolder(ItemEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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
                            MediaDetailsByIdQuery.Media media = response.getData().Media();
                            Glide.with(context).load(media.bannerImage()).into(binding.ivImage);
                            binding.tvTitle.setText(media.title().english());
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
    }
}