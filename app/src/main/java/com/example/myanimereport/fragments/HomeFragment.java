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

import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.LoginActivity;
import com.example.myanimereport.adapters.EntriesAdapter;
import com.example.myanimereport.databinding.FragmentHomeBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.utils.EndlessRecyclerViewScrollListener;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private List<Entry> entries;
    private EntriesAdapter adapter;
    public static final int NEW_ENTRY_REQUEST_CODE = 1;

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
        adapter = new EntriesAdapter(getContext(), entries);
        binding.rvEntries.setLayoutManager(layoutManager);
        binding.rvEntries.setAdapter(adapter);

        // Button listeners
        binding.btnLogOut.setOnClickListener(this::logOutOnClick);
        binding.btnCreate.setOnClickListener(this::createOnClick);

        // Add entries to the recycler view
        queryEntries(0);

        // Add placeholder entries
//        for (int i = 0; i < 11; i++) entries.add(new Entry());
//        adapter.notifyDataSetChanged();

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
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // specify type of data
        query.setSkip(skip); // skip the first skip items
        query.setLimit(20); // limit query to 20 items
        query.whereEqualTo(Entry.KEY_USER, ParseUser.getCurrentUser()); // limit entries to current user's
        query.addDescendingOrder("createdAt"); // order posts by creation date
        query.findInBackground((entriesFound, e) -> { // start async query for entries
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

    /* After returning from a entry activity, update the entry at the position. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            entries.add(0, data.getParcelableExtra("entry"));
            adapter.notifyItemInserted(0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}