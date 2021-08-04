package com.example.myanimereport.models;

import android.util.Log;
import androidx.core.util.Pair;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KNN {

    List<String> allGenres;
    List<String> userIds;
    Map<String, Map<String, List<Double>>> rawData; // From Parse
    double[][] userFeatures; // Built from raw data

    /** User Features
     *       | genre1 avg rating | genre1 count rank | genre2 avg rating | genre2 count rank | ...
     * ----- | ----------------- | ----------------- | ----------------- | ----------------- | ...
     * user1 | 8.0               | 1                 | 5.5               | 2                 | ...
     * user2 | 5.5               | 2                 | 3.0               | 4                 | ...
     * ...   | ...               | ...               | ...               | ...               | ...
     */

    long start;

    public KNN() {
        allGenres = new ArrayList<>();
        userIds = new ArrayList<>();
        rawData = new HashMap<>();
        start = System.currentTimeMillis();
        queryRawData();
    }

    /* Gets the raw data for user genre info from Parse. */
    public void queryRawData() {
        System.out.println("querying raw data... " + (System.currentTimeMillis() - start));

        ParseQuery<UserGenre> query = ParseQuery.getQuery(UserGenre.class); // Specify type of data
        query.findInBackground((rows, e) -> { // Start async query
            // Check for errors
            if (e != null) {
                Log.e("KNN", "Error when getting entries.", e);
                return;
            }

            for (UserGenre row: rows) {
                String userId = row.getUser().getObjectId();
                String genre = row.getGenre();
                Double rating = row.getRating();

                // Track lists of all genres and all users
                if (!allGenres.contains(genre)) allGenres.add(genre);
                if (!userIds.contains(userId)) userIds.add(userId);

                // Convert Parse data into a map of {userId: {genre: ratings}}
                Map<String, List<Double>> userData = rawData.get(userId);
                if (userData == null) {
                    // If first time seeing user, initialize the user's genre-rating map
                    userData = new HashMap<>();
                    List<Double> ratings = new ArrayList<>();
                    ratings.add(rating);
                    userData.put(genre, ratings);
                    rawData.put(userId, userData);
                } else {
                    // Modify the user's genre-rating map
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

    /* Builds the user features based on the raw data. */
    public void buildUserFeatures() {
        System.out.println("building user features... " + (System.currentTimeMillis() - start));

        // Each row represents a user. Each genre has 2 columns, one for avgRating, one for count.
        userFeatures = new double[userIds.size()][allGenres.size() * 2];

        for (int userIndex = 0; userIndex < userIds.size(); userIndex++) {
            for (int genreIndex = 0; genreIndex < allGenres.size(); genreIndex++) {
                String userId = userIds.get(userIndex);
                String genre = allGenres.get(genreIndex);

                double avgRating = -1.0;
                double count = 0.0;

                // Populate the dataframe based on raw data
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

    /* Feature engineering:
     * Instead of comparing genre counts, convert them to ranks and compare the ranks. */
    public void convertCountsToRank() {
        System.out.println("converting counts to rank... " + (System.currentTimeMillis() - start));

        for (int userIndex = 0; userIndex < userIds.size(); userIndex++) {
            // Store the count of each genre
            List<Double> counts = new ArrayList<>();
            for (int genreIndex = 0; genreIndex < allGenres.size(); genreIndex++) {
                counts.add(userFeatures[userIndex][genreIndex * 2 + 1]);
            }

            // Sort the counts from high to low
            ArrayList<Double> sortedCounts = new ArrayList<>(counts);
            sortedCounts.sort((c1, c2) -> c2.compareTo(c1));

            // Replace the counts by their ranks (index in the sorted counts list)
            for (int genreIndex = 0; genreIndex < allGenres.size(); genreIndex++) {
                int rank = sortedCounts.indexOf(counts.get(genreIndex));
                userFeatures[userIndex][genreIndex * 2 + 1] = rank;
            }
        }
        printUserFeatures();
        System.out.println("done setting up knn! " + (System.currentTimeMillis() - start));
    }

    /* Prints the user features matrix. */
    public void printUserFeatures() {
        for (double[] userFeature : userFeatures) {
            for (double item: userFeature) {
                System.out.printf("%.2f ", item);
            }
            System.out.println();
        }
    }

    /* Returns the average of a list. */
    public double getAverage(List<Double> list) {
        double sum = 0.0;
        for (Double d: list) sum += d;
        return sum / list.size();
    }

    /* Returns the K nearest neighbors of the passed in userId. */
    public List<String> kNearestNeighbors(String userId, int K) {
        List<String> output = new ArrayList<>();
        List<Pair<Integer, Double>> userDistancePairs = new ArrayList<>();
        int userIndex = userIds.indexOf(userId);

        // Store the distance from each other user
        for (int neighborIndex = 0; neighborIndex < userIds.size(); neighborIndex++) {
            double d = getEuclideanDistance(userFeatures[userIndex], userFeatures[neighborIndex]);
            userDistancePairs.add(new Pair<>(neighborIndex, d));
        }

        // Sort the distance from low to high
        userDistancePairs.sort((p1, p2) -> p1.second.compareTo(p2.second));

        // Return the K users with the smallest distances
        for (int i = 0; i < userDistancePairs.size(); i++) {
            int neighborIndex = userDistancePairs.get(i).first;
            if (neighborIndex != userIndex) {
                String neighborId = userIds.get(neighborIndex);
                output.add(neighborId);
                if (output.size() >= K) break;
            }
        }
        return output;
    }

    /* Returns the euclidean distance between two vectors. */
    public double getEuclideanDistance(double[] feature1, double[] feature2) {
        double distance = 0.0;
        for (int i = 0; i < feature1.length; i++) {
            distance += Math.pow(feature1[i] - feature2[i], 2);
        }
        return Math.sqrt(distance);
    }
}
