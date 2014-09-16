package at.tomtasche.datmix;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.methods.PlaylistRequest;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.PlaylistTrack;
import com.wrapper.spotify.models.User;

public class MixFragment extends ListFragment implements
		PlayerNotificationCallback, ConnectionStateCallback,
		Player.InitializationObserver, OnItemClickListener, OnClickListener {

	private static final String LOG_TAG = "datMix";

	private static final String EXTRA_PLAYLIST_ID = "playlist_id";
	private static final String EXTRA_MODE = "mode";

	private HandlerThread backgroundThread;

	private Handler backgroundHandler;
	private Handler mainHandler;

	private SpotifyBridge spotifyBridge;
	private Player player;

	private String playlistId;
	private MixMode mode;

	private List<String> trackNames;
	private List<String> trackUris;

	private int currentlyPlayingIndex;

	private boolean isUiReady;
	private Object lock;

	private ArrayAdapter<String> adapter;

	private View rootView;
	private TextView emptyTextView;

	public static MixFragment newInstance(String accessToken,
			String playlistId, MixMode mode) {
		MixFragment mixFragment = new MixFragment();

		Bundle args = new Bundle();
		args.putString(SpotifyBridge.EXTRA_ACCESS_TOKEN, accessToken);
		args.putString(EXTRA_PLAYLIST_ID, playlistId);
		args.putInt(EXTRA_MODE, mode.ordinal());

		mixFragment.setArguments(args);

		return mixFragment;
	}

	public MixFragment() {
		trackNames = new LinkedList<String>();
		trackUris = new LinkedList<String>();

		lock = new Object();

		currentlyPlayingIndex = 0;
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

		int modeOrdinal = getArguments().getInt(EXTRA_MODE);
		mode = MixMode.values()[modeOrdinal];

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
					
					

					Collections.reverse(trackNames);
					Collections.reverse(trackUris);

					synchronized (lock) {
						if (!isUiReady) {
							lock.wait();
						}
					}

					mainHandler.post(new Runnable() {

						@Override
						public void run() {
							adapter.notifyDataSetChanged();

							emptyTextView.setVisibility(View.GONE);
							getListView().setVisibility(View.VISIBLE);
						}
					});
				} catch (Exception e) {
					Log.e(LOG_TAG,
							"something went wrong fetching playlist with id "
									+ playlistId, e);
				}
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.list_with_bar, container, false);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getActivity().setTitle("Choose a track");

		adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, android.R.id.text1,
				trackNames);

		setListAdapter(adapter);

		getListView().setOnItemClickListener(this);

		emptyTextView = (TextView) rootView.findViewById(R.id.text_empty);

		// TODO: only while playing
		getListView().setKeepScreenOn(true);

		rootView.findViewById(R.id.button_pause).setOnClickListener(this);
		rootView.findViewById(R.id.button_skip).setOnClickListener(this);

		synchronized (lock) {
			isUiReady = true;

			lock.notify();
		}
	}

	@Override
	public void onResume() {
		player.resume();

		super.onResume();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		startPlaying(position);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_pause:
			if (player.isPlaying()) {
				player.pause();
			} else {
				player.resume();
			}

			break;
		case R.id.button_skip:
			skipTrack();

			break;
		default:
			break;
		}
	}

	private synchronized void startPlaying(int position) {
		// hack because we don't have proper callbacks for the currently playing
		// song right now
		currentlyPlayingIndex = position - 1;

		playTrack(position);
	}

	private synchronized void playTrack(int position) {
		if (position >= trackUris.size()) {
			Log.d(LOG_TAG, "cant play track at position " + position + " of "
					+ trackUris.size());

			return;
		}

		String trackUri = trackUris.get(position);
		Log.d(LOG_TAG, "playing track with name " + trackNames.get(position));

		player.play(trackUri);
	}

	private void queueTrack(int position) {
		if (position >= trackUris.size()) {
			Log.d(LOG_TAG, "cant queue track at position " + position + " of "
					+ trackUris.size());

			return;
		}

		String trackUri = trackUris.get(position);
		Log.d(LOG_TAG, "queuing track with name " + trackNames.get(position));

		player.clearQueue();
		player.queue(trackUri);
	}

	private void skipTrack() {
		int oldTrackIndex = currentlyPlayingIndex;

		startPlaying(currentlyPlayingIndex + 1);

		logTrackSkipped(oldTrackIndex);
	}

	@Override
	public void onInitialized() {
		player.addConnectionStateCallback(this);
		player.addPlayerNotificationCallback(this);
	}

	@Override
	public synchronized void onPlaybackEvent(EventType eventType) {
		Log.d(LOG_TAG, "Playback event received: " + eventType.name());

		if (eventType == EventType.TRACK_CHANGED) {
			currentlyPlayingIndex++;

			logTrackPlayed(currentlyPlayingIndex);

			queueTrack(currentlyPlayingIndex + 1);
		}
	}

	private synchronized void logTrackPlayed(int position) {
		TrackHistory trackHistory = getHistory(position);
		trackHistory.increasePlayCount();
		trackHistory.save();

		Log.d(LOG_TAG, "track with name " + trackNames.get(position)
				+ " and uri " + trackUris.get(position)
				+ " saved with new playcount " + trackHistory.getPlayCount());
	}

	private synchronized void logTrackSkipped(int position) {
		TrackHistory trackHistory = getHistory(position);
		trackHistory.increaseSkipCount();
		trackHistory.save();

		Log.d(LOG_TAG, "track with name " + trackNames.get(position)
				+ " and uri " + trackUris.get(position)
				+ " saved with new skipcount " + trackHistory.getSkipCount());
	}

	private TrackHistory getHistory(int position) {
		String trackUri = trackUris.get(position);

		List<TrackHistory> trackHistories = TrackHistory.find(
				TrackHistory.class, "spotify_uri = ?", trackUri);

		TrackHistory trackHistory;
		if (trackHistories.isEmpty()) {
			trackHistory = new TrackHistory(trackUri);
		} else {
			trackHistory = trackHistories.get(0);
		}

		return trackHistory;
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
	public void onPause() {
		player.pause();

		super.onPause();
	}

	@Override
	public void onStop() {
		Spotify.destroyPlayer(this);

		backgroundThread.interrupt();
		backgroundThread.quit();

		super.onStop();
	}

	public enum MixMode {
		RADIO, AWESOME;
	}
}
