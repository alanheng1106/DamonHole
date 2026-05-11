package com.example.damonhole;

import android.app.Activity;
import android.content.Intent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationUtils {

    /**
     * Set up bottom navigation for activities outside MainActivity.
     * Clicking an item will navigate back to MainActivity with the specified tab.
     */
    public static void setupBottomNav(Activity activity) {
        BottomNavigationView navView = activity.findViewById(R.id.bottomNavigationView);
        if (navView == null)
            return;

        ViewCompat.setOnApplyWindowInsetsListener(navView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        navView.setOnItemSelectedListener(item -> {
            navigateToTab(activity, item.getItemId());
            return true;
        });
    }

    public static void syncBottomNavSelection(Activity activity) {
        // No-op for Fragments as they are hosted in MainActivity which handles sync.
        // For secondary activities, we might not need this if they don't highlight tabs.
    }

    private static void navigateToTab(Activity activity, int tabId) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("targetTabId", tabId);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void setupBackToHome(androidx.activity.ComponentActivity activity) {
        activity.getOnBackPressedDispatcher().addCallback(activity, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
        });
    }
}