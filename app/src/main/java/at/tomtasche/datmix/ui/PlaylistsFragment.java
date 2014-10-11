package at.tomtasche.datmix.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import java.util.LinkedList;
import java.util.List;

import at.tomtasche.datmix.service.SpotifyService;
import at.tomtasche.datmix.spotify.rest.Playlist;

public class PlaylistsFragment extends ListFragment implements
        OnItemClickListener {

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;
    private Handler mainHandler;

    private PlaylistListener listener;

    private List<Playlist> playlists;

    private ArrayAdapter<Playlist> adapter;

    private ServiceConnection serviceConnection;
    private SpotifyService.SpotifyBridge spotifyBridge;

    public static PlaylistsFragment newInstance(String accessToken) {
        PlaylistsFragment mixFragment = new PlaylistsFragment();
        return mixFragment;
    }

    public PlaylistsFragment() {
        playlists = new LinkedList<Playlist>();

        serviceConnection = new SpotifyServiceConnection();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundThread = new HandlerThread("spotify-thread");
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());
        mainHandler = new Handler();

        backgroundHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    playlists = spotifyBridge.getPlaylists();

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

        adapter = new ArrayAdapter<Playlist>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1,
                playlists);

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

        Intent intent = new Intent(getActivity(), SpotifyService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Playlist playlist = adapter.getItem(position);
        listener.onPlaylistSelected(playlist.getId());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        getActivity().unbindService(serviceConnection);
    }

    @Override
    public void onStop() {
        super.onStop();

        backgroundThread.quit();
    }

    public interface PlaylistListener {

        public void onPlaylistSelected(String playlistId);
    }

    public class SpotifyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            spotifyBridge = (SpotifyService.SpotifyBridge) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            spotifyBridge = null;
        }
    }
}
