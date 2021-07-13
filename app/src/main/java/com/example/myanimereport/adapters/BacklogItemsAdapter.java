package com.example.myanimereport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myanimereport.databinding.ItemBacklogBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import java.util.List;
import java.util.Locale;

public class BacklogItemsAdapter extends RecyclerView.Adapter<BacklogItemsAdapter.ViewHolder> {

    private final Context context;
    private final List<BacklogItem> items;

    /* Constructor takes the context and the list of backlog items in the recycler view. */
    public BacklogItemsAdapter(Context context, List<BacklogItem> items) {
        this.context = context;
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
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemBacklogBinding binding;

        /* Constructor takes in a binding for the item's view. */
        public ViewHolder(ItemBacklogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /* Binds the item's data to the view's components. */
        public void bind(BacklogItem item) {
            Anime anime = item.getAnime();
            binding.tvTitle.setText(anime.getTitleEnglish());
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
        }
    }
}