package com.example.damonhole.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import java.util.ArrayList;
import java.util.List;

public class DynamicThemeManager {

    public static class AppPalette {
        @ColorInt public final int primaryContainer;
        @ColorInt public final int onPrimaryContainer;
        @ColorInt public final int surfaceContainer;
        @ColorInt public final int onSurface;
        @ColorInt public final int onSurfaceVariant;
        @ColorInt public final int accent;
        @ColorInt public final int accentContrast;
        @ColorInt public final int divider;
        public final boolean isDark;

        public AppPalette(int primaryContainer, int onPrimaryContainer, int surfaceContainer, int onSurface, int onSurfaceVariant, int accent, int accentContrast, int divider, boolean isDark) {
            this.primaryContainer = primaryContainer;
            this.onPrimaryContainer = onPrimaryContainer;
            this.surfaceContainer = surfaceContainer;
            this.onSurface = onSurface;
            this.onSurfaceVariant = onSurfaceVariant;
            this.accent = accent;
            this.accentContrast = accentContrast;
            this.divider = divider;
            this.isDark = isDark;
        }
    }

    public interface ThemeChangeListener {
        void onThemeChanged(AppPalette palette);
    }

    private static DynamicThemeManager instance;
    private AppPalette currentPalette;
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    private DynamicThemeManager() {}

    public static synchronized DynamicThemeManager getInstance() {
        if (instance == null) {
            instance = new DynamicThemeManager();
        }
        return instance;
    }

    public void addListener(ThemeChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            if (currentPalette != null) {
                listener.onThemeChanged(currentPalette);
            }
        }
    }

    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    public void updatePalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;

            // Extract core colors
            int dominant = palette.getDominantColor(Color.parseColor("#121212"));
            int vibrant = palette.getVibrantColor(palette.getMutedColor(dominant));
            
            // M3-like role derivation
            boolean isDarkBackground = ColorUtils.calculateLuminance(vibrant) < 0.5;
            
            int primaryContainer = vibrant;
            int onPrimaryContainer = getContrastColor(primaryContainer);
            int surfaceContainer = ColorUtils.blendARGB(vibrant, isDarkBackground ? Color.BLACK : Color.WHITE, 0.85f);
            int onSurface = getContrastColor(surfaceContainer);
            int onSurfaceVariant = ColorUtils.blendARGB(onSurface, surfaceContainer, 0.3f);
            int accent = palette.getLightVibrantColor(palette.getLightMutedColor(vibrant));
            int accentContrast = getContrastColor(accent);
            int divider = ColorUtils.blendARGB(onSurface, surfaceContainer, 0.8f);

            currentPalette = new AppPalette(
                primaryContainer,
                onPrimaryContainer,
                surfaceContainer,
                onSurface,
                onSurfaceVariant,
                accent,
                accentContrast,
                divider,
                isDarkBackground
            );

            notifyListeners();
        });
    }

    public void reset() {
        currentPalette = null;
        notifyListeners();
    }

    public AppPalette getCurrentPalette() {
        return currentPalette;
    }

    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            listener.onThemeChanged(currentPalette);
        }
    }

    @ColorInt
    private int getContrastColor(@ColorInt int color) {
        boolean isDark = ColorUtils.calculateLuminance(color) < 0.5;
        if (isDark) {
            // Return a very light version of the color or white
            return ColorUtils.blendARGB(color, Color.WHITE, 0.9f);
        } else {
            // Return a very dark version of the color or black
            return ColorUtils.blendARGB(color, Color.BLACK, 0.8f);
        }
    }
}
