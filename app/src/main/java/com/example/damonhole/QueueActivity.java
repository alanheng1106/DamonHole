package com.example.damonhole;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends AppCompatActivity {

    private SongAdapter adapter;
    private TextView tvEmpty;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(SongItem song) {
                if (controller != null) {
                    int count = controller.getMediaItemCount();
                    for (int i = 0; i < count; i++) {
                        MediaItem item = controller.getMediaItemAt(i);
                        if (item.mediaId.equals(song.videoId)) {
                            controller.seekTo(i, 0);
                            controller.play();
                            finish(); 
                            break;
                        }
                    }
                }
            }

            @Override
            public void onMoreClick(View view, SongItem song) {
                // Future queue options
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, AudioService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                refreshQueue();
                controller.addListener(new Player.Listener() {
                    @Override
                    public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
                        refreshQueue();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("QueueActivity", "Controller error", e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaController.releaseFuture(controllerFuture);
    }

    private void refreshQueue() {
        if (controller == null) return;
        List<SongItem> songs = new ArrayList<>();
        int count = controller.getMediaItemCount();
        for (int i = 0; i < count; i++) {
            MediaItem item = controller.getMediaItemAt(i);
            String title = item.mediaMetadata.title != null ? item.mediaMetadata.title.toString() : getString(R.string.unknown);
            String artist = item.mediaMetadata.artist != null ? item.mediaMetadata.artist.toString() : getString(R.string.unknown);
            songs.add(new SongItem(item.mediaId, title, artist));
        }
        adapter.setSongs(songs);
        tvEmpty.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
