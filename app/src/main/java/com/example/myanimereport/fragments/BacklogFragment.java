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
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myanimereport.R;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.BacklogItemsAdapter;
import com.example.myanimereport.databinding.FragmentBacklogBinding;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.utils.EndlessRecyclerViewScrollListener;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.List;
import java.util.Objects;

public class BacklogFragment extends Fragment {

    public static final int VIEW_BACKLOG_ITEM_REQUEST_CODE = 5;

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

        // Button listeners
        binding.btnMenu.setOnClickListener(v -> MainActivity.binding.drawerLayout.openDrawer(GravityCompat.START));

        // Set up adapter and layout of recycler view
        items = ParseApplication.backlogItems;
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        adapter = new BacklogItemsAdapter(this, items);
        binding.rvBacklogItems.setLayoutManager(layoutManager);
        binding.rvBacklogItems.setAdapter(adapter);

        // Divider between items
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.item_divider)));
        binding.rvBacklogItems.addItemDecoration(divider);
        queryBacklogItems(0);

        // Endless scrolling
        binding.rvBacklogItems.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                queryBacklogItems(items.size());
            }
        });
    }

    /* When the backlog tab is clicked, refresh the recycler view. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        adapter.notifyDataSetChanged(); // Items may have been added from the match tab
        checkItemsExist();
    }

    /* Queries the items 10 at a time. Skips the first skip items. */
    public void queryBacklogItems(int skip) {
        ParseQuery<BacklogItem> query = ParseQuery.getQuery(BacklogItem.class); // Specify type of data
        query.setSkip(skip); // Skip the first skip items
        query.setLimit(10); // Limit query to 10 items
        query.whereEqualTo(BacklogItem.KEY_USER, ParseUser.getCurrentUser()); // Limit items to current user's
        query.addAscendingOrder("createdAt"); // Order by creation date
        query.findInBackground((itemsFound, e) -> { // Start async query for backlog items
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting backlog items.", e);
                return;
            }

            // Add items to the recycler view and notify its adapter of new data
            BacklogItem.setAnimes(itemsFound);
            items.addAll(itemsFound);
            adapter.notifyDataSetChanged();
            checkItemsExist();
        });
    }

    /* Shows a message if user has no backlog items. */
    private void checkItemsExist() {
        if (items.isEmpty()) {
            binding.rvBacklogItems.setVisibility(View.INVISIBLE);
            binding.rlMessage.setVisibility(View.VISIBLE);
            binding.tvGoMatch.setOnClickListener(this::goMatch);
        } else {
            binding.rvBacklogItems.setVisibility(View.VISIBLE);
            binding.rlMessage.setVisibility(View.INVISIBLE);
        }
    }

    /* Navigates to the match tab. */
    public void goMatch(View view) {
        FragmentManager manager = getFragmentManager();
        if (manager == null) return;
        Fragment matchFragment = manager.findFragmentByTag("match");
        if (matchFragment == null) return;
        manager.beginTransaction().hide(this).show(matchFragment).commit();
        MainActivity.binding.navView.setSelectedItemId(R.id.navigation_match);
    }

    /* After deleting an item from details activity, update the recycler view. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VIEW_BACKLOG_ITEM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int position = data.getIntExtra("position", -1);
            items.remove(position);
            adapter.notifyItemRemoved(position);
            checkItemsExist();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}