package com.example.myanimereport.models;

import java.util.ArrayList;
import java.util.List;

/* A media object from the AniList API. */
public class Anime {
    private Integer mediaId; // Unique id for the anime in the AniList database
    private String title; // The official English title of the anime
    private String description; // Short description of the media’s story and characters
    private Double averageScore; // A weighted average score of all the user’s scores of the media
    private Integer seasonYear; // The season year the media was initially released in
    private String coverImage; // URL of the cover image of the media
    private String bannerImage; // URL of the banner image of the media
    private String trailerId; // Id of the media's Youtube trailer
    private List<String> genres; // List of genres of the media

    /* Default constructor. */
    public Anime() {
        mediaId = 155;
        title = "Detective Conan";
        description = "The story follows the high school detective Shinichi Kudo who was transformed" +
                " into a child while investigating a mysterious organization and solves a multitude " +
                "of cases while impersonating his childhood best friend's father and other characters.";
        averageScore = 9.8;
        seasonYear = 1994;
        coverImage = "https://i.pinimg.com/originals/ad/b5/cd/adb5cdc9cd0c353f23cb04ca58d7a17d.png";
        bannerImage = "https://i.pinimg.com/originals/7d/4a/f2/7d4af24e4a4578f9783dcd61f080fdda.jpg";
        trailerId = "https://www.youtube.com/watch?v=HSow7Ep6l_4";
        genres = new ArrayList<>();
        genres.add("drama");
        genres.add("science fiction");
    }

    /* Getters. */
    public Integer getMediaId() {
        return mediaId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public Integer getSeasonYear() {
        return seasonYear;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public String getTrailerId() {
        return trailerId;
    }

    public List<String> getGenres() {
        return genres;
    }
}