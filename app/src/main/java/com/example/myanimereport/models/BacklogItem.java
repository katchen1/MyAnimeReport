package com.example.myanimereport.models;

import android.util.Log;
import androidx.annotation.NonNull;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaDetailsByIdListQuery;
import com.example.MediaDetailsByIdQuery;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

/* BacklogItem (Parse model). */
@ParseClassName("BacklogItem")
public class BacklogItem extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_MEDIA_ID = "mediaId";

    private Anime anime;

    /* Default constructor required by Parse. */
    public BacklogItem() { }

    /* Getters and setters. */
    public Anime getAnime() {
        return anime;
    }

    public void setAnime() {
        ParseApplication.apolloClient.query(new MediaDetailsByIdQuery(getMediaId())).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdQuery.Data> response) {
                    anime = new Anime(response);
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage());
                }
            }
        );
    }

    public static void setAnimes(List<BacklogItem> items) {
        System.out.println("Set animes.");
        List<Integer> ids = new ArrayList<>();
        for (BacklogItem item: items) ids.add(item.getMediaId());
        ParseApplication.apolloClient.query(new MediaDetailsByIdListQuery(1, ids)).enqueue(
            new ApolloCall.Callback<MediaDetailsByIdListQuery.Data>() {
                @Override
                public void onResponse(@NonNull Response<MediaDetailsByIdListQuery.Data> response) {
                    if (response.getData().Page() == null) return;
                    if (response.getData().Page().media() == null) return;
                    for (MediaDetailsByIdListQuery.Medium m: response.getData().Page().media()) {
                        Anime anime = new Anime(m.fragments().mediaFragment());
                        ParseApplication.seenMediaIds.add(anime.getMediaId());
                        int index = ids.indexOf(anime.getMediaId());
                        items.get(index).setAnime(anime);
                    }
                }

                @Override
                public void onFailure(@NonNull ApolloException e) {
                    Log.e("Apollo", e.getMessage() + e.getCause());
                }
            }
        );
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

    public Integer getMediaId() {
        return getInt(KEY_MEDIA_ID);
    }

    public void setMediaId(Integer mediaId) {
        put(KEY_MEDIA_ID, mediaId);
    }
}