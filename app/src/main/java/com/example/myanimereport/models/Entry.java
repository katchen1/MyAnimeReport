package com.example.myanimereport.models;

import androidx.annotation.NonNull;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaDetailsByIdListQuery;
import com.example.MediaDetailsByIdQuery;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/* Entry (Parse model). */
@ParseClassName("Entry")
public class Entry extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_MONTH_WATCHED = "monthWatched";
    public static final String KEY_YEAR_WATCHED = "yearWatched";
    public static final String KEY_RATING = "rating";
    public static final String KEY_NOTE = "note";
    public static final String KEY_USER_ID = "userId";

    private Anime anime;

    /* Default constructor required by Parse. */
    public Entry() { }

    /* Alternative constructor. */
    public Entry(Integer mediaId, Integer monthWatched, Integer yearWatched, Double rating, String note) {
        setUser(ParseUser.getCurrentUser());
        setUserId(ParseUser.getCurrentUser().getObjectId());
        setMediaId(mediaId);
        setMonthWatched(monthWatched);
        setYearWatched(yearWatched);
        setRating(rating);
        setNote(note);
    }

    /* Queries anime of an entry. */
    public void setAnime(Runnable callback) {
        ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(getMediaId())).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                    anime = new Anime(response);
                    callback.run();
                }

                @Override
                public void onFailure(@NonNull ApolloException e) { }
            }
        );
    }

    /* Queries animes of a list of entries. */
    public static void setAnimes(List<Entry> entries, Runnable callback) {
        List<Integer> ids = new ArrayList<>();
        for (Entry entry: entries) ids.add(entry.getMediaId());
        queryAnimes(1, ids, entries, callback);
    }

    /* Queries animes by a list of ids. */
    public static void queryAnimes(int page, List<Integer> ids, List<Entry> entries, Runnable callback) {
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
                        ParseApplication.seenMediaIds.add(anime.getMediaId());
                        int index = ids.indexOf(anime.getMediaId());
                        entries.get(index).setAnime(anime);
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1, ids, entries, callback);
                    } else {
                        callback.run();
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) { }
            }
        );
    }

    /* Getters and setters. */
    public Anime getAnime() {
        return anime;
    }

    public void setAnime(Anime anime) {
        this.anime = anime;
    }

    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }

    public String getUserId() {
        return getString(KEY_USER_ID);
    }

    public void setUserId(String userId) {
        put(KEY_USER_ID, userId);
    }

    public Integer getMediaId() {
        return getInt(KEY_MEDIA_ID);
    }

    public void setMediaId(Integer mediaId) {
        put(KEY_MEDIA_ID, mediaId);
    }

    public Integer getMonthWatched() {
        return getInt(KEY_MONTH_WATCHED);
    }

    public void setMonthWatched(Integer monthWatched) {
        put(KEY_MONTH_WATCHED, monthWatched);
    }

    public Integer getYearWatched() {
        return getInt(KEY_YEAR_WATCHED);
    }

    public void setYearWatched(Integer monthWatched) {
        put(KEY_YEAR_WATCHED, monthWatched);
    }

    public Double getRating() {
        return getDouble(KEY_RATING);
    }

    public void setRating(Double rating) {
        put(KEY_RATING, rating);
    }

    public String getNote() {
        return getString(KEY_NOTE);
    }

    public void setNote(String note) {
        put(KEY_NOTE, note);
    }

    public YearMonth getDateWatched() {
        return YearMonth.of(getYearWatched(), getMonthWatched());
    }

    /* Two entries are the same if they refer to the same anime. */
    public boolean equals(Object object) {
        if (getClass() != object.getClass()) return false;
        return ((Entry) object).getMediaId().equals(getMediaId());
    }
}