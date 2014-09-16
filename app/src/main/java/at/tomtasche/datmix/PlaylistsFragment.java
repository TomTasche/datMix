package at.tomtasche.datmix;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.wrapper.spotify.Api;
import com.wrapper.spotify.methods.CurrentUserRequest;
import com.wrapper.spotify.methods.UserPlaylistsRequest;
import com.wrapper.spotify.models.Page;
import com.wrapper.spotify.models.SimplePlaylist;
import com.wrapper.spotify.models.User;

public class PlaylistsFragment extends ListFragment implements
		OnItemClickListener {

	private HandlerThread backgroundThread;

	private Handler backgroundHandler;
	private Handler mainHandler;

	private SpotifyBridge spotifyBridge;

	private PlaylistListener listener;

	private List<String> playlistNames;
	private List<String> playlistIds;

	private ArrayAdapter<String> adapter;

	public static PlaylistsFragment newInstance(String accessToken) {
		PlaylistsFragment mixFragment = new PlaylistsFragment();

		Bundle args = new Bundle();
		args.putString(SpotifyBridge.EXTRA_ACCESS_TOKEN, accessToken);

		mixFragment.setArguments(args);

		return mixFragment;
	}

	public PlaylistsFragment() {
		playlistNames = new LinkedList<String>();
		playlistIds = new LinkedList<String>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		backgroundThread = new HandlerThread("spotify-thread");
		backgroundThread.start();

		backgroundHandler = new Handler(backgroundThread.getLooper());
		mainHandler = new Handler();

		spotifyBridge = new SpotifyBridge(getArguments());

		backgroundHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					Api api = spotifyBridge.getApi();

					CurrentUserRequest userRequest = api.getMe().build();
					User user = userRequest.get();
					String userId = user.getId();

					UserPlaylistsRequest request = api.getPlaylistsForUser(
							userId).build();
					Page<SimplePlaylist> playlists = request.get();
					for (SimplePlaylist playlist : playlists.getItems()) {
						playlistNames.add(playlist.getName());
						playlistIds.add(playlist.getId());
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

		getActivity().setTitle("Choose a playlist");

		adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, android.R.id.text1,
				playlistNames);

		setListAdapter(adapter);

		setEmptyText("Loading...");

		getListView().setOnItemClickListener(this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof PlaylistListener) {
			listener = (PlaylistListener) activity;
		} else {
			throw new IllegalArgumentException(
					"activity must implement PlaylistListener");
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String playlistId = playlistIds.get(position);
		listener.onPlaylistSelected(playlistId);
	}

	@Override
	public void onStop() {
		super.onStop();

		backgroundThread.quit();
	}

	public interface PlaylistListener {

		public void onPlaylistSelected(String playlistId);
	}
}