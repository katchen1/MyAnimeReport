package com.example.myanimereport.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myanimereport.R;
import com.example.myanimereport.databinding.ActivityLoginBinding;
import com.example.myanimereport.databinding.ForgotPasswordBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.ParseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hide action bar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Set up focus change and click listeners
        binding.etUsername.setOnFocusChangeListener(this::etOnChangeFocus);
        binding.etPassword.setOnFocusChangeListener(this::etOnChangeFocus);

        // Check if user is already logged in
        if (ParseUser.getCurrentUser() != null) goMainActivity();

        // Forgot password
        binding.tvForgotPassword.setOnClickListener(this::forgotPasswordOnClick);

        // Change status bar color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this ,R.color.dark_gray));
    }

    /* Shows or hides the password. */
    public void togglePasswordVisibility(View view) {
        // Get current visibility
        PasswordTransformationMethod ptm = PasswordTransformationMethod.getInstance();
        HideReturnsTransformationMethod hrtm = HideReturnsTransformationMethod.getInstance();
        boolean invisible = binding.etPassword.getTransformationMethod() == ptm;

        // Show or hide based on current visibility
        if (invisible) {
            binding.btnVisibility.setImageResource(R.drawable.ic_baseline_visibility_24);
            binding.etPassword.setTransformationMethod(hrtm);
        } else {
            binding.btnVisibility.setImageResource(R.drawable.ic_baseline_visibility_off_24);
            binding.etPassword.setTransformationMethod(ptm);
        }

        // Move cursor to end of text
        binding.etPassword.setSelection(binding.etPassword.getText().length());
    }

    /* Allows the user to send themselves a password reset email. */
    private void forgotPasswordOnClick(View view) {
        ForgotPasswordBinding dialogBinding = ForgotPasswordBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.getRoot())
            .setTitle("Forgot Password?")
            .setPositiveButton("Send", (dialog, which) -> {
                String email = dialogBinding.etEmail.getText().toString();
                sendPasswordResetEmail(email);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .show();
    }

    /* Sends a password reset email to the specified email address. */
    private void sendPasswordResetEmail(String email) {
        ParseUser.requestPasswordResetInBackground(email, e -> {
            if (e == null) {
                Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
