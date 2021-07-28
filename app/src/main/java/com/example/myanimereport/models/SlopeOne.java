package com.example.myanimereport.models;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaDetailsByIdListQuery;
import com.example.myanimereport.activities.MainActivity;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Slope One algorithm implementation
 */
public class SlopeOne {

    private List<String> userList;
    private List<Integer> animeList;
    private int numUsers;
    private int numAnimes;
    private double[][] ratings;
    private final List<Pair<Integer, Double>> predictedRatings = new ArrayList<>();
    private final Set<Integer> allAnimes = new HashSet<>();
    private final List<Anime> shownAnimes;

    public SlopeOne(List<Anime> shownAnimes) {
        this.shownAnimes = shownAnimes;
        getInputData();
    }

    /* Fetches all user's ratings on all animes from Parse. */
    public void getInputData() {
        Map<String, HashMap<Integer, Double>> inputData = new HashMap<>();
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.include("User");
        query.findInBackground((entriesFound, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e("SlopeOne", "Error when getting entries.", e);
                return;
            }

            // Extract rating from entry
            for (Entry entry: entriesFound) {
                String username = entry.getUsername();
                if (!inputData.containsKey(username)) {
                    inputData.put(username, new HashMap<>());
                }
                Map<Integer, Double> userRatings = inputData.get(username);
                if (userRatings != null) userRatings.put(entry.getMediaId(), entry.getRating());
                allAnimes.add(entry.getMediaId());
            }

            // Create the dataframe of user ratings
            userList = new ArrayList<>(inputData.keySet());
            animeList = new ArrayList<>(allAnimes);
            numUsers = userList.size();
            numAnimes = animeList.size();
            ratings = new double[numUsers][numAnimes];
            for (int i = 0; i < numUsers; i++) {
                for (int j = 0; j < numAnimes; j++) {
                    String username = userList.get(i);
                    Integer anime = animeList.get(j);
                    ratings[i][j] = -1.0;
                    Map<Integer, Double> userData = inputData.get(username);
                    if (userData != null) {
                        Double rating = userData.get(anime);
                        if (rating != null) ratings[i][j] = rating;
                    }
                }
            }
            predict();
        });
    }

    /* Predicts the current user's ratings on unseen animes. */
    public void predict() {
        String currUser = ParseUser.getCurrentUser().getUsername();
        int currUserIndex = userList.indexOf(currUser);
        List<Integer> predictIndices = new ArrayList<>();
        List<Integer> basedOnIndices = new ArrayList<>();

        // Separate the animes that have been rated by the user from those that have not
        for (int j = 0; j < numAnimes; j++) {
            if (ratings[currUserIndex][j] < 0) predictIndices.add(j);
            else basedOnIndices.add(j);
        }

        // For each anime rating to predict
        for (Integer j1: predictIndices) {
            List<Pair<Double, Integer>> ratingWeightPairs = new ArrayList<>();

            // For each existing anime rating
            for (Integer j2: basedOnIndices) {
                int otherUserCount = 0;
                double otherUserRatingDiffSum = 0.0;

                // Find other users who rated both animes
                for (int i = 0; i < numUsers; i++) {
                    if (ratings[i][j1] >= 0 && ratings[i][j2] >= 0) {
                        otherUserCount++;
                        otherUserRatingDiffSum += ratings[i][j1] - ratings[i][j2];
                    }
                }

                // Predict based on exiting anime rating
                if (otherUserCount > 0) {
                    double otherUserRatingDiffAvg = otherUserRatingDiffSum / otherUserCount;
                    double predictedRating = ratings[currUserIndex][j2] + otherUserRatingDiffAvg;
                    ratingWeightPairs.add(new Pair<>(predictedRating, otherUserCount));
                }
            }

            // Final prediction (weighted)
            double num = 0.0;
            double den = 0.0;
            for (Pair<Double, Integer> p: ratingWeightPairs) {
                num += p.first * p.second;
                den += p.second;
            }
            if (den > 0) predictedRatings.add(new Pair<>(animeList.get(j1), num / den));
            else predictedRatings.add(new Pair<>(animeList.get(j1), -1.0));
        }

        // Added animes to the recommendation list in the match tab
        predictedRatings.sort((p1, p2) -> p2.second.compareTo(p1.second));

        // Remove animes that user has rejected over 3 times within the past week
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = cal.getTime();
        ParseQuery<Rejection> query = ParseQuery.getQuery(Rejection.class); // Specify type of data
        query.whereEqualTo(Rejection.KEY_USER, ParseUser.getCurrentUser()); // Limit to current user
        query.whereGreaterThan(Rejection.KEY_UPDATED_AT, sevenDaysAgo); // Within the past week
        query.findInBackground((rejectionsFound, e) -> { // Start async query for rejections
            // Check for errors
            if (e != null) {
                Log.e("SlopeOne", "Error when getting rejections.", e);
                return;
            }
            Map<Integer, Integer> rejectionCount = new HashMap<>();
            for (Rejection r: rejectionsFound) {
                Integer id = r.getMediaId();
                if (!rejectionCount.containsKey(id)) rejectionCount.put(id, 0);
                rejectionCount.put(id, rejectionCount.get(id) + 1);
            }
            List<Integer> rejections = new ArrayList<>();
            for (Integer id: rejectionCount.keySet()) if (rejectionCount.get(id) >= 3) rejections.add(id);
            predictedRatings.removeIf((p) -> rejections.contains(p.first));

            List<Integer> ids = new ArrayList<>();
            List<Double> ratings = new ArrayList<>();
            for (Pair<Integer, Double> p: predictedRatings) {
                ids.add(p.first);
                ratings.add(p.second);
            }

            shownAnimes.clear();
            queryAnimes(1, ids, ratings, shownAnimes);
        });
    }

    /* Query animes of a list of anime ids. */
    public void queryAnimes(int page, List<Integer> ids, List<Double> ratings, List<Anime> animes) {
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
                        animes.add(anime);
                        anime.predictedRating = ratings.get(ids.indexOf(anime.getMediaId()));
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1, ids, ratings, animes);
                    } else {
                        animes.sort((a1, a2) -> ids.indexOf(a1.getMediaId()) - ids.indexOf(a2.getMediaId()));
                        ParseApplication.currentActivity.runOnUiThread(() -> {
                            MainActivity.matchFragment.getAdapter().notifyDataSetChanged();
                            MainActivity.homeFragment.hideProgressBar();
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
    }
}