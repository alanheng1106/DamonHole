package com.example.damonhole;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaylistsActivity extends AppCompatActivity {

    private RecyclerView rvPlaylists;
    private PlaylistAdapter adapter;
    private TextView tvLikedSongsCount;
    private List<Playlist> playlists = new ArrayList<>();

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    // FAB Menu Components
    private boolean isFabExpanded = false;
    private FloatingActionButton fabMain, fabCreate, fabImport;
    private LinearLayout layoutFabCreate, layoutFabImport;

    private static final String YOUTUBE_API_KEY = "AIzaSyDYhuEVyx28D6Ve7Wd4yNCm9MlPjbEEw0U";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        tvLikedSongsCount = findViewById(R.id.tvLikedSongsCount);
        rvPlaylists = findViewById(R.id.rvPlaylists);

        findViewById(R.id.cardLikedSongs).setOnClickListener(v ->
                startActivity(new Intent(this, LikedSongsActivity.class)));

        // Setup FAB Menu
        fabMain = findViewById(R.id.fabMain);
        fabCreate = findViewById(R.id.fabCreate);
        fabImport = findViewById(R.id.fabImport);
        layoutFabCreate = findViewById(R.id.layoutFabCreate);
        layoutFabImport = findViewById(R.id.layoutFabImport);

        // Pre-configure initial animation state
        layoutFabCreate.setAlpha(0f);
        layoutFabCreate.setTranslationY(20f);
        layoutFabImport.setAlpha(0f);
        layoutFabImport.setTranslationY(20f);

        fabMain.setOnClickListener(v -> toggleFabMenu());

        fabCreate.setOnClickListener(v -> {
            toggleFabMenu();
            PlaylistManager.getInstance(this).showCreatePlaylistDialog(this, name -> loadPlaylists());
        });

        fabImport.setOnClickListener(v -> {
            toggleFabMenu();
            showImportYoutubeDialog();
        });

        setupRecyclerView();
        loadPlaylists();
        updateLikedSongsCount();

        NavigationUtils.setupBottomNav(this);
    }

    private void toggleFabMenu() {
        isFabExpanded = !isFabExpanded;
        if (isFabExpanded) {
            layoutFabCreate.setVisibility(View.VISIBLE);
            layoutFabImport.setVisibility(View.VISIBLE);

            fabMain.animate().rotation(45f).setDuration(200).start();
            layoutFabCreate.animate().translationY(0f).alpha(1f).setDuration(200).start();
            layoutFabImport.animate().translationY(0f).alpha(1f).setDuration(200).start();
        } else {
            fabMain.animate().rotation(0f).setDuration(200).start();

            layoutFabCreate.animate().translationY(20f).alpha(0f).setDuration(200)
                    .withEndAction(() -> {
                        if (!isFabExpanded) layoutFabCreate.setVisibility(View.GONE);
                    }).start();

            layoutFabImport.animate().translationY(20f).alpha(0f).setDuration(200)
                    .withEndAction(() -> {
                        if (!isFabExpanded) layoutFabImport.setVisibility(View.GONE);
                    }).start();
        }
    }

private void importYoutubePlaylist(String url) {
    // 1. Extract the ID from the URL
    String playlistId = extractPlaylistId(url);
    
    if (playlistId == null) {
        Toast.makeText(this, R.string.invalid_playlist_url, Toast.LENGTH_SHORT).show();
        return;
    }

    Toast.makeText(this, R.string.fetching_playlist, Toast.LENGTH_SHORT).show();

    // 2. Run the network request on a background thread
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            // The API endpoint for getting items inside a playlist
            String apiUrl = "https://www.googleapis.com/youtube/v3/playlistItems?" +
                    "part=snippet&maxResults=50&playlistId=" + playlistId + "&key=" + YOUTUBE_API_KEY;

            URL urlObj = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 3. Parse the JSON response
            JSONObject jsonObject = new JSONObject(response.toString());
            JSONArray items = jsonObject.getJSONArray("items");

            List<SongItem> fetchedSongs = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject snippet = items.getJSONObject(i).getJSONObject("snippet");
                String title = snippet.getString("title");
                String author = snippet.getString("videoOwnerChannelTitle");
                String videoId = snippet.getJSONObject("resourceId").getString("videoId");

                // Skip private/deleted videos
                if (title.equals("Private video") || title.equals("Deleted video")) continue;

                // Create your song object (assuming this matches your constructor)
                fetchedSongs.add(new SongItem(videoId, title, author));
            }

            // 4. Update the UI and save data on the main thread
            runOnUiThread(() -> {
                if (!fetchedSongs.isEmpty()) {
                    // Create a new playlist with the imported songs
                    PlaylistManager.getInstance(this).createPlaylist(getString(R.string.imported_playlist_name), fetchedSongs);
                    loadPlaylists(); // Refresh your RecyclerView
                    Toast.makeText(this, getString(R.string.imported_songs_count, fetchedSongs.size()), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.no_songs_found, Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> 
                Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show()
            );
        }
    });
}

    private void showImportYoutubeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_youtube, null);
        EditText input = dialogView.findViewById(R.id.etYoutubeUrl);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.action_import, (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        importYoutubePlaylist(url);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

private String extractPlaylistId(String url) {
    String pattern = "(?:list=)([a-zA-Z0-9_-]+)";
    Pattern compiledPattern = Pattern.compile(pattern);
    Matcher matcher = compiledPattern.matcher(url);
    if (matcher.find()) {
        return matcher.group(1);
    }
    return null;
}

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this, new ComponentName(this, AudioService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try { controller = controllerFuture.get(); }
            catch (Exception e) { e.printStackTrace(); }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        MediaController.releaseFuture(controllerFuture);
        controller = null;
    }

    private void setupRecyclerView() {
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaylistAdapter(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Intent intent = new Intent(PlaylistsActivity.this, PlaylistDetailActivity.class);
                intent.putExtra("playlistId", playlist.id);
                intent.putExtra("playlistName", playlist.name);
                startActivity(intent);
            }

            @Override
            public void onPlayClick(Playlist playlist) {
                if (playlist.songs.isEmpty()) {
                    Toast.makeText(PlaylistsActivity.this,
                            getString(R.string.playlist_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (controller == null) {
                    Toast.makeText(PlaylistsActivity.this,
                            getString(R.string.player_not_ready), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<MediaItem> mediaItems = new ArrayList<>();
                for (SongItem song : playlist.songs) {
                    String thumb = "https://img.youtube.com/vi/" + song.videoId + "/hqdefault.jpg";
                    mediaItems.add(new MediaItem.Builder()
                            .setUri("youtube://" + song.videoId)
                            .setMediaId(song.videoId)
                            .setMediaMetadata(new MediaMetadata.Builder()
                                     .setTitle(song.title)
                                    .setArtist(song.author)
                                    .setArtworkUri(android.net.Uri.parse(thumb))
                                    .build())
                            .build());
                }

                controller.setMediaItems(mediaItems, 0, C.TIME_UNSET);
                controller.prepare();
                controller.play();

                Toast.makeText(PlaylistsActivity.this,
                        getString(R.string.playing_playlist, playlist.name),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Playlist playlist) {
                new AlertDialog.Builder(PlaylistsActivity.this)
                        .setTitle(R.string.delete_playlist_title)
                        .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            PlaylistManager.getInstance(PlaylistsActivity.this).removePlaylist(playlist.id);
                            loadPlaylists();
                            Toast.makeText(PlaylistsActivity.this, R.string.playlist_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
        rvPlaylists.setAdapter(adapter);
    }

    private void loadPlaylists() {
        adapter.setPlaylists(PlaylistManager.getInstance(this).getPlaylists());
    }

    private void updateLikedSongsCount() {
        int count = LikedSongsManager.getInstance(this).getAll().size();
        tvLikedSongsCount.setText(getString(R.string.songs_count, count));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylists();
        updateLikedSongsCount();
        NavigationUtils.syncBottomNavSelection(this);
    }
}