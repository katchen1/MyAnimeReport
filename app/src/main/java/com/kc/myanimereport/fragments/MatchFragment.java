package com.kc.myanimereport.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.kc.myanimereport.R;
import com.kc.myanimereport.activities.MainActivity;
import com.kc.myanimereport.adapters.CardStackAdapter;
import com.kc.myanimereport.databinding.ActivityMainBinding;
import com.kc.myanimereport.databinding.FragmentMatchBinding;
import com.kc.myanimereport.models.Anime;
import com.kc.myanimereport.models.BacklogItem;
import com.kc.myanimereport.models.KNN;
import com.kc.myanimereport.models.ParseApplication;
import com.kc.myanimereport.models.Rejection;
import com.kc.myanimereport.models.SlopeOne;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MatchFragment extends Fragment implements CardStackListener {

    private FragmentMatchBinding binding;
    private List<Anime> animes;
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;
    private SlopeOne slopeOne;
    private KNN knn;
    private ColorStateList colorTheme, colorRipple;

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
        knn = new KNN(5);

        // Set the button listeners
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);
        binding.btnRewind.setOnClickListener(this::rewind);
        binding.btnMenu.setOnClickListener(this::openNavDrawer);

        // Set up the card stack
        layoutManager = new CardStackLayoutManager(getContext(), this);
        adapter = new CardStackAdapter(getContext(), animes);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setCanScrollVertical(false);
        binding.cardStack.setLayoutManager(layoutManager);
        binding.cardStack.setAdapter(adapter);

        // Message when no more recs
        String message = getString(R.string.no_recs) + "<br/>" + getString(R.string.come_back_later);
        binding.tvMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT));
        binding.tvMessage.setVisibility(View.INVISIBLE);

        // Colors used by card dragging animation
        colorTheme = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.theme));
        colorRipple = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.theme_dark));
        resetButtonColors();
    }

    /* Generate recommendations when the tab is clicked for the first time. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        if (slopeOne == null) slopeOne = new SlopeOne(animes, knn, 10);
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
        binding.btnSortDateAdded.setVisibility(View.GONE);
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
        // If nothing to rewind, don't do anything
        int posBefore = layoutManager.getTopPosition();
        binding.cardStack.rewind();
        int posAfter = layoutManager.getTopPosition();
        if (posBefore == posAfter) return;

        // Query anime to un-reject
        Anime anime = animes.get(layoutManager.getTopPosition());
        ParseQuery<Rejection> query = ParseQuery.getQuery(Rejection.class); // Specify type of data
        query.whereEqualTo(Rejection.KEY_USER, ParseUser.getCurrentUser()); // Limit to current user
        query.whereEqualTo(Rejection.KEY_MEDIA_ID, anime.getMediaId()); // The anime to un-reject
        query.orderByDescending(Rejection.KEY_CREATED_AT);
        query.findInBackground((rejectionsFound, e) -> { // Start async query for rejections
            // Check for errors
            if (e != null) {
                return;
            }

            // Un-reject the anime
            if (rejectionsFound.size() > 0) {
                Rejection r = rejectionsFound.get(0);
                r.deleteInBackground();
            }
        });
    }

    /* Highlights button. */
    @Override
    public void onCardDragging(Direction direction, float ratio) {
        if (direction == Direction.Left) {
            // Highlight reject button
            binding.btnReject.setBackgroundTintList(colorRipple);
            binding.btnAccept.setBackgroundTintList(colorTheme);
        } else if (direction == Direction.Right) {
            // Highlight accept button
            binding.btnReject.setBackgroundTintList(colorTheme);
            binding.btnAccept.setBackgroundTintList(colorRipple);
        }
    }

    /* Resets button colors. */
    public void resetButtonColors() {
        binding.btnReject.setBackgroundTintList(colorTheme);
        binding.btnAccept.setBackgroundTintList(colorTheme);
    }

    /* Displays message if reached the end of card stack. */
    public void checkAtEnd() {
        boolean atEnd = layoutManager.getTopPosition() == adapter.getItemCount();
        if (atEnd) binding.tvMessage.setVisibility(View.VISIBLE);
        else binding.tvMessage.setVisibility(View.INVISIBLE);
    }

    /* If swipe right, adds the anime to the user's backlog and removes it from the stack. */
    @Override
    public void onCardSwiped(Direction direction) {
        resetButtonColors();

        // Handle swipe
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
            item.setCreationDate(new Date());
            item.setAnime(anime);
            item.saveInBackground(e -> {
                if (e == null) {
                    // Add the item to the backlog and notify its adapter
                    boolean sortedOldest = MainActivity.backlogFragment.sortedOldest();
                    List<BacklogItem> items = MainActivity.backlogFragment.getItems();
                    if (sortedOldest) {
                        ParseApplication.backlogItems.add(item);
                        items.add(item);
                        MainActivity.backlogFragment.getAdapter().notifyItemInserted(items.size() - 1);
                    }
                    else {
                        ParseApplication.backlogItems.add(0, item);
                        items.add(0, item);
                        MainActivity.backlogFragment.getAdapter().notifyItemInserted(0);
                    }
                    MainActivity.backlogFragment.checkItemsExist();
                } else {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else if (direction == Direction.Left) {
            // Create a rejection
            Rejection rejection = new Rejection();
            rejection.setMediaId(anime.getMediaId());
            rejection.setUser(ParseUser.getCurrentUser());
            rejection.saveInBackground();
        }
        checkAtEnd();
    }

    @Override
    public void onCardRewound() {
        checkAtEnd();
    }

    /* Resets button colors. */
    @Override
    public void onCardCanceled() {
        resetButtonColors();
    }

    @Override
    public void onCardAppeared(View view, int position) { }

    @Override
    public void onCardDisappeared(View view, int position) { }
}