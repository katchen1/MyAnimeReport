package com.kc.myanimereport.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

/* Rejection (Parse model). */
@ParseClassName("Rejection")
public class Rejection extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_MEDIA_ID = "mediaId";

    /* Default constructor required by Parse. */
    public Rejection() { }

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
}