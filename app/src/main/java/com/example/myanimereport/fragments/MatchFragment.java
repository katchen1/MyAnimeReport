package com.example.myanimereport.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaAllQuery;
import com.example.myanimereport.activities.MainActivity;
import com.example.myanimereport.adapters.CardStackAdapter;
import com.example.myanimereport.databinding.FragmentMatchBinding;
import com.example.myanimereport.models.Anime;
import com.example.myanimereport.models.BacklogItem;
import com.example.myanimereport.models.ParseApplication;
import com.parse.ParseUser;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MatchFragment extends Fragment implements CardStackListener {

    private FragmentMatchBinding binding;
    private List<Anime> allAnime;
    private CardStackLayoutManager layoutManager;
    private CardStackAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        allAnime = new ArrayList<>();

        // Set the button listeners
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);
        binding.btnRewind.setOnClickListener(this::rewind);
        binding.btnMenu.setOnClickListener(v -> MainActivity.binding.drawerLayout.openDrawer(GravityCompat.START));

        // Set up the card stack
        layoutManager = new CardStackLayoutManager(getContext(), this);
        adapter = new CardStackAdapter(getContext(), allAnime);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setCanScrollVertical(false);
        binding.cardStack.setLayoutManager(layoutManager);
        binding.cardStack.setAdapter(adapter);
        queryAnimePage(1);
    }

    /* Recursive function for pagination. Fetches all the anime with popularity > 30000. */
    public void queryAnimePage(int page) {
        ParseApplication.apolloClient.query(new MediaAllQuery(page)).enqueue(
            new ApolloCall.Callback<MediaAllQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaAllQuery.Data> response) {
                    // Null checking
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    if (response.getData().Page().pageInfo() == null) return;
                    if (response.getData().Page().pageInfo().hasNextPage() == null) return;

                    // Add the animes to the list
                    for (MediaAllQuery.Medium m: Objects.requireNonNull(response.getData().Page().media())) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        allAnime.add(anime);
                    }
                    Collections.shuffle(allAnime);
                    if (response.getData().Page().pageInfo().hasNextPage()) queryAnimePage(page + 1);
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }

    /* When the match tab is clicked, remove all the seen animes from the card stack. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        allAnime.removeIf(anime -> ParseApplication.seenMediaIds.contains(anime.getMediaId()));
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

    @Override
    public void onCardDragging(Direction direction, float ratio) { }

    /* If swipe right, adds the anime to the user's backlog and removes it from the stack. */
    @Override
    public void onCardSwiped(Direction direction) {
        if (direction == Direction.Right) {
            BacklogItem item = new BacklogItem();
            int position = layoutManager.getTopPosition() - 1;
            Anime anime = allAnime.get(position);
            allAnime.remove(anime);
            adapter.notifyItemRemoved(position);
            item.setMediaId(anime.getMediaId());
            item.setUser(ParseUser.getCurrentUser());
            item.setAnime(anime);
            item.saveInBackground(e -> {
                if (e == null) {
                    ParseApplication.backlogItems.add(item);
                    Toast.makeText(getContext(), "Added to backlog.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
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