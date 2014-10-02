package at.tomtasche.datmix;

import android.os.Bundle;

import com.spotify.sdk.android.Spotify;

import at.tomtasche.datmix.spotify.SpotifyService;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class SpotifyBridge {

	public static final String EXTRA_ACCESS_TOKEN = "access_token";

	private String accessToken;

	private SpotifyService api;

	private Spotify spotify;

	public SpotifyBridge(Bundle bundle) {
		this(bundle.getString(EXTRA_ACCESS_TOKEN));
	}

	public SpotifyBridge(String accessToken) {
		this.accessToken = accessToken;
	}

	public SpotifyService getApi() {
		if (api == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("https://api.spotify.com/v1")
                    .setRequestInterceptor(new AccessTokenHeaderInterceptor())
                    .build();

            api = restAdapter.create(SpotifyService.class);
		}

		return api;
	}

	public Spotify getSpotify() {
		if (spotify == null) {
			spotify = new Spotify(accessToken);
		}

		return spotify;
	}

    private class AccessTokenHeaderInterceptor implements RequestInterceptor {

        @Override
        public void intercept(RequestFacade request) {
            request.addHeader("Authorization", "Bearer " + accessToken);
        }
    }
}
