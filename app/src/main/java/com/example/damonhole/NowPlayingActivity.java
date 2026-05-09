package com.example.damonhole;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class NowPlayingActivity extends AppCompatActivity {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private View rootLayout;
    private ImageView ivArtwork;
    private TextView tvTitle, tvArtist, tvPosition, tvDuration;
    private FloatingActionButton btnPlayPause;
    private MaterialButton btnPrev, btnNext, btnShuffle, btnRepeat, btnLike, btnQueue;
    private Slider seekSlider;

    private ObjectAnimator artworkAnimator;

    private LikedSongsManager likedManager;

    private String currentVideoId = null;
    private String currentTitle   = null;
    private String currentAuthor  = null;

    // When the user is dragging the slider we pause the auto-updater
    private boolean userIsDragging = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            updateSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        setContentView(R.layout.activity_now_playing);

        likedManager = LikedSongsManager.getInstance(this);

        rootLayout   = findViewById(R.id.rootLayout);
        ivArtwork    = findViewById(R.id.ivArtwork);
        tvTitle      = findViewById(R.id.tvTitle);
        tvTitle.setSelected(true); // enable marquee
        tvArtist     = findViewById(R.id.tvArtist);
        tvArtist.setSelected(true); // enable marquee
        tvPosition   = findViewById(R.id.tvPosition);
        tvDuration   = findViewById(R.id.tvDuration);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrev      = findViewById(R.id.btnPrev);
        btnNext      = findViewById(R.id.btnNext);
        btnShuffle   = findViewById(R.id.btnShuffle);
        btnRepeat    = findViewById(R.id.btnRepeat);
        btnLike      = findViewById(R.id.btnLike);
        btnQueue     = findViewById(R.id.btnQueue);
        seekSlider   = findViewById(R.id.seekSlider);

        // Circular artwork clip
        ivArtwork.setClipToOutline(true);
        ivArtwork.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        setupControls();
        setupArtworkAnimation();

        // M3 nav bar: set up once in onCreate
        NavigationUtils.setupBottomNav(this);
    }

    private void setupArtworkAnimation() {
        artworkAnimator = ObjectAnimator.ofFloat(ivArtwork, View.ROTATION, 0f, 360f);
        artworkAnimator.setDuration(15000); // 15 seconds per rotation
        artworkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        artworkAnimator.setInterpolator(new LinearInterpolator());
    }

    private void toggleArtworkAnimation(boolean isPlaying) {
        if (artworkAnimator == null) return;
        if (isPlaying) {
            if (artworkAnimator.isPaused()) {
                artworkAnimator.resume();
            } else if (!artworkAnimator.isStarted()) {
                artworkAnimator.start();
            }
        } else {
            artworkAnimator.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationUtils.syncBottomNavSelection(this);
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.isPlaying()) controller.pause();
            else controller.play();
        });

        btnPrev.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.getCurrentPosition() > 3_000) controller.seekTo(0);
            else controller.seekToPreviousMediaItem();
        });

        btnNext.setOnClickListener(v -> {
            if (controller != null) controller.seekToNextMediaItem();
        });

        btnShuffle.setOnClickListener(v -> {
            if (controller == null) return;
            boolean shuffle = !controller.getShuffleModeEnabled();
            controller.setShuffleModeEnabled(shuffle);
            btnShuffle.setAlpha(shuffle ? 1.0f : 0.5f);
        });

        btnRepeat.setOnClickListener(v -> {
            if (controller == null) return;
            int next;
            switch (controller.getRepeatMode()) {
                case Player.REPEAT_MODE_OFF: next = Player.REPEAT_MODE_ONE; break;
                case Player.REPEAT_MODE_ONE: next = Player.REPEAT_MODE_ALL; break;
                default:                     next = Player.REPEAT_MODE_OFF; break;
            }
            controller.setRepeatMode(next);
            updateRepeatIcon(next);
        });

        btnLike.setOnClickListener(v -> {
            if (currentVideoId == null) return;
            SongItem current = new SongItem(currentVideoId, currentTitle, currentAuthor);
            boolean nowLiked = likedManager.toggle(current);
            updateLikeButton(nowLiked);
            Toast.makeText(this,
                    nowLiked ? R.string.added_to_liked : R.string.removed_from_liked,
                    Toast.LENGTH_SHORT).show();
        });

        // M3 Slider listeners
        seekSlider.setLabelFormatter(value -> formatMs((long) value));
        seekSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                userIsDragging = true;
                handler.removeCallbacks(seekUpdater);
            }
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (controller != null) {
                    controller.seekTo((long) slider.getValue());
                }
                userIsDragging = false;
                handler.post(seekUpdater);
            }
        });

        // Update time label while dragging showing exact timestamp
        seekSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                tvPosition.setText(formatMs((long) value));
            }
        });

        findViewById(R.id.btnQueue).setOnClickListener(v ->
                startActivity(new Intent(this, QueueActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, AudioService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                updateMetadataUI();
                updatePlayPauseIcon(controller.isPlaying());
                handler.post(seekUpdater);

                controller.addListener(new Player.Listener() {
                    @Override
                    public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                        updateMetadataUI();
                    }
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updatePlayPauseIcon(isPlaying);
                    }
                    @Override
                    public void onRepeatModeChanged(int repeatMode) {
                        updateRepeatIcon(repeatMode);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(seekUpdater);
        MediaController.releaseFuture(controllerFuture);
        super.onStop();
    }

    // -----------------------------------------------------------------------
    // UI updates
    // -----------------------------------------------------------------------
    private void updateMetadataUI() {
        if (controller == null || controller.getCurrentMediaItem() == null) return;
        MediaMetadata meta = controller.getCurrentMediaItem().mediaMetadata;

        // Reset slider to 0 for new song to prevent out-of-bounds if new song is shorter
        seekSlider.setValue(0);
        seekSlider.setValueTo(100); // Temporary default until duration is loaded

        currentTitle  = meta.title  != null ? meta.title.toString()  : getString(R.string.app_name);
        currentAuthor = meta.artist != null ? meta.artist.toString() : getString(R.string.music_fan);

        try {
            String id = controller.getCurrentMediaItem().mediaId;
            currentVideoId = (id != null && !id.isEmpty()) ? id : null;
        } catch (Exception ignored) {}

        tvTitle.setText(currentTitle);
        tvArtist.setText(currentAuthor);

        if (currentVideoId != null) {
            updateLikeButton(likedManager.isLiked(currentVideoId));
        }

        if (meta.artworkUri != null) {
            Glide.with(this)
                .asBitmap()
                .load(meta.artworkUri.toString())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ivArtwork.setImageBitmap(resource);
                        applyPalette(resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
        }
    }

    private void applyPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;
            
            // Extract a vibrant or muted color for the background
            int bgColor = palette.getVibrantColor(
                palette.getMutedColor(
                    palette.getDominantColor(Color.parseColor("#121212"))
                )
            );

            // Apply background color to rootLayout
            rootLayout.setBackgroundColor(bgColor);

            // Determine if background is light or dark to set text colors
            boolean isDark = isDark(bgColor);
            int textColor = isDark ? Color.WHITE : Color.BLACK;
            int subTextColor = isDark ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#B3000000");

            tvTitle.setTextColor(textColor);
            tvArtist.setTextColor(subTextColor);
            tvPosition.setTextColor(subTextColor);
            tvDuration.setTextColor(subTextColor);

            // Adjust button tints
            android.content.res.ColorStateList iconColor = android.content.res.ColorStateList.valueOf(textColor);
            btnPrev.setIconTint(iconColor);
            btnNext.setIconTint(iconColor);
            btnShuffle.setIconTint(iconColor);
            btnRepeat.setIconTint(iconColor);
            btnQueue.setIconTint(iconColor);
            
            // Adjust slider colors
            seekSlider.setThumbTintList(iconColor);
            seekSlider.setTrackActiveTintList(iconColor);
            seekSlider.setTrackInactiveTintList(android.content.res.ColorStateList.valueOf(textColor).withAlpha(64));
            
            // Adjust Play/Pause FAB
            btnPlayPause.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(textColor));
            btnPlayPause.setImageTintList(android.content.res.ColorStateList.valueOf(bgColor));

            // Adjust Bottom Navigation Bar
            com.google.android.material.bottomnavigation.BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
            if (navView != null) {
                navView.setBackgroundColor(bgColor);
                android.content.res.ColorStateList navColorList = new android.content.res.ColorStateList(
                    new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                    },
                    new int[]{
                        textColor,
                        subTextColor
                    }
                );
                navView.setItemIconTintList(navColorList);
                navView.setItemTextColor(navColorList);
                // Also adjust the indicator color (the pill behind the icon)
                navView.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf(textColor).withAlpha(31)); // ~12% opacity
            }
        });
    }

    private boolean isDark(int color) {
        return androidx.core.graphics.ColorUtils.calculateLuminance(color) < 0.5;
    }

    private void updateLikeButton(boolean liked) {
        btnLike.setIconResource(liked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        if (liked) {
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
            btnLike.setIconTint(android.content.res.ColorStateList.valueOf(tv.data));
        } else {
            btnLike.setIconTint(null);
        }
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        toggleArtworkAnimation(isPlaying);
    }

    private void updateRepeatIcon(int mode) {
        switch (mode) {
            case Player.REPEAT_MODE_ONE:
                btnRepeat.setIconResource(R.drawable.ic_repeat_one);
                btnRepeat.setAlpha(1.0f);
                break;
            case Player.REPEAT_MODE_ALL:
                btnRepeat.setIconResource(R.drawable.ic_repeat);
                btnRepeat.setAlpha(1.0f);
                break;
            default:
                btnRepeat.setIconResource(R.drawable.ic_repeat);
                btnRepeat.setAlpha(0.5f);
                break;
        }
    }

    private void updateSeekBar() {
        if (controller == null || userIsDragging) return;
        long duration = controller.getDuration();
        long position = controller.getCurrentPosition();
        if (duration > 0) {
            // Use absolute milliseconds for smoother and more accurate seeking
            if (seekSlider.getValueTo() != duration) {
                seekSlider.setValueTo(duration);
            }
            // Clamp position within [0, duration] to avoid Slider errors
            float safePosition = Math.max(0, Math.min(position, duration));
            seekSlider.setValue(safePosition);

            tvPosition.setText(formatMs(position));
            tvDuration.setText(formatMs(duration));
        }
    }

    private String formatMs(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}