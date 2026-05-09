package com.example.damonhole;

public class SongItem {
    public String videoId;
    public String title;
    public String author;
    public long addedAt;

    public SongItem() {
        // Required for Firebase
    }

    public SongItem(String videoId, String title, String author) {
        this.videoId = videoId;
        this.title   = title;
        this.author  = author;
        this.addedAt = System.currentTimeMillis();
    }

    public SongItem(String videoId, String title, String author, long addedAt) {
        this.videoId = videoId;
        this.title   = title;
        this.author  = author;
        this.addedAt = addedAt;
    }
}
