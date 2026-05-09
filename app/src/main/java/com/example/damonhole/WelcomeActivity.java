package com.example.damonhole;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        // Apply theme preference
        boolean isDarkMode = getSharedPreferences("theme_prefs", MODE_PRIVATE).getBoolean("isDarkMode", true);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnLanguage).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LanguageActivity.class));
        });
    }
}