package at.tomtasche.datmix.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;

import at.tomtasche.datmix.R;
import at.tomtasche.datmix.service.SpotifyService;

public class MainActivity extends Activity implements PlaylistsFragment.PlaylistListener,
        OnClickListener {

    private static final String FRAGMENT_TAG_PLAYLISTS = "playlists";
    private static final String FRAGMENT_TAG_MIX = "mix";

    private MixFragment.MixMode mode;

    private ServiceConnection serviceConnection;
    private SpotifyService.SpotifyBridge spotifyBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        findViewById(R.id.button_mode_radio).setOnClickListener(this);
        findViewById(R.id.button_mode_awesome).setOnClickListener(this);

        AuthenticationUtil.startAuthentication(this);

        serviceConnection = new SpotifyServiceConnection();

        Intent intent = new Intent(this, SpotifyService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null) {
            String accessToken = AuthenticationUtil.finishAuthentication(uri);

            // TODO: might be null here
            spotifyBridge.initialize(accessToken);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_mode_radio) {
            mode = MixFragment.MixMode.RADIO;
        } else if (v.getId() == R.id.button_mode_awesome) {
            mode = MixFragment.MixMode.AWESOME;
        }

        // TODO: use a ModeFragment instead?
        findViewById(R.id.layout_mode_buttons).setVisibility(View.GONE);

        PlaylistsFragment playlistsFragment = PlaylistsFragment
                .newInstance();

        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        transaction.add(android.R.id.content, playlistsFragment,
                FRAGMENT_TAG_PLAYLISTS);
        transaction.commit();
    }

    @Override
    public void onPlaylistSelected(String playlistId) {
        Fragment playlistsFragment = getFragmentManager().findFragmentByTag(
                FRAGMENT_TAG_PLAYLISTS);
        if (playlistsFragment == null) {
            throw new IllegalStateException("playlist-fragment missing");
        }

        MixFragment mixFragment = MixFragment.newInstance(
                playlistId, mode);

        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        transaction.remove(playlistsFragment);
        transaction.add(android.R.id.content, mixFragment, FRAGMENT_TAG_MIX);
        transaction.addToBackStack(FRAGMENT_TAG_PLAYLISTS + "-to-"
                + FRAGMENT_TAG_MIX);
        transaction.commit();
    }

    // TODO: find a cleaner solution to pass this to fragments
    public SpotifyService.SpotifyBridge getSpotifyBridge() {
        return spotifyBridge;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(serviceConnection);
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