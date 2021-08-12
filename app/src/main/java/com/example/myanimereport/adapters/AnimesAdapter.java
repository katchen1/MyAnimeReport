package com.example.myanimereport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.databinding.ItemAnimeBinding;
import com.example.myanimereport.models.Anime;
import java.util.List;

public class AnimesAdapter extends RecyclerView.Adapter<AnimesAdapter.ViewHolder> {

    private final EditText etTitle;
    private final Context context;
    private final List<Anime> animes;
    private final EntryActivity activity;

    /* Constructor takes the context and the list of animes in the recycler view. */
    public AnimesAdapter(EntryActivity activity, EditText etTitle, List<Anime> animes) {
        this.activity = activity;
        this.etTitle = etTitle;
        this.context = etTitle.getContext();
        this.animes = animes;
    }

    /* Creates a view holder for the anime. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAnimeBinding.inflate(LayoutInflater.from(context)));
    }

    /* Binds the anime at the passed in position to the view holder. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(animes.get(position));
    }

    /* Returns the number of animes in the list. */
    @Override
    public int getItemCount() {
        return animes.size();
    }

    /* Defines the view holder for an anime. */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ItemAnimeBinding binding;

        /* Constructor takes in a binding for the anime's view. */
        public ViewHolder(ItemAnimeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(this);
        }

        /* Binds the anime's data to the view's components. */
        public void bind(Anime anime) {
            if (anime == null) return;
            if (anime.getTitleEnglish() != null) binding.tvTitle.setText(anime.getTitleEnglish());
            if (anime.getCoverImage() != null) Glide.with(context).load(anime.getCoverImage()).into(binding.ivImage);
        }

        /* When the anime is clicked, set text in the search view to be the title. */
        @Override
        public void onClick(View v) {
            Anime anime = animes.get(getAdapterPosition());
            if (anime != null && anime.getTitleEnglish() != null) etTitle.setText(anime.getTitleEnglish());
            etTitle.setSelection(etTitle.getText().length());
            activity.hideTitleSuggestion();
            activity.hideFocus(etTitle);
        }
    }
}