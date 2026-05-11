package com.example.damonhole;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LikedSongsActivity extends AppCompatActivity {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private LikedSongsManager likedManager;
    private LikedAdapter adapter;
    private TextView tvEmpty, tvCount;
    private View loadingOverlay;
    private List<SongItem> songs;

    private String currentSortOrder = "date"; // "date", "title_az", "title_za"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_songs);

        likedManager = LikedSongsManager.getInstance(this);

        MaterialButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSort = findViewById(R.id.btnSort);
        tvEmpty  = findViewById(R.id.tvEmpty);
        tvCount  = findViewById(R.id.tvCount);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnSort.setOnClickListener(this::showSortMenu);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);

        // Setup SwipeRefreshLayout (hide default spinner, use M3 LoadingIndicator)
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefresh);
        View refreshIndicator = findViewById(R.id.refreshIndicator);
        if (swipeRefresh != null) {
            swipeRefresh.setProgressViewOffset(true, 0, 0);
            swipeRefresh.setOnRefreshListener(() -> {
                swipeRefresh.setRefreshing(false);
                if (refreshIndicator != null) refreshIndicator.setVisibility(View.VISIBLE);
                refreshList();
                if (refreshIndicator != null) refreshIndicator.setVisibility(View.GONE);
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LikedAdapter(new ArrayList<>(), song -> {
            Toast.makeText(this, getString(R.string.loading_song, song.title), Toast.LENGTH_SHORT).show();
            extractAndPlay(song.videoId);
        }, videoId -> {
            likedManager.unlike(videoId);
            refreshList();
        });
        recyclerView.setAdapter(adapter);

        // Swipe to unlike
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@androidx.annotation.NonNull RecyclerView rv,
                                            @androidx.annotation.NonNull RecyclerView.ViewHolder vh,
                                            @androidx.annotation.NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override public void onSwiped(@androidx.annotation.NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                SongItem song = adapter.getSongAt(pos);
                likedManager.unlike(song.videoId);
                refreshList();
            }
        }).attachToRecyclerView(recyclerView);

        refreshList();
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
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
            return true;
        });
        menu.show();
    }

    private void applySorting() {
        if (songs == null || songs.isEmpty()) return;

        switch (currentSortOrder) {
            case "date":
                // Assuming likedManager returns by date, if not we'd need addedAt
                songs = new ArrayList<>(likedManager.getAll());
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
        adapter.updateSongs(new ArrayList<>(songs));
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        songs = new ArrayList<>(likedManager.getAll());
        applySorting();
        int count = songs.size();
        tvCount.setText(getString(R.string.songs_count, count));
        tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void extractAndPlay(String videoId) {
        if (controller == null || songs == null || songs.isEmpty()) return;
        
        List<MediaItem> mediaItems = new ArrayList<>();
        int startIndex = 0;

        for (int i = 0; i < songs.size(); i++) {
            SongItem song = songs.get(i);
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

    // -----------------------------------------------------------------------
    // Adapter
    // -----------------------------------------------------------------------
    static class LikedAdapter extends RecyclerView.Adapter<LikedAdapter.VH> {

        interface OnSongClick   { void onSongClick(SongItem song); }
        interface OnUnlikeClick { void onUnlike(String videoId); }

        private List<SongItem> songs;
        private final OnSongClick   onSongClick;
        private final OnUnlikeClick onUnlike;

        LikedAdapter(List<SongItem> songs, OnSongClick click, OnUnlikeClick unlike) {
            this.songs       = songs;
            this.onSongClick = click;
            this.onUnlike    = unlike;
        }

        void updateSongs(List<SongItem> newSongs) {
            this.songs = newSongs;
            notifyDataSetChanged();
        }

        SongItem getSongAt(int pos) { return songs.get(pos); }

        @androidx.annotation.NonNull
        @Override
        public VH onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_liked_song, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull VH holder, int pos) {
            SongItem song = songs.get(pos);
            holder.tvTitle.setText(song.title);
            holder.tvAuthor.setText(song.author);
            
            String thumbnailUrl = "https://img.youtube.com/vi/" + song.videoId + "/hqdefault.jpg";
            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(holder.ivThumb);
                    
            holder.itemView.setOnClickListener(v -> onSongClick.onSongClick(song));
            holder.btnUnlike.setOnClickListener(v -> onUnlike.onUnlike(song.videoId));
        }

        @Override public int getItemCount() { return songs.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAuthor;
            MaterialButton btnUnlike;
            com.google.android.material.imageview.ShapeableImageView ivThumb;
            VH(View v) {
                super(v);
                tvTitle   = v.findViewById(R.id.tvTitle);
                tvAuthor  = v.findViewById(R.id.tvAuthor);
                btnUnlike = v.findViewById(R.id.btnUnlike);
                ivThumb   = v.findViewById(R.id.ivThumb);
            }
        }
    }
}
