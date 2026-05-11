package com.example.damonhole;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends BaseTabFragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvUserEmail;
    private com.google.android.material.materialswitch.MaterialSwitch switchDarkMode;
    private SharedPreferences themePrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        themePrefs = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

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

        view.findViewById(R.id.itemUserProfile).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileActivity.class));
        });
        view.findViewById(R.id.itemChangePassword).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ChangePasswordActivity.class));
        });
        view.findViewById(R.id.itemFAQs).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), FAQActivity.class));
        });
        view.findViewById(R.id.itemLanguage).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LanguageActivity.class));
        });
        view.findViewById(R.id.itemCheckUpdate).setOnClickListener(v -> {
            new UpdateManager(requireActivity()).checkForUpdates(true);
        });
        view.findViewById(R.id.itemEqualizer).setOnClickListener(v -> {
            Intent intent = new Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            SharedPreferences prefs = requireContext().getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE);
            int sessionId = prefs.getInt("audio_session_id", 0);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, requireContext().getPackageName());
            intent.putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC);
            try {
                startActivityForResult(intent, 0);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(requireContext(), R.string.system_eq_not_found, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void fetchUserInfo() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            tvUserEmail.setText(mAuth.getCurrentUser().getEmail());
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (isAdded() && documentSnapshot.exists()) {
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
        LikedSongsManager.getInstance(requireContext()).clearLocalData();
        PlaylistManager.getInstance(requireContext()).clearLocalData();
        Intent intent = new Intent(requireContext(), WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onThemeChanged(com.example.damonhole.ui.DynamicThemeManager.AppPalette palette) {
        if (getView() == null) return;

        if (palette == null) {
            getView().setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tvUserName.setTextColor(android.graphics.Color.WHITE);
            return;
        }

        getView().setBackgroundColor(palette.surfaceContainer);
        tvUserName.setTextColor(palette.onSurface);
        tvUserEmail.setTextColor(palette.onSurfaceVariant);
    }
}
