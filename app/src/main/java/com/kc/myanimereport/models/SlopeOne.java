package com.kc.myanimereport.models;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.kc.MediaAllQuery;
import com.kc.MediaDetailsByIdListQuery;
import com.kc.myanimereport.activities.MainActivity;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Slope One algorithm implementation. */
public class SlopeOne {

    private final List<Pair<Integer, Double>> predictedRatings = new ArrayList<>();
    private final List<Anime> shownAnimes;
    private final List<AnimePair> animePairs;
    private final KNN knn; // The KNN model
    private final Integer knnWeight; // Determines how much neighbor ratings will impact slope one
    private final Map<String, Map<Integer, Double>> neighborsData; // {userId: {mediaId: rating}}

    public SlopeOne(List<Anime> shownAnimes, KNN knn, int knnWeight) {
        this.knn = knn;
        this.knnWeight = knnWeight;
        this.shownAnimes = shownAnimes;
        animePairs = new ArrayList<>();
        neighborsData = new HashMap<>();

        // Start slope one calculations
        MainActivity.matchFragment.showProgressBar();
        getInputData();
    }

    /* Gets the precalculated data. */
    public void getInputData() {
        ParseQuery<AnimePair> query = ParseQuery.getQuery(AnimePair.class);
        query.findInBackground((pairs, e) -> {
            // Check for errors
            if (e != null) {
                return;
            }
            animePairs.addAll(pairs);
            getNearestNeighborsData();
        });
    }

    /* Gets the K nearest neighbors and the neighbors' ratings. */
    public void getNearestNeighborsData() {
        // Get neighbors
        List<String> neighborIds = knn.kNearestNeighbors(ParseUser.getCurrentUser().getObjectId());

        // Get neighbor ratings
        ParseQuery<Entry> query = ParseQuery.getQuery(Entry.class); // Specify type of data
        query.whereContainedIn("userId", neighborIds); // Limit user to neighbors
        query.findInBackground((entries, e) -> { // Start async query for entries
            // Check for errors
            if (e != null) {
                return;
            }

            for (Entry entry: entries) {
                String userId = entry.getUserId();
                Integer mediaId = entry.getMediaId();
                Double rating = entry.getRating();

                // Store the neighbor anime ratings
                Map<Integer, Double> userData = neighborsData.get(userId);
                if (userData == null) {
                    userData = new HashMap<>();
                    userData.put(mediaId, rating);
                    neighborsData.put(userId, userData);
                } else {
                    userData.put(mediaId, rating);
                }
            }
            predict();
        });
    }

    /* Predicts the current user's ratings on unseen animes. */
    public void predict() {
        if (ParseApplication.entries.isEmpty()) {
            weaveInRandomAnimes();
            return;
        }

        // For all unseen animes
        for (Integer toPredict: ParseApplication.entryMediaIdAllUsers) {
            if (!ParseApplication.seenMediaIds.contains(toPredict)) {

                // Predict a rating based on each seen anime
                List<Pair<Double, Integer>> ratingWeightPairs = new ArrayList<>();
                for (AnimePair pair: animePairs) {

                    // Retrieve precalculated count and diff for slope one
                    if (pair.getMediaId1().equals(toPredict) || pair.getMediaId2().equals(toPredict)) {
                        int sign = pair.getMediaId1().equals(toPredict) ? 1 : -1;
                        Integer basedOn = sign == 1 ? pair.getMediaId2() : pair.getMediaId1();

                        // Generate the predicted rating based on a seen anime using average difference
                        for (Entry entry : ParseApplication.entries) {
                            if (entry.getMediaId().equals(basedOn)) {
                                double diffAvg = pair.getDiffSum() / pair.getCount();
                                Double predictedRating = entry.getRating() + sign * diffAvg;
                                ratingWeightPairs.add(new Pair<>(predictedRating, pair.getCount()));
                                incorporateKNN(toPredict, basedOn, entry, ratingWeightPairs);
                            }
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
            }
        }

        // Sort by predicted rating (descending)
        predictedRatings.sort((p1, p2) -> p2.second.compareTo(p1.second));
        removeRejections();
    }

    /* Increases weight of predicted ratings based on K nearest neighbors. */
    private void incorporateKNN(Integer toPredict, Integer basedOn, Entry entry,
                                List<Pair<Double, Integer>> ratingWeightPairs) {
        // For each neighbor
        for (String neighborId: neighborsData.keySet()) {
            Map<Integer, Double> neighborData = neighborsData.get(neighborId);
            if (neighborData != null) {
                Double toPredictRating = neighborData.get(toPredict);
                Double basedOnRating = neighborData.get(basedOn);

                // Predict the current user's rating based on neighbor's rating difference
                if (toPredictRating != null && basedOnRating != null) {
                    Double diff = toPredictRating - basedOnRating;
                    Double predictedRating = entry.getRating() + diff;

                    // Give it a higher weight
                    ratingWeightPairs.add(new Pair<>(predictedRating, knnWeight));
                }
            }
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

            // Remove recommendations with 3 or more rejections
            predictedRatings.removeIf((p) -> rejections.contains(p.first));
            shownAnimes.clear();
            queryAnimes(1);
        });
    }

    /* Query animes of a list of anime ids. */
    public void queryAnimes(int page) {
        List<Integer> ids = new ArrayList<>();
        List<Double> ratings = new ArrayList<>();
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
                        shownAnimes.add(anime);
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1);
                    } else {
                        shownAnimes.sort((a1, a2) -> ids.indexOf(a1.getMediaId()) - ids.indexOf(a2.getMediaId()));
                        weaveInRandomAnimes();
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) { }
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
                    randomAnimes.removeIf((a) -> ParseApplication.seenMediaIds.contains(a.getMediaId()));
                    randomAnimes.removeIf(shownAnimes::contains);
                    Collections.shuffle(randomAnimes);

                    // Insert the random animes in the shown animes
                    int index = 0;
                    for (int i = 0; i < randomAnimes.size(); i++) {
                        if (shownAnimes.size() > index) shownAnimes.add(index, randomAnimes.get(i));
                        else shownAnimes.add(randomAnimes.get(i));

                        // 2 random, 1 non-random, 2 random, 1 non-random, ...
                        index++;
                        if ((index + 1) % 3 == 0) index++;
                    }

                    // Lightly shuffle the shown animes
                    partialShuffle(shownAnimes);

                    // Notify the adapter
                    ParseApplication.currentActivity.runOnUiThread(() -> {
                        MainActivity.matchFragment.getAdapter().notifyDataSetChanged();
                        MainActivity.matchFragment.hideProgressBar();
                    });
                }

                @Override
                public void onFailure(@NonNull ApolloException e) { }
            }
        );
    }

    /* Lightly shuffles a list by swapping random elements.
     * Elements swapped must be close enough to each other (indices less than 3 apart). */
    public void partialShuffle(List<Anime> animes) {
        int n = animes.size();
        int numSwaps = n / 2;
        for (int i = 0; i < numSwaps; i++) {
            // Indices of elements to swap
            int index1 = (int) (Math.random() * n);
            int dist = (int) (Math.random() * 7) - 3; // {-3, -2, 1, 0, 1, 2, 3}
            int index2 = index1 + dist;
            index2 = Math.max(0, Math.min(n - 1, index2)); // Validate index by clamping

            // Swap
            Anime temp = animes.get(index1);
            animes.set(index1, animes.get(index2));
            animes.set(index2, temp);
        }
    }
}