package com.example.damonhole;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvGreeting;

    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private SearchBar searchBar;
    private SearchView searchView;
    private SongAdapter songAdapter;
    private PlaylistAdapter playlistAdapter;
    private RecyclerView recyclerView;
    private TextView tvSectionTitle;
    private Button btnCreatePlaylist;

    private PlaylistManager playlistManager;
    private LikedSongsManager likedManager;
    private com.google.android.material.progressindicator.CircularProgressIndicator searchLoadingIndicator;

    private SearchHistoryAdapter historyAdapter;
    private final List<String> searchHistory = new ArrayList<>();
    private SharedPreferences historyPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NewPipe.init(new AndroidDownloader());
        new UpdateManager(this).checkForUpdates(false);
        playlistManager = PlaylistManager.getInstance(this);
        likedManager = LikedSongsManager.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        historyPrefs = getSharedPreferences("search_history", MODE_PRIVATE);
        
        tvUserName = findViewById(R.id.tvUserName);
        tvGreeting = findViewById(R.id.tvGreeting);

        updateGreeting();
        fetchUserInfo();

        searchBar = findViewById(R.id.searchBar);
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);
        searchLoadingIndicator = findViewById(R.id.progressIndicator);

        findViewById(R.id.ivProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupAdapters();
        setupSearchHistory();

        searchView.setupWithSearchBar(searchBar);
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = searchView.getText().toString().trim();
            if (!query.isEmpty()) {
                addToHistory(query);
                searchView.hide();
                searchBar.setText(query);
                searchSongs(query);
            }
            return true;
        });

        btnCreatePlaylist.setOnClickListener(v -> {
            playlistManager.showCreatePlaylistDialog(this, name -> {
                playlistAdapter.setPlaylists(playlistManager.getPlaylists());
            });
        });

        NavigationUtils.setupBottomNav(this);
        loadRecentHits();
    }

    private void setupSearchHistory() {
        RecyclerView rvHistory = findViewById(R.id.rvSearchHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistory();
        historyAdapter = new SearchHistoryAdapter(searchHistory, new SearchHistoryAdapter.HistoryListener() {
            @Override
            public void onQueryClick(String query) {
                searchView.getEditText().setText(query);
                searchView.hide();
                searchBar.setText(query);
                searchSongs(query);
            }

            @Override
            public void onDeleteClick(String query) {
                removeFromHistory(query);
            }
        });
        rvHistory.setAdapter(historyAdapter);
    }

    private void loadHistory() {
        searchHistory.clear();
        String historyString = historyPrefs.getString("queries_list", "");
        if (!historyString.isEmpty()) {
            String[] parts = historyString.split("\\|");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    searchHistory.add(part);
                }
            }
        }
    }

    private void addToHistory(String query) {
        searchHistory.remove(query); // Remove if already exists to move to top
        searchHistory.add(0, query);
        if (searchHistory.size() > 10) searchHistory.remove(searchHistory.size() - 1);
        
        saveHistory();
        historyAdapter.notifyDataSetChanged();
    }

    private void removeFromHistory(String query) {
        searchHistory.remove(query);
        saveHistory();
        historyAdapter.notifyDataSetChanged();
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchHistory.size(); i++) {
            sb.append(searchHistory.get(i));
            if (i < searchHistory.size() - 1) sb.append("|");
        }
        historyPrefs.edit().putString("queries_list", sb.toString()).apply();
    }

    private void loadRecentHits() {
        if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.VISIBLE);
        searchExecutor.execute(() -> {
            List<SongItem> results = new ArrayList<>();
            try {
                KioskExtractor<?> extractor = ServiceList.YouTube.getKioskList().getExtractorById("Music", null);
                extractor.fetchPage();

                for (InfoItem item : extractor.getInitialPage().getItems()) {
                    if (item instanceof StreamInfoItem) {
                        StreamInfoItem stream = (StreamInfoItem) item;
                        String videoId = extractVideoId(stream.getUrl());
                        if (videoId != null) {
                            results.add(new SongItem(videoId, stream.getName(), stream.getUploaderName()));
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Music kiosk fetch failed", e);
            }

            if (results.isEmpty()) {
                try {
                    String fallbackQuery = "Top hit songs " + Calendar.getInstance().get(Calendar.YEAR);
                    SearchExtractor searchExtractor = ServiceList.YouTube.getSearchExtractor(fallbackQuery);
                    searchExtractor.fetchPage();

                    for (InfoItem item : searchExtractor.getInitialPage().getItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem stream = (StreamInfoItem) item;
                            String videoId = extractVideoId(stream.getUrl());
                            if (videoId != null) {
                                results.add(new SongItem(videoId, stream.getName(), stream.getUploaderName()));
                            }
                        }
                    }
                } catch (Exception fallbackEx) {
                    android.util.Log.e("MainActivity", "Fallback music search failed", fallbackEx);
                }
            }

            runOnUiThread(() -> {
                if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                if (!results.isEmpty()) {
                    tvSectionTitle.setVisibility(View.VISIBLE);
                    tvSectionTitle.setText(getString(R.string.recent_hits));
                    songAdapter.setSongs(results);
                    recyclerView.setAdapter(songAdapter);
                } else {
                    tvSectionTitle.setVisibility(View.GONE);
                }
            });
        });
    }

    private void setupAdapters() {
        songAdapter = new SongAdapter(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(SongItem song) {
                extractAndPlay(song.title, song.author, song.videoId);
            }
            @Override
            public void onMoreClick(View view, SongItem song) {
                showSongOptionsMenu(view, song);
            }
        });

        playlistAdapter = new PlaylistAdapter(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                tvSectionTitle.setText(playlist.name);
                songAdapter.setSongs(playlist.songs);
                recyclerView.setAdapter(songAdapter);
                btnCreatePlaylist.setVisibility(View.GONE);
            }
            @Override
            public void onPlayClick(Playlist playlist) {
                if (playlist.songs.isEmpty()) return;
                Toast.makeText(MainActivity.this, getString(R.string.playing_playlist, playlist.name), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onMoreClick(View view, Playlist playlist) {
                PopupMenu popup = new PopupMenu(MainActivity.this, view);
                popup.getMenu().add(0, 1, 0, R.string.rename);
                popup.getMenu().add(0, 2, 0, R.string.delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        playlistManager.showRenamePlaylistDialog(MainActivity.this, playlist, newName -> {
                            playlistAdapter.setPlaylists(playlistManager.getPlaylists());
                        });
                    } else if (item.getItemId() == 2) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.delete_playlist_title)
                                .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
                                .setPositiveButton(R.string.delete, (dialog, which) -> {
                                    playlistManager.removePlaylist(playlist.id);
                                    playlistAdapter.setPlaylists(playlistManager.getPlaylists());
                                    Toast.makeText(MainActivity.this, R.string.playlist_deleted, Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                    return true;
                });
                popup.show();
            }
        });
        recyclerView.setAdapter(songAdapter);
    }

    private void showSongOptionsMenu(View anchor, SongItem song) {
        PopupMenu menu = new PopupMenu(this, anchor);
                menu.getMenu().add(R.string.add_to_playlist);
        menu.getMenu().add(likedManager.isLiked(song.videoId) ? R.string.unlike : R.string.like);
        menu.setOnMenuItemClickListener(item -> {
            if (getString(R.string.add_to_playlist).contentEquals(item.getTitle())) {
                showAddToPlaylistDialog(song);
            } else {
                likedManager.toggle(song);
                Toast.makeText(this, R.string.liked_songs_updated, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private void showAddToPlaylistDialog(SongItem song) {
        List<Playlist> playlists = playlistManager.getPlaylists();
        if (playlists.isEmpty()) {
            Toast.makeText(this, R.string.create_playlist_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_to_playlist)
                .setItems(names, (dialog, which) -> {
                    playlistManager.addSongToPlaylist(playlists.get(which).id, song);
                    Toast.makeText(this, getString(R.string.added_to_playlist, playlists.get(which).name), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationUtils.syncBottomNavSelection(this);
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

    private void updateGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (timeOfDay >= 5 && timeOfDay < 12) greetingRes = R.string.good_morning;
        else if (timeOfDay >= 12 && timeOfDay < 17) greetingRes = R.string.good_afternoon;
        else if (timeOfDay >= 17 && timeOfDay < 21) greetingRes = R.string.good_evening;
        else greetingRes = R.string.good_night;
        tvGreeting.setText(getString(greetingRes));
    }

    private void fetchUserInfo() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                tvUserName.setText(user.getName());
                                likedManager.syncFromFirebase();
                                playlistManager.syncFromFirebase();
                            }
                        }
                    });
        }
    }

    private void searchSongs(String query) {
        if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.VISIBLE);
        searchExecutor.execute(() -> {
            try {
                SearchExtractor extractor = ServiceList.YouTube.getSearchExtractor(query);
                extractor.fetchPage();
                List<SongItem> results = new ArrayList<>();
                for (InfoItem item : extractor.getInitialPage().getItems()) {
                    if (item instanceof StreamInfoItem) {
                        StreamInfoItem stream = (StreamInfoItem) item;
                        String videoId = extractVideoId(stream.getUrl());
                        if (videoId != null) {
                            results.add(new SongItem(videoId, stream.getName(), stream.getUploaderName()));
                        }
                    }
                }
                runOnUiThread(() -> {
                    if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                    tvSectionTitle.setVisibility(View.VISIBLE);
                    tvSectionTitle.setText(getString(R.string.search_results));
                    songAdapter.setSongs(results);
                    recyclerView.setAdapter(songAdapter);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.search_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void extractAndPlay(String title, String author, String videoId) {
        if (controller == null) return;
        List<SongItem> allSongs = songAdapter.getSongs();
        if (allSongs == null || allSongs.isEmpty()) return;
        Toast.makeText(this, getString(R.string.loading_song, title), Toast.LENGTH_SHORT).show();
        List<MediaItem> mediaItems = new java.util.ArrayList<>();
        int startIndex = 0;
        for (int i = 0; i < allSongs.size(); i++) {
            SongItem song = allSongs.get(i);
            if (song.videoId.equals(videoId)) startIndex = i;
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
        controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET);
        controller.prepare();
        controller.play();
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        if (url.contains("v=")) {
            String id = url.split("v=")[1];
            return id.contains("&") ? id.split("&")[0] : id;
        }
        if (url.contains("youtu.be/")) {
            String id = url.split("youtu.be/")[1];
            return id.contains("?") ? id.split("\\?")[0] : id;
        }
        return null;
    }

    // --- Search History Adapter ---
    private static class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.VH> {
        interface HistoryListener {
            void onQueryClick(String query);
            void onDeleteClick(String query);
        }
        private final List<String> items;
        private final HistoryListener listener;
        SearchHistoryAdapter(List<String> items, HistoryListener listener) {
            this.items = items;
            this.listener = listener;
        }
        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            String q = items.get(position);
            holder.tvQuery.setText(q);
            holder.itemView.setOnClickListener(v -> listener.onQueryClick(q));
            holder.btnClear.setOnClickListener(v -> listener.onDeleteClick(q));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvQuery;
            View btnClear;
            VH(View v) {
                super(v);
                tvQuery = v.findViewById(R.id.tvSearchQuery);
                btnClear = v.findViewById(R.id.btnClearItem);
            }
        }
    }
}
