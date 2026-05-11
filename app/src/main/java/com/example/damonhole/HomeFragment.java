package com.example.damonhole;

import android.content.Context;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

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

public class HomeFragment extends BaseTabFragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvGreeting;

    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    private SearchBar searchBar;
    private SearchView searchView;
    private SongAdapter songAdapter;
    private PlaylistAdapter playlistAdapter;
    private RecyclerView recyclerView;
    private TextView tvSectionTitle;
    private Button btnCreatePlaylist;

    private PlaylistManager playlistManager;
    private LikedSongsManager likedManager;
    private View searchLoadingIndicator;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View refreshIndicator;

    private SearchHistoryAdapter historyAdapter;
    private final List<String> searchHistory = new ArrayList<>();
    private SharedPreferences historyPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        NewPipe.init(new AndroidDownloader());
        playlistManager = PlaylistManager.getInstance(context);
        likedManager = LikedSongsManager.getInstance(context);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        historyPrefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE);
        
        tvUserName = view.findViewById(R.id.tvUserName);
        tvGreeting = view.findViewById(R.id.tvGreeting);

        updateGreeting();
        fetchUserInfo();

        searchBar = view.findViewById(R.id.searchBar);
        searchView = view.findViewById(R.id.searchView);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);
        searchLoadingIndicator = view.findViewById(R.id.progressIndicator);

        // Setup SwipeRefreshLayout for pull-to-refresh (hide default spinner, use M3 LoadingIndicator)
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        refreshIndicator = view.findViewById(R.id.refreshIndicator);
        swipeRefresh.setProgressViewOffset(true, 0, 0); // Hide default spinner off-screen
        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false); // Immediately hide default spinner
            if (refreshIndicator != null) refreshIndicator.setVisibility(View.VISIBLE);
            loadRecentHits();
        });

        view.findViewById(R.id.ivProfile).setOnClickListener(v -> {
            startActivity(new Intent(context, ProfileActivity.class));
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Play item entry animation only on first creation, then clear it
        android.view.animation.LayoutAnimationController lac =
                android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);
        recyclerView.setLayoutAnimation(lac);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.setLayoutAnimation(null);
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        setupAdapters();
        setupSearchHistory(view);

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
            playlistManager.showCreatePlaylistDialog(context, name -> {
                playlistAdapter.setPlaylists(playlistManager.getPlaylists());
            });
        });

        loadRecentHits();
    }

    private void setupSearchHistory(View view) {
        RecyclerView rvHistory = view.findViewById(R.id.rvSearchHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        
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
        searchHistory.remove(query); 
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
                android.util.Log.e("HomeFragment", "Music kiosk fetch failed", e);
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
                    android.util.Log.e("HomeFragment", "Fallback music search failed", fallbackEx);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                    if (refreshIndicator != null) refreshIndicator.setVisibility(View.GONE);
                    if (!results.isEmpty()) {
                        tvSectionTitle.setVisibility(View.VISIBLE);
                        tvSectionTitle.setText(getString(R.string.recent_hits));
                        songAdapter.setSongs(results);
                        recyclerView.setAdapter(songAdapter);
                    } else {
                        tvSectionTitle.setVisibility(View.GONE);
                    }
                });
            }
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
                Toast.makeText(requireContext(), getString(R.string.playing_playlist, playlist.name), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onMoreClick(View view, Playlist playlist) {
                PopupMenu popup = new PopupMenu(requireContext(), view);
                popup.getMenu().add(0, 1, 0, R.string.rename);
                popup.getMenu().add(0, 2, 0, R.string.delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        playlistManager.showRenamePlaylistDialog(requireActivity(), playlist, newName -> {
                            playlistAdapter.setPlaylists(playlistManager.getPlaylists());
                        });
                    } else if (item.getItemId() == 2) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.delete_playlist_title)
                                .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
                                .setPositiveButton(R.string.delete, (dialog, which) -> {
                                    playlistManager.removePlaylist(playlist.id);
                                    playlistAdapter.setPlaylists(playlistManager.getPlaylists());
                                    Toast.makeText(requireContext(), R.string.playlist_deleted, Toast.LENGTH_SHORT).show();
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
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(R.string.add_to_playlist);
        menu.getMenu().add(likedManager.isLiked(song.videoId) ? R.string.unlike : R.string.like);
        menu.setOnMenuItemClickListener(item -> {
            if (getString(R.string.add_to_playlist).contentEquals(item.getTitle())) {
                showAddToPlaylistDialog(song);
            } else {
                likedManager.toggle(song);
                Toast.makeText(requireContext(), R.string.liked_songs_updated, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private void showAddToPlaylistDialog(SongItem song) {
        List<Playlist> playlists = playlistManager.getPlaylists();
        if (playlists.isEmpty()) {
            Toast.makeText(requireContext(), R.string.create_playlist_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_to_playlist)
                .setItems(names, (dialog, which) -> {
                    playlistManager.addSongToPlaylist(playlists.get(which).id, song);
                    Toast.makeText(requireContext(), getString(R.string.added_to_playlist, playlists.get(which).name), Toast.LENGTH_SHORT).show();
                })
                .show();
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
                        if (isAdded() && documentSnapshot.exists()) {
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                        tvSectionTitle.setVisibility(View.VISIBLE);
                        tvSectionTitle.setText(getString(R.string.search_results));
                        songAdapter.setSongs(results);
                        recyclerView.setAdapter(songAdapter);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (searchLoadingIndicator != null) searchLoadingIndicator.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), getString(R.string.search_failed), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void extractAndPlay(String title, String author, String videoId) {
        MediaController controller = getMusicController();
        if (controller == null) return;
        List<SongItem> allSongs = songAdapter.getSongs();
        if (allSongs == null || allSongs.isEmpty()) return;
        Toast.makeText(requireContext(), getString(R.string.loading_song, title), Toast.LENGTH_SHORT).show();
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
