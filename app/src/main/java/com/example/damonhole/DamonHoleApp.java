package com.example.damonhole;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class DamonHoleApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply Material 3 Dynamic Colors globally to all activities
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
