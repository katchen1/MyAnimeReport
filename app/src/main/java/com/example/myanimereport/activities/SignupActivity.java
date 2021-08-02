package com.example.myanimereport.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up focus change and click listeners
        binding.etUsername.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etEmail.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etPassword.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etConfirmPassword.setOnFocusChangeListener(this::etOnChangeFocus);
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
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // Check password confirmation
        if (!confirmPassword.equals(password)) {
            Toast.makeText(this, "Password fields must be identical.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for empty inputs
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Field cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        signUpUser(username, email, password);
    }

    /* Tries to sign up in the parse database. Navigates to the main activity on success. */
    public void signUpUser(String username, String email, String password) {
        // Create the ParseUser
        ParseUser user = new ParseUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        // Invoke signUpInBackground
        user.signUpInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Account created! Please verify email before logging in.", Toast.LENGTH_SHORT).show();
                ParseUser.logOut();
                goLoginActivity();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Navigates to the login activity. */
    private void goLoginActivity() {
        Intent i = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    /* When user clicks outside of the edit texts, hide the soft keyboard. */
    private void etOnChangeFocus(View view, boolean hasFocus) {
        if (!hasFocus) hideFocus(view);
    }

    /* Hides the soft keyboard. */
    public void hideFocus(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}