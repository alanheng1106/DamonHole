package com.example.damonhole;

import androidx.fragment.app.Fragment;
import androidx.media3.session.MediaController;

/**
 * Base fragment for all top-level tab fragments.
 * Provides easy access to the host activity's MediaController.
 */
public abstract class BaseTabFragment extends Fragment {
    
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
}
