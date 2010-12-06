package com.androsz.electricsleepbeta.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.androsz.electricsleepbeta.preference.CustomTitlebarPreferenceActivity;

public class SettingsActivity extends CustomTitlebarPreferenceActivity {

	public static double DEFAULT_MIN_SENSITIVITY = 0;
	public static double DEFAULT_ALARM_SENSITIVITY = 0.5;
	public static double MAX_ALARM_SENSITIVITY = 2;

	@Override
	protected int getContentAreaLayoutId() {
		// TODO Auto-generated method stub
		return R.xml.settings;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new Thread(new Runnable() {

			@Override
			public void run() {
				getPreferenceScreen().findPreference(
						getText(R.string.pref_alarms))
						.setOnPreferenceClickListener(
								new OnPreferenceClickListener() {

									@Override
									public boolean onPreferenceClick(
											final Preference preference) {

										// startActivity(AlarmDatabase
										// .changeAlarmSettings(getPackageManager()));
										startActivity(new Intent(
												SettingsActivity.this,
												AlarmClock.class));
										return true;
									}
								});

				getPreferenceScreen().findPreference(
						getText(R.string.pref_calibration))
						.setOnPreferenceClickListener(
								new OnPreferenceClickListener() {

									@Override
									public boolean onPreferenceClick(
											final Preference preference) {
										startActivity(new Intent(
												SettingsActivity.this,
												CalibrationWizardActivity.class));
										return true;
									}
								});

				getPreferenceScreen().findPreference(
						getText(R.string.alarmclock_settings))
						.setOnPreferenceClickListener(
								new OnPreferenceClickListener() {

									@Override
									public boolean onPreferenceClick(
											final Preference preference) {
										startActivity(new Intent(
												SettingsActivity.this,
												com.androsz.electricsleepbeta.alarmclock.SettingsActivity.class));
										return true;
									}
								});
			}
		}).start();

		final SharedPreferences serviceIsRunningPrefs = getSharedPreferences(
				"serviceIsRunning", Context.MODE_PRIVATE);
		if (serviceIsRunningPrefs.getBoolean("serviceIsRunning", false)) {
			Toast.makeText(
					this,
					R.string.changes_made_to_these_settings_except_alarms_will_not_take_effect_until_the_next_time_you_start_sleep,
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		new Thread(new Runnable() {

			@Override
			public void run() {
				final SharedPreferences.Editor ed = getSharedPreferences(
						getString(R.string.prefs_version), Context.MODE_PRIVATE)
						.edit();
				ed.putInt(getString(R.string.prefs_version), getResources()
						.getInteger(R.integer.prefs_version));
				ed.commit();
			}
		}).start();

	}
}
