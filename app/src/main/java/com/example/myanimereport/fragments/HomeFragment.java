package com.example.myanimereport.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    public static final int NEW_ENTRY_REQUEST_CODE = 1;
    public static final int VIEW_ENTRY_REQUEST_CODE = 2;

    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private List<Entry> entries; // Shown in the recycler view
    private List<Entry> allEntries; // All entries that the user has
    private EntriesAdapter adapter;
    private GridLayoutManager layoutManager;
    private Set<String> allGenres;
    private Set<String> selectedGenres;

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
        allEntries = ParseApplication.entries;
        entries = new ArrayList<>();
        allGenres = ParseApplication.genres;
        selectedGenres = new HashSet<>();
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

        // Search view
        EditText searchEditText = binding.searchView.findViewById(R.id.search_src_text);
        searchEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        searchEditText.setTextCursorDrawable(null);
        binding.searchView.setOnClickListener(v -> binding.searchView.setIconified(false));

        // Handle text change in search bar
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                binding.searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update the entries list to show only matching titles
                List<Entry> updatedEntries = new ArrayList<>();
                for (Entry entry: allEntries) {
                    if (newText.isEmpty()) updatedEntries.add(entry);
                    else if (entry.getAnime() != null) {
                        String title = entry.getAnime().getTitleEnglish().toLowerCase();
                        if (title.contains(newText.toLowerCase())) updatedEntries.add(entry);
                    }
                }
                adapter.updateEntries(updatedEntries);
                selectedGenres.clear();
                return false;
            }
        });

        // Hide keyboard when recycler view is scrolled
        binding.rvEntries.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                hideSoftKeyboard();
            }
        });

        // Pull to refresh
        binding.swipeContainer.setOnRefreshListener(() -> {
            entries.clear();
            allEntries.clear();
            queryEntries(false);
        });
        binding.swipeContainer.setColorSchemeResources(R.color.theme);

        // Add entries to the recycler view
        queryEntries(true);
    }

    /* Hides the soft keyboard. */
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) binding.rvEntries.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.rvEntries.getWindowToken(), 0);
        binding.searchView.clearFocus();
    }

    /* Opens the navigation drawer. */
    private void openNavDrawer(View view) {
        hideSoftKeyboard();
        ActivityMainBinding binding = MainActivity.binding;
        binding.btnLayout.setVisibility(View.VISIBLE);
        binding.btnSort.setVisibility(View.VISIBLE);
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortTitle.setVisibility(View.GONE);
        binding.btnSortWatchDate.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.btnDeleteAllEntries.setVisibility(View.VISIBLE);
        binding.btnSortDateAdded.setVisibility(View.GONE);
        binding.btnDeleteBacklog.setVisibility(View.GONE);
        binding.btnFilter.setVisibility(View.VISIBLE);
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    /* Queries the entries all at once. */
    public void queryEntries(boolean firstQuery) {
        if (firstQuery) showProgressBar();
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground((entriesFound, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting entries.", e);
                return;
            }

            // Keep track of seen anime ids of all users (for Slope One)
            List<Entry> userEntries = new ArrayList<>();
            for (Entry entry: entriesFound) {
                ParseApplication.entryMediaIdAllUsers.add(entry.getMediaId());
                if (entry.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
                    userEntries.add(entry);
                }
            }

            // Add entries to the recycler view and notify its adapter of new data
            Entry.setAnimes(userEntries);
            allEntries.addAll(userEntries);
            entries.addAll(userEntries);
            adapter.notifyDataSetChanged();
            checkEntriesExist();
            binding.swipeContainer.setRefreshing(false);
            if (firstQuery) MainActivity.homeFragment.hideProgressBar();
        });
    }

    /* Clears the search view. */
    public void clearSearch() {
        binding.searchView.setQuery("", false);
        binding.searchView.clearFocus();
    }

    /* Shows the progress bar. */
    public void showProgressBar() {
        binding.pbProgressAction.setVisibility(View.VISIBLE);
        binding.rvEntries.setVisibility(View.INVISIBLE);
        binding.rlMessage.setVisibility(View.INVISIBLE);
    }

    /* Hides the progress bar. */
    public void hideProgressBar() {
        binding.pbProgressAction.setVisibility(View.INVISIBLE);
        binding.rvEntries.setVisibility(View.VISIBLE);
        binding.rlMessage.setVisibility(View.VISIBLE);
        checkEntriesExist();
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
        List<String> genres = new ArrayList<>(allGenres);
        genres.sort(String::compareTo);
        genres.add(0, "All");

        // Add a checkbox for each genre to the linear layout
        List<CheckBox> cbs = new ArrayList<>();
        for (String genre: genres) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(genre);
            cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            if ((selectedGenres.isEmpty() && !entries.isEmpty()) || selectedGenres.contains(genre)) cb.setChecked(true);
            dialogBinding.linearLayoutGenres.addView(cb);
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
                // Update selected genres
                selectedGenres.clear();
                for (CheckBox cb: cbs) if (cb.isChecked()) selectedGenres.add(cb.getText().toString());

                // Show only entries containing at least one selected genre
                entries.clear();
                entries.addAll(ParseApplication.entries);
                entries.removeIf(entry -> {
                   if (entry.getAnime() == null) return true;
                   return Collections.disjoint(entry.getAnime().getGenres(), selectedGenres);
                });

                // Update recycler view and close drawer
                adapter.notifyDataSetChanged();
                MainActivity.binding.drawerLayout.closeDrawer(GravityCompat.START);
                clearSearch();
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
        if (ParseApplication.entries.isEmpty()) {
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

    public List<Entry> getEntries() {
        return entries;
    }

    /* Inserts an entry at the very front of the list and resets all filters. */
    public void insertEntryAtFront(Entry entry) {
        ParseApplication.entries.add(0, entry);
        entries.clear();
        entries.addAll(ParseApplication.entries);
        selectedGenres.clear();
        adapter.notifyItemInserted(0);
        binding.rvEntries.smoothScrollToPosition(0);
        checkEntriesExist();
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
            int allPosition = data.getIntExtra("allPosition", -1);
            if (data.hasExtra("entry")) {
                // Entry updated
                Entry entry = data.getParcelableExtra("entry");
                entries.set(position, entry);
                adapter.notifyItemChanged(position);
            } else {
                // Entry deleted
                entries.remove(position);
                allEntries.remove(allPosition);
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