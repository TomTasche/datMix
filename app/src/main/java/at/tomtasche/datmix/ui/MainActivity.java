package at.tomtasche.datmix.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import at.tomtasche.datmix.R;

public class MainActivity extends Activity implements PlaylistsFragment.PlaylistListener,
        OnClickListener {

    private static final String FRAGMENT_TAG_PLAYLISTS = "playlists";
    private static final String FRAGMENT_TAG_MIX = "mix";

    private String accessToken;

    private MixFragment.MixMode mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        findViewById(R.id.button_mode_radio).setOnClickListener(this);
        findViewById(R.id.button_mode_awesome).setOnClickListener(this);

        AuthenticationUtil.startAuthentication(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        if (uri != null) {
            accessToken = AuthenticationUtil.finishAuthentication(uri);
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
                .newInstance(accessToken);

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

        MixFragment mixFragment = MixFragment.newInstance(accessToken,
                playlistId, mode);

        FragmentTransaction transaction = getFragmentManager()
                .beginTransaction();
        transaction.remove(playlistsFragment);
        transaction.add(android.R.id.content, mixFragment, FRAGMENT_TAG_MIX);
        transaction.addToBackStack(FRAGMENT_TAG_PLAYLISTS + "-to-"
                + FRAGMENT_TAG_MIX);
        transaction.commit();
    }
}