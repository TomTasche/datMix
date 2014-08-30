package at.tomtasche.datmix;

import android.os.Bundle;

import com.spotify.sdk.android.Spotify;
import com.wrapper.spotify.Api;

public class SpotifyBridge {

	public static final String EXTRA_ACCESS_TOKEN = "access_token";

	private String accessToken;

	private Api api;

	private Spotify spotify;

	public SpotifyBridge(Bundle bundle) {
		this(bundle.getString(EXTRA_ACCESS_TOKEN));
	}

	public SpotifyBridge(String accessToken) {
		this.accessToken = accessToken;
	}

	public Api getApi() {
		if (api == null) {
			api = Api.builder().clientId(AuthenticationUtil.CLIENT_ID)
					.clientSecret("5387f3eee6ae4395bfdf503200c0ffdf")
					.accessToken(accessToken).build();
		}

		return api;
	}

	public Spotify getSpotify() {
		if (spotify == null) {
			spotify = new Spotify(accessToken);
		}

		return spotify;
	}
}
