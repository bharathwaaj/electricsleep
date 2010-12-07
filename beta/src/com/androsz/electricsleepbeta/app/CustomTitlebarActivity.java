package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class CustomTitlebarActivity extends Activity {

	protected GoogleAnalyticsTracker analytics;

	protected abstract int getContentAreaLayoutId();

	public void hideTitleButton1() {
		final ImageButton btn1 = (ImageButton) findViewById(R.id.title_button_1);
		btn1.setVisibility(View.INVISIBLE);
		findViewById(R.id.title_sep_1).setVisibility(View.INVISIBLE);
	}

	public void hideTitleButton2() {
		final ImageButton btn2 = (ImageButton) findViewById(R.id.title_button_2);
		btn2.setVisibility(View.INVISIBLE);
		findViewById(R.id.title_sep_2).setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		analytics = GoogleAnalyticsTracker.getInstance();
		analytics.start(getString(R.string.analytics_ua_number), this);
		analytics.trackPageView("/" + getLocalClassName());

		getWindow().setFormat(PixelFormat.RGBA_8888);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		View layoutRoot = this.getLayoutInflater().inflate(getContentAreaLayoutId(), null);
		layoutRoot.setBackgroundResource(R.drawable.gradient_background_vert);
		this.setContentView(layoutRoot);
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titlebar);

		TextView title = (TextView) findViewById(R.id.title_text);
		title.setText(getTitle());
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.titlebar_menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		final SharedPreferences userPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		final boolean analyticsOn = userPrefs.getBoolean(
				getString(R.string.pref_analytics), true);
		if (analyticsOn) {
			analytics.dispatch();
		}
		analytics.stop();
	}

	public void onHomeClick(final View v) {
		final Intent intent = new Intent(v.getContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuItemTutorial:
			startActivity(new Intent(this, WelcomeTutorialWizardActivity.class));
			break;
		case R.id.menuItemAbout:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.menuItemDonate:
			analytics.trackPageView("donate");
			final Uri marketUri = Uri
					.parse("market://details?id=com.androsz.electricsleepdonate");
			final Intent marketIntent = new Intent(Intent.ACTION_VIEW,
					marketUri);
			startActivity(marketIntent);
			break;
		case R.id.menuItemSettings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.menuItemReport:
			startActivity(new Intent(
					"android.intent.action.VIEW",
					Uri.parse("http://code.google.com/p/electricsleep/issues/entry")));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void setHomeButtonAsLogo() {
		final ImageButton btnHome = (ImageButton) findViewById(R.id.title_home_button);
		btnHome.setImageResource(R.drawable.icon_small);
		btnHome.setEnabled(false);
		btnHome.setFocusable(false);
	}

	public void showTitleButton1(final int drawableResourceId) {
		final ImageButton btn1 = (ImageButton) findViewById(R.id.title_button_1);
		btn1.setVisibility(View.VISIBLE);
		btn1.setImageResource(drawableResourceId);
		findViewById(R.id.title_sep_1).setVisibility(View.VISIBLE);
	}

	public void showTitleButton2(final int drawableResourceId) {
		final ImageButton btn2 = (ImageButton) findViewById(R.id.title_button_2);
		btn2.setVisibility(View.VISIBLE);
		btn2.setImageResource(drawableResourceId);
		findViewById(R.id.title_sep_2).setVisibility(View.VISIBLE);
	}
}
