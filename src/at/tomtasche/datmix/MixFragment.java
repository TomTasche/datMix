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

	private static final String ARGUMENT_ACCESS_TOKEN = "access_token";

	private Player player;

	public static MixFragment newInstance(String accessToken) {
		MixFragment mixFragment = new MixFragment();

		Bundle args = new Bundle();
		args.putString(ARGUMENT_ACCESS_TOKEN, accessToken);

		mixFragment.setArguments(args);

		return mixFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String accessToken = getArguments().getString(ARGUMENT_ACCESS_TOKEN);
		Spotify spotify = new Spotify(accessToken);

		player = spotify.getPlayer(getActivity(), "DatMix", this, this);
	}

	@Override
	public void onInitialized() {
		player.addConnectionStateCallback(this);
		player.addPlayerNotificationCallback(this);

		player.setShuffle(true);

		player.play("spotify:user:1149138533:playlist:54yXJVZs8TaF8YpOwvQ2In");
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
