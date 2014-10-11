package at.tomtasche.datmix.ui;

import android.app.Activity;
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

import java.util.LinkedList;
import java.util.List;

import at.tomtasche.datmix.R;
import at.tomtasche.datmix.service.SpotifyService;
import at.tomtasche.datmix.spotify.rest.PlaylistTrack;

public class MixFragment extends ListFragment implements
        OnItemClickListener, OnClickListener, SpotifyService.SpotifyBridgeListener {

    private static final String LOG_TAG = "datMix";

    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_ACCESS_TOKEN = "access_token";

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;
    private Handler mainHandler;

    private String playlistId;
    private MixMode mode;

    private List<PlaylistTrack> tracks;

    private int currentlyPlayingIndex;

    private boolean isUiReady;
    private Object lock;

    private ArrayAdapter<PlaylistTrack> adapter;

    private View rootView;
    private TextView emptyTextView;

    private SpotifyService.SpotifyBridge spotifyBridge;

    public static MixFragment newInstance(
            String playlistId, MixMode mode) {
        MixFragment mixFragment = new MixFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_PLAYLIST_ID, playlistId);
        args.putInt(EXTRA_MODE, mode.ordinal());

        mixFragment.setArguments(args);

        return mixFragment;
    }

    public MixFragment() {
        tracks = new LinkedList<PlaylistTrack>();

        lock = new Object();

        currentlyPlayingIndex = 0;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        spotifyBridge = ((MainActivity) activity).getSpotifyBridge();
        spotifyBridge.setListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundThread = new HandlerThread("spotify-thread");
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());
        mainHandler = new Handler();

        playlistId = getArguments().getString(EXTRA_PLAYLIST_ID);

        int modeOrdinal = getArguments().getInt(EXTRA_MODE);
        mode = MixMode.values()[modeOrdinal];
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

        adapter = new ArrayAdapter<PlaylistTrack>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1,
                tracks);

        setListAdapter(adapter);

        getListView().setOnItemClickListener(this);

        emptyTextView = (TextView) rootView.findViewById(R.id.text_empty);

        rootView.findViewById(R.id.button_pause).setOnClickListener(this);
        rootView.findViewById(R.id.button_skip).setOnClickListener(this);

        synchronized (lock) {
            isUiReady = true;

            lock.notify();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        backgroundHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    tracks.addAll(spotifyBridge.getTracksForPlaylist(playlistId));

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
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        startPlaying(position);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_pause:
                spotifyBridge.togglePlaying();

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
        if (position >= tracks.size()) {
            Log.d(LOG_TAG, "cant play track at position " + position + " of "
                    + tracks.size());

            return;
        }

        PlaylistTrack track = getTrack(position);
        Log.d(LOG_TAG, "playing track with name " + track.getTrack().getName());
        spotifyBridge.playTrack(track);
    }

    private void queueTrack(int position) {
        if (position >= tracks.size()) {
            Log.d(LOG_TAG, "cant queue track at position " + position + " of "
                    + tracks.size());

            return;
        }

        PlaylistTrack track = getTrack(position);
        Log.d(LOG_TAG, "queuing track with name " + track.getTrack().getName());
        spotifyBridge.queueTrack(track);
    }

    private void skipTrack() {
        final int oldTrackIndex = currentlyPlayingIndex;

        startPlaying(currentlyPlayingIndex + 1);

        // TODO: do this in batches (i.e. after the user skipped several tracks already)
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                spotifyBridge.logTrackSkipped(getTrack(oldTrackIndex));

                try {
                    PlaylistTrack track = getTrack(oldTrackIndex);
                    spotifyBridge.rankDownTrack(playlistId, track.getTrack().getUri(), oldTrackIndex);
                } catch (Exception e) {
                    Log.e(LOG_TAG,
                            "something went wrong modifying playlist with id "
                                    + playlistId, e);
                }
            }
        });
    }

    private PlaylistTrack getTrack(int position) {
        return adapter.getItem(position);
    }

    @Override
    public void onInitialized() {
    }

    @Override
    public void onTrackChanged() {
        currentlyPlayingIndex++;

        spotifyBridge.logTrackPlayed(getTrack(currentlyPlayingIndex));

        queueTrack(currentlyPlayingIndex + 1);
    }

    @Override
    public void onStop() {
        super.onStop();

        backgroundThread.interrupt();
        backgroundThread.quit();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        spotifyBridge.setListener(null);
    }

    public enum MixMode {
        RADIO, AWESOME;
    }
}
