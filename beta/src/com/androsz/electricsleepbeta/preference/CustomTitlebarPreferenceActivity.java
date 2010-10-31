package com.androsz.electricsleepbeta.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.app.HomeActivity;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public abstract class CustomTitlebarPreferenceActivity extends
		PreferenceActivity {

	protected abstract int getContentAreaLayoutId();

	protected GoogleAnalyticsTracker analytics;

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
		analytics = GoogleAnalyticsTracker.getInstance();
		analytics.start(getString(R.string.analytics_ua_number), this);
		analytics.trackPageView("/" + getLocalClassName());

		getWindow().setFormat(PixelFormat.RGBA_8888);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		setTheme(R.style.Theme_electricsleep);
		final ListView lvw = getListView();
		lvw.setCacheColorHint(0);
		lvw.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.gradient_background_vert));
		addPreferencesFromResource(getContentAreaLayoutId());
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titlebar);
		((TextView) findViewById(R.id.title_text)).setText(getTitle());
	}

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

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// final MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.titlebar_menu, menu);
		return false;
	}

	public void onHomeClick(final View v) {
		final Intent intent = new Intent(v.getContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		/*
		 * switch (item.getItemId()) { case R.id.menuItemDonate: final Uri
		 * marketUri = Uri
		 * .parse("market://details?id=com.androsz.electricsleepbetadonate");
		 * final Intent marketIntent = new Intent(Intent.ACTION_VIEW,
		 * marketUri); startActivity(marketIntent); return true; case
		 * R.id.menuItemSettings: startActivity(new Intent(this,
		 * SettingsActivity.class)); return true; default: return false; }
		 */
		return false;
	}

	public void setHomeButtonAsLogo() {
		final ImageButton btnHome = (ImageButton) findViewById(R.id.title_home_button);
		btnHome.setImageResource(R.drawable.icon);
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
