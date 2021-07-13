package com.example.myanimereport.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/* Entry (Parse model). */
@ParseClassName("Entry")
public class Entry extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_MONTH_WATCHED = "monthWatched";
    public static final String KEY_YEAR_WATCHED = "yearWatched";
    public static final String KEY_RATING = "rating";
    public static final String KEY_NOTE = "note";

    public Entry() {
        setUser(ParseUser.getCurrentUser());
        setMediaId(155); // The AniList mediaId of the anime watched
        setMonthWatched(1); // The month when the user watched the anime
        setYearWatched(2012); // The year when the user watched the anime
        setRating(10.0); // The user’s rating of the anime (out of 10)
        setNote("My favorite anime!"); // An optional reflective note on the anime
    }

    public Entry(Integer mediaId, Integer monthWatched, Integer yearWatched, Double rating, String note) {
        setUser(ParseUser.getCurrentUser());
        setMediaId(mediaId);
        setMonthWatched(monthWatched);
        setYearWatched(yearWatched);
        setRating(rating);
        setNote(note);
    }

    /* Getters and setters. */
    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
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

    public Anime getAnime() {
        return new Anime();
    }
}