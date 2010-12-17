package com.androsz.electricsleepbeta.app;

import java.io.IOException;
import java.io.StreamCorruptedException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.androsz.electricsleepbeta.content.StartSleepReceiver;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepRecord;
import com.androsz.electricsleepbeta.widget.SleepChart;

/**
 * Front-door {@link Activity} that displays high-level features the application
 * offers to users.
 */
public class HomeActivity extends CustomTitlebarActivity {

	private SleepChart sleepChart;

	private void addChartView() throws StreamCorruptedException,
			IllegalArgumentException, IOException, ClassNotFoundException {
		sleepChart = (SleepChart) findViewById(R.id.home_sleep_chart);

		final Cursor cursor = managedQuery(SleepContentProvider.CONTENT_URI,
				null, null, new String[] { getString(R.string.to) },
				SleepRecord.KEY_TITLE);
		final TextView reviewTitleText = (TextView) findViewById(R.id.home_review_title_text);
		if (cursor == null) {
			sleepChart.setVisibility(View.GONE);
			reviewTitleText
					.setText(getString(R.string.home_review_title_text_empty));
		} else {
			cursor.moveToLast();
			sleepChart.setVisibility(View.VISIBLE);
			sleepChart.sync(cursor);
			reviewTitleText.setText(getString(R.string.home_review_title_text));
		}
	}

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_home;
	}
	
	public void onAlarmsClick(final View v) {
		startActivity(new Intent(this, AlarmClock.class));
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// do this before the google analytics tracker can send any data
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		super.onCreate(savedInstanceState);

		// showTitleButton1(R.drawable.ic_title_share);
		// showTitleButton2(R.drawable.ic_title_refresh);
		setHomeButtonAsLogo();

		final SharedPreferences userPrefs = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				getString(R.string.prefs_version), 0);
		if (prefsVersion == 0) {
			startActivity(new Intent(this, WelcomeTutorialWizardActivity.class)
					.putExtra("required", true));
		} else {
			try {
				analytics.trackEvent("version", getPackageManager()
						.getPackageInfo(this.getPackageName(), 0).versionName,
						"", 0);
			} catch (final NameNotFoundException e) {
				analytics.trackEvent("version", "???", "", 0);
			}
			StartSleepReceiver.enforceCalibrationBeforeStartingSleep(this,
					null, null);
		}
	}
	
	public void onHistoryClick(final View v) {
		startActivity(new Intent(this, HistoryActivity.class));
	}

	@Override
	public void onHomeClick(final View v) {
		// do nothing b/c home is home!
	}

	@Override
	protected void onPause() {
		super.onPause();
		// removeChartView();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {
		try {
			super.onRestoreInstanceState(savedState);
		} catch (final RuntimeException re) {
		}
		sleepChart = (SleepChart) savedState.getSerializable("sleepChart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			addChartView();
		} catch (final StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("sleepChart", sleepChart);
	}

	public void onSleepClick(final View v) throws Exception {

		sendBroadcast(new Intent(StartSleepReceiver.START_SLEEP));
	}
}
