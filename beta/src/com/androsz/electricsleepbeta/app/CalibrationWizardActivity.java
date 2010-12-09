package com.androsz.electricsleepbeta.app;

import java.util.Locale;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.util.DeviceUtil;

public class CalibrationWizardActivity extends CustomTitlebarWizardActivity
		implements OnInitListener {

	private class DelayedStartAlarmCalibrationTask extends
			AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(final Void... params) {

			try {
				Thread.sleep(DELAYED_START_TIME);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			notifyUser(CalibrationWizardActivity.this
					.getString(R.string.move_once));
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
					CalibrateForResultActivity.class), R.id.alarmTest);

			notifyUser(CalibrationWizardActivity.this
					.getString(R.string.starting_in));
		}
	}

	private double alarmTriggerCalibration;

	private TextToSpeech textToSpeech;

	private boolean ttsAvailable = false;

	private boolean useTTS = false;
	private static AsyncTask<Void, Void, Void> currentTask;
	private static final int TEST_TTS_INSTALLED = 0x1337;

	public static final int ALARM_CALIBRATION_TIME = 500;

	private static final int DELAYED_START_TIME = 0;

	private void checkTextToSpeechInstalled() {
		final Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		try {

			startActivityForResult(checkIntent, TEST_TTS_INSTALLED);
		} catch (final ActivityNotFoundException re) {
			// prevents crash from:
			// No Activity found to handle Intent {
			// act=android.speech.tts.engine.CHECK_TTS_DATA }
		}
	}

	@Override
	protected int getWizardLayoutId() {
		return R.layout.wizard_calibration;
	}

	private void notifyUser(final String message) {
		notifyUser(message, true);
	}

	private void notifyUser(final String message, final boolean toast) {
		if (ttsAvailable && useTTS && textToSpeech != null) {
			textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
		}
		if (toast) {
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent data) {
		if (resultCode == CalibrateForResultActivity.CALIBRATION_FAILED) {
			notifyUser(getString(R.string.calibration_failed));
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
			notifyUser(String.format("%s %.2f",
					getString(R.string.alarm_trigger_sensitivity_set_to),
					alarmTriggerCalibration));
			stopService(new Intent(this, SleepAccelerometerService.class));
			break;
		case TEST_TTS_INSTALLED:
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// success, create the TTS instance
				if (textToSpeech == null) {
					textToSpeech = new TextToSpeech(this, this);
				}
			} else {
				// missing data, install it
				final Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
			return;
		}
		viewFlipper.showNext();
		setupNavigationButtons();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showTitleButton1(android.R.drawable.ic_lock_silent_mode);
		checkTextToSpeechInstalled();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (textToSpeech != null) {
			textToSpeech.shutdown();
		}
	}

	@Override
	protected void onFinishWizardActivity() {
		final SharedPreferences.Editor ed = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext()).edit();
		ed.putFloat(getString(R.string.pref_alarm_trigger_sensitivity),
				(float) alarmTriggerCalibration);
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
		finish();
	}

	@Override
	public void onInit(final int arg0) {
		if (arg0 == TextToSpeech.SUCCESS) {
			if (textToSpeech.isLanguageAvailable(Locale.ENGLISH) == TextToSpeech.LANG_AVAILABLE) {
				textToSpeech.setLanguage(Locale.US);
				ttsAvailable = true;
				return;
			}
		}
		ttsAvailable = true;
	}

	@Override
	protected void onPrepareLastSlide() {
		final TextView textViewAlarm = (TextView) findViewById(R.id.alarmResult);
		textViewAlarm.setText(String.format("%.2f", alarmTriggerCalibration));
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {

		super.onRestoreInstanceState(savedState);

		viewFlipper.setDisplayedChild(savedState.getInt("child"));

		alarmTriggerCalibration = savedState.getDouble("alarm");

		useTTS = savedState.getBoolean("useTTS");
		updateTitleButtonTTS();

		setupNavigationButtons();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("child", viewFlipper.getDisplayedChild());

		outState.putDouble("alarm", alarmTriggerCalibration);
		outState.putBoolean("useTTS", useTTS);
	}

	public void onTitleButton1Click(final View v) {
		final int messageId = (useTTS = !useTTS) ? R.string.message_tts_on
				: R.string.message_tts_off;

		updateTitleButtonTTS();
		notifyUser(getString(messageId));
	}

	@Override
	protected boolean onWizardActivity() {
		boolean didActivity = false;
		final int currentChildId = viewFlipper.getCurrentView().getId();
		switch (currentChildId) {
		case R.id.alarmTest:
			currentTask = new DelayedStartAlarmCalibrationTask().execute(null,
					null, null);
			didActivity = true;
			break;
		}
		return didActivity;
	}

	private void updateTitleButtonTTS() {
		showTitleButton1(useTTS ? android.R.drawable.ic_lock_silent_mode_off
				: android.R.drawable.ic_lock_silent_mode);
	}
}
