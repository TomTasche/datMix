package at.tomtasche.datmix;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.methods.UserPlaylistsRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.SimplePlaylist;
import com.wrapper.spotify.models.User;

public class MixFragment extends Fragment implements
		PlayerNotificationCallback, ConnectionStateCallback,
		Player.InitializationObserver {

	private static final String ARGUMENT_ACCESS_TOKEN = "access_token";

	private HandlerThread handlerThread;

	private Handler handler;

	private Spotify spotify;
	private Player player;

	private Api api;

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
		spotify = new Spotify(accessToken);

		player = spotify.getPlayer(getActivity(), "datMix", this, this);

		handlerThread = new HandlerThread("spotify-thread");
		handlerThread.start();

		handler = new Handler(handlerThread.getLooper());

		api = Api.builder().clientId(AuthenticationUtil.CLIENT_ID)
				.clientSecret("5387f3eee6ae4395bfdf503200c0ffdf")
				.accessToken(accessToken).build();
	}

	@Override
	public void onInitialized() {
		player.addConnectionStateCallback(this);
		player.addPlayerNotificationCallback(this);

		handler.post(new Runnable() {

			@Override
			public void run() {
				try {
					CurrentUserRequest userRequest = api.getMe().build();
					User user = userRequest.get();
					String userId = user.getId();

					Log.e("spoti", userId);

					UserPlaylistsRequest request = api.getPlaylistsForUser(
							userId).build();
					Page<SimplePlaylist> playlists = request.get();
					for (SimplePlaylist playlist : playlists.getItems()) {
						Log.e("spoti", playlist.getName());
					}

					String playlistUri = playlists.getItems().get(0).getUri();
					player.play(playlistUri);
				} catch (Exception e) {
					Log.e("spoti", "something went wrong!", e);
				}
			}
		});
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
