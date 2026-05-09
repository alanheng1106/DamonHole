package com.example.damonhole;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.color.DynamicColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvUserEmail;
    private com.google.android.material.materialswitch.MaterialSwitch switchDarkMode;
    private SharedPreferences themePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Dark Mode Logic
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", true);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themePrefs.edit().putBoolean("isDarkMode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        fetchUserInfo();

        // M3 nav bar: set up once in onCreate, never again in onResume
        NavigationUtils.setupBottomNav(this);

        // Clicks
        findViewById(R.id.itemUserProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
        findViewById(R.id.itemChangePassword).setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });
        findViewById(R.id.itemFAQs).setOnClickListener(v -> {
            startActivity(new Intent(this, FAQActivity.class));
        });
        findViewById(R.id.itemLanguage).setOnClickListener(v -> {
            startActivity(new Intent(this, LanguageActivity.class));
        });
        findViewById(R.id.itemCheckUpdate).setOnClickListener(v -> {
            new UpdateManager(this).checkForUpdates(true);
        });
        findViewById(R.id.itemEqualizer).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            android.content.SharedPreferences prefs = getSharedPreferences("equalizer_prefs", MODE_PRIVATE);
            int sessionId = prefs.getInt("audio_session_id", 0);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC);
            try {
                startActivityForResult(intent, 0);
            } catch (android.content.ActivityNotFoundException e) {
                android.widget.Toast.makeText(this, R.string.system_eq_not_found, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationUtils.syncBottomNavSelection(this);
    }

    private void fetchUserInfo() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            tvUserEmail.setText(mAuth.getCurrentUser().getEmail());
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                tvUserName.setText(user.getName());
                            }
                        }
                    });
        }
    }

    private void logout() {
        mAuth.signOut();
        LikedSongsManager.getInstance(this).clearLocalData();
        PlaylistManager.getInstance(this).clearLocalData();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}