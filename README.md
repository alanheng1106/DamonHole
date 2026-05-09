# DamonHole 🎵

DamonHole is a feature-rich, open-source Android music player that leverages the power of the NewPipe Extractor to provide a seamless streaming experience from YouTube's vast library, all wrapped in a modern Material 3 design.

![Banner](https://img.shields.io/badge/DamonHole-Music-blue?style=for-the-badge&logo=android)
![Version](https://img.shields.io/badge/Version-1.0-green?style=for-the-badge)

## ✨ Features

- **🚀 YouTube Streaming**: Search and stream high-quality audio directly from YouTube without premium subscriptions.
- **🎨 Material 3 Design**: Clean, modern interface with support for Dynamic Colors and Dark Mode.
- **🔑 User Authentication**: Secure login and registration powered by Firebase Authentication.
- **📂 Playlist Management**: Create custom playlists, add your favorite tracks, and sync them across devices via Firebase Firestore.
- **❤️ Liked Songs**: Save your favorite tracks with a single tap.
- **🎚️ Advanced Equalizer**: Customize your sound with system-integrated equalizer support and various presets.
- **🎧 Background Playback**: Keep the music going even when the screen is off or you're using other apps.
- **🔄 Auto-Updates**: Stay up to date with built-in background and manual update checking.
- **🌍 Multi-language Support**: Available in English, Chinese (Simplified), and Bahasa Melayu.

## 🛠️ Tech Stack

- **Android SDK**: Written in Java, targeting API 35 (Min SDK 26).
- **Backend**: [Firebase](https://firebase.google.com/) (Auth, Firestore, Analytics).
- **Extractor**: [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) for YouTube content discovery.
- **Media**: [Media3 (ExoPlayer)](https://developer.android.com/guide/topics/media/media3) for high-quality audio playback.
- **Image Loading**: [Glide](https://github.com/bumptech/glide) for smooth image and artwork rendering.

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala or later.
- Java Development Kit (JDK) 11.
- A Firebase project (for authentication and data syncing).

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/alanheng1106/DamonHole.git
   ```

2. **Add Firebase**:
   - Create a project on the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.example.damonhole`.
   - Download the `google-services.json` file and place it in the `app/` directory.

3. **Build and Run**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Connect your Android device or emulator and click **Run**.

## 🔄 Update System Configuration

To use the built-in update system:

1. Host an `update.json` file on a server or GitHub.
2. The JSON structure should look like this:
   ```json
   {
     "versionCode": 2,
     "versionName": "1.1",
     "updateUrl": "https://link-to-your-new-apk.apk"
   }
   ```
3. Update the `UPDATE_JSON_URL` in `com.example.damonhole.UpdateManager` with your raw file URL.

## 🤝 Contributing

Contributions are welcome! If you have suggestions or want to fix bugs, feel free to open an issue or submit a pull request.

## ⚖️ License

This project is for educational purposes. Please ensure you comply with YouTube's Terms of Service when using or distributing this application.

---
Built with ❤️ by [alanheng1106](https://github.com/alanheng1106)
