package com.example.damonhole;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.media3.session.MediaController;
import com.example.damonhole.ui.DynamicThemeManager;

/**
 * Base fragment for all top-level tab fragments.
 * Provides easy access to the host activity's MediaController.
 */
public abstract class BaseTabFragment extends Fragment implements DynamicThemeManager.ThemeChangeListener {
    
    public MediaController getMusicController() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getMusicController();
        }
        return null;
    }

    /**
     * Called when the MediaController is ready in the host activity.
     */
    public void onMediaControllerReady(MediaController controller) {
        // To be overridden by subclasses
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DynamicThemeManager.getInstance().addListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DynamicThemeManager.getInstance().removeListener(this);
    }

    @Override
    public void onThemeChanged(DynamicThemeManager.AppPalette palette) {
        // To be overridden by subclasses
    }
}
