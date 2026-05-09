package com.example.damonhole;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String name;
    private String firstName;
    private String lastName;
    private String mobile;
    private String email;
    private String profileImageUrl;
    private List<SongItem> likedSongs;
    private List<Playlist> playlists;

    public User() {
        // Required for Firebase
    }

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.likedSongs = new ArrayList<>();
        this.playlists = new ArrayList<>();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public List<SongItem> getLikedSongs() { return likedSongs; }
    public void setLikedSongs(List<SongItem> likedSongs) { this.likedSongs = likedSongs; }

    public List<Playlist> getPlaylists() { return playlists; }
    public void setPlaylists(List<Playlist> playlists) { this.playlists = playlists; }
}