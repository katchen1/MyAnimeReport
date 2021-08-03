package com.example.myanimereport.models;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaAllQuery;
import com.example.MediaDetailsByIdListQuery;
import com.example.myanimereport.activities.EntryActivity;
import com.example.myanimereport.activities.MainActivity;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* Slope One algorithm implementation. */
public class SlopeOne {

    private List<String> userList; // List of all users
    private List<Integer> animeList; // List of all animes rated by users
    private double[][] ratings; // ratings[i][j] = user i's rating of anime j
    private final List<Pair<Integer, Double>> predictedRatings = new ArrayList<>();
    private final List<Anime> shownAnimes;

    long start;

    public SlopeOne(List<Anime> shownAnimes) {
        this.shownAnimes = shownAnimes;
        MainActivity.matchFragment.showProgressBar();
        start = System.currentTimeMillis();
        System.out.println("getting input data... " + (System.currentTimeMillis() - start));
        getInputData();
    }

    /* Fetches all user's ratings on all animes from Parse. */
    public void getInputData() {
        predict();
    }

    /* Predicts the current user's ratings on unseen animes. */
    public void predict() {
        if (ParseApplication.entries.isEmpty()) {
            weaveInRandomAnimes();
            return;
        }

        System.out.println("Predicting!");
        addPrediction(0);
    }

    public void addPrediction(int index) {
        if (index == ParseApplication.entryMediaIdAllUsers.size()) {
            // Sort by predicted rating (descending)
            predictedRatings.sort((p1, p2) -> p2.second.compareTo(p1.second));
            System.out.println("removing rejections: " + (System.currentTimeMillis() - start));
            removeRejections();
            return;
        }

        List<Integer> toPredicts = new ArrayList<>(ParseApplication.entryMediaIdAllUsers);
        Integer toPredict = toPredicts.get(index);
        if (!ParseApplication.seenMediaIds.contains(toPredict)) {
            ParseQuery<AnimePair> query1 = ParseQuery.getQuery(AnimePair.class);
            query1.whereEqualTo("mediaId1", toPredict);
            ParseQuery<AnimePair> query2 = ParseQuery.getQuery(AnimePair.class);
            query2.whereEqualTo("mediaId2", toPredict);
            List<ParseQuery<AnimePair>> list = new ArrayList<>();
            list.add(query1);
            list.add(query2);
            ParseQuery<AnimePair> query = ParseQuery.or(list);
            query.findInBackground((pairs, e) -> {
                // Check for errors
                if (e != null) {
                    Log.e("SlopeOne", "Error when getting anime pairs.", e);
                    return;
                }

                List<Pair<Double, Integer>> ratingWeightPairs = new ArrayList<>();
                for (AnimePair pair : pairs) {
                    int sign = pair.getMediaId1().equals(toPredict) ? 1 : -1;
                    Integer basedOn = sign == 1 ? pair.getMediaId2() : pair.getMediaId1();
                    for (Entry entry : ParseApplication.entries) {
                        if (entry.getMediaId().equals(basedOn)) {
                            double diffAvg = pair.getDiffSum() / pair.getCount();
                            Double predictedRating = entry.getRating() + sign * diffAvg;
                            ratingWeightPairs.add(new Pair<>(predictedRating, pair.getCount()));
                        }
                    }
                }

                // Final prediction (weighted)
                double num = 0.0;
                double den = 0.0;
                for (Pair<Double, Integer> p : ratingWeightPairs) {
                    num += p.first * p.second;
                    den += p.second;
                }
                if (den > 0) predictedRatings.add(new Pair<>(toPredict, num / den));
                else predictedRatings.add(new Pair<>(toPredict, -1.0));
                addPrediction(index + 1);
            });
        } else {
            addPrediction(index + 1);
        }
    }

    /* Removes animes that user has rejected over 3 times within the past week. */
    public void removeRejections() {
        // Get the date from a week ago
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date aWeekAgo = cal.getTime();

        // Query rejections
        ParseQuery<Rejection> query = ParseQuery.getQuery(Rejection.class); // Specify type of data
        query.whereEqualTo(Rejection.KEY_USER, ParseUser.getCurrentUser()); // Limit to current user
        query.whereGreaterThan(Rejection.KEY_UPDATED_AT, aWeekAgo); // Within the past week
        query.findInBackground((rejectionsFound, e) -> { // Start async query for rejections
            // Check for errors
            if (e != null) {
                Log.e("SlopeOne", "Error when getting rejections. " + e.getMessage(), e);
                return;
            }

            // Build a count map and keep track of animes with 3 or more rejections
            Map<Integer, Integer> rejectionCount = new HashMap<>();
            List<Integer> rejections = new ArrayList<>();
            for (Rejection r: rejectionsFound) {
                Integer id = r.getMediaId();
                Integer count = rejectionCount.get(id);
                if (count == null) count = 0;
                rejectionCount.put(id, count + 1);
                if (count + 1 >= 3) rejections.add(id);
            }

            // Remove seen animes or recommendations with 3 or more rejections
            predictedRatings.removeIf((p) -> rejections.contains(p.first));
            predictedRatings.removeIf((p) -> ParseApplication.seenMediaIds.contains(p.first));


            shownAnimes.clear();

            System.out.println("querying animes: " + (System.currentTimeMillis() - start));
            queryAnimes(1);
        });
    }

    /* Query animes of a list of anime ids. */
    public void queryAnimes(int page) {
        List<Integer> ids = new ArrayList<>();
        List<Double> ratings = new ArrayList<>();
        System.out.println("predictedRatings size: " + predictedRatings.size());
        for (Pair<Integer, Double> p: predictedRatings) {
            ids.add(p.first);
            ratings.add(p.second);
        }

        ParseApplication.apolloClient.query(new MediaDetailsByIdListQuery(page, ids)).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdListQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdListQuery.Data> response) {
                    // Null checking
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    if (response.getData().Page().pageInfo() == null) return;
                    if (response.getData().Page().pageInfo().hasNextPage() == null) return;

                    // Current page
                    for (MediaDetailsByIdListQuery.Medium m: response.getData().Page().media()) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        anime.setPredictedRating(ratings.get(ids.indexOf(anime.getMediaId())));
                        shownAnimes.add(anime);
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1);
                    } else {
                        shownAnimes.sort((a1, a2) -> ids.indexOf(a1.getMediaId()) - ids.indexOf(a2.getMediaId()));
                        System.out.println("shownAnimes size (before weave): " + shownAnimes.size());
                        System.out.println("weaving in random animes: " + (System.currentTimeMillis() - start));
                        weaveInRandomAnimes();
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }

    /* Weaves in random animes (once in 5 recommendations). */
    public void weaveInRandomAnimes() {
        ParseApplication.apolloClient.query(new MediaAllQuery(1, ParseApplication.seenMediaIds)).enqueue(
            new ApolloCall.Callback<MediaAllQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaAllQuery.Data> response) {
                    // Null checking
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    if (response.getData().Page().pageInfo() == null) return;
                    if (response.getData().Page().pageInfo().hasNextPage() == null) return;

                    // Generate and shuffle list of random animes
                    List<Anime> randomAnimes = new ArrayList<>();
                    for (MediaAllQuery.Medium m: response.getData().Page().media()) {
                        randomAnimes.add(new Anime(m.fragments().mediaFragment()));
                    }
                    Collections.shuffle(randomAnimes);

                    // Insert the random animes in the shown animes
                    for (int i = 0; i < randomAnimes.size(); i++) {
                        int index = i * 5 + 4;
                        if (shownAnimes.size() > index) shownAnimes.add(index, randomAnimes.get(i));
                        else shownAnimes.add(randomAnimes.get(i));
                    }

                    // Notify the adapter
                    ParseApplication.currentActivity.runOnUiThread(() -> {
                        MainActivity.matchFragment.getAdapter().notifyDataSetChanged();
                        MainActivity.matchFragment.hideProgressBar();
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }
}