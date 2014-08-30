package at.tomtasche.datmix;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import at.tomtasche.datmix.PlaylistsFragment.PlaylistListener;

public class MainActivity extends Activity implements PlaylistListener {

	private static final String FRAGMENT_TAG_PLAYLISTS = "playlists";
	private static final String FRAGMENT_TAG_MIX = "mix";

	private String accessToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AuthenticationUtil.startAuthentication(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Uri uri = intent.getData();
		if (uri != null) {
			accessToken = AuthenticationUtil.finishAuthentication(uri);

			PlaylistsFragment playlistsFragment = PlaylistsFragment
					.newInstance(accessToken);

			FragmentTransaction transaction = getFragmentManager()
					.beginTransaction();
			transaction.add(android.R.id.content, playlistsFragment,
					FRAGMENT_TAG_PLAYLISTS);
			transaction.commit();
		}
	}

	@Override
	public void onPlaylistSelected(String playlistId) {
		Fragment playlistsFragment = getFragmentManager().findFragmentByTag(
				FRAGMENT_TAG_PLAYLISTS);
		if (playlistsFragment == null) {
			throw new IllegalStateException("playlist-fragment missing");
		}

		MixFragment mixFragment = MixFragment.newInstance(accessToken,
				playlistId);

		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.remove(playlistsFragment);
		transaction.add(android.R.id.content, mixFragment, FRAGMENT_TAG_MIX);
		transaction.addToBackStack(FRAGMENT_TAG_PLAYLISTS + "-to-"
				+ FRAGMENT_TAG_MIX);
		transaction.commit();
	}
}