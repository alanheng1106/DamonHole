package com.example.damonhole;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class AudioService extends MediaSessionService {

    private static final String CHANNEL_ID = "damonhole_playback";

    // Must match AndroidDownloader's User-Agent so YouTube CDN accepts the request
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36";

    private MediaSession mediaSession = null;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000);

        androidx.media3.datasource.ResolvingDataSource.Resolver resolver = new androidx.media3.datasource.ResolvingDataSource.Resolver() {
            @Override
            public androidx.media3.datasource.DataSpec resolveDataSpec(androidx.media3.datasource.DataSpec dataSpec) throws java.io.IOException {
                if ("youtube".equals(dataSpec.uri.getScheme())) {
                    String videoId = dataSpec.uri.getHost();
                    if (videoId == null) videoId = dataSpec.uri.getAuthority();
                    if (videoId != null) {
                        try {
                            String youtubeUrl = "https://www.youtube.com/watch?v=" + videoId;
                            org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor extractor = 
                                (org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor) 
                                org.schabi.newpipe.extractor.ServiceList.YouTube.getStreamExtractor(youtubeUrl);
                            extractor.fetchPage();
                            java.util.List<org.schabi.newpipe.extractor.stream.AudioStream> audioStreams = extractor.getAudioStreams();
                            if (audioStreams != null && !audioStreams.isEmpty()) {
                                org.schabi.newpipe.extractor.stream.AudioStream bestStream = audioStreams.get(0);
                                for (org.schabi.newpipe.extractor.stream.AudioStream stream : audioStreams) {
                                    if (stream.getBitrate() > bestStream.getBitrate()) bestStream = stream;
                                }
                                return dataSpec.withUri(android.net.Uri.parse(bestStream.getContent()));
                            }
                        } catch (Exception e) {
                            throw new java.io.IOException("Failed to resolve YouTube URL", e);
                        }
                    }
                }
                return dataSpec;
            }
        };

        androidx.media3.datasource.ResolvingDataSource.Factory resolvingFactory = new androidx.media3.datasource.ResolvingDataSource.Factory(httpFactory, resolver);

        ExoPlayer player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingFactory))
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                        true
                )
                .setHandleAudioBecomingNoisy(true)
                .build();

        // Media3 handles startForeground() automatically — do NOT call it manually
        mediaSession = new MediaSession.Builder(this, player).build();

        player.addAnalyticsListener(new androidx.media3.exoplayer.analytics.AnalyticsListener() {
            @Override
            public void onAudioSessionIdChanged(androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime eventTime, int audioSessionId) {
                android.content.SharedPreferences prefs = getSharedPreferences("equalizer_prefs", MODE_PRIVATE);
                prefs.edit().putInt("audio_session_id", audioSessionId).apply();
            }
        });
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}