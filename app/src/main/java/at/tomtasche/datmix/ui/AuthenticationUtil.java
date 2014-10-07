package at.tomtasche.datmix.ui;

import android.app.Activity;
import android.net.Uri;

import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;

public class AuthenticationUtil {

	public static final String CLIENT_ID = "d97234a8f169455f8aa4dcea56f509f2";
	public static final String REDIRECT_URI = "reddio-app://spotify-callback";

	public static void startAuthentication(Activity callbackActivity) {
		SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
				new String[] { "user-read-private", "streaming",
						"playlist-read-private" }, null, callbackActivity);
	}

	public static String finishAuthentication(Uri uri) {
		AuthenticationResponse response = SpotifyAuthentication
				.parseOauthResponse(uri);

		return response.getAccessToken();
	}
}
