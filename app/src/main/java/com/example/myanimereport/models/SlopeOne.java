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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Slope One algorithm implementation
 */
public class SlopeOne {

    List<String> userList;
    List<Integer> animeList;
    int numUsers;
    int numAnimes;
    double[][] ratings;
    List<Pair<Integer, Double>> predictedRatings = new ArrayList<>();
    final Set<Integer> allAnimes = new HashSet<>();

    public SlopeOne() {
        getInputData();
    }

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

            // Extract user ratings from entries and add to input data
            for (Entry entry: entriesFound) {
                String username = entry.getUsername();
                if (username.equals("bob") || username.equals("alice") || username.equals("nathan")) {
                    if (!inputData.containsKey(username)) {
                        inputData.put(username, new HashMap<>());
                    }
                    Map<Integer, Double> userRatings = inputData.get(username);
                    if (userRatings != null) userRatings.put(entry.getMediaId(), entry.getRating());
                    allAnimes.add(entry.getMediaId());
                }
            }

            // Create the dataframe
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
                        if (rating != null) {
                            ratings[i][j] = rating;
                        }
                    }
                }
            }
            predict();
        });
    }

    public void predict() {
        String currUser = ParseUser.getCurrentUser().getUsername();
        int currUserIndex = userList.indexOf(currUser);
        List<Integer> predictIndices = new ArrayList<>();
        List<Integer> basedOnIndices = new ArrayList<>();

        // Separate the animes that have been rated by the user from those that have not
        for (int j = 0; j < animeList.size(); j++) {
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
                for (int i = 0; i < userList.size(); i++) {
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
        List<Anime> shownAnimes = MainActivity.matchFragment.getAnimes();
        shownAnimes.clear();
        List<Integer> ids = new ArrayList<>();
        for (Pair<Integer, Double> p: predictedRatings) ids.add(p.first);
        queryAnimes(1, ids, shownAnimes);
    }

    public void queryAnimes(int page, List<Integer> ids, List<Anime> animes) {
        ParseApplication.apolloClient.query(new MediaDetailsByIdListQuery(page, ids)).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdListQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdListQuery.Data> response) {
                    // Null checking
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    if (response.getData().Page().pageInfo() == null) return;
                    if (response.getData().Page().pageInfo().hasNextPage() == null) return;

                    // Set animes for the page
                    for (MediaDetailsByIdListQuery.Medium m: response.getData().Page().media()) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        animes.add(anime);
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1, ids, animes);
                    } else {
                        animes.sort((a1, a2) -> ids.indexOf(a1.getMediaId()) - ids.indexOf(a2.getMediaId()));
                        ParseApplication.currentActivity.runOnUiThread(() -> {
                            MainActivity.matchFragment.getAdapter().notifyDataSetChanged();
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

    public static void print(double[][] matrix) {
        String output = "";
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[0].length; c++) {
                output += matrix[r][c] + " ";
            }
            output += "\n";
        }
        System.out.println(output);
    }
}