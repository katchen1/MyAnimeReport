package com.example.myanimereport.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.myanimereport.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.fragments.BacklogFragment;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.fragments.MatchFragment;
import com.example.myanimereport.fragments.ReportFragment;
import com.example.myanimereport.models.ParseApplication;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    public static ActivityMainBinding binding;
    private FragmentManager manager;
    private String sortedBy = "creationDateDescending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up fragments
        manager = getSupportFragmentManager();
        final Fragment homeFragment = new HomeFragment();
        final Fragment reportFragment = new ReportFragment();
        final Fragment matchFragment = new MatchFragment();
        final Fragment backlogFragment = new BacklogFragment();

        // Use a one-element array to store the active fragment to make it effectively final
        final Fragment[] activeFragment = { homeFragment };

        // Add fragments to the fragment manager
        manager.beginTransaction()
                .add(R.id.contentMain, backlogFragment, "backlog").hide(backlogFragment)
                .add(R.id.contentMain, matchFragment, "match").hide(matchFragment)
                .add(R.id.contentMain, reportFragment, "report").hide(reportFragment)
                .add(R.id.contentMain, homeFragment, "home").commit();

        // Handle tab changes
        binding.navView.setOnNavigationItemSelectedListener(item -> {
            // Get the target fragment
            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                default:
                    fragment = homeFragment;
                    break;
                case R.id.navigation_report:
                    fragment = reportFragment;
                    break;
                case R.id.navigation_match:
                    fragment = matchFragment;
                    break;
                case R.id.navigation_backlog:
                    fragment = backlogFragment;
                    break;
            }

            // Use hide()+show() instead of replace() to prevent the fragments from being destroyed
            manager.beginTransaction().hide(activeFragment[0]).show(fragment).commit();
            activeFragment[0] = fragment;
            return true;
        });

        setUpDrawerView();
    }

    public void setUpDrawerView() {
        binding.tvName.setText(ParseUser.getCurrentUser().getString("name"));
        binding.tvUsername.setText(ParseUser.getCurrentUser().getUsername());
    }

    public void expandOnClick(View view) {
        int currVisibility = binding.btnEditName.getVisibility();
        int targetVisibility = View.VISIBLE;
        int targetBtnResource = R.drawable.ic_baseline_keyboard_arrow_up_24;
        if (currVisibility == View.VISIBLE) {
            targetVisibility = View.GONE;
            targetBtnResource = R.drawable.ic_baseline_keyboard_arrow_down_24;
        }
        binding.btnEditName.setVisibility(targetVisibility);
        binding.btnEditPassword.setVisibility(targetVisibility);
        binding.btnLogOut.setVisibility(targetVisibility);
        binding.btnExpand.setImageResource(targetBtnResource);
    }

    /* Logs out and returns to the login page. */
    public void logOutOnClick(View view) {
        ParseUser.logOut();
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);
        ParseApplication.entries.clear();
        ParseApplication.backlogItems.clear();
        ParseApplication.seenMediaIds.clear();
        finish();
    }

    /* Toggles between grid and list layouts for entries. */
    public void btnLayoutOnClick(View view) {
        HomeFragment homeFragment = (HomeFragment) manager.findFragmentByTag("home");
        if (homeFragment != null) homeFragment.switchLayout();
        String targetText = "Grid Layout";
        int targetResource = R.drawable.ic_baseline_grid_on_24;
        if (binding.tvLayoutType.getText().equals("Grid Layout")) {
            targetText = "List Layout";
            targetResource = R.drawable.ic_baseline_list_alt_24;
        }
        binding.tvLayoutType.setText(targetText);
        binding.ivLayoutType.setImageResource(targetResource);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /* Lets the user choose how to sort the entries. */
    public void btnSortOnClick(View view) {
        int currVisibility = binding.btnSortCreationDate.getVisibility();
        int targetVisibility = currVisibility == View.VISIBLE? View.GONE: View.VISIBLE;
        binding.btnSortCreationDate.setVisibility(targetVisibility);
        binding.btnSortAnimeTitle.setVisibility(targetVisibility);
        binding.btnSortRating.setVisibility(targetVisibility);
    }

    /* Sorts by creation date. */
    public void btnSortCreationDateOnClick(View view) {
        boolean descending = sortedBy.equals("creationDateDescending");

        // If already descending, sort ascending
        if (descending) {
            ParseApplication.entries.sort((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt()));
            sortedBy = "creationDateAscending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
            binding.ivSortCreationDate.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        }

        // Otherwise sort descending
        else {
            ParseApplication.entries.sort((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()));
            sortedBy = "creationDateDescending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
            binding.ivSortCreationDate.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
        }
        binding.tvSort.setText(R.string.entry_creation_date);

        // Reset default order for other sort types
        binding.ivSortAnimeTitle.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
        binding.ivSortRating.setImageResource(R.drawable.ic_baseline_arrow_downward_24);

        // Notify adapter
        HomeFragment homeFragment = (HomeFragment) manager.findFragmentByTag("home");
        if (homeFragment != null) homeFragment.getAdapter().notifyDataSetChanged();
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortAnimeTitle.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /* Sorts by anime title. */
    public void btnSortAnimeTitleOnClick(View view) {
        boolean ascending = sortedBy.equals("titleAscending");

        // If already ascending, sort descending
        if (ascending) {
            ParseApplication.entries.sort((e1, e2) -> e2.getAnime().getTitleEnglish().compareTo(e1.getAnime().getTitleEnglish()));
            sortedBy = "titleDescending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
            binding.ivSortAnimeTitle.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
        }

        // Otherwise sort ascending
        else {
            ParseApplication.entries.sort((e1, e2) -> e1.getAnime().getTitleEnglish().compareTo(e2.getAnime().getTitleEnglish()));
            sortedBy = "titleAscending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
            binding.ivSortAnimeTitle.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        }
        binding.tvSort.setText(R.string.anime_title);

        // Reset default order for other sort types
        binding.ivSortCreationDate.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        binding.ivSortRating.setImageResource(R.drawable.ic_baseline_arrow_downward_24);

        HomeFragment homeFragment = (HomeFragment) manager.findFragmentByTag("home");
        if (homeFragment != null) homeFragment.getAdapter().notifyDataSetChanged();
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortAnimeTitle.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /* Sorts by creation date. */
    public void btnSortRatingOnClick(View view) {
        boolean descending = sortedBy.equals("ratingDescending");

        // If already descending, sort ascending
        if (descending) {
            ParseApplication.entries.sort((e1, e2) -> e1.getRating().compareTo(e2.getRating()));
            sortedBy = "ratingAscending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
            binding.ivSortRating.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        }

        // Otherwise sort descending
        else {
            ParseApplication.entries.sort((e1, e2) -> e2.getRating().compareTo(e1.getRating()));
            sortedBy = "ratingDescending";
            binding.ivSort.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
            binding.ivSortRating.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
        }
        binding.tvSort.setText(R.string.rating);

        // Reset default order for other sort types
        binding.ivSortCreationDate.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        binding.ivSortAnimeTitle.setImageResource(R.drawable.ic_baseline_arrow_upward_24);

        // Notify adapter
        HomeFragment homeFragment = (HomeFragment) manager.findFragmentByTag("home");
        if (homeFragment != null) homeFragment.getAdapter().notifyDataSetChanged();
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortAnimeTitle.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }
}