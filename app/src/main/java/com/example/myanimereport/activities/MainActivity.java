package com.example.myanimereport.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.myanimereport.R;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.databinding.EditNameBinding;
import com.example.myanimereport.databinding.EditPasswordBinding;
import com.example.myanimereport.fragments.BacklogFragment;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.fragments.MatchFragment;
import com.example.myanimereport.fragments.ReportFragment;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.utils.CustomAlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    public static ActivityMainBinding binding;
    public static HomeFragment homeFragment;
    public static ReportFragment reportFragment;
    public static MatchFragment matchFragment;
    public static BacklogFragment backlogFragment;
    public static  FragmentManager manager;

    private String sortedBy = "Entry Creation Date Descending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up fragments
        manager = getSupportFragmentManager();
        homeFragment = new HomeFragment();
        reportFragment = new ReportFragment();
        matchFragment = new MatchFragment();
        backlogFragment = new BacklogFragment();

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

        // Set up drawer view
        ParseUser user = ParseUser.getCurrentUser();
        String name = user.has("name")? user.getString("name"): user.getUsername();
        binding.tvName.setText(name);
        binding.tvUsername.setText(user.getUsername());

        // Change status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this ,R.color.dark_gray));
    }

    /* Logs out and returns to the login page. */
    public void logOutOnClick(View view) {
        ParseUser.logOut();
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);
        ParseApplication.entries.clear();
        ParseApplication.backlogItems.clear();
        ParseApplication.seenMediaIds.clear();
        ParseApplication.genres.clear();
        finish();
    }

    /* Shows or hides buttons related to the user's account. */
    public void accountOnClick(View view) {
        // Make account options visible unless already visible
        int targetVisibility = View.VISIBLE;
        int targetBtnResource = R.drawable.ic_baseline_keyboard_arrow_up_24;
        if (binding.btnEditName.getVisibility() == View.VISIBLE) {
            targetVisibility = View.GONE;
            targetBtnResource = R.drawable.ic_baseline_keyboard_arrow_down_24;
        }

        binding.btnEditName.setVisibility(targetVisibility);
        binding.btnEditPassword.setVisibility(targetVisibility);
        binding.btnLogOut.setVisibility(targetVisibility);
        binding.btnExpand.setImageResource(targetBtnResource);
    }

    /* Toggles between grid and list layouts for entries. */
    public void btnLayoutOnClick(View view) {
        // Change layout in the home fragment
        homeFragment.switchLayout();

        // Update UI
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

    /* Shows or hides the sort options. */
    public void btnSortOnClick(View view) {
        int targetVisibility = binding.btnSortRating.getVisibility() == View.VISIBLE? View.GONE: View.VISIBLE;
        setSortVisibility(targetVisibility);
    }

    /* Sets the visibility of the sort options. */
    public void setSortVisibility(int visibility) {
        binding.btnSortCreationDate.setVisibility(visibility);
        binding.btnSortTitle.setVisibility(visibility);
        binding.btnSortRating.setVisibility(visibility);
        binding.btnSortWatchDate.setVisibility(visibility);
    }

    /* Restore default order for all sort options in the UI. */
    public void restoreDefaultOrder() {
        binding.ivSortCreationDate.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        binding.ivSortRating.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
        binding.ivSortTitle.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
        binding.ivSortWatchDate.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
    }

    /* Sorts the entries. sortBy is one of "Entry Creation Date", "Rating", Title", or "Watch Date",
     * and iv is the corresponding image button in the UI. */
    public void sort(String sortBy, ImageButton ib) {
        // Determine the default/non-default orders and corresponding icons
        String defaultOrder = sortBy.equals("Title")? "Ascending": "Descending";
        boolean inDefaultOrder = sortedBy.equals(sortBy + " " + defaultOrder);
        String nonDefaultOrder = defaultOrder.equals("Descending")? "Ascending": "Descending";
        int defaultIcon = defaultOrder.equals("Descending")?
                R.drawable.ic_baseline_arrow_downward_24:
                R.drawable.ic_baseline_arrow_upward_24;
        int nonDefaultIcon = defaultOrder.equals("Descending")?
                R.drawable.ic_baseline_arrow_upward_24:
                R.drawable.ic_baseline_arrow_downward_24;
        restoreDefaultOrder();

        // If already in default order, sort in the non-default order. Otherwise sort in default order.
        int sign = inDefaultOrder? -1: 1;
        switch (sortBy) {
            case "Entry Creation Date":
                homeFragment.getEntries().sort((e1, e2) -> sign * e2.getCreatedAt().compareTo(e1.getCreatedAt()));
                break;
            case "Rating":
                homeFragment.getEntries().sort((e1, e2) -> sign * e2.getRating().compareTo(e1.getRating()));
                break;
            case "Title":
                homeFragment.getEntries().sort((e1, e2) -> sign * e1.getAnime().getTitleEnglish().compareTo(e2.getAnime().getTitleEnglish()));
                break;
            case "Watch Date":
                homeFragment.getEntries().sort((e1, e2) -> sign * e2.getDateWatched().compareTo(e1.getDateWatched()));
                break;
        }

        // Update UI
        sortedBy = sortBy + " " + (inDefaultOrder? nonDefaultOrder: defaultOrder);
        binding.ivSort.setImageResource(inDefaultOrder? nonDefaultIcon: defaultIcon);
        ib.setImageResource(inDefaultOrder? defaultIcon: nonDefaultIcon);
        binding.tvSort.setText(sortBy);

        // Notify adapter and close the drawer
        homeFragment.getAdapter().notifyDataSetChanged();
        setSortVisibility(View.GONE);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /* Sorts by creation date. */
    public void btnSortCreationDateOnClick(View view) {
        sort("Entry Creation Date", binding.ivSortCreationDate);
    }

    /* Sorts by anime title. */
    public void btnSortTitleOnClick(View view) {
        sort("Title", binding.ivSortTitle);
    }

    /* Sorts by rating. */
    public void btnSortRatingOnClick(View view) {
        sort("Rating", binding.ivSortRating);
    }

    /* Sorts by watch date. */
    public void btnSortWatchDateOnClick(View view) {
        sort("Watch Date", binding.ivSortWatchDate);
    }

    /* Sorts backlog items by date added. */
    public void btnSortDateAddedOnClick(View view) {
        backlogFragment.flipOrder();
        if (backlogFragment.getDescending()) binding.tvSortDateAdded.setText(R.string.newest);
        else binding.tvSortDateAdded.setText(R.string.oldest);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /* Allows the user to filter anime genres. */
    public void filterOnClick(View view) {
        homeFragment.filterGenres();
    }

    /* Allows the user to edit their name. */
    public void editNameOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        EditNameBinding dialogBinding = EditNameBinding.inflate(getLayoutInflater());
        AlertDialog alert = new MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialog, which) -> {
                // Update the user's name
                ParseUser.getCurrentUser().put("name", dialogBinding.etName.getText().toString());
                ParseUser.getCurrentUser().saveInBackground();
                Toast.makeText(MainActivity.this, "Name saved.", Toast.LENGTH_SHORT).show();
                binding.tvName.setText(dialogBinding.etName.getText().toString());
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();
        alert.show();
        CustomAlertDialog.style(alert, getApplicationContext());
    }

    /* Allows the user to edit their password. */
    public void editPasswordOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        EditPasswordBinding dialogBinding = EditPasswordBinding.inflate(getLayoutInflater());
        AlertDialog alert = new MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.getRoot())
            .setPositiveButton("Save", (dialog, which) -> {
                String oldPassword = dialogBinding.etOldPassword.getText().toString();
                String password = dialogBinding.etPassword.getText().toString();
                String confirmPassword = dialogBinding.etConfirmPassword.getText().toString();
                ParseUser user = ParseUser.getCurrentUser();

                // Check if old password is entered correctly
                ParseUser.logInInBackground(user.getUsername(), oldPassword, (u, e) -> {
                    if (u != null) {
                        // If old password is correct, update their password
                        if (!password.equals(confirmPassword)) {
                            Toast.makeText(MainActivity.this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        user.setPassword(password);
                        user.saveInBackground();
                        Toast.makeText(MainActivity.this, "Password updated.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Old password is incorrect.", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();
        alert.show();
        CustomAlertDialog.style(alert, getApplicationContext());
    }

    /* Deletes all entries of the current user. */
    public void deleteAllEntriesOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        AlertDialog alert = new MaterialAlertDialogBuilder(this)
            .setTitle("Delete All Entries")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete All", (dialog, which) -> {
                // Delete all entries in Parse
                for (Entry entry: ParseApplication.entries) entry.deleteInBackground();
                ParseApplication.entries.clear();
                Toast.makeText(MainActivity.this, "Entries deleted.", Toast.LENGTH_SHORT).show();

                // Update UI
                homeFragment.getAdapter().notifyDataSetChanged();
                homeFragment.checkEntriesExist();
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();
        alert.show();
        CustomAlertDialog.style(alert, getApplicationContext());
    }

    /* Deletes all backlog items of the current user. */
    public void deleteBacklogOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        AlertDialog alert = new MaterialAlertDialogBuilder(this)
            .setTitle("Clear To-Watch List")
            .setMessage("Are you sure?")
            .setPositiveButton("Clear", (dialog, which) -> {
                // Delete all backlog items
                for (BacklogItem item: ParseApplication.backlogItems) item.deleteInBackground();
                ParseApplication.backlogItems.clear();
                Toast.makeText(MainActivity.this, "Backlog deleted.", Toast.LENGTH_SHORT).show();

                // Update UI
                BacklogFragment backlogFragment = (BacklogFragment) manager.findFragmentByTag("backlog");
                if (backlogFragment != null) {
                    backlogFragment.getAdapter().notifyDataSetChanged();
                    backlogFragment.checkItemsExist();
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();
        alert.show();
        CustomAlertDialog.style(alert, getApplicationContext());
    }
}