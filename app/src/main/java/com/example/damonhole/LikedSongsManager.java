package com.example.damonhole;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class LikedSongsManager {
    private static final String PREFS_NAME = "liked_songs";
    private static final String KEY_SONGS  = "songs";

    private static LikedSongsManager instance;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    private LikedSongsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public static LikedSongsManager getInstance(Context context) {
        if (instance == null) instance = new LikedSongsManager(context);
        return instance;
    }

    public boolean isLiked(String videoId) {
        for (SongItem song : getAll()) {
            if (song.videoId.equals(videoId)) return true;
        }
        return false;
    }

    public boolean toggle(SongItem song) {
        if (isLiked(song.videoId)) {
            unlike(song.videoId);
            return false;
        } else {
            like(song);
            return true;
        }
    }

    public void like(SongItem song) {
        if (isLiked(song.videoId)) return;
        List<SongItem> songs = getAll();
        songs.add(0, song);
        saveLocally(songs);
        syncToFirebase(songs);
    }

    public void unlike(String videoId) {
        List<SongItem> songs = getAll();
        songs.removeIf(s -> s.videoId.equals(videoId));
        saveLocally(songs);
        syncToFirebase(songs);
    }

    public List<SongItem> getAll() {
        List<SongItem> result = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_SONGS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                result.add(new SongItem(
                        obj.getString("videoId"),
                        obj.getString("title"),
                        obj.getString("author")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void syncFromFirebase() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.getLikedSongs() != null) {
                    saveLocally(user.getLikedSongs());
                }
            }
        });
    }

    public void clearLocalData() {
        prefs.edit().remove(KEY_SONGS).apply();
    }

    private void syncToFirebase(List<SongItem> songs) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).update("likedSongs", songs);
    }

    private void saveLocally(List<SongItem> songs) {
        try {
            JSONArray arr = new JSONArray();
            for (SongItem song : songs) {
                JSONObject obj = new JSONObject();
                obj.put("videoId", song.videoId);
                obj.put("title",   song.title);
                obj.put("author",  song.author);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_SONGS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}