package com.example.myanimereport.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.myanimereport.databinding.ActivityEntryDetailsBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.Entry;
import java.text.DateFormatSymbols;
import java.util.Locale;
import com.example.myanimereport.models.ParseApplication;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.parceler.Parcels;

public class EntryDetailsActivity extends AppCompatActivity {

    public static final int EDIT_ENTRY_REQUEST_CODE = 3;

    private ActivityEntryDetailsBinding binding;
    private Entry entry; // The entry whose information is being shown
    private Integer position; // The position of the entry in the adapter
    private Anime anime; // The anime of the entry
    private boolean editable; // Whether or not the user is allowed to edit the entry
    private Integer allPosition; // The position of the entry in the allEntries list

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
        anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));
        editable = getIntent().getBooleanExtra("editable", true);
        allPosition = getIntent().getIntExtra("position", -1);

        // Show the entry's information
        populateEntryView();
    }

    /* Shows the entry's information. */
    public void populateEntryView() {
        if (entry == null || anime == null) finish();

        // Data unrelated to the anime
        if (entry.getMonthWatched() != null) {
            String month = (new DateFormatSymbols().getMonths()[entry.getMonthWatched() - 1]);
            binding.tvMonthWatched.setText(month);
        }
        if (entry.getYearWatched() != null) {
            binding.tvYearWatched.setText(String.format(Locale.getDefault(), "%d", entry.getYearWatched()));
        }
        if (entry.getRating() != null) {
            binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f", entry.getRating()));
        }
        if (entry.getNote() != null) {
            binding.tvNote.setText(entry.getNote());
        }

        // Data related to the anime
        Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        binding.tvTitle.setText(anime.getTitleEnglish());
        binding.cvEntry.setStrokeColor(anime.getColor());

        // View-only mode
        if (!editable) {
            binding.btnEdit.setVisibility(View.GONE);
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    /* Shows the anime's details. */
    public void btnInfoOnClick(View view) {
        Intent intent = new Intent(EntryDetailsActivity.this, AnimeDetailsActivity.class);
        intent.putExtra("anime", Parcels.wrap(anime));

        // Animate the transition
        Activity activity = ParseApplication.currentActivity;
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, binding.cvEntry, "card");
        startActivity(intent, options.toBundle());
    }

    /* Navigates to the Entry Activity to edit it. */
    public void btnEditOnClick(View view) {
        Intent intent = new Intent(EntryDetailsActivity.this, EntryActivity.class);
        intent.putExtra("entry", entry); // Pass in the entry to edit
        intent.putExtra("anime", Parcels.wrap(anime)); // Also pass in the anime to reduce queries
        startActivityForResult(intent, EDIT_ENTRY_REQUEST_CODE);
    }

    /* Prompts a confirm dialog and deletes the entry. */
    public void btnDeleteOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Entry")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete", (dialog, which) -> entry.deleteInBackground(e -> {
                if (e == null) {
                    // Return to the home list and pass back the position so it can be deleted in the RV
                    Toast.makeText(EntryDetailsActivity.this, "Entry deleted.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("position", position);
                    intent.putExtra("allPosition", allPosition);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(EntryDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }))
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();

        alertDialog.show();
    }

    /* After returning from a entry edit activity, update the entry. */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_ENTRY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            entry = data.getParcelableExtra("entry");
            anime = Parcels.unwrap(data.getParcelableExtra("anime"));
            populateEntryView();
        }
    }

    /* Returns to the home list and passes back the updated entry so it can be redrawn. */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("entry", entry);
        intent.putExtra("position", position);
        intent.putExtra("allPosition", allPosition);
        intent.putExtra("anime", Parcels.wrap(anime));
        setResult(RESULT_OK, intent);
        finish();
    }
}