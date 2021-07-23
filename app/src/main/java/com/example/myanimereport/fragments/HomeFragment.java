package com.example.myanimereport.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.databinding.FragmentHomeBinding;
import com.example.myanimereport.databinding.GenreFilterBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    public static final int NEW_ENTRY_REQUEST_CODE = 1;
    public static final int VIEW_ENTRY_REQUEST_CODE = 2;

    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private List<Entry> entries;
    private EntriesAdapter adapter;
    private GridLayoutManager layoutManager;
    private List<String> selectedGenres;

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
        entries.addAll(ParseApplication.entries);
        selectedGenres = new ArrayList<>();
        layoutManager = new GridLayoutManager(getContext(), 2);
        adapter = new EntriesAdapter(this, entries, true);
        binding.rvEntries.setLayoutManager(layoutManager);
        binding.rvEntries.setAdapter(adapter);

        // Remove the default on-change animations of the recycler view
        SimpleItemAnimator animator = (SimpleItemAnimator) binding.rvEntries.getItemAnimator();
        if (animator != null) animator.setSupportsChangeAnimations(false);

        // Button listeners
        binding.btnCreate.setOnClickListener(this::createOnClick);
        binding.tvCreate.setOnClickListener(this::createOnClick);
        binding.btnMenu.setOnClickListener(this::openNavDrawer);

        // Add entries to the recycler view
        queryEntries(0);
    }

    /* Opens the navigation drawer. */
    private void openNavDrawer(View view) {
        ActivityMainBinding binding = MainActivity.binding;
        binding.btnLayout.setVisibility(View.VISIBLE);
        binding.btnSort.setVisibility(View.VISIBLE);
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortTitle.setVisibility(View.GONE);
        binding.btnSortWatchDate.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.btnDeleteAllEntries.setVisibility(View.VISIBLE);
        binding.btnDeleteBacklog.setVisibility(View.GONE);
        binding.btnFilter.setVisibility(View.VISIBLE);
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    /* Queries the entries 10 at a time. Skips the first skip items. */
    public void queryEntries(int skip) {
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(50); // Limit query to 50 items
        query.whereEqualTo(Entry.KEY_USER, ParseUser.getCurrentUser()); // Limit entries to current user's
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground((entriesFound, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting entries.", e);
                return;
            }

            // Add entries to the recycler view and notify its adapter of new data
            Entry.setAnimes(entriesFound);
            ParseApplication.entries.addAll(entriesFound);
            entries.addAll(entriesFound);
            adapter.notifyDataSetChanged();
            checkEntriesExist();
        });
    }

    /* Toggles between list and grid layouts. */
    public void switchLayout() {
        if (layoutManager.getSpanCount() == 1) {
            layoutManager.setSpanCount(2);
            adapter.setGridView(true);
        } else {
            layoutManager.setSpanCount(1);
            adapter.setGridView(false);
        }
        adapter.notifyItemRangeChanged(0, entries.size());
    }

    /* Filters by genre. */
    public void filterGenres () {
        GenreFilterBinding dialogBinding = GenreFilterBinding.inflate(getLayoutInflater());

        // Get available genres
        List<String> genres = new ArrayList<>();
        for (Entry entry: ParseApplication.entries) {
            if (entry.getAnime() != null) {
                for (String genre: entry.getAnime().getGenres()) {
                    if (!genres.contains(genre)) genres.add(genre);
                }
            }
        }
        genres.sort(String::compareTo);
        genres.add(0, "All");

        // Add a checkbox for each genre
        List<CheckBox> cbs = new ArrayList<>();
        for (String genre: genres) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(genre);
            cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            if ((selectedGenres.isEmpty() && !entries.isEmpty()) || selectedGenres.contains(genre)) cb.setChecked(true);
            dialogBinding.llGenres.addView(cb);
            cbs.add(cb);
        }

        // Handle "All" checkbox
        cbs.get(0).setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox cb: cbs) cb.setChecked(isChecked);
        });

        // Show the filter
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Genre Filter")
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialog, which) -> {
                // Show only entries with selected genres
                selectedGenres.clear();
                for (CheckBox cb: cbs) if (cb.isChecked()) selectedGenres.add(cb.getText().toString());
                entries.clear();
                entries.addAll(ParseApplication.entries);
                entries.removeIf(entry -> {
                   if (entry.getAnime() == null) return true;
                   return Collections.disjoint(entry.getAnime().getGenres(), selectedGenres);
                });
                adapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Genres updated.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .show();
    }

    /* Creates an entry and adds it to the beginning of the list. */
    private void createOnClick(View view) {
        Intent i = new Intent(getContext(), EntryActivity.class);
        startActivityForResult(i, NEW_ENTRY_REQUEST_CODE);
    }

    /* Shows a message if user has no entries. */
    public void checkEntriesExist() {
        if (entries.isEmpty()) {
            binding.rvEntries.setVisibility(View.INVISIBLE);
            binding.rlMessage.setVisibility(View.VISIBLE);
        } else {
            binding.rvEntries.setVisibility(View.VISIBLE);
            binding.rlMessage.setVisibility(View.INVISIBLE);
        }
    }

    public EntriesAdapter getAdapter() {
        return adapter;
    }

    /* Inserts an entry at the very front of the list. */
    public void insertEntryAtFront(Entry entry) {
        ParseApplication.entries.add(0, entry);
        entries.clear();
        entries.addAll(ParseApplication.entries);
        selectedGenres.clear();
        adapter.notifyItemInserted(0);
        binding.rvEntries.smoothScrollToPosition(0);
    }

    /* After returning from a entry activity, update the entry at its position. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // A new entry was created, add it to the front of the list
            Entry entry = data.getParcelableExtra("entry");
            insertEntryAtFront(entry);
        }

        if (requestCode == VIEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Returning from an entry details activity
            int position = data.getIntExtra("position", -1);
            if (data.hasExtra("entry")) {
                // Entry updated
                Entry entry = data.getParcelableExtra("entry");
                entries.set(position, entry);
                adapter.notifyItemChanged(position);
            } else {
                // Entry deleted
                entries.remove(position);
                adapter.notifyItemRemoved(position);
            }
            checkEntriesExist();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}