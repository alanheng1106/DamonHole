package com.example.damonhole;

import android.content.ComponentName;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.example.damonhole.ui.DynamicThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements DynamicThemeManager.ThemeChangeListener {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private BottomNavigationView bottomNav;

    private final Map<Integer, Fragment> fragments = new HashMap<>();
    private int currentSelectedId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigationView);

        // Initialize fragments
        fragments.put(R.id.nav_home, new HomeFragment());
        fragments.put(R.id.nav_playlist, new PlaylistsFragment());
        fragments.put(R.id.nav_now_playing, new NowPlayingFragment());
        fragments.put(R.id.nav_settings, new SettingsFragment());

        // Set initial fragment
        if (savedInstanceState == null) {
            switchFragment(R.id.nav_home);
        } else {
            currentSelectedId = savedInstanceState.getInt("selectedId", R.id.nav_home);
            bottomNav.setSelectedItemId(currentSelectedId);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });

        DynamicThemeManager.getInstance().addListener(this);

        // Handle Back button: if not on Home, go to Home
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentSelectedId != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    finish();
                }
            }
        });

        // Initialize Media3
        SessionToken token = new SessionToken(this, new ComponentName(this, AudioService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                notifyFragmentsControllerReady();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent != null && intent.hasExtra("targetTabId")) {
            int tabId = intent.getIntExtra("targetTabId", R.id.nav_home);
            bottomNav.setSelectedItemId(tabId);
        }
    }

    private void switchFragment(int targetId) {
        if (targetId == currentSelectedId
                && getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) {
            return;
        }

        Fragment targetFragment = fragments.get(targetId);
        if (targetFragment == null) return;

        int currentPos = getPosition(currentSelectedId);
        int targetPos  = getPosition(targetId);

        int enterAnim, exitAnim;
        if (targetPos > currentPos) {
            // Forward: slide in from right, parallax exit left
            enterAnim = R.anim.slide_in_right;
            exitAnim  = R.anim.slide_out_left;
        } else {
            // Backward: slide in from left (parallax return), slide out right
            enterAnim = R.anim.slide_in_left;
            exitAnim  = R.anim.slide_out_right;
        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(enterAnim, exitAnim)
                .replace(R.id.fragmentContainer, targetFragment)
                .commit();

        currentSelectedId = targetId;
    }

    private int getPosition(int navId) {
        if (navId == R.id.nav_home)
            return 0;
        if (navId == R.id.nav_playlist)
            return 1;
        if (navId == R.id.nav_now_playing)
            return 2;
        if (navId == R.id.nav_settings)
            return 3;
        return 0;
    }

    public MediaController getMusicController() {
        return controller;
    }

    private void notifyFragmentsControllerReady() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current instanceof BaseTabFragment) {
            ((BaseTabFragment) current).onMediaControllerReady(controller);
        }
    }

    @Override
    public void onThemeChanged(DynamicThemeManager.AppPalette palette) {
        if (palette == null) {
            // Revert to default
            findViewById(android.R.id.content).setBackgroundColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            bottomNav.setBackgroundResource(R.color.nav_bg_default); // Assuming there's a default
            return;
        }

        // Apply to Activity Root
        findViewById(android.R.id.content).setBackgroundColor(palette.surfaceContainer);
        getWindow().setStatusBarColor(palette.surfaceContainer);

        // Apply to Bottom Nav
        bottomNav.setBackgroundColor(palette.surfaceContainer);
        
        android.content.res.ColorStateList navColorList = new android.content.res.ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                },
                new int[] {
                        palette.primaryContainer,
                        palette.onSurfaceVariant
                });
        bottomNav.setItemIconTintList(navColorList);
        bottomNav.setItemTextColor(navColorList);
        bottomNav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(palette.primaryContainer).withAlpha(31));
    }

    public void updateBottomNavColors(int bgColor, int textColor, int subTextColor) {
        // This method is now legacy, using onThemeChanged instead.
        // But keeping it for now to avoid breaking existing calls before Refactor.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedId", currentSelectedId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaController.releaseFuture(controllerFuture);
    }
}
