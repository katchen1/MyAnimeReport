package com.example.myanimereport.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myanimereport.databinding.ActivityLoginBinding;
import com.parse.ParseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Check if user is already logged in
        if (ParseUser.getCurrentUser() != null) goMainActivity();
    }

    /* Logs in with the provided username and password. */
    public void logInOnClick(View v) {
        String username = binding.etUsername.getText().toString();
        String password = binding.etPassword.getText().toString();
        loginUser(username, password);
    }

    /* Tries to log in to the parse database. Navigates to main activity on success. */
    private void loginUser(String username, String password) {
        ParseUser.logInInBackground(username, password, (user, e) -> {
            if (e != null) {
                Toast.makeText(this, "Username or password is incorrect.", Toast.LENGTH_SHORT).show();
                return;
            }
            goMainActivity();
        });
    }

    public void signUpOnClick(View view) {
        Intent i = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(i);
        finish();
    }

    /* Navigates to the main activity. */
    private void goMainActivity() {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
