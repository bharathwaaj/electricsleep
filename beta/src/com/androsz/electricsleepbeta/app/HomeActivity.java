package com.androsz.electricsleepbeta.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.service.SleepAccelerometerService;
import com.androsz.electricsleepbeta.view.SleepChartView;

/**
 * Front-door {@link Activity} that displays high-level features the application
 * offers to users.
 */
public class HomeActivity extends CustomTitlebarActivity {

	private SleepChartView sleepChartView;

	private void addChartView() {
		sleepChartView = (SleepChartView) findViewById(R.id.home_sleep_chart);

		final Cursor cursor = managedQuery(SleepContentProvider.CONTENT_URI,
				null, null, new String[] { getString(R.string.to) },
				SleepHistoryDatabase.KEY_SLEEP_DATE_TIME + " DESC");

		final TextView reviewTitleText = (TextView) findViewById(R.id.home_review_title_text);
		if (cursor == null) {
			sleepChartView.setVisibility(View.GONE);
			reviewTitleText
					.setText(getString(R.string.home_review_title_text_empty));
		} else {
			cursor.moveToLast();
			sleepChartView.setVisibility(View.VISIBLE);
			sleepChartView.syncWithCursor(cursor);
			reviewTitleText.setText(getString(R.string.home_review_title_text));
		}
	}

	private void enforceCalibrationBeforeStartingSleep(final Intent service,
			final Intent activity) {

		final SharedPreferences userPrefs = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				getString(R.string.prefs_version), 0);
		String message = "";
		if (prefsVersion == 0) {
			message = getString(R.string.message_not_calibrated);
		} else if (prefsVersion != getResources().getInteger(
				R.integer.prefs_version)) {
			message = getString(R.string.message_prefs_not_compatible);
			PreferenceManager.getDefaultSharedPreferences(this).edit().clear()
					.commit();
			PreferenceManager.setDefaultValues(this, R.xml.settings, true);
		}

		if (message.length() > 0) {
			message += getString(R.string.message_recommend_calibration);
			final AlertDialog.Builder dialog = new AlertDialog.Builder(
					HomeActivity.this)
					.setMessage(message)
					.setCancelable(false)
					.setPositiveButton(getString(R.string.calibrate),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(HomeActivity.this,
											CalibrationWizardActivity.class));
								}
							})
					.setNeutralButton(getString(R.string.manual),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(HomeActivity.this,
											SettingsActivity.class));
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			dialog.show();
		} else if (service != null && activity != null) {
			startService(service);
			startActivity(activity);
		}
	}

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_home;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// do this before the google analytics tracker can send any data
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		super.onCreate(savedInstanceState);

		showTitleButton1(R.drawable.ic_title_share);
		// showTitleButton2(R.drawable.ic_title_refresh);
		setHomeButtonAsLogo();

		final SharedPreferences userPrefs = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				getString(R.string.prefs_version), 0);
		if (prefsVersion == 0) {
			SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor accelerometer = sensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (accelerometer != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("Vendor: " + accelerometer.getVendor());
				sb.append(" | Version: " + accelerometer.getVersion());
				sb.append(" | Range: " + accelerometer.getMaximumRange());
				sb.append(" | Power: " + accelerometer.getPower());
				sb.append(" | Resolution: " + accelerometer.getResolution());
				sb.append(" | Type: " + accelerometer.getType());
				analytics.trackEvent("accelerometer", accelerometer.getName(),
						sb.toString(), 0);
			}
			startActivity(new Intent(this, WelcomeTutorialWizardActivity.class)
					.putExtra("required", true));
		} else {
			enforceCalibrationBeforeStartingSleep(null, null);
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
		sleepChartView = (SleepChartView) savedState
				.getSerializable("sleepChartView");
	}

	@Override
	protected void onResume() {
		super.onResume();
		addChartView();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("sleepChartView", sleepChartView);
	}

	public void onSleepClick(final View v) throws Exception {

		final SharedPreferences userPrefs = PreferenceManager
				.getDefaultSharedPreferences(HomeActivity.this);
		final double minSensitivity = userPrefs.getFloat(
				getString(R.string.pref_minimum_sensitivity), -1);
		final double alarmTriggerSensitivity = userPrefs.getFloat(
				getString(R.string.pref_alarm_trigger_sensitivity), -1);

		final boolean useAlarm = userPrefs.getBoolean(
				getString(R.string.pref_use_alarm), false);
		final int alarmWindow = Integer.parseInt(userPrefs.getString(
				getString(R.string.pref_alarm_window), "-1"));
		final boolean airplaneMode = userPrefs.getBoolean(
				getString(R.string.pref_airplane_mode), false);

		if (minSensitivity < 0 || alarmTriggerSensitivity < 0 || useAlarm
				&& alarmWindow < 0) {
			final AlertDialog.Builder dialog = new AlertDialog.Builder(
					HomeActivity.this)
					.setMessage(getString(R.string.invalid_settings))
					.setCancelable(false)
					.setPositiveButton("Calibrate",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(HomeActivity.this,
											CalibrationWizardActivity.class));
								}
							})
					.setNeutralButton("Manual",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(HomeActivity.this,
											SettingsActivity.class));
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			dialog.show();
			return;
		}

		final Intent serviceIntent = new Intent(HomeActivity.this,
				SleepAccelerometerService.class);
		serviceIntent.putExtra("min", minSensitivity);
		serviceIntent.putExtra("alarm", alarmTriggerSensitivity);
		serviceIntent.putExtra("useAlarm", useAlarm);
		serviceIntent.putExtra("alarmWindow", alarmWindow);
		serviceIntent.putExtra("airplaneMode", airplaneMode);
		enforceCalibrationBeforeStartingSleep(serviceIntent, new Intent(
				HomeActivity.this, SleepActivity.class));
	}

	public void onTitleButton1Click(final View v) {
		Toast.makeText(this,
				"this will be used to share the app with friends later...",
				Toast.LENGTH_SHORT).show();
	}
}
