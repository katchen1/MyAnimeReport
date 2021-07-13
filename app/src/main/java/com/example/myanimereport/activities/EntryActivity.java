package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaDetailsByTitleQuery;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ActivityEntryBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import com.parse.ParseException;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class EntryActivity extends AppCompatActivity {

    private final String[] months = new DateFormatSymbols().getMonths();
    private ActivityEntryBinding binding;
    private Integer mediaId; // The mediaId of the entry's anime, -1 if not found
    private Integer searchMediaId; // The mediaId of the closest anime returned by the GraphQL query

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Creating a new entry, so haven't found the anime's mediaId yet
        mediaId = -1;

        // Add text changed listener to the title search bar
        binding.etTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleTextChange();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Set up click and focus change listeners
        binding.tvTitle.setOnClickListener(this::tvTitleOnClick);
        binding.etTitle.setOnFocusChangeListener(this::etOnChangeFocus);

        // Set up number pickers for month and year
        setUpNumberPickers();
    }

    /* When user clicks the suggested title, set the title to the suggested title. */
    private void tvTitleOnClick(View view) {
        binding.etTitle.setText(binding.tvTitle.getText().toString());
        hideTitleSuggestion();
        hideFocus(binding.etTitle);
    }

    /* When user clicks outside of the edit text, hide the soft keyboard. */
    private void etOnChangeFocus(View view, boolean hasFocus) {
        if (!hasFocus) hideFocus(view);
    }

    /* Hides the soft keyboard. */
    public void hideFocus(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        hideTitleSuggestion();
    }

    /* When user types in the title search bar, check if they've found a valid anime. */
    public void handleTextChange() {
        String et = binding.etTitle.getText().toString();
        String tv = binding.tvTitle.getText().toString();

        // If the typed title matches the suggested title, remember the anime
        if (et.equals(tv)) {
            binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
            mediaId = searchMediaId;
            return;
        }

        // If haven't found a match, continue to search for a match via the AniList API
        binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.red));
        mediaId = -1;
        ParseApplication.apolloClient.query(new MediaDetailsByTitleQuery(et)).enqueue(
            new ApolloCall.Callback<MediaDetailsByTitleQuery.Data>() {
                @Override
                public void onResponse(@NotNull Response<MediaDetailsByTitleQuery.Data> response) {
                    runOnUiThread(() -> {
                        // Try to find the English or Romaji title
                        String title = response.getData().Media().title().english();
                        if (title == null) title = response.getData().Media().title().romaji();

                        // If cannot find either, exit early
                        if (title == null){
                            hideTitleSuggestion();
                            return;
                        }

                        // Show the suggestion and remember the Anime's id
                        binding.tvTitle.setText(title);
                        searchMediaId = response.getData().Media().id();
                        showTitleSuggestion();
                    });
                }

                @Override
                public void onFailure(@NotNull ApolloException e) {
                    runOnUiThread(() -> hideTitleSuggestion());
                }
            }
        );
    }

    /* Shows the title suggestion below the search bar. */
    public void showTitleSuggestion() {
        binding.tvTitle.setVisibility(View.VISIBLE);
    }

    /* Hides the title suggestion below the search bar. */
    public void hideTitleSuggestion() {
        binding.tvTitle.setVisibility(View.GONE);
    }

    /* Sets up the number pickers for month and year. */
    public void setUpNumberPickers() {
        // Month
        binding.npMonthWatched.setMaxValue(12);
        binding.npMonthWatched.setMinValue(1);
        binding.npMonthWatched.setDisplayedValues(months);
        binding.npMonthWatched.setValue(Calendar.getInstance().get(Calendar.MONTH) + 1);

        // Year
        binding.npYearWatched.setMaxValue(Calendar.getInstance().get(Calendar.YEAR));
        binding.npYearWatched.setMinValue(1900);
        binding.npYearWatched.setValue(Calendar.getInstance().get(Calendar.YEAR));
    }

    /* Saves the entry. */
    public void saveOnClick(View v) {
        Integer month = binding.npMonthWatched.getValue();
        Integer year = binding.npYearWatched.getValue();
        String rating = binding.etRating.getText().toString();
        String note = binding.etNote.getText().toString();

        // Check if media id is valid
        if (mediaId == -1) {
            binding.etTitle.setText("");
            Toast.makeText(EntryActivity.this, "Invalid title.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if rating is valid
        if (rating.isEmpty() || Double.parseDouble(rating) > 10 || Double.parseDouble(rating) < 0) {
            binding.etRating.setText("");
            Toast.makeText(EntryActivity.this, "Invalid rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new entry and save it
        Entry entry = new Entry(mediaId, month, year, Double.parseDouble(rating), note);
        entry.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(EntryActivity.this, "Entry saved.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EntryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}