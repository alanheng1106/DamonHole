package com.example.damonhole;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.damonhole.ui.M3PullRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
import com.example.damonhole.ui.DynamicThemeManager;

public class PlaylistsFragment extends BaseTabFragment {

    private RecyclerView rvPlaylists;
    private PlaylistAdapter adapter;
    private TextView tvLikedSongsCount;
    private M3PullRefreshLayout swipeRefresh;

    // FAB Menu Components
    private boolean isFabExpanded = false;
    private FloatingActionButton fabMain, fabCreate, fabImport;
    private View layoutFabCreate, layoutFabImport;
    private View progressIndicator;

    private static final String YOUTUBE_API_KEY = "AIzaSyDYhuEVyx28D6Ve7Wd4yNCm9MlPjbEEw0U";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLikedSongsCount = view.findViewById(R.id.tvLikedSongsCount);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        progressIndicator = view.findViewById(R.id.progressIndicator);
        TextView tvTitle = view.findViewById(R.id.tvTitle);

        view.findViewById(R.id.cardLikedSongs).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LikedSongsActivity.class)));

        // Setup FAB Menu
        fabMain = view.findViewById(R.id.fabMain);
        fabCreate = view.findViewById(R.id.fabCreate);
        fabImport = view.findViewById(R.id.fabImport);
        layoutFabCreate = view.findViewById(R.id.layoutFabCreate);
        layoutFabImport = view.findViewById(R.id.layoutFabImport);

        // Pre-configure initial animation state
        layoutFabCreate.setAlpha(0f);
        layoutFabCreate.setTranslationY(20f);
        layoutFabImport.setAlpha(0f);
        layoutFabImport.setTranslationY(20f);

        fabMain.setOnClickListener(v -> toggleFabMenu());

        fabCreate.setOnClickListener(v -> {
            toggleFabMenu();
            PlaylistManager.getInstance(requireContext()).showCreatePlaylistDialog(requireActivity(), name -> loadPlaylists());
        });

        fabImport.setOnClickListener(v -> {
            toggleFabMenu();
            showImportYoutubeDialog();
        });

        // Setup SwipeRefreshLayout (hide default spinner, use M3 LoadingIndicator)
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadPlaylists();
                updateLikedSongsCount();
                swipeRefresh.setRefreshing(false);
            });
        }

        setupRecyclerView();
        loadPlaylists();
        updateLikedSongsCount();
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

    private void importYoutubePlaylist(String name, String url) {
        String playlistId = extractPlaylistId(url);
        
        if (playlistId == null) {
            Toast.makeText(requireContext(), R.string.invalid_playlist_url, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), R.string.fetching_playlist, Toast.LENGTH_SHORT).show();
        if (progressIndicator != null) progressIndicator.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<SongItem> fetchedSongs = new ArrayList<>();
                String nextPageToken = null;

                // Loop through all pages (YouTube API returns max 50 per page)
                do {
                    String apiUrl = "https://www.googleapis.com/youtube/v3/playlistItems?" +
                            "part=snippet&maxResults=50&playlistId=" + playlistId + "&key=" + YOUTUBE_API_KEY;
                    if (nextPageToken != null) {
                        apiUrl += "&pageToken=" + nextPageToken;
                    }

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

                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray items = jsonObject.getJSONArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject snippet = items.getJSONObject(i).getJSONObject("snippet");
                        String title = snippet.getString("title");
                        String author = snippet.getString("videoOwnerChannelTitle");
                        String videoId = snippet.getJSONObject("resourceId").getString("videoId");

                        if (title.equals("Private video") || title.equals("Deleted video")) continue;
                        fetchedSongs.add(new SongItem(videoId, title, author));
                    }

                    // Get next page token (null if no more pages)
                    nextPageToken = jsonObject.optString("nextPageToken", null);

                } while (nextPageToken != null);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                        if (!fetchedSongs.isEmpty()) {
                            PlaylistManager.getInstance(requireContext()).createPlaylist(name, fetchedSongs);
                            loadPlaylists(); 
                            Toast.makeText(requireContext(), getString(R.string.imported_songs_count, fetchedSongs.size()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), R.string.no_songs_found, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressIndicator != null) progressIndicator.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        adapter.setPlaylists(PlaylistManager.getInstance(requireContext()).getPlaylists());
                        Toast.makeText(requireContext(), R.string.import_failed, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showImportYoutubeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_youtube, null);
        EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);
        EditText etYoutubeUrl = dialogView.findViewById(R.id.etYoutubeUrl);

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.action_import, (dialog, which) -> {
                    String name = etPlaylistName.getText().toString().trim();
                    String url = etYoutubeUrl.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.enter_playlist_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (!url.isEmpty()) {
                        importYoutubePlaylist(name, url);
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
    public void onThemeChanged(DynamicThemeManager.AppPalette palette) {
        if (getView() == null) return;

        TextView tvTitle = getView().findViewById(R.id.tvTitle);
        TextView tvYourPlaylists = getView().findViewById(R.id.tvYourPlaylists);

        if (palette == null) {
            getView().setBackgroundColor(Color.TRANSPARENT);
            if (tvTitle != null) tvTitle.setTextColor(Color.WHITE);
            return;
        }

        getView().setBackgroundColor(palette.surfaceContainer);
        if (tvTitle != null) tvTitle.setTextColor(palette.onSurface);
        if (tvYourPlaylists != null) tvYourPlaylists.setTextColor(palette.onSurface);
    }

    private void setupRecyclerView() {
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Play item entry animation only on first creation, then clear it
        android.view.animation.LayoutAnimationController lac =
                android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_from_bottom);
        rvPlaylists.setLayoutAnimation(lac);
        rvPlaylists.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rvPlaylists.setLayoutAnimation(null);
                rvPlaylists.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        adapter = new PlaylistAdapter(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
                intent.putExtra("playlistId", playlist.id);
                intent.putExtra("playlistName", playlist.name);
                startActivity(intent);
            }

            @Override
            public void onPlayClick(Playlist playlist) {
                if (playlist.songs.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.playlist_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                MediaController controller = getMusicController();
                if (controller == null) {
                    Toast.makeText(requireContext(), getString(R.string.player_not_ready), Toast.LENGTH_SHORT).show();
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

                Toast.makeText(requireContext(), getString(R.string.playing_playlist, playlist.name), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMoreClick(View view, Playlist playlist) {
                PopupMenu popup = new PopupMenu(requireContext(), view);
                popup.getMenu().add(0, 1, 0, R.string.rename);
                popup.getMenu().add(0, 2, 0, R.string.delete);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        PlaylistManager.getInstance(requireContext()).showRenamePlaylistDialog(requireActivity(), playlist, newName -> loadPlaylists());
                    } else if (item.getItemId() == 2) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.delete_playlist_title)
                                .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
                                .setPositiveButton(R.string.delete, (dialog, which) -> {
                                    PlaylistManager.getInstance(requireContext()).removePlaylist(playlist.id);
                                    loadPlaylists();
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
        rvPlaylists.setAdapter(adapter);
    }

    private void loadPlaylists() {
        if (adapter != null) {
            adapter.setPlaylists(PlaylistManager.getInstance(requireContext()).getPlaylists());
        }
    }

    private void updateLikedSongsCount() {
        int count = LikedSongsManager.getInstance(requireContext()).getAll().size();
        tvLikedSongsCount.setText(getString(R.string.songs_count, count));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
        updateLikedSongsCount();
    }
}
