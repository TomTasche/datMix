package at.tomtasche.datmix;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends Activity {

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
			String accessToken = AuthenticationUtil.finishAuthentication(uri);

			MixFragment mixFragment = MixFragment.newInstance(accessToken);

			FragmentTransaction transaction = getFragmentManager()
					.beginTransaction();
			transaction.add(android.R.id.content, mixFragment);
			transaction.commit();
		}
	}
}