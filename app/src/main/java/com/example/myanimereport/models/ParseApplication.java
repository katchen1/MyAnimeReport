package com.example.myanimereport.models;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.apollographql.apollo.ApolloClient;
import com.parse.Parse;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.List;

public class ParseApplication extends Application {

    public static ApolloClient apolloClient;
    public static Activity currentActivity;
    public static List<Entry> entries;
    public static List<BacklogItem> backlogItems;
    public static List<Integer> seenMediaIds;

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

        // List of entries shared between all activity/fragments
        entries = new ArrayList<>();

        // List of backlog items shared between all activities/fragments
        backlogItems = new ArrayList<>();

        // List of seen media ids shared between all activities/fragments
        seenMediaIds = new ArrayList<>();

        // Register callback to keep the current activity updated
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) { }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) { }

            @Override
            public void onActivityPaused(@NonNull Activity activity) { }

            @Override
            public void onActivityStopped(@NonNull Activity activity) { }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }
}