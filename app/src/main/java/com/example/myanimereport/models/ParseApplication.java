package com.example.myanimereport.models;

import android.app.Application;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
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
    }
}