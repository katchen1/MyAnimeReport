package com.example.myanimereport.models;

import android.util.Log;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KNN {

    List<String> allGenres;
    Map<String, Map<String, List<Double>>> rawData;
    List<String> userIds;
    double[][] userFeatures;

    /** User Features
     *       | genre1 avg rating | genre1 count rank | genre2 avg rating | genre2 count rank | ...
     * ----- | ----------------- | ----------------- | ----------------- | ----------------- | ...
     * user1 | 8.0               | 1                 | 5.5               | 2                 | ...
     * user2 | 5.5               | 2                 | 3.0               | 4                 | ...
     * ...   | ...               | ...               | ...               | ...               | ...
     */

    public KNN() {
        allGenres = new ArrayList<>();
        rawData = new HashMap<>();
        queryRawData();
    }

    public void queryRawData() {
        ParseQuery<UserGenre> query = ParseQuery.getQuery(UserGenre.class); // Specify type of data
        query.findInBackground((rows, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                Log.e("KNN", "Error when getting entries.", e);
                return;
            }

            for (UserGenre row: rows) {
                String userId = row.getUser().getObjectId();
                String genre = row.getGenre();
                Double rating = row.getRating();

                if (!allGenres.contains(genre)) allGenres.add(genre);
                if (!userIds.contains(userId)) userIds.add(userId);

                Map<String, List<Double>> userData = rawData.get(userId);
                if (userData == null) {
                    userData = new HashMap<>();
                    List<Double> ratings = new ArrayList<>();
                    ratings.add(rating);
                    userData.put(genre, ratings);
                    rawData.put(userId, userData);
                } else {
                    List<Double> ratings = userData.get(genre);
                    if (ratings == null) {
                        ratings = new ArrayList<>();
                        ratings.add(rating);
                        userData.put(genre, ratings);
                    } else {
                        ratings.add(rating);
                    }
                }
            }

            buildUserFeatures();
        });
    }

    public void buildUserFeatures() {
        userFeatures = new double[userIds.size()][allGenres.size() * 2];
        for (int userIndex = 0; userIndex < userIds.size(); userIndex++) {
            for (int genreIndex = 0; genreIndex < allGenres.size(); genreIndex++) {
                String userId = userIds.get(userIndex);
                String genre = allGenres.get(genreIndex);

                double avgRating = -1.0;
                double count = 0.0;

                Map<String, List<Double>> userData = rawData.get(userId);
                if (userData != null) {
                    List<Double> ratings = userData.get(genre);
                    if (ratings != null) {
                        avgRating = getAverage(ratings);
                        count = ratings.size();
                    }
                }

                userFeatures[userIndex][genreIndex * 2] = avgRating;
                userFeatures[userIndex][genreIndex * 2 + 1] = count;
            }
        }

        convertCountsToRank();
    }

    public void convertCountsToRank() {

    }

    public double getAverage(List<Double> list) {
        double sum = 0.0;
        for (Double d: list) sum += d;
        return sum / list.size();
    }

    public List<ParseUser> kNearestNeighbors(ParseUser user) {
        return new ArrayList<>();
    }
}
