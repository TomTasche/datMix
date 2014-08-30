package at.tomtasche.datmix;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;

public class MixFragment extends Fragment implements
		PlayerNotificationCallback, ConnectionStateCallback,
		Player.InitializationObserver {

	private static final String EXTRA_PLAYLIST_URI = "playlist_uri";

	private SpotifyBridge spotifyBridge;

	private String playlistUri;

	private Player player;

	public static MixFragment newInstance(String accessToken, String playlistUri) {
		MixFragment mixFragment = new MixFragment();

		Bundle args = new Bundle();
		args.putString(SpotifyBridge.EXTRA_ACCESS_TOKEN, accessToken);
		args.putString(EXTRA_PLAYLIST_URI, playlistUri);

		mixFragment.setArguments(args);

		return mixFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		spotifyBridge = new SpotifyBridge(getArguments());

		playlistUri = getArguments().getString(EXTRA_PLAYLIST_URI);

		player = spotifyBridge.getSpotify().getPlayer(getActivity(), "datMix",
				this, this);
	}

	@Override
	public void onInitialized() {
		player.addConnectionStateCallback(this);
		player.addPlayerNotificationCallback(this);

		player.play(playlistUri);
	}

	@Override
	public void onError(Throwable throwable) {
		Log.e("MainActivity",
				"Could not initialize player: " + throwable.getMessage());
	}

	@Override
	public void onLoggedIn() {
		Log.d("MainActivity", "User logged in");
	}

	@Override
	public void onLoggedOut() {
		Log.d("MainActivity", "User logged out");
	}

	@Override
	public void onTemporaryError() {
		Log.d("MainActivity", "Temporary error occurred");
	}

	@Override
	public void onNewCredentials(String s) {
		Log.d("MainActivity", "User credentials blob received");
	}

	@Override
	public void onConnectionMessage(String message) {
		Log.d("MainActivity", "Received connection message: " + message);
	}

	@Override
	public void onPlaybackEvent(EventType eventType) {
		Log.d("MainActivity", "Playback event received: " + eventType.name());

		if (eventType == EventType.PLAY) {
			player.setShuffle(true);
		}
	}

	@Override
	public void onResume() {
		player.resume();

		super.onResume();
	}

	@Override
	public void onPause() {
		player.pause();

		super.onPause();
	}

	@Override
	public void onStop() {
		Spotify.destroyPlayer(this);

		super.onStop();
	}
}
