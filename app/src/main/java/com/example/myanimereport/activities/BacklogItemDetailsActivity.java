package com.example.myanimereport.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.myanimereport.databinding.ActivityBacklogItemDetailsBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.ParseApplication;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.parceler.Parcels;
import java.util.Locale;

public class BacklogItemDetailsActivity extends AppCompatActivity {

    private ActivityBacklogItemDetailsBinding binding;
    private BacklogItem item; // The item whose information is being shown
    private Integer position; // The position of the item in the adapter
    private Anime anime; // The anime of the item

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBacklogItemDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide status bar and action bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Get the passed in data
        item = getIntent().getParcelableExtra("item");
        position = getIntent().getIntExtra("position", -1);
        anime = Parcels.unwrap(getIntent().getParcelableExtra("anime"));

        // Show the backlog item's information
        populateBacklogItemView();
    }

    /* Shows the backlog item's information. */
    public void populateBacklogItemView() {
        if (anime == null) finish();
        if (anime.getCoverImage() != null) Glide.with(this).load(anime.getCoverImage()).into(binding.ivImage);
        if (anime.getTitleEnglish() != null) binding.tvTitle.setText(anime.getTitleEnglish());
        if (anime.getColor() != null) binding.cvItem.setStrokeColor(anime.getColor());
        if (anime.getAverageScore() != null) binding.tvRating.setText(String.format(Locale.getDefault(),
                "%.1f", anime.getAverageScore()));
    }

    /* Shows the anime's details. */
    public void btnInfoOnClick(View view) {
        Intent intent = new Intent(BacklogItemDetailsActivity.this, AnimeDetailsActivity.class);
        intent.putExtra("anime", Parcels.wrap(anime));

        // Animate the transition
        Activity activity = ParseApplication.currentActivity;
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, binding.cvItem, "card");
        startActivity(intent, options.toBundle());
    }

    /* Deletes the backlog item. */
    public void btnDeleteOnClick(View view) {
        // Using a Material Dialog with layout defined in res/values/themes.xml
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Backlog Item")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete", (dialog, which) -> item.deleteInBackground(e -> {
                if (e == null) {
                    // Return to the home list and pass back the position so it can be deleted in the RV
                    Toast.makeText(BacklogItemDetailsActivity.this, "Item deleted.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("position", position);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Toast.makeText(BacklogItemDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }))
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();

        // Hide status bar of the alert dialog's window
        alertDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        alertDialog.show();
    }
}