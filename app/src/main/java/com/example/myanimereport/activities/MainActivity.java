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
}