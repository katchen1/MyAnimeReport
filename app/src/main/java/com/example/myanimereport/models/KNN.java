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

    }

    public List<ParseUser> kNearestNeighbors(ParseUser user) {
        return new ArrayList<>();
    }
}
