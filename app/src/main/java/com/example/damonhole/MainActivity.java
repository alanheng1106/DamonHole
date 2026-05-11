package com.example.damonhole;

import android.content.ComponentName;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

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

        // Get the currently displayed fragment (null on first call)
        Fragment currentFragment = fragments.get(currentSelectedId);
        if (currentFragment == targetFragment) currentFragment = null;

        int currentPos = getPosition(currentSelectedId);
        int targetPos  = getPosition(targetId);
        boolean isForward = targetPos > currentPos;

        NavigationTransitions.INSTANCE.switchTab(
                getSupportFragmentManager(),
                R.id.fragmentContainer,
                currentFragment,
                targetFragment,
                isForward
        );

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

    public void updateBottomNavColors(int bgColor, int textColor, int subTextColor) {
        bottomNav.setBackgroundColor(bgColor);
        android.content.res.ColorStateList navColorList = new android.content.res.ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                },
                new int[] {
                        textColor,
                        subTextColor
                });
        bottomNav.setItemIconTintList(navColorList);
        bottomNav.setItemTextColor(navColorList);
        bottomNav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(textColor).withAlpha(31));
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
