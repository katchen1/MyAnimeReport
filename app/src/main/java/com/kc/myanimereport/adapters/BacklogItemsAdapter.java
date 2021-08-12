package com.kc.myanimereport.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kc.myanimereport.activities.AnimeDetailsActivity;
import com.kc.myanimereport.activities.EntryActivity;
import com.kc.myanimereport.activities.MainActivity;
import com.kc.myanimereport.databinding.ItemBacklogBinding;
import com.kc.myanimereport.fragments.BacklogFragment;
import com.kc.myanimereport.fragments.HomeFragment;
import com.kc.myanimereport.models.Anime;
import com.kc.myanimereport.models.BacklogItem;
import com.kc.myanimereport.models.ParseApplication;
import com.kc.myanimereport.R;
import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseUser;
import org.parceler.Parcels;
import java.util.List;
import java.util.Locale;

public class BacklogItemsAdapter extends RecyclerView.Adapter<BacklogItemsAdapter.ViewHolder> {

    private final BacklogFragment fragment;
    private final Context context;
    private final List<BacklogItem> items;

    /* Constructor takes the context and the list of backlog items in the recycler view. */
    public BacklogItemsAdapter(BacklogFragment fragment, List<BacklogItem> items) {
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

    /* Returns the context of the adapter. */
    public Context getContext() {
        return context;
    }

    /* When user swipes to delete, remove the item and show a message. */
    public void deleteItem(int position) {
        BacklogItem item = items.get(position);

        // Remember deleted item to allow undo
        BacklogItem deletedItem = new BacklogItem();
        deletedItem.setUser(ParseUser.getCurrentUser());
        deletedItem.setMediaId(item.getMediaId());
        deletedItem.setCreationDate(item.getCreatedAt());

        // Delete the item
        item.deleteInBackground();
        items.remove(position);
        notifyItemRemoved(position);

        // Remove it from the real list as well
        int allPosition = ParseApplication.backlogItems.indexOf(item);
        ParseApplication.backlogItems.remove(allPosition);
        fragment.checkItemsExist();

        // Display undo snackbar
        View view = MainActivity.backlogFragment.getView();
        if (view != null) {
            Snackbar snack = Snackbar.make(view, "Item deleted.", Snackbar.LENGTH_SHORT);
            snack.setActionTextColor(context.getColor(R.color.theme));
            snack.setAction("Undo", v -> undoDelete(deletedItem, position, allPosition));
            snack.show();
        }
    }

    /* Undoes a delete by adding the item back to the list and recycler view. */
    private void undoDelete(BacklogItem item, int position, int allPosition) {
        item.saveInBackground();
        items.add(position, item);
        notifyItemInserted(position);
        ParseApplication.backlogItems.add(allPosition, item);
        fragment.checkItemsExist();
    }

    /* When user checks a backlog item off the list, add an entry for it. */
    public void addItemAsEntry(int position) {
        BacklogItem item = items.get(position);
        int allPosition = ParseApplication.backlogItems.indexOf(item);
        Intent intent = new Intent(context, EntryActivity.class);
        intent.putExtra("anime", Parcels.wrap(item.getAnime()));
        intent.putExtra("position", position);
        intent.putExtra("allPosition", allPosition);
        fragment.startActivityForResult(intent, HomeFragment.NEW_ENTRY_REQUEST_CODE);
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
            if (anime == null) return;
            if (anime.getTitleEnglish() != null) binding.tvTitle.setText(anime.getTitleEnglish());

            // If no rating info, hide the rating component
            if (anime.getAverageScore() != null && anime.getAverageScore() != -1) {
                binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", anime.getAverageScore()));
            } else {
                binding.tvRating.setVisibility(View.INVISIBLE);
                binding.tvRatingOutOf.setVisibility(View.INVISIBLE);
                binding.ivRatingIcon.setVisibility(View.INVISIBLE);
            }
        }

        /* When the backlog item is clicked, expand it to show the anime details. */
        @Override
        public void onClick(View v) {
            // Check if anime data has been set
            BacklogItem item = items.get(getAdapterPosition());
            if (item.getAnime() == null) return;

            // Navigate to the anime details activity
            Intent intent = new Intent(context, AnimeDetailsActivity.class);
            intent.putExtra("anime", Parcels.wrap(item.getAnime())); // Pass in the entry's anime
            fragment.startActivity(intent);
        }
    }
}