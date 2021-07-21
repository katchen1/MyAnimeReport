package com.example.myanimereport.activities;

import android.os.Bundle;
import android.view.View;

import com.example.myanimereport.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.fragments.BacklogFragment;
import com.example.myanimereport.fragments.HomeFragment;
import com.example.myanimereport.fragments.MatchFragment;
import com.example.myanimereport.fragments.ReportFragment;

public class MainActivity extends AppCompatActivity {

    public static ActivityMainBinding binding;

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
        final FragmentManager manager = getSupportFragmentManager();
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
    }
}