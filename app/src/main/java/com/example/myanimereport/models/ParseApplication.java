package com.example.myanimereport.models;

import android.app.Application;
import android.util.Log;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.MediaDetailsByTitleQuery;
import com.parse.Parse;
import com.parse.ParseObject;
import org.jetbrains.annotations.NotNull;

public class ParseApplication extends Application {

    public static ApolloClient apolloClient;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the parse models
        ParseObject.registerSubclass(Entry.class);
        ParseObject.registerSubclass(BacklogItem.class);

        // Initialize the parse application
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("OdPlemxtlEabuWLwikXOLhel5YXJWdzJNMDrrrVn")
                .clientKey("twJERNUXhCGFVxzruL5KfoWwlGjgsKLFYlgS8ugJ")
                .server("https://parseapi.back4app.com")
                .build()
        );

        // Initialize the Apollo client
        apolloClient = ApolloClient.builder().serverUrl("https://graphql.anilist.co/post").build();

        // Run a simple test
        apolloClient.query(new MediaDetailsByTitleQuery("conan")).enqueue(
            new ApolloCall.Callback<MediaDetailsByTitleQuery.Data>() {
                @Override
                public void onResponse(@NotNull Response<MediaDetailsByTitleQuery.Data> response) {
                    Log.i("Apollo", "Response data: " + response.getData().toString());
                }

                @Override
                public void onFailure(@NotNull ApolloException e) {
                    Log.i("Apollo", "Error: ", e);
                }
            }
        );
    }
}