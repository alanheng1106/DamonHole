# DamonHole 🎵

![DamonHole Banner](DamonHole.png)

DamonHole is a premium, open-source Android music player that leverages the power of the **NewPipe Extractor** to provide a seamless streaming experience from YouTube's vast library, all wrapped in a modern **Material 3** design.

[![Version](https://img.shields.io/badge/Version-1.1-purple?style=for-the-badge)](https://github.com/alanheng1106/DamonHole/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-Educational-orange?style=for-the-badge)](LICENSE)

## ✨ Features

- **🚀 YouTube Streaming**: Search and stream high-quality audio directly from YouTube without premium subscriptions.
- **📥 Playlist Import**: Easily import public YouTube playlists by simply pasting the link and giving it a name.
- **🎨 Material 3 Design**: Clean, modern interface with support for Dynamic Colors and Dark Mode.
- **🌟 Expressive Visuals**: Features the Material 3 Expressive Loading Indicator (star-shaped) for a playful and modern feel.
- **🔑 User Authentication**: Secure login and registration powered by Firebase Authentication.
- **📂 Playlist Management**: Create custom playlists, add your favorite tracks, and sync them across devices via Firebase Firestore.
- **❤️ Liked Songs**: Save your favorite tracks with a single tap.
- **🎚️ Advanced Equalizer**: Customize your sound with system-integrated equalizer support and various presets.
- **🎧 Background Playback**: Keep the music going even when the screen is off or you're using other apps.
- **🔄 Auto-Updates**: Stay up to date with built-in background and manual update checking.
- **🌍 Multi-language Support**: Fully localized in English, Chinese (Simplified), and Bahasa Melayu.

## 🛠️ Tech Stack

- **Language**: Java (Android SDK, API 35)
- **Backend**: [Firebase](https://firebase.google.com/) (Auth, Firestore, Analytics)
- **Extractor**: [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)
- **Media Engine**: [Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3)
- **UI Components**: Material Components for Android (Material 3)

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala or later.
- JDK 11+.
- A Firebase project with `google-services.json`.

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/alanheng1106/DamonHole.git
   ```

2. **Add Firebase**:
   - Download your `google-services.json` from the Firebase Console.
   - Place it in the `app/` directory.

3. **Build and Run**:
   - Open in Android Studio, sync Gradle, and hit **Run**.

## 🔄 Update System

DamonHole features a self-hosted update system. You can configure it by updating the `UPDATE_JSON_URL` in `UpdateManager.java` to point to your own `update.json` file.

## 🤝 Contributing

Contributions make the open-source community an amazing place to learn and create. Any contributions you make are **greatly appreciated**.

## ⚖️ License

This project is for educational purposes. Please comply with YouTube's Terms of Service when using this application.

---
Built with ❤️ by [alanheng1106](https://github.com/alanheng1106)
