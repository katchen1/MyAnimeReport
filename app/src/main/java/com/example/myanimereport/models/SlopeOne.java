package com.example.myanimereport.models;

import android.util.Log;

import androidx.core.util.Pair;
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
    Map<Integer, Double> predictedRatings = new HashMap<>();
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
            if (den > 0) predictedRatings.put(animeList.get(j1), num / den);
            else predictedRatings.put(animeList.get(j1), -1.0);
        }
    }

    public Map<Integer, Double> getPredictedRatings() {
        return predictedRatings;
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