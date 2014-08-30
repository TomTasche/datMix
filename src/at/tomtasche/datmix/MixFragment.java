package at.tomtasche.datmix;

import java.util.LinkedList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.PlaylistTrack;
import com.wrapper.spotify.models.User;

public class MixFragment extends ListFragment implements
		PlayerNotificationCallback, ConnectionStateCallback,
		Player.InitializationObserver, OnItemClickListener {

	private static final String EXTRA_PLAYLIST_ID = "playlist_id";

	private HandlerThread backgroundThread;

	private Handler backgroundHandler;
	private Handler mainHandler;

	private SpotifyBridge spotifyBridge;
	private Player player;

	private String playlistId;

	private List<String> trackNames;
	private List<String> trackUris;

	private ArrayAdapter<String> adapter;

	public static MixFragment newInstance(String accessToken, String playlistId) {
		MixFragment mixFragment = new MixFragment();

		Bundle args = new Bundle();
		args.putString(SpotifyBridge.EXTRA_ACCESS_TOKEN, accessToken);
		args.putString(EXTRA_PLAYLIST_ID, playlistId);

		mixFragment.setArguments(args);

		return mixFragment;
	}

	public MixFragment() {
		trackNames = new LinkedList<String>();
		trackUris = new LinkedList<String>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		backgroundThread = new HandlerThread("spotify-thread");
		backgroundThread.start();

		backgroundHandler = new Handler(backgroundThread.getLooper());
		mainHandler = new Handler();

		spotifyBridge = new SpotifyBridge(getArguments());

		playlistId = getArguments().getString(EXTRA_PLAYLIST_ID);

		player = spotifyBridge.getSpotify().getPlayer(getActivity(), "datMix",
				this, this);

		backgroundHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Api api = spotifyBridge.getApi();

					CurrentUserRequest userRequest = api.getMe().build();
					User user = userRequest.get();
					String userId = user.getId();

					PlaylistTracksRequest tracksRequest = api
							.getPlaylistTracks(userId, playlistId).build();
					Page<PlaylistTrack> tracks = tracksRequest.get();

					for (PlaylistTrack track : tracks.getItems()) {
						trackNames.add(track.getTrack().getName());
						trackUris.add(track.getTrack().getUri());
					}

					if (adapter != null) {
						mainHandler.post(new Runnable() {

							@Override
							public void run() {
								adapter.notifyDataSetChanged();
							}
						});
					}
				} catch (Exception e) {
					Log.e("spoti", "something went wrong!", e);
				}
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getActivity().setTitle("Choose a track");

		adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, android.R.id.text1,
				trackNames);

		setListAdapter(adapter);

		setEmptyText("Loading...");

		getListView().setOnItemClickListener(this);
	}

	@Override
	public void onResume() {
		player.resume();

		super.onResume();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String trackUri = trackUris.get(position);
		player.play(trackUri);
	}

	@Override
	public void onInitialized() {
		player.addConnectionStateCallback(this);
		player.addPlayerNotificationCallback(this);
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
	public void onPause() {
		player.pause();

		super.onPause();
	}

	@Override
	public void onStop() {
		Spotify.destroyPlayer(this);

		backgroundThread.quit();

		super.onStop();
	}
}
