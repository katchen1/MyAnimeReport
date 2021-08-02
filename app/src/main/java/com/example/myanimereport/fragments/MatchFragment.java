package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.CardStackAdapter;
import com.example.myanimereport.databinding.ActivityMainBinding;
import com.example.myanimereport.databinding.FragmentMatchBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.ParseApplication;
import com.example.myanimereport.models.Rejection;
import com.example.myanimereport.models.SlopeOne;
import com.parse.ParseUser;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import java.util.ArrayList;
import java.util.List;

public class MatchFragment extends Fragment implements CardStackListener {

    private FragmentMatchBinding binding;
    private List<Anime> animes;
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private SlopeOne slopeOne;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animes = new ArrayList<>();

        // Set the button listeners
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);
        binding.btnRewind.setOnClickListener(this::rewind);
        binding.btnSkip.setOnClickListener(this::skip);
        binding.btnMenu.setOnClickListener(this::openNavDrawer);

        // Set up the card stack
        layoutManager = new CardStackLayoutManager(getContext(), this);
        adapter = new CardStackAdapter(getContext(), animes);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setCanScrollVertical(false);
        binding.cardStack.setLayoutManager(layoutManager);
        binding.cardStack.setAdapter(adapter);
    }

    /* Generate recommendations when the tab is clicked for the first time. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        if (slopeOne == null) slopeOne = new SlopeOne(animes);
    }

    /* Shows the progress bar. */
    public void showProgressBar() {
        binding.pbProgressAction.setVisibility(View.VISIBLE);
        binding.cardStack.setVisibility(View.INVISIBLE);
        binding.rlButtons.setVisibility(View.INVISIBLE);
    }

    /* Hides the progress bar. */
    public void hideProgressBar() {
        binding.pbProgressAction.setVisibility(View.INVISIBLE);
        binding.cardStack.setVisibility(View.VISIBLE);
        binding.rlButtons.setVisibility(View.VISIBLE);
    }

    /* Opens the navigation drawer. */
    private void openNavDrawer(View view) {
        ActivityMainBinding binding = MainActivity.binding;
        binding.btnLayout.setVisibility(View.GONE);
        binding.btnSort.setVisibility(View.GONE);
        binding.btnSortCreationDate.setVisibility(View.GONE);
        binding.btnSortTitle.setVisibility(View.GONE);
        binding.btnSortWatchDate.setVisibility(View.GONE);
        binding.btnSortRating.setVisibility(View.GONE);
        binding.btnDeleteAllEntries.setVisibility(View.GONE);
        binding.btnDeleteBacklog.setVisibility(View.GONE);
        binding.btnFilter.setVisibility(View.GONE);
        binding.drawerLayout.openDrawer(GravityCompat.START);
    }

    /* Returns the adapter of the card stack recycler view. */
    public CardStackAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /* Accepts an anime. */
    private void accept(View view) {
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
            .setDirection(Direction.Right)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(new AccelerateInterpolator())
            .build();
        layoutManager.setSwipeAnimationSetting(setting);
        binding.cardStack.swipe();
    }

    /* Rejects an anime. */
    private void reject(View view) {
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
            .setDirection(Direction.Left)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(new AccelerateInterpolator())
            .build();
        layoutManager.setSwipeAnimationSetting(setting);
        binding.cardStack.swipe();
    }

    /* Rewinds an anime. */
    private void rewind(View view) {
        binding.cardStack.rewind();
    }

    /* Skips an anime. */
    private void skip(View view) {
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(Direction.Bottom)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(new AccelerateInterpolator())
                .build();
        layoutManager.setSwipeAnimationSetting(setting);
        binding.cardStack.swipe();
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) { }

    /* If swipe right, adds the anime to the user's backlog and removes it from the stack. */
    @Override
    public void onCardSwiped(Direction direction) {
        int position = layoutManager.getTopPosition() - 1;
        Anime anime = animes.get(position);
        if (direction == Direction.Right) {
            // Remove the anime from the recycler view
            animes.remove(anime);
            adapter.notifyItemRemoved(position);

            // Create a backlog item
            BacklogItem item = new BacklogItem();
            item.setMediaId(anime.getMediaId());
            item.setUser(ParseUser.getCurrentUser());
            item.setAnime(anime);
            item.saveInBackground(e -> {
                if (e == null) {
                    // Add the item to the backlog and notify its adapter
                    ParseApplication.backlogItems.add(item);
                    Toast.makeText(getContext(), "Added to backlog.", Toast.LENGTH_SHORT).show();
                    MainActivity.backlogFragment.getAdapter().notifyItemInserted(ParseApplication.backlogItems.size() - 1);
                    MainActivity.backlogFragment.checkItemsExist();
                } else {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else if (direction == Direction.Left) {
            // Remove the anime from the recycler view
            animes.remove(anime);
            adapter.notifyItemRemoved(position);

            // Create a rejection
            Rejection rejection = new Rejection();
            rejection.setMediaId(anime.getMediaId());
            rejection.setUser(ParseUser.getCurrentUser());
            rejection.saveInBackground();

            // Todo: remove this line
            Toast.makeText(getContext(), "Predicted rating: " + anime.getPredictedRating(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCardRewound() { }

    @Override
    public void onCardCanceled() { }

    @Override
    public void onCardAppeared(View view, int position) { }

    @Override
    public void onCardDisappeared(View view, int position) { }
}