package com.androsz.electricsleepbeta.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;

public class CalibrationWizardActivity extends CustomTitlebarWizardActivity {

	private class AlarmCalibrationTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(final Void... params) {

			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			final Intent i = new Intent(CalibrationWizardActivity.this,
					SleepAccelerometerService.class);
			stopService(i);
			i.putExtra("interval", ALARM_CALIBRATION_TIME);
			i.putExtra("alarm", SettingsActivity.MAX_ALARM_SENSITIVITY);
			startService(i);
		}

		@Override
		protected void onPreExecute() {
			startActivityForResult(new Intent(CalibrationWizardActivity.this,
					CalibrateAlarmActivity.class), R.id.alarmTest);
		}
	}

	private class ScreenBugCalibrationTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(final Void... params) {

			// try {
			// Thread.sleep(10000);
			// } catch (final InterruptedException e) {
			// e.printStackTrace();
			// }
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			// sendBroadcast(new Intent(
			// CheckForScreenBugAccelerometerService.BUG_PRESENT));
		}

		@Override
		protected void onPreExecute() {
			startActivityForResult(new Intent(CalibrationWizardActivity.this,
					CheckForScreenBugActivity.class), R.id.screenTest);
			final Intent i = new Intent(CalibrationWizardActivity.this,
					CheckForScreenBugAccelerometerService.class);
			stopService(i);
			startService(i);
		}
	}

	private double alarmTriggerCalibration;
	private boolean screenBugPresent;

	private static AsyncTask<Void, Void, Void> currentTask;

	public static final int ALARM_CALIBRATION_TIME = 500;

	@Override
	protected int getWizardLayoutId() {
		return R.layout.wizard_calibration;
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent data) {
		if (resultCode == CalibrateForResultActivity.CALIBRATION_FAILED) {
			if (currentTask != null) {
				currentTask.cancel(true);
			} else {
				stopService(new Intent(this, SleepAccelerometerService.class));
			}
			return;
		}
		switch (requestCode) {
		case R.id.alarmTest:
			alarmTriggerCalibration = data.getDoubleExtra("y", 0);
			stopService(new Intent(this, SleepAccelerometerService.class));
			break;
		case R.id.screenTest:
			screenBugPresent = data.getAction().equals(
					CheckForScreenBugAccelerometerService.BUG_PRESENT);
			break;
		}
		viewFlipper.showNext();
		setupNavigationButtons();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onFinishWizardActivity() {
		final SharedPreferences.Editor ed = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext()).edit();
		ed.putFloat(getString(R.string.pref_alarm_trigger_sensitivity),
				(float) alarmTriggerCalibration);
		ed.putBoolean(getString(R.string.pref_force_screen), screenBugPresent);
		ed.commit();

		final SharedPreferences.Editor ed2 = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE).edit();
		ed2.putInt(getString(R.string.prefs_version), getResources()
				.getInteger(R.integer.prefs_version));
		ed2.commit();
		ed.commit();

		final SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		final Sensor accelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerometer != null) {
			final StringBuffer sb = new StringBuffer();
			sb.append(Build.MODEL);
			sb.append("|" + Build.BOARD);
			sb.append("|" + accelerometer.getName());
			analytics.trackEvent("calibration-by-hardware", sb.toString(),
					String.format("%.2f", alarmTriggerCalibration), 0);
		} else {
			analytics.trackEvent("calibration-null-accelerometer", "alarm",
					String.format("%.2f", alarmTriggerCalibration), 0);
		}
		analytics.trackEvent("screen-bug-by-hardware", Build.MODEL, ""
				+ screenBugPresent, 0);
		finish();
	}

	@Override
	protected void onPrepareLastSlide() {
		final TextView textViewAlarm = (TextView) findViewById(R.id.alarmResult);
		textViewAlarm.setText(String.format("%.2f", alarmTriggerCalibration));
		final TextView textViewScreen = (TextView) findViewById(R.id.screenResult);
		textViewScreen.setText(screenBugPresent + "");
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {

		super.onRestoreInstanceState(savedState);

		viewFlipper.setDisplayedChild(savedState.getInt("child"));

		alarmTriggerCalibration = savedState.getDouble("alarm");
		screenBugPresent = savedState.getBoolean("screenBug");

		setupNavigationButtons();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("child", viewFlipper.getDisplayedChild());

		outState.putDouble("alarm", alarmTriggerCalibration);
		outState.putBoolean("screenBug", screenBugPresent);
	}

	@Override
	protected boolean onWizardActivity() {
		boolean didActivity = false;
		final int currentChildId = viewFlipper.getCurrentView().getId();
		switch (currentChildId) {
		case R.id.alarmTest:
			currentTask = new AlarmCalibrationTask().execute(null, null, null);
			didActivity = true;
			break;
		case R.id.screenTest:
			currentTask = new ScreenBugCalibrationTask().execute(null, null,
					null);
			didActivity = true;
			break;
		}
		return didActivity;
	}
}
