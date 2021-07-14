package com.example.myanimereport.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.example.MediaDetailsByIdQuery;
import com.example.myanimereport.databinding.ActivityEntryDetailsBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import com.example.myanimereport.models.ParseApplication;
import java.text.DateFormatSymbols;
import java.util.Locale;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EntryDetailsActivity extends AppCompatActivity {

    public static final int EDIT_ENTRY_REQUEST_CODE = 3;

    private final String TAG = "EntryDetailsActivity";
    private ActivityEntryDetailsBinding binding;
    private Entry entry; // The entry whose information is being shown
    private Integer position; // The position of the entry in the adapter
    private Anime anime; // The anime of the entry

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Get the passed in data
        entry = getIntent().getParcelableExtra("entry");
        position = getIntent().getIntExtra("position", -1);

        // Show the entry's information
        populateEntryView();
    }

    /* Shows the entry's information. */
    public void populateEntryView() {
        // Data unrelated to the anime
        String month = (new DateFormatSymbols().getMonths()[entry.getMonthWatched() - 1]);
        binding.tvMonthWatched.setText(month);
        binding.tvYearWatched.setText(String.format(Locale.getDefault(), "%d", entry.getYearWatched()));
        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
        binding.tvNote.setText(entry.getNote());

        // Make a query for the anime's cover image and title
        Integer mediaId = entry.getMediaId();
        ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(mediaId)).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                    // View editing needs to happen in the main thread, not the background thread
                    ParseApplication.currentActivity.runOnUiThread(() -> {
                        anime = new Anime(response);
                        Glide.with(EntryDetailsActivity.this).load(anime.getCoverImage()).into(binding.ivImage);
                        binding.tvTitle.setText(anime.getTitleEnglish());
                        binding.cvAnime.setStrokeColor(anime.getColor());
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        );
    }

    /* Shows the anime's details. */
    public void btnInfoOnClick(View view) {
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
    }

    /* Navigates to the Entry Activity to edit it. */
    public void btnEditOnClick(View view) {
        Intent intent = new Intent(EntryDetailsActivity.this, EntryActivity.class);
        intent.putExtra("entry", entry); // Pass in the entry to edit
        intent.putExtra("title", anime.getTitleEnglish()); // Also pass in the title to reduce queries
        startActivityForResult(intent, EDIT_ENTRY_REQUEST_CODE);
    }

    /* Prompts a confirm dialog and deletes the entry. */
    public void btnDeleteOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete", (dialog, which) -> entry.saveInBackground(e -> {
                if (e == null) {
                    // Return to the home list and pass back the position so it can be deleted in the RV
                    Toast.makeText(EntryDetailsActivity.this, "Entry deleted.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("position", position);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(EntryDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }))
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .show();
    }

    /* After returning from a entry edit activity, update the entry. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            entry = data.getParcelableExtra("entry");
            populateEntryView();
        }
    }

    /* Returns to the home list and passes back the updated entry so it can be redrawn. */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("entry", entry);
        intent.putExtra("position", position);
        setResult(RESULT_OK, intent);
        finish();
    }
}