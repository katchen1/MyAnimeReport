package com.example.myanimereport.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import com.example.MediaPageByTitleQuery;
import com.example.myanimereport.R;
import com.example.myanimereport.adapters.AnimesAdapter;
import com.example.myanimereport.databinding.ActivityEntryBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import org.parceler.Parcels;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EntryActivity extends AppCompatActivity {

    private final String[] months = new DateFormatSymbols().getMonths(); // To convert month string and int
    private ActivityEntryBinding binding;
    private Integer mode; // 0 for creating a new entry; 1 for editing an existing entry
    private Integer mediaId; // The mediaId of the entry's anime, -1 if not found
    private Integer searchMediaId; // The mediaId of the closest anime returned by the GraphQL query
    private Entry entry; // The entry being edited
    private Integer position; // Position of the anime in the backlog recycler view
    private List<Anime> queriedAnimes; // Suggested animes based on title search
    private AnimesAdapter adapter; // Adapter for recycler view for queriedAnimes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up recycler view for queried animes
        queriedAnimes = new ArrayList<>();
        adapter = new AnimesAdapter(this, binding.etTitle, queriedAnimes);
        binding.rvAnimes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAnimes.setAdapter(adapter);
        DividerItemDecoration divider = new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.item_divider)));
        binding.rvAnimes.addItemDecoration(divider);

        // Set up focus change and click listeners
        binding.etTitle.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etNote.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etRating.setOnFocusChangeListener(this::etOnChangeFocus);

        // Set up number pickers for month and year
        setUpNumberPickers();

        // Determine if creating or editing
        if (!getIntent().hasExtra("entry")) {
            // Creating a new entry, mediaId has not been found
            mode = 0;
            binding.tvToolbar.setText(R.string.add_entry);
            mediaId = -1;

            // Creating a new entry from a backlog anime
            if (getIntent().hasExtra("anime")) {
                Anime anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));
                binding.etTitle.setText(anime.getTitleEnglish());
                mediaId = anime.getMediaId();
                position = getIntent().getIntExtra("position", -1);
            }
        } else {
            // Editing an existing entry, set the entry to be the one passed in
            mode = 1;
            binding.tvToolbar.setText(R.string.edit_entry);
            entry = getIntent().getParcelableExtra("entry");
            mediaId = entry.getMediaId();

            // Populate the views
            // The anime of the entry
            Anime anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));
            if (anime == null) return;
            if (anime.getTitleEnglish() != null) binding.etTitle.setText(anime.getTitleEnglish());
            if (entry.getMonthWatched() != null) binding.npMonthWatched.setValue(entry.getMonthWatched());
            if (entry.getYearWatched() != null) binding.npYearWatched.setValue(entry.getYearWatched());
            if (entry.getRating() != null) binding.etRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
            if (entry.getNote() != null) binding.etNote.setText(entry.getNote());
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
        for (Anime anime: queriedAnimes) {
            if (anime.getTitleEnglish().equals(search)) {
                binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
                mediaId = anime.getMediaId();
                return;
            }
        }

        // If haven't found a match, continue to search for a match
        binding.etTitle.setTextColor(ContextCompat.getColor(this, R.color.red));
        mediaId = -1;
        queryTitle(search);
    }

    /* Searches for an anime by title via AniList GraphQL. */
    public void queryTitle(String search) {
        ParseApplication.apolloClient.query(new MediaPageByTitleQuery(search)).enqueue(
            new ApolloCall.Callback<MediaPageByTitleQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaPageByTitleQuery.Data> response) {
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    runOnUiThread(() -> {
                        // Add the animes to the list
                        queriedAnimes.clear();
                        for (MediaPageByTitleQuery.Medium m : response.getData().Page().media()) {
                            queriedAnimes.add(new Anime(m.fragments().mediaFragment()));
                        }
                        adapter.notifyDataSetChanged();
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
        binding.rvAnimes.setVisibility(View.VISIBLE);
    }

    /* Hides the title suggestion below the search bar. */
    public void hideTitleSuggestion() {
        binding.rvAnimes.setVisibility(View.GONE);
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

        // Check if user already has an entry for this anime
        for (Entry entry: ParseApplication.entries) {
            if (entry.getMediaId().equals(mediaId)) {
                Toast.makeText(EntryActivity.this, "Already have an entry for this anime.", Toast.LENGTH_SHORT).show();
                return;
            }
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
        entry.setAnime();
        entry.saveInBackground(e -> {
            if (e == null) {
                // Pass back the entry so it can be inserted in the recycler view
                Toast.makeText(EntryActivity.this, "Entry created.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("entry", entry);
                intent.putExtra("position", position);
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
        entry.setAnime();
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
                intent.putExtra("anime", Parcels.wrap(entry.getAnime()));
                setResult(RESULT_OK, intent);
                finish();
            } else {
                Toast.makeText(EntryActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Returns to the home list and passes back the updated entry so it can be redrawn. */
    @Override
    public void onBackPressed() {
        MainActivity.backlogFragment.getAdapter().notifyDataSetChanged();
        finish();
    }
}