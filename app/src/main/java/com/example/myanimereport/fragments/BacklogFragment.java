package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myanimereport.R;
import com.example.myanimereport.adapters.BacklogItemsAdapter;
import com.example.myanimereport.databinding.FragmentBacklogBinding;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.Entry;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BacklogFragment extends Fragment {

    private final String TAG = "BacklogFragment";
    private FragmentBacklogBinding binding;
    private List<BacklogItem> items;
    private BacklogItemsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBacklogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up adapter and layout of recycler view
        items = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        adapter = new BacklogItemsAdapter(getContext(), items);
        binding.rvBacklogItems.setLayoutManager(layoutManager);
        binding.rvBacklogItems.setAdapter(adapter);

        // Divider between items
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.item_divider)));
        binding.rvBacklogItems.addItemDecoration(divider);

        queryBacklogItems(0);
    }

    /* Queries the items 10 at a time. Skips the first skip items. */
    public void queryBacklogItems(int skip) {
        ParseQuery<BacklogItem> query = ParseQuery.getQuery(BacklogItem.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(10); // Limit query to 10 items
        query.whereEqualTo(BacklogItem.KEY_USER, ParseUser.getCurrentUser()); // Limit entries to current user's
        query.addDescendingOrder("createdAt"); // Order posts by creation date
        query.findInBackground((itemsFound, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting backlog items.", e);
                return;
            }

            // Add entries to the recycler view and notify its adapter of new data
            for (BacklogItem item: itemsFound) {
                item.setAnime();
                items.add(item);
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}