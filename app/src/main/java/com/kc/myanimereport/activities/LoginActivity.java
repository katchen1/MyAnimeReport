package com.kc.myanimereport.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.kc.myanimereport.R;
import com.kc.myanimereport.databinding.ActivityLoginBinding;
import com.kc.myanimereport.databinding.ForgotPasswordBinding;
import com.kc.myanimereport.utils.CustomAlertDialog;
import com.kc.myanimereport.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.ParseUser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Utils.togglePasswordVisibility(binding.etPassword, binding.btnVisibility);
    }

    /* Allows the user to send themselves a password reset email. */
    private void forgotPasswordOnClick(View view) {
        ForgotPasswordBinding dialogBinding = ForgotPasswordBinding.inflate(getLayoutInflater());
        AlertDialog alert = new MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.getRoot())
            .setTitle("Forgot password?")
            .setPositiveButton("Send", (dialog, which) -> {
                String email = dialogBinding.etEmail.getText().toString();
                sendPasswordResetEmail(email);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
            .create();
        alert.show();
        CustomAlertDialog.style(alert, getApplicationContext());
    }

    /* Sends a password reset email to the specified email address. */
    private void sendPasswordResetEmail(String email) {
        ParseUser.requestPasswordResetInBackground(email, e -> {
            if (!isEmailValid(email)) {
                Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show();
            } else if (e == null) {
                Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Checks is an email address is valid. */
    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
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

    /* Navigates the the sign up activity. */
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
