package com.example.myanimereport.models;

import android.graphics.Color;
import com.apollographql.apollo.api.Response;
import com.example.MediaDetailsByIdQuery;
import com.example.fragment.MediaFragment;
import org.parceler.Parcel;
import java.util.List;
import java.util.Objects;

/* A media object from the AniList API. */
@Parcel
public class Anime {
    protected Integer mediaId; // Unique id for the anime in the AniList database
    protected String titleEnglish; // The official English title
    protected String titleRomaji; // Romanization of the native language title
    protected String titleNative; // Official title in its native language
    protected String description; // Short description of the media’s story and characters
    protected Double averageScore; // A weighted average score of all the user’s scores of the media
    protected Integer seasonYear; // The season year the media was initially released in
    protected String coverImage; // URL of the cover image of the media
    protected String bannerImage; // URL of the banner image of the media
    protected List<String> genres; // List of genres of the media
    protected String color; // Primary color of the cover image
    protected Integer episodes; // Number of episodes
    protected String siteUrl; // Url to AniList site

    /* Default constructor. */
    public Anime() {}

    /* Alternative constructor (from GraphQL response object). */
    public Anime(Response<MediaDetailsByIdQuery.Data> response) {

        MediaFragment media = Objects.requireNonNull(
                Objects.requireNonNull(response.getData()).Media()).fragments().mediaFragment();
        mediaId = media.id();
        this.titleEnglish = Objects.requireNonNull(media.title()).english();
        this.titleRomaji = Objects.requireNonNull(media.title()).romaji();
        this.titleNative = Objects.requireNonNull(media.title()).native_();
        this.description = media.description();
        this.averageScore = media.averageScore() != null? media.averageScore() / 10.0: -1.0;
        this.seasonYear = media.seasonYear();
        this.coverImage = Objects.requireNonNull(media.coverImage()).extraLarge();
        this.bannerImage = media.bannerImage();
        this.genres = media.genres();
        this.color = Objects.requireNonNull(media.coverImage()).color();
        this.episodes = media.episodes();
        this.siteUrl = media.siteUrl();
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
        this.siteUrl = media.siteUrl();
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

    public String getSiteUrl() {
        return siteUrl;
    }

    /* Two animes are the same if they have the same id. */
    public boolean equals(Object object) {
        if (getClass() != object.getClass()) return false;
        return ((Anime) object).getMediaId().equals(getMediaId());
    }
}