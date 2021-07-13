package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.myanimereport.R;

public class EntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
    }

    public void saveOnClick(View v) {
        Toast.makeText(this, "Not implemented.", Toast.LENGTH_SHORT).show();

    }
}