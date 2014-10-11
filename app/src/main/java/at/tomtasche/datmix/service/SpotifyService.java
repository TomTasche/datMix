package at.tomtasche.datmix.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import at.tomtasche.datmix.spotify.rest.AccessTokenHeaderInterceptor;
import at.tomtasche.datmix.spotify.rest.Me;
import at.tomtasche.datmix.spotify.rest.Paged;
import at.tomtasche.datmix.spotify.rest.Playlist;
import at.tomtasche.datmix.spotify.rest.PlaylistTrack;
import at.tomtasche.datmix.spotify.rest.PositionedTrack;
import at.tomtasche.datmix.spotify.rest.PositionedTracksContainer;
import at.tomtasche.datmix.spotify.rest.SpotifyRestService;
import at.tomtasche.datmix.storage.TrackHistory;
import retrofit.RestAdapter;
import retrofit.client.Response;

/**
 * Created by tom on 10/10/14.
 */
public class SpotifyService extends Service implements PlayerNotificationCallback, ConnectionStateCallback,
        Player.InitializationObserver {

    private static final String LOG_TAG = "datMix";

    private SpotifyRestService api;

    private Spotify spotify;
    private Player player;

    private SpotifyBridge spotifyBridge;
    private SpotifyBridgeListener bridgeListener;

    @Override
    public void onCreate() {
        super.onCreate();

        spotifyBridge = new SpotifyBridge();
    }

    public void initialize(String accessToken) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.spotify.com/v1")
                .setRequestInterceptor(new AccessTokenHeaderInterceptor(accessToken))
                //.setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(SpotifyRestService.class);

        spotify = new Spotify(accessToken);

        player = spotify.getPlayer(this, "datMix", this, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return spotifyBridge;
    }

    @Override
    public void onInitialized() {
        player.addConnectionStateCallback(this);
        player.addPlayerNotificationCallback(this);
    }

    @Override
    public synchronized void onPlaybackEvent(PlayerNotificationCallback.EventType eventType) {
        Log.d(LOG_TAG, "Playback event received: " + eventType.name());

        if (eventType == PlayerNotificationCallback.EventType.TRACK_CHANGED && bridgeListener != null) {
            bridgeListener.onTrackChanged();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e(LOG_TAG, "Could not initialize player: " + throwable.getMessage());
    }

    @Override
    public void onLoggedIn() {
        Log.d(LOG_TAG, "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d(LOG_TAG, "User logged out");
    }

    @Override
    public void onTemporaryError() {
        Log.d(LOG_TAG, "Temporary error occurred");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.d(LOG_TAG, "User credentials blob received");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(LOG_TAG, "Received connection message: " + message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class SpotifyBridge extends Binder {

        public void initialize(String accessToken) {
            SpotifyService.this.initialize(accessToken);
        }

        public void setListener(SpotifyBridgeListener bridgeListener) {
            SpotifyService.this.bridgeListener = bridgeListener;
        }

        public List<Playlist> getPlaylists() {
            Me me = api.getMe();
            String userId = me.getId();

            Paged<Playlist[]> playlists = api.getPlaylists(userId);
            return Arrays.asList(playlists.getItems());
        }

        public List<PlaylistTrack> getTracksForPlaylist(String playlistId) {
            Me me = api.getMe();
            String userId = me.getId();

            Paged<PlaylistTrack[]> tracks = api.getTracks(userId, playlistId);
            return Arrays.asList(tracks.getItems());
        }

        public void rankDownTrack(String playlistId, String trackUri, int trackPosition) {
            Me me = api.getMe();
            String userId = me.getId();
            Integer[] positions = new Integer[]{trackPosition};

            PositionedTrack removeTrack = new PositionedTrack(trackUri, positions);
            PositionedTracksContainer removeTracksContainer = new PositionedTracksContainer(removeTrack);
            Response removeResponse = api.removeTrack(userId, playlistId, removeTracksContainer);
            if (removeResponse.getStatus() != 200) {
                Log.e(LOG_TAG,
                        "something went wrong removing track with uri " + trackUri + " from playlist with id "
                                + playlistId);
            }

            // not nice, but very simple
            // add track again at its old index+1
            positions[0]++;

            PositionedTrack addTrack = new PositionedTrack(trackUri, positions);
            PositionedTracksContainer addTracksContainer = new PositionedTracksContainer(addTrack);
            Response addResponse = api.addTrack(userId, playlistId, Arrays.asList(trackUri), positions[0]);
            if (addResponse.getStatus() != 201) {
                Log.e(LOG_TAG,
                        "something went wrong adding track with uri " + trackUri + " to playlist with id "
                                + playlistId);
            }
        }

        public synchronized void logTrackPlayed(PlaylistTrack track) {
            TrackHistory trackHistory = getHistory(track);
            trackHistory.increasePlayCount();
            trackHistory.save();

            Log.d(LOG_TAG, "track with name " + track.getTrack().getName()
                    + " and uri " + track.getTrack().getUri()
                    + " saved with new playcount " + trackHistory.getPlayCount());
        }

        public synchronized void logTrackSkipped(PlaylistTrack track) {
            TrackHistory trackHistory = getHistory(track);
            trackHistory.increaseSkipCount();
            trackHistory.save();

            Log.d(LOG_TAG, "track with name " + track.getTrack().getName()
                    + " and uri " + track.getTrack().getUri()
                    + " saved with new skipcount " + trackHistory.getSkipCount());
        }

        private TrackHistory getHistory(PlaylistTrack track) {
            List<TrackHistory> trackHistories = TrackHistory.find(
                    TrackHistory.class, "spotify_uri = ?", track.getTrack().getUri());

            TrackHistory trackHistory;
            if (trackHistories.isEmpty()) {
                trackHistory = new TrackHistory(track.getTrack().getUri());
            } else {
                trackHistory = trackHistories.get(0);
            }

            return trackHistory;
        }

        public void playTrack(PlaylistTrack track) {
            player.play(track.getTrack().getUri());
        }

        public void queueTrack(PlaylistTrack track) {
            player.clearQueue();
            player.queue(track.getTrack().getUri());
        }

        public void togglePlaying() {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.resume();
            }
        }

        public void stop() {
            Spotify.destroyPlayer(this);

            stopSelf();
        }
    }

    public interface SpotifyBridgeListener {

        public void onInitialized();

        public void onTrackChanged();
    }
}
