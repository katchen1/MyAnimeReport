package com.example.myanimereport.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/* UserGenre (Parse model). */
@ParseClassName("UserGenre")
public class UserGenre extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_GENRE = "genre";
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_RATING = "rating";

    /* Default constructor required by Parse. */
    public UserGenre() { }

    /* Getters and setters. */
    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }

    public String getGenre() {
        return getString(KEY_GENRE);
    }

    public void setGenre(String mediaId) {
        put(KEY_GENRE, mediaId);
    }

    public Integer getMediaId() {
        return getInt(KEY_MEDIA_ID);
    }

    public void setMediaId(Integer mediaId) {
        put(KEY_MEDIA_ID, mediaId);
    }

    public Double getRating() {
        return getDouble(KEY_RATING);
    }

    public void setRating(Double rating) {
        put(KEY_RATING, rating);
    }
}
