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
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myanimereport.R;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.BacklogItemsAdapter;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.databinding.FragmentBacklogBinding;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.utils.SwipeToDeleteCallback;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BacklogFragment extends Fragment {

    private final String TAG = "BacklogFragment";
    private FragmentBacklogBinding binding;
    private List<BacklogItem> allItems;
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
        binding.btnMenu.setOnClickListener(this::openNavDrawer);

        // Set up adapter and layout of recycler view
        allItems = ParseApplication.backlogItems;
        items = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        adapter = new BacklogItemsAdapter(this, items);
        binding.rvBacklogItems.setLayoutManager(layoutManager);
        binding.rvBacklogItems.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(binding.rvBacklogItems);

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
                List<BacklogItem> updatedItems = new ArrayList<>();
                for (BacklogItem item: allItems) {
                    if (newText.isEmpty()) updatedItems.add(item);
                    else if (item.getAnime() != null) {
                        String title = item.getAnime().getTitleEnglish().toLowerCase();
                        if (title.contains(newText.toLowerCase())) updatedItems.add(item);
                    }
                }
                adapter.updateItems(updatedItems);
                return false;
            }
        });

        // Hide keyboard when recycler view is scrolled
        binding.rvBacklogItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                hideSoftKeyboard();
            }
        });

        // Divider between items
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.item_divider)));
        binding.rvBacklogItems.addItemDecoration(divider);
        queryBacklogItems();
    }

    /* Resets the RV whenever the tab is clicked. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        items.clear();
        items.addAll(allItems);
        adapter.notifyDataSetChanged();
        checkItemsExist();
        binding.searchView.setQuery("", false);
        binding.searchView.clearFocus();
    }

    /* Hides the soft keyboard. */
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) binding.rvBacklogItems.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.rvBacklogItems.getWindowToken(), 0);
        binding.searchView.clearFocus();
    }

    /* Opens the navigation drawer. */
    private void openNavDrawer(View view) {
        ActivityMainBinding binding = MainActivity.binding;
        binding.btnLayout.setVisibility(View.GONE);
        binding.btnSort.setVisibility(View.GONE);
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortTitle.setVisibility(View.GONE);
        binding.btnSortWatchDate.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.btnDeleteAllEntries.setVisibility(View.GONE);
        binding.btnDeleteBacklog.setVisibility(View.VISIBLE);
        binding.btnFilter.setVisibility(View.GONE);
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    /* Returns the adapter. */
    public BacklogItemsAdapter getAdapter() {
        return adapter;
    }

    /* Queries all backlog items. */
    public void queryBacklogItems() {
        ParseQuery<BacklogItem> query = ParseQuery.getQuery(BacklogItem.class); // Specify type of data
        query.whereEqualTo(BacklogItem.KEY_USER, ParseUser.getCurrentUser()); // Limit items to current user's
        query.addDescendingOrder("createdAt"); // Order by creation date
        query.findInBackground((itemsFound, e) -> { // Start async query for backlog items
            // Check for errors
            if (e != null) {
                Log.e(TAG, "Error when getting backlog items.", e);
                return;
            }

            // Add items to the recycler view and notify its adapter of new data
            BacklogItem.setAnimes(itemsFound);
            allItems.addAll(itemsFound);
            items.addAll(itemsFound);
            adapter.notifyDataSetChanged();
            checkItemsExist();
        });
    }

    /* Shows a message if user has no backlog items. */
    public void checkItemsExist() {
        if (allItems.isEmpty()) {
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
        MainActivity.manager.beginTransaction().hide(this).show(MainActivity.matchFragment).commit();
        MainActivity.binding.navView.setSelectedItemId(R.id.navigation_match);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == HomeFragment.NEW_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // A new entry was created, added it to the home list
            Entry entry = data.getParcelableExtra("entry");
            int position = data.getIntExtra("position", -1);
            int allPosition = data.getIntExtra("allPosition", -1);
            MainActivity.homeFragment.insertEntryAtFront(entry);

            // Remove the anime from the backlog
            items.get(position).deleteInBackground();
            items.remove(position);
            allItems.remove(allPosition);
            adapter.notifyItemRemoved(position);
            checkItemsExist();

            // Navigate to the home tab
            MainActivity.manager.beginTransaction().hide(this).show(MainActivity.homeFragment).commit();
            MainActivity.binding.navView.setSelectedItemId(R.id.navigation_home);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}