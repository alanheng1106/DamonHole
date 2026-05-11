package com.example.damonhole;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.damonhole.ui.M3PullRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private String playlistId;
    private String playlistName;
    private List<SongItem> songs;
    private String currentSortOrder = "date";

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView tvName, tvEmpty, tvSongCount;
    private View btnSort;
    private View loadingOverlay;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getStringExtra("playlistId");
        playlistName = getIntent().getStringExtra("playlistName");

        tvName = findViewById(R.id.tvPlaylistName);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSongCount = findViewById(R.id.tvSongCount);
        recyclerView = findViewById(R.id.recyclerView);
        btnSort = findViewById(R.id.btnSort);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        tvName.setText(playlistName);
        findViewById(R.id.btnBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnSort.setOnClickListener(this::showSortMenu);

        // Setup M3 PullRefreshLayout
        M3PullRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadPlaylistSongs);

        setupRecyclerView();

        // Show loading overlay immediately
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

        // Load data with a slight delay to ensure smooth transition and visible loader
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadPlaylistSongs();
            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        }, 600);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(SongItem song) {
                extractAndPlay(song.videoId);
            }

            @Override
            public void onMoreClick(View view, SongItem song) {
                android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(PlaylistDetailActivity.this, view);
                popupMenu.getMenu().add(0, 1, 0, getString(R.string.play));
                popupMenu.getMenu().add(0, 2, 0, getString(R.string.delete));
                
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            extractAndPlay(song.videoId);
                            return true;
                        case 2:
                            new androidx.appcompat.app.AlertDialog.Builder(PlaylistDetailActivity.this)
                                    .setTitle(R.string.delete)
                                    .setMessage(getString(R.string.delete_playlist_confirm, song.title))
                                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                                        songs.remove(song);
                                        PlaylistManager.getInstance(PlaylistDetailActivity.this)
                                                .updatePlaylistSongs(playlistId, songs, currentSortOrder);
                                        applySorting(); // Refreshes adapter
                                    })
                                    .setNegativeButton(R.string.cancel, null)
                                    .show();
                            return true;
                        default:
                            return false;
                    }
                });
                popupMenu.show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadPlaylistSongs() {
        List<Playlist> playlists = PlaylistManager.getInstance(this).getPlaylists();
        for (Playlist p : playlists) {
            if (p.id.equals(playlistId)) {
                songs = new ArrayList<>(p.songs);
                currentSortOrder = p.sortOrder;
                break;
            }
        }

        if (songs == null || songs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            adapter.setSongs(new ArrayList<>());
            if (tvSongCount != null) tvSongCount.setText(getString(R.string.songs_count, 0));
        } else {
            tvEmpty.setVisibility(View.GONE);
            applySorting();
            if (tvSongCount != null) tvSongCount.setText(getString(R.string.songs_count, songs.size()));
        }
        
        M3PullRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    private void showSortMenu(View v) {
        android.widget.PopupMenu menu = new android.widget.PopupMenu(this, v);
        menu.getMenu().add(0, 1, 0, getString(R.string.sort_date));
        menu.getMenu().add(0, 2, 0, getString(R.string.sort_title_az));
        menu.getMenu().add(0, 3, 0, getString(R.string.sort_title_za));
        menu.getMenu().add(0, 4, 0, getString(R.string.sort_artist));

        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: currentSortOrder = "date"; break;
                case 2: currentSortOrder = "title_az"; break;
                case 3: currentSortOrder = "title_za"; break;
                case 4: currentSortOrder = "artist"; break;
            }
            applySorting();
            PlaylistManager.getInstance(this).updatePlaylistSongs(playlistId, songs, currentSortOrder);
            return true;
        });
        menu.show();
    }

    private void applySorting() {
        if (songs == null || songs.isEmpty()) return;

        switch (currentSortOrder) {
            case "date":
                Collections.sort(songs, (s1, s2) -> Long.compare(s1.addedAt, s2.addedAt));
                break;
            case "title_az":
                Collections.sort(songs, (s1, s2) -> s1.title.compareToIgnoreCase(s2.title));
                break;
            case "title_za":
                Collections.sort(songs, (s1, s2) -> s2.title.compareToIgnoreCase(s1.title));
                break;
            case "artist":
                Collections.sort(songs, (s1, s2) -> s1.author.compareToIgnoreCase(s2.author));
                break;
        }
        adapter.setShowDragHandle(false);
        adapter.setSongs(new ArrayList<>(songs));
    }

    private void extractAndPlay(String videoId) {
        if (controller == null || songs == null || songs.isEmpty()) return;
        
        List<MediaItem> mediaItems = new ArrayList<>();
        int playingIndex = -1;
        
        for (int i = 0; i < songs.size(); i++) {
            SongItem song = songs.get(i);
            if (song.videoId.equals(videoId)) {
                playingIndex = i;
            }
            
            String thumbnailUrl = "https://img.youtube.com/vi/" + song.videoId + "/hqdefault.jpg";
            mediaItems.add(new MediaItem.Builder()
                    .setUri("youtube://" + song.videoId)
                    .setMediaId(song.videoId)
                    .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.author)
                            .setArtworkUri(android.net.Uri.parse(thumbnailUrl))
                            .build())
                    .build());
        }
        
        if (playingIndex != -1) {
            controller.setMediaItems(mediaItems, playingIndex, C.TIME_UNSET);
            controller.prepare();
            controller.play();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, AudioService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaController.releaseFuture(controllerFuture);
    }
}
