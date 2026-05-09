package com.example.damonhole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist {
    public String id;
    public String name;
    public String author;
    public List<SongItem> songs;
    public String sortOrder = "custom"; // "date", "title_az", "title_za", "custom"

    public Playlist() {
        // Required for Firebase
    }

    public Playlist(String name, String author) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.author = author;
        this.songs = new ArrayList<>();
    }

    public Playlist(String id, String name, String author, List<SongItem> songs, String sortOrder) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.songs = songs;
        this.sortOrder = sortOrder;
    }
}