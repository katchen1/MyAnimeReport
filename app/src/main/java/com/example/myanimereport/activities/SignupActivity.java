package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.example.myanimereport.databinding.ActivitySignupBinding;
import com.parse.ParseUser;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /* Returns to the login page. */
    public void logInOnClick(View v) {
        Intent i = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    /* Signs up with the provided username and password. */
    public void signUpOnClick(View view) {
        String username = binding.etUsername.getText().toString();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // Check password confirmation
        if (!confirmPassword.equals(password)) {
            Toast.makeText(this, "Password fields must be identical.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for empty inputs
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        signUpUser(username, password);
    }

    /* Tries to sign up in the parse database. Navigates to the main activity on success. */
    public void signUpUser(String username, String password) {
        // Create the ParseUser
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setPassword(password);

        // Invoke signUpInBackground
        user.signUpInBackground(e -> {
            if (e == null) {
                goMainActivity();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Navigates to the main activity. */
    private void goMainActivity() {
        Intent i = new Intent(SignupActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}