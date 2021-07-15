package com.example.myanimereport.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.LoginActivity;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.FragmentHomeBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.utils.EndlessRecyclerViewScrollListener;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import org.parceler.Parcels;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public static final int NEW_ENTRY_REQUEST_CODE = 1;
    public static final int VIEW_ENTRY_REQUEST_CODE = 2;

    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private List<Entry> entries;
    private EntriesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up adapter and layout of recycler view
        entries = new ArrayList<>();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        adapter = new EntriesAdapter(this, entries);
        binding.rvEntries.setLayoutManager(layoutManager);
        binding.rvEntries.setAdapter(adapter);

        // Remove the default on-change animations of the recycler view
        SimpleItemAnimator animator = (SimpleItemAnimator) binding.rvEntries.getItemAnimator();
        if (animator != null) animator.setSupportsChangeAnimations(false);

        // Button listeners
        binding.btnLogOut.setOnClickListener(this::logOutOnClick);
        binding.btnCreate.setOnClickListener(this::createOnClick);

        // Add entries to the recycler view
        queryEntries(0);

        // Endless scrolling
        binding.rvEntries.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                queryEntries(entries.size());
            }
        });
    }

    /* Queries the entries 20 at a time. Skips the first skip items. */
    public void queryEntries(int skip) {
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(10); // Limit query to 10 items
        query.whereEqualTo(Entry.KEY_USER, ParseUser.getCurrentUser()); // Limit entries to current user's
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground((entriesFound, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting entries.", e);
                return;
            }

            // Add entries to the recycler view and notify its adapter of new data
            entries.addAll(entriesFound);
            adapter.notifyDataSetChanged();
        });
    }

    /* Logs out and returns to the login page. */
    private void logOutOnClick(View view) {
        ParseUser.logOut();
        Intent i = new Intent(getContext(), LoginActivity.class);
        startActivity(i);
        if (getActivity() != null) getActivity().finish();
    }

    /* Creates an entry and adds it to the beginning of the list. */
    private void createOnClick(View view) {
        Intent i = new Intent(getContext(), EntryActivity.class);
        startActivityForResult(i, NEW_ENTRY_REQUEST_CODE);
    }

    /* After returning from a entry activity, update the entry at its position. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // A new entry was created, add it to the front of the list
            Entry entry = data.getParcelableExtra("entry");
            Anime anime = Parcels.unwrap(data.getParcelableExtra("anime"));
            entry.setAnime(anime);
            entries.add(0, entry);
            adapter.notifyItemInserted(0);
            binding.rvEntries.smoothScrollToPosition(0); // Scroll to the top to see the new entry
        }

        if (requestCode == VIEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Returning from an entry details activity
            int position = data.getIntExtra("position", -1);
            if (data.hasExtra("entry")) {
                // Entry updated
                Entry entry = data.getParcelableExtra("entry");
                entries.set(position, entry);
                adapter.notifyItemChanged(position);
                entry.setAnime();
            } else {
                // Entry deleted
                entries.remove(position);
                adapter.notifyItemRemoved(position);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}