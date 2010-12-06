package com.androsz.electricsleepbeta.receiver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.app.CalibrationWizardActivity;
import com.androsz.electricsleepbeta.app.SettingsActivity;
import com.androsz.electricsleepbeta.app.SleepActivity;
import com.androsz.electricsleepbeta.service.SleepAccelerometerService;

public class StartSleepReceiver extends BroadcastReceiver {

	public final static String START_SLEEP = "com.androsz.electricsleepbeta.START_SLEEP";

	public static void enforceCalibrationBeforeStartingSleep(
			final Context context, final Intent service, final Intent activity) {
		final SharedPreferences userPrefs = context
				.getSharedPreferences(
						context.getString(R.string.prefs_version),
						Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				context.getString(R.string.prefs_version), 0);
		String message = "";
		if (prefsVersion == 0) {
			message = context.getString(R.string.message_not_calibrated);
		} else if (prefsVersion != context.getResources().getInteger(
				R.integer.prefs_version)) {
			message = context.getString(R.string.message_prefs_not_compatible);
			PreferenceManager.getDefaultSharedPreferences(context).edit()
					.clear().commit();
			PreferenceManager.setDefaultValues(context, R.xml.settings, true);
		}

		if (message.length() > 0) {
			message += context
					.getString(R.string.message_recommend_calibration);
			final AlertDialog.Builder dialog = new AlertDialog.Builder(context)
					.setMessage(message)
					.setCancelable(false)
					.setPositiveButton(context.getString(R.string.calibrate),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									context.startActivity(new Intent(context,
											CalibrationWizardActivity.class));
								}
							})
					.setNeutralButton(context.getString(R.string.manual),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									context.startActivity(new Intent(context,
											SettingsActivity.class));
								}
							})
					.setNegativeButton(context.getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			try {
				dialog.show();
			} catch (final Exception e) {
			}
		} else if (service != null && activity != null) {
			context.startService(service);
			context.startActivity(activity);
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final SharedPreferences userPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final double alarmTriggerSensitivity = userPrefs.getFloat(
				context.getString(R.string.pref_alarm_trigger_sensitivity), -1);
		final int sensorDelay = Integer.parseInt(userPrefs.getString(
				context.getString(R.string.pref_sensor_delay), ""
						+ SensorManager.SENSOR_DELAY_NORMAL));
		final boolean useAlarm = userPrefs.getBoolean(
				context.getString(R.string.pref_use_alarm), false);
		final int alarmWindow = Integer.parseInt(userPrefs.getString(
				context.getString(R.string.pref_alarm_window), "-1"));
		final boolean airplaneMode = userPrefs.getBoolean(
				context.getString(R.string.pref_airplane_mode), false);
		final boolean forceScreenOn = userPrefs.getBoolean(
				context.getString(R.string.pref_force_screen), false);

		if (alarmTriggerSensitivity < 0 || useAlarm && alarmWindow < 0) {
			final AlertDialog.Builder dialog = new AlertDialog.Builder(context)
					.setMessage(context.getString(R.string.invalid_settings))
					.setCancelable(false)
					.setPositiveButton("Calibrate",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									context.startActivity(new Intent(context,
											CalibrationWizardActivity.class));
								}
							})
					.setNeutralButton("Manual",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									context.startActivity(new Intent(context,
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
			try {
				dialog.show();
			} catch (final Exception e) {
			}
			return;
		}

		final Intent serviceIntent = new Intent(context,
				SleepAccelerometerService.class);
		serviceIntent.putExtra("alarm", alarmTriggerSensitivity);
		serviceIntent.putExtra("sensorDelay", sensorDelay);
		serviceIntent.putExtra("useAlarm", useAlarm);
		serviceIntent.putExtra("alarmWindow", alarmWindow);
		serviceIntent.putExtra("airplaneMode", airplaneMode);
		serviceIntent.putExtra("forceScreenOn", forceScreenOn);
		enforceCalibrationBeforeStartingSleep(context, serviceIntent,
				new Intent(context, SleepActivity.class)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}
}
