package com.example.damonhole;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

public class NowPlayingFragment extends BaseTabFragment {

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

    private boolean userIsDragging = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seekUpdater = new Runnable() {
        @Override public void run() {
            updateSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
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
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_now_playing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        likedManager = LikedSongsManager.getInstance(requireContext());

        rootLayout   = view.findViewById(R.id.rootLayout);
        ivArtwork    = view.findViewById(R.id.ivArtwork);
        tvTitle      = view.findViewById(R.id.tvTitle);
        tvTitle.setSelected(true); // enable marquee
        tvArtist     = view.findViewById(R.id.tvArtist);
        tvArtist.setSelected(true); // enable marquee
        tvPosition   = view.findViewById(R.id.tvPosition);
        tvDuration   = view.findViewById(R.id.tvDuration);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnPrev      = view.findViewById(R.id.btnPrev);
        btnNext      = view.findViewById(R.id.btnNext);
        btnShuffle   = view.findViewById(R.id.btnShuffle);
        btnRepeat    = view.findViewById(R.id.btnRepeat);
        btnLike      = view.findViewById(R.id.btnLike);
        btnQueue     = view.findViewById(R.id.btnQueue);
        seekSlider   = view.findViewById(R.id.seekSlider);

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
        
        handler.post(seekUpdater);
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

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            MediaController controller = getMediaController();
            if (controller == null) return;
            if (controller.isPlaying()) controller.pause();
            else controller.play();
        });

        btnPrev.setOnClickListener(v -> {
            MediaController controller = getMediaController();
            if (controller == null) return;
            if (controller.getCurrentPosition() > 3_000) controller.seekTo(0);
            else controller.seekToPreviousMediaItem();
        });

        btnNext.setOnClickListener(v -> {
            MediaController controller = getMediaController();
            if (controller != null) controller.seekToNextMediaItem();
        });

        btnShuffle.setOnClickListener(v -> {
            MediaController controller = getMediaController();
            if (controller == null) return;
            boolean shuffle = !controller.getShuffleModeEnabled();
            controller.setShuffleModeEnabled(shuffle);
            btnShuffle.setAlpha(shuffle ? 1.0f : 0.5f);
        });

        btnRepeat.setOnClickListener(v -> {
            MediaController controller = getMediaController();
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
            Toast.makeText(requireContext(),
                    nowLiked ? R.string.added_to_liked : R.string.removed_from_liked,
                    Toast.LENGTH_SHORT).show();
        });

        seekSlider.setLabelFormatter(value -> formatMs((long) value));
        seekSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                userIsDragging = true;
                handler.removeCallbacks(seekUpdater);
            }
            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                MediaController controller = getMediaController();
                if (controller != null) {
                    controller.seekTo((long) slider.getValue());
                }
                userIsDragging = false;
                handler.post(seekUpdater);
            }
        });

        seekSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                tvPosition.setText(formatMs((long) value));
            }
        });

        btnQueue.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QueueActivity.class)));
    }

    @Override
    public void onMediaControllerReady(MediaController controller) {
        super.onMediaControllerReady(controller);
        controller.addListener(playerListener);
        updateMetadataUI();
        updatePlayPauseIcon(controller.isPlaying());
        updateRepeatIcon(controller.getRepeatMode());
        btnShuffle.setAlpha(controller.getShuffleModeEnabled() ? 1.0f : 0.5f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(seekUpdater);
        MediaController controller = getMediaController();
        if (controller != null) {
            controller.removeListener(playerListener);
        }
    }

    private void updateMetadataUI() {
        MediaController controller = getMediaController();
        if (controller == null || controller.getCurrentMediaItem() == null) return;
        MediaMetadata meta = controller.getCurrentMediaItem().mediaMetadata;

        seekSlider.setValue(0);
        seekSlider.setValueTo(100); 

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
                        if (isAdded()) {
                            ivArtwork.setImageBitmap(resource);
                            applyPalette(resource);
                        }
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
        }
    }

    private void applyPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null || !isAdded()) return;
            
            int bgColor = palette.getVibrantColor(
                palette.getMutedColor(
                    palette.getDominantColor(Color.parseColor("#121212"))
                )
            );

            rootLayout.setBackgroundColor(bgColor);

            boolean isDark = isDark(bgColor);
            int textColor = isDark ? Color.WHITE : Color.BLACK;
            int subTextColor = isDark ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#B3000000");

            tvTitle.setTextColor(textColor);
            tvArtist.setTextColor(subTextColor);
            tvPosition.setTextColor(subTextColor);
            tvDuration.setTextColor(subTextColor);

            android.content.res.ColorStateList iconColor = android.content.res.ColorStateList.valueOf(textColor);
            btnPrev.setIconTint(iconColor);
            btnNext.setIconTint(iconColor);
            btnShuffle.setIconTint(iconColor);
            btnRepeat.setIconTint(iconColor);
            btnQueue.setIconTint(iconColor);
            
            seekSlider.setThumbTintList(iconColor);
            seekSlider.setTrackActiveTintList(iconColor);
            seekSlider.setTrackInactiveTintList(android.content.res.ColorStateList.valueOf(textColor).withAlpha(64));
            
            btnPlayPause.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(textColor));
            btnPlayPause.setImageTintList(android.content.res.ColorStateList.valueOf(bgColor));

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateBottomNavColors(bgColor, textColor, subTextColor);
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
            requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true);
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
        MediaController controller = getMediaController();
        if (controller == null || userIsDragging) return;
        long duration = controller.getDuration();
        long position = controller.getCurrentPosition();
        if (duration > 0) {
            if (seekSlider.getValueTo() != duration) {
                seekSlider.setValueTo(duration);
            }
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
