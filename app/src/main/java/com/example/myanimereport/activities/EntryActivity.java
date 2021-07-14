package com.example.myanimereport.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.content.Intent;
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
import com.example.fragment.MediaFragment;
import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ActivityEntryBinding;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class EntryActivity extends AppCompatActivity {

    private final String[] months = new DateFormatSymbols().getMonths(); // To convert month string and int
    private ActivityEntryBinding binding;
    private Integer mode; // 0 for creating a new entry; 1 for editing an existing entry
    private Integer mediaId; // The mediaId of the entry's anime, -1 if not found
    private Integer searchMediaId; // The mediaId of the closest anime returned by the GraphQL query
    private Entry entry; // The entry being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up focus change and click listeners
        binding.etTitle.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etNote.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etRating.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.tvTitle.setOnClickListener(this::tvTitleOnClick);

        // Set up number pickers for month and year
        setUpNumberPickers();

        // Determine if creating or editing
        if (!getIntent().hasExtra("entry")) {
            // Creating a new entry, mediaId has not been found
            mode = 0;
            binding.tvToolbar.setText(R.string.add_entry);
            mediaId = -1;
        } else {
            // Editing an existing entry, set the entry to be the one passed in
            mode = 1;
            binding.tvToolbar.setText(R.string.edit_entry);
            entry = getIntent().getParcelableExtra("entry");
            mediaId = entry.getMediaId();

            // Populate the views
            binding.etTitle.setText(getIntent().getStringExtra("title"));
            binding.npMonthWatched.setValue(entry.getMonthWatched());
            binding.npYearWatched.setValue(entry.getYearWatched());
            binding.etRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
            binding.etNote.setText(entry.getNote());
        }


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
    }

    /* When user clicks the suggested title, set the title to the suggested title. */
    private void tvTitleOnClick(View view) {
        binding.etTitle.setText(binding.tvTitle.getText().toString());
        hideTitleSuggestion();
        hideFocus(binding.etTitle);
    }

    /* When user clicks outside of the edit texts, hide the soft keyboard. */
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
        // If the typed title matches the suggested title, remember the anime
        String search = binding.etTitle.getText().toString();
        String suggested = binding.tvTitle.getText().toString();
        if (search.equals(suggested)) {
            binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
            mediaId = searchMediaId;
            return;
        }

        // If haven't found a match, continue to search for a match
        binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.red));
        mediaId = -1;
        queryTitle(search);
    }

    /* Searches for an anime by title via AniList GraphQL. */
    public void queryTitle(String search) {
        ParseApplication.apolloClient.query(new MediaDetailsByTitleQuery(search)).enqueue(
            new ApolloCall.Callback<MediaDetailsByTitleQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByTitleQuery.Data> response) {
                    // View editing needs to happen on the main thread, not the background thread
                    runOnUiThread(() -> {
                        // Try to find the English or Romaji title
                        MediaFragment media = response.getData().Media().fragments().mediaFragment();
                        String title = media.title().english();
                        if (title == null) title = media.title().romaji();

                        // Exit early if can't find either
                        if (title == null){
                            hideTitleSuggestion();
                            return;
                        }

                        // Show the suggestion and remember the Anime's id
                        binding.tvTitle.setText(title);
                        searchMediaId = media.id();
                        showTitleSuggestion();
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
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
        // Get the user's inputs
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

        if (mode == 0) createNewEntry(month, year, Double.parseDouble(rating), note);
        else updateExistingEntry(month, year, Double.parseDouble(rating), note);
    }

    /* Creates a new entry, saves it, and return to the home list. */
    public void createNewEntry(Integer month, Integer year, Double rating, String note) {
        entry = new Entry(mediaId, month, year, rating, note);
        entry.saveInBackground(e -> {
            if (e == null) {
                // Pass back the entry so it can be inserted in the recycler view
                Toast.makeText(EntryActivity.this, "Entry created.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("entry", entry);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(EntryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Updates an existing entry with the newly filled information. */
    private void updateExistingEntry(Integer month, Integer year, Double rating, String note) {
        entry.setMediaId(mediaId);
        entry.setMonthWatched(month);
        entry.setYearWatched(year);
        entry.setRating(rating);
        entry.setNote(note);
        entry.saveInBackground(e -> {
            if (e == null) {
                // Pass back the entry so it can be redrawn in the entry details activity
                Toast.makeText(EntryActivity.this, "Entry updated.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("entry", entry);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(EntryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}