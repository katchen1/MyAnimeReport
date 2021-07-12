package com.example.myanimereport.models;

/* Entry (Parse model). */
public class Entry {
    Integer mediaId; //	The AniList mediaId of the anime watched
    Integer monthWatched; // The month when the user watched the anime
    Integer yearWatched; // The year when the user watched the anime
    Double rating; // The user’s rating of the anime (out of 10)
    String note; // An optional reflective note on the anime

    public Entry() {
        mediaId = 155;
        monthWatched = 1;
        yearWatched = 2012;
        rating = 10.0;
        note = "My favorite anime!";
    }

    /* Getters. */
    public Anime getAnime() {
        return new Anime(); // Change this to query the anime with mediaId
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public Integer getMonthWatched() {
        return monthWatched;
    }

    public Integer getYearWatched() {
        return yearWatched;
    }

    public Double getRating() {
        return rating;
    }

    public String getNote() {
        return note;
    }
}