package com.kc.myanimereport.models;

import androidx.annotation.NonNull;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.kc.MediaDetailsByIdListQuery;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/* BacklogItem (Parse model). */
@ParseClassName("BacklogItem")
public class BacklogItem extends ParseObject {
    public static final String KEY_USER = "user";
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_CREATION_DATE = "creationDate";

    private Anime anime;

    /* Default constructor required by Parse. */
    public BacklogItem() { }

    /* Queries the animes of a list of backlog items. */
    public static void setAnimes(List<BacklogItem> items, Runnable callback) {
        List<Integer> ids = new ArrayList<>();
        for (BacklogItem item: items) ids.add(item.getMediaId());
        queryAnimes(1, ids, items, callback);
    }

    /* Queries animes by a list of ids. */
    public static void queryAnimes(int page, List<Integer> ids, List<BacklogItem> items, Runnable callback) {
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
                        items.get(index).setAnime(anime);
                    }

                    // Next page
                    if (response.getData().Page().pageInfo().hasNextPage()) {
                        queryAnimes(page + 1, ids, items, callback);
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

    public Integer getMediaId() {
        return getInt(KEY_MEDIA_ID);
    }

    public void setMediaId(Integer mediaId) {
        put(KEY_MEDIA_ID, mediaId);
    }

    public Date getCreationDate() {
        return getDate(KEY_CREATION_DATE);
    }

    public void setCreationDate(Date date) {
        put(KEY_CREATION_DATE, date);
    }

    /* Two backlog items are the same if they refer to the same anime. */
    public boolean equals(Object object) {
        if (getClass() != object.getClass()) return false;
        return ((BacklogItem) object).getMediaId().equals(getMediaId());
    }
}