package com.example.damonhole;

import android.app.Activity;
import android.content.Intent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationUtils {

    /**
     * Call from onCreate() to wire up the listener and edge-to-edge insets.
     * Also call from onResume() to re-sync the selected icon — needed because
     * REORDER_TO_FRONT skips onCreate on return, so the highlight must be
     * refreshed in onResume without re-attaching the listener.
     */
    public static void setupBottomNav(Activity activity) {
        BottomNavigationView navView = activity.findViewById(R.id.bottomNavigationView);
        if (navView == null) return;

        // Edge-to-edge: extend behind the system gesture bar
        ViewCompat.setOnApplyWindowInsetsListener(navView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Sync the highlighted tab to whichever Activity is currently in front.
        // Null the listener first so setSelectedItemId() doesn't trigger navigation.
        int targetId = getTargetId(activity);
        if (targetId != -1) {
            navView.setOnItemSelectedListener(null);
            navView.setSelectedItemId(targetId);
        }

        // Re-attach the real listener every time (safe — lambdas are cheap,
        // and this is the only way to have a fresh reference to `activity`).
        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (!(activity instanceof MainActivity)) navigateTo(activity, MainActivity.class);
                return true;
            } else if (id == R.id.nav_playlist) {
                if (!(activity instanceof PlaylistsActivity)) navigateTo(activity, PlaylistsActivity.class);
                return true;
            } else if (id == R.id.nav_now_playing) {
                if (!(activity instanceof NowPlayingActivity)) navigateTo(activity, NowPlayingActivity.class);
                return true;
            } else if (id == R.id.nav_settings) {
                if (!(activity instanceof SettingsActivity)) navigateTo(activity, SettingsActivity.class);
                return true;
            }
            return false;
        });
    }

    /**
     * Call from onResume() ONLY — re-syncs the selected icon without
     * re-doing the inset listener setup.
     */
    public static void syncBottomNavSelection(Activity activity) {
        BottomNavigationView navView = activity.findViewById(R.id.bottomNavigationView);
        if (navView == null) return;

        int targetId = getTargetId(activity);
        if (targetId != -1 && navView.getSelectedItemId() != targetId) {
            navView.setOnItemSelectedListener(null);
            navView.setSelectedItemId(targetId);
            // Re-attach listener so the nav still works after the sync
            navView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    if (!(activity instanceof MainActivity)) navigateTo(activity, MainActivity.class);
                    return true;
                } else if (id == R.id.nav_playlist) {
                    if (!(activity instanceof PlaylistsActivity)) navigateTo(activity, PlaylistsActivity.class);
                    return true;
                } else if (id == R.id.nav_now_playing) {
                    if (!(activity instanceof NowPlayingActivity)) navigateTo(activity, NowPlayingActivity.class);
                    return true;
                } else if (id == R.id.nav_settings) {
                    if (!(activity instanceof SettingsActivity)) navigateTo(activity, SettingsActivity.class);
                    return true;
                }
                return false;
            });
        }
    }

    private static int getTargetId(Activity activity) {
        if (activity instanceof MainActivity)       return R.id.nav_home;
        if (activity instanceof PlaylistsActivity)  return R.id.nav_playlist;
        if (activity instanceof NowPlayingActivity) return R.id.nav_now_playing;
        if (activity instanceof SettingsActivity)   return R.id.nav_settings;
        return -1;
    }

    private static void navigateTo(Activity activity, Class<?> targetClass) {
        Intent intent = new Intent(activity, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }
}