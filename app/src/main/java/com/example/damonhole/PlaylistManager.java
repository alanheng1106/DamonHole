package com.example.damonhole;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlaylistManager {
    public interface OnPlaylistCreatedListener {
        void onCreated(String name);
    }
    private static final String PREFS_NAME = "playlists_prefs";
    private static final String KEY_PLAYLISTS = "playlists";

    private static PlaylistManager instance;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final Context context;

    private PlaylistManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public static PlaylistManager getInstance(Context context) {
        if (instance == null) instance = new PlaylistManager(context);
        return instance;
    }

    public List<Playlist> getPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_PLAYLISTS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.getString("name");
                String author = obj.getString("author");
                String sortOrder = obj.optString("sortOrder", "custom");
                JSONArray songArr = obj.getJSONArray("songs");
                List<SongItem> songs = new ArrayList<>();
                for (int j = 0; j < songArr.length(); j++) {
                    JSONObject sObj = songArr.getJSONObject(j);
                    songs.add(new SongItem(
                            sObj.getString("videoId"),
                            sObj.getString("title"),
                            sObj.getString("author"),
                            sObj.optLong("addedAt", System.currentTimeMillis())
                    ));
                }
                playlists.add(new Playlist(id, name, author, songs, sortOrder));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return playlists;
    }

    public void createPlaylist(String name, String author) {
        List<Playlist> playlists = getPlaylists();
        playlists.add(new Playlist(name, author));
        saveLocally(playlists);
        syncToFirebase(playlists);
    }

    public void addSongToPlaylist(String playlistId, SongItem song) {
        List<Playlist> playlists = getPlaylists();
        for (Playlist p : playlists) {
            if (p.id.equals(playlistId)) {
                boolean exists = false;
                for (SongItem s : p.songs) {
                    if (s.videoId.equals(song.videoId)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    p.songs.add(song);
                    saveLocally(playlists);
                    syncToFirebase(playlists);
                }
                break;
            }
        }
    }

    public void updatePlaylistSongs(String playlistId, List<SongItem> songs, String sortOrder) {
        List<Playlist> playlists = getPlaylists();
        for (Playlist p : playlists) {
            if (p.id.equals(playlistId)) {
                p.songs = songs;
                p.sortOrder = sortOrder;
                saveLocally(playlists);
                syncToFirebase(playlists);
                break;
            }
        }
    }

    public void removePlaylist(String playlistId) {
        List<Playlist> playlists = getPlaylists();
        playlists.removeIf(p -> p.id.equals(playlistId));
        saveLocally(playlists);
        syncToFirebase(playlists);
    }

    public void syncFromFirebase() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.getPlaylists() != null) {
                    saveLocally(user.getPlaylists());
                }
            }
        });
    }

    public void clearLocalData() {
        prefs.edit().remove(KEY_PLAYLISTS).apply();
    }

    public void showCreatePlaylistDialog(Context context, OnPlaylistCreatedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_playlist, null);
        EditText etPlaylistName = dialogView.findViewById(R.id.etPlaylistName);

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.create), (dialog, which) -> {
                    String name = etPlaylistName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createPlaylist(name, context.getString(R.string.me));
                        if (listener != null) listener.onCreated(name);
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }

    private void syncToFirebase(List<Playlist> playlists) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).update("playlists", playlists);
    }

    private void saveLocally(List<Playlist> playlists) {
        try {
            JSONArray arr = new JSONArray();
            for (Playlist p : playlists) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.id);
                obj.put("name", p.name);
                obj.put("author", p.author);
                obj.put("sortOrder", p.sortOrder);
                JSONArray songArr = new JSONArray();
                for (SongItem s : p.songs) {
                    JSONObject sObj = new JSONObject();
                    sObj.put("videoId", s.videoId);
                    sObj.put("title", s.title);
                    sObj.put("author", s.author);
                    sObj.put("addedAt", s.addedAt);
                    songArr.put(sObj);
                }
                obj.put("songs", songArr);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createPlaylist(String name, List<SongItem> songs) {
        // 1. Fetch your current list of playlists
        List<Playlist> currentPlaylists = getPlaylists();

        // 2. Create the new playlist (using "Me" as the author, just like your dialog does)
        Playlist newPlaylist = new Playlist(name, context.getString(R.string.me));

        // 3. Add the fetched YouTube songs to this new playlist
        newPlaylist.songs = songs;

        // 4. Add it to your internal list
        currentPlaylists.add(newPlaylist);

        // 5. Save it using YOUR specific methods!
        saveLocally(currentPlaylists);
        syncToFirebase(currentPlaylists);
    }
}