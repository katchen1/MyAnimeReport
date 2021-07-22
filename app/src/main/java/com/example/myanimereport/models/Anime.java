package com.example.myanimereport.models;

import android.graphics.Color;

import com.apollographql.apollo.api.Response;
import com.example.MediaDetailsByIdQuery;
import com.example.fragment.MediaFragment;
import org.parceler.Parcel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* A media object from the AniList API. */
@Parcel
public class Anime {
    Integer mediaId; // Unique id for the anime in the AniList database
    String titleEnglish; // The official English title
    String titleRomaji; // Romanization of the native language title
    String titleNative; // Official title in its native language
    String description; // Short description of the media’s story and characters
    Double averageScore; // A weighted average score of all the user’s scores of the media
    Integer seasonYear; // The season year the media was initially released in
    String coverImage; // URL of the cover image of the media
    String bannerImage; // URL of the banner image of the media
    List<String> genres; // List of genres of the media
    String color; // Primary color of the cover image
    Integer episodes; // Number of episodes

    /* Default constructor. */
    public Anime() {
        mediaId = 155;
        titleEnglish = "Case Closed";
        titleRomaji = "Meitantei Conan";
        titleNative = "名探偵コナン";
        description = "The story follows the high school detective Shinichi Kudo who was transformed" +
                " into a child while investigating a mysterious organization and solves a multitude " +
                "of cases while impersonating his childhood best friend's father and other characters.";
        averageScore = 9.8;
        seasonYear = 1994;
        coverImage = "https://i.pinimg.com/originals/ad/b5/cd/adb5cdc9cd0c353f23cb04ca58d7a17d.png";
        bannerImage = "https://i.pinimg.com/originals/7d/4a/f2/7d4af24e4a4578f9783dcd61f080fdda.jpg";
        genres = new ArrayList<>();
        genres.add("drama");
        genres.add("science fiction");
        color = "#000000";
        episodes = 1000;
    }

    /* Alternative constructor. */
    public Anime(Integer mediaId, String titleEnglish, String titleRomaji, String titleNative,
                 String description, Double averageScore, Integer seasonYear, String coverImage,
                 String bannerImage, List<String> genres, String color, Integer episodes) {
        this.mediaId = mediaId;
        this.titleEnglish = titleEnglish;
        this.titleRomaji = titleRomaji;
        this.titleNative = titleNative;
        this.description = description;
        this.averageScore = averageScore;
        this.seasonYear = seasonYear;
        this.coverImage = coverImage;
        this.bannerImage = bannerImage;
        this.genres = genres;
        this.color = color;
        this.episodes = episodes;
    }

    /* Alternative constructor (from GraphQL response object). */
    public Anime(Response<MediaDetailsByIdQuery.Data> response) {

        MediaFragment media = Objects.requireNonNull(
                Objects.requireNonNull(response.getData()).Media()).fragments().mediaFragment();
        mediaId = media.id();
        this.titleEnglish = Objects.requireNonNull(media.title()).english();
        this.titleRomaji = Objects.requireNonNull(media.title()).romaji();
        this.titleNative = Objects.requireNonNull(media.title()).native_();
        this.description = media.description();
        this.averageScore = Objects.requireNonNull(media.averageScore()) / 10.0;
        this.seasonYear = media.seasonYear();
        this.coverImage = Objects.requireNonNull(media.coverImage()).extraLarge();
        this.bannerImage = media.bannerImage();
        this.genres = media.genres();
        this.color = Objects.requireNonNull(media.coverImage()).color();
        this.episodes = media.episodes();
    }

    /* Alternative constructor (from MediaFragment). */
    public Anime(MediaFragment media) {
        mediaId = media.id();
        this.titleEnglish = Objects.requireNonNull(media.title()).english();
        this.titleRomaji = Objects.requireNonNull(media.title()).romaji();
        this.titleNative = Objects.requireNonNull(media.title()).native_();
        this.description = media.description();
        this.averageScore = media.averageScore() != null? media.averageScore() / 10.0: -1;
        this.seasonYear = media.seasonYear();
        this.coverImage = Objects.requireNonNull(media.coverImage()).extraLarge();
        this.bannerImage = media.bannerImage();
        this.genres = media.genres();
        this.color = Objects.requireNonNull(media.coverImage()).color();
        this.episodes = media.episodes();
    }

    /* Getters. */
    public Integer getMediaId() {
        return mediaId;
    }

    public String getTitleEnglish() {
        return titleEnglish != null? titleEnglish: titleRomaji;
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
        return coverImage != null? coverImage: bannerImage;
    }

    public String getBannerImage() {
        return bannerImage != null? bannerImage: coverImage;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Integer getColor() {
        return color != null? Color.parseColor(color): Color.parseColor("#EEEEEE");
    }

    public Integer getEpisodes() {
        return episodes;
    }
}