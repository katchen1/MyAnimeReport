package com.example.myanimereport.models;

/* BacklogItem (Parse model). */
public class BacklogItem {
    Integer mediaId; // The AniList mediaId of the anime in the backlog

    public BacklogItem() {
        mediaId = 155;
    }

    /* Getters. */
    public Anime getAnime() {
        return new Anime(); // Change this to query the anime with mediaId
    }

    public Integer getMediaId() {
        return mediaId;
    }
}