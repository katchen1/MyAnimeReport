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
import androidx.fragment.app.Fragment;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaAllQuery;
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

        // Present an anime for the user to accept or reject
        binding.btnAccept.setOnClickListener(this::accept);
        binding.btnReject.setOnClickListener(this::reject);
        binding.btnRewind.setOnClickListener(this::rewind);

        // Set up the card stack
        layoutManager = new CardStackLayoutManager(getContext(), this);
        adapter = new CardStackAdapter(getContext(), allAnime);
        layoutManager.setStackFrom(StackFrom.Top);
        layoutManager.setVisibleCount(3);
        layoutManager.setCanScrollVertical(false);
        binding.cardStack.setLayoutManager(layoutManager);
        binding.cardStack.setAdapter(adapter);

        // Get all the media
        queryAnimePage(1);
    }

    public void queryAnimePage(int page) {
        ParseApplication.apolloClient.query(new MediaAllQuery(page)).enqueue(
            new ApolloCall.Callback<MediaAllQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaAllQuery.Data> response) {
                    for (MediaAllQuery.Medium m: response.getData().Page().media()) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        allAnime.add(anime);
                    }
                    Collections.shuffle(allAnime);
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimePage(page + 1);
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) return;
        System.out.println("Before: " + allAnime.size());
        allAnime.removeIf(anime -> ParseApplication.seenMediaIds.contains(anime.getMediaId()));
        System.out.println("After: " + allAnime.size());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /* Adds the anime to the user's backlog and generates a new anime. */
    private void accept(View view) {
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(new AccelerateInterpolator())
                .build();
        layoutManager.setSwipeAnimationSetting(setting);
        binding.cardStack.swipe();
    }

    /* Generates a new anime. */
    private void reject(View view) {
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(new AccelerateInterpolator())
                .build();
        layoutManager.setSwipeAnimationSetting(setting);
        binding.cardStack.swipe();
    }

    private void rewind(View view) {
        binding.cardStack.rewind();
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) { }

    @Override
    public void onCardSwiped(Direction direction) {
        if (direction == Direction.Right) {
            BacklogItem item = new BacklogItem();
            Anime currentAnime = allAnime.get(layoutManager.getTopPosition() - 1);
            allAnime.remove(currentAnime);
            adapter.notifyItemRemoved(layoutManager.getTopPosition() - 1);
            item.setMediaId(currentAnime.getMediaId());
            item.setUser(ParseUser.getCurrentUser());
            item.saveInBackground(e -> {
                if (e == null) {
                    // Pass back the entry so it can be inserted in the recycler view
                    Toast.makeText(getContext(), "Added to backlog.", Toast.LENGTH_SHORT).show();
                    item.setAnime(currentAnime);
                    ParseApplication.backlogItems.add(item);
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