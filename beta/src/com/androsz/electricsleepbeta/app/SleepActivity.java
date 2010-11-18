package com.androsz.electricsleepbeta.app;

import java.util.Calendar;
import java.util.List;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.Alarm;
import com.androsz.electricsleepbeta.alarmclock.AlarmClock;
import com.androsz.electricsleepbeta.alarmclock.Alarms;
import com.androsz.electricsleepbeta.service.SleepAccelerometerService;
import com.androsz.electricsleepbeta.widget.SleepChart;

public class SleepActivity extends CustomTitlebarActivity {

	private class WaitForSeriesDataProgressDialog extends ProgressDialog {
		public WaitForSeriesDataProgressDialog(final Context context) {
			super(context);
		}

		public WaitForSeriesDataProgressDialog(final Context context,
				final int theme) {
			// super(context);
			super(context, theme);
		}

		@Override
		public void onBackPressed() {
			SleepActivity.this.onBackPressed();
		}
	}

	public static final String UPDATE_CHART = "com.androsz.electricsleepbeta.UPDATE_CHART";

	public static final String SYNC_CHART = "com.androsz.electricsleepbeta.SYNC_CHART";

	private SleepChart sleepChart;

	private ProgressDialog waitForSeriesData;

	private final BroadcastReceiver updateChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {

			if (sleepChart != null) {
				sleepChart.syncByAdding(intent.getDoubleExtra("x", 0),
						intent.getDoubleExtra("y", 0),
						intent.getDoubleExtra("min",
								SettingsActivity.DEFAULT_MIN_SENSITIVITY),
						intent.getDoubleExtra("alarm",
								SettingsActivity.DEFAULT_ALARM_SENSITIVITY));

				if (sleepChart.makesSenseToDisplay()
						&& waitForSeriesData != null) {
					waitForSeriesData.dismiss();
				}
			}
		}
	};

	private final BroadcastReceiver syncChartReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(final Context context, final Intent intent) {

			sleepChart = (SleepChart) findViewById(R.id.sleep_movement_chart);
			// sleepChart.xySeriesMovement.mX = (List<Double>) intent
			// .getSerializableExtra("currentSeriesX");

			// sleepChart.xySeriesMovement.mY = (List<Double>) intent
			// .getSerializableExtra("currentSeriesY");

			sleepChart.xySeriesMovement.mX = (List<Double>) intent
					.getSerializableExtra("currentSeriesX");
			sleepChart.xySeriesMovement.mY = (List<Double>) intent
					.getSerializableExtra("currentSeriesY");
			sleepChart.redraw(intent.getDoubleExtra("min",
					SettingsActivity.DEFAULT_MIN_SENSITIVITY), intent
					.getDoubleExtra("alarm",
							SettingsActivity.DEFAULT_ALARM_SENSITIVITY));
			final boolean useAlarm = intent.getBooleanExtra("useAlarm", false);
			if (useAlarm) {
				try {
					final Alarm alarm = Alarms.calculateNextAlert(context);
					if (alarm != null) {
						final Calendar alarmTime = Calendar.getInstance();
						alarmTime.setTimeInMillis(alarm.time);

						java.text.DateFormat df = DateFormat
								.getDateFormat(context);
						String dateTime = df.format(alarmTime.getTime());
						df = DateFormat.getTimeFormat(context);
						dateTime = df.format(alarmTime.getTime()) + " on "
								+ dateTime;

						Toast.makeText(context, "Bound to alarm @ " + dateTime,
								Toast.LENGTH_LONG).show();
					}
				} catch (final Exception e) {

				}
			} else {
				Toast.makeText(context, "Not bound to any alarms.",
						Toast.LENGTH_LONG).show();
			}

			if (sleepChart.makesSenseToDisplay()
					&& waitForSeriesData != null) {
				waitForSeriesData.dismiss();
				waitForSeriesData = null;
			} else {
				showWaitForSeriesDataIfNeeded();
			}
		}
	};

	private final BroadcastReceiver sleepStoppedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			finish();
		}
	};

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_sleep;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {

		this.setTitle(R.string.monitoring_sleep);
		super.onCreate(savedInstanceState);

		showTitleButton1(R.drawable.ic_title_stop_default);
		showTitleButton2(R.drawable.ic_title_alarm);
		registerReceiver(sleepStoppedReceiver, new IntentFilter(
				SleepAccelerometerService.SLEEP_STOPPED));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(sleepStoppedReceiver);
	}

	@Override
	protected void onPause() {
		if (waitForSeriesData != null && waitForSeriesData.isShowing()) {
			waitForSeriesData.dismiss();
		}
		unregisterReceiver(updateChartReceiver);
		unregisterReceiver(syncChartReceiver);
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {

		try {
			super.onRestoreInstanceState(savedState);
		} catch (final java.lang.RuntimeException rte) {
			// sendBroadcast(new
			// Intent(SleepAccelerometerService.POKE_SYNC_CHART));
		}
		sleepChart = (SleepChart) savedState
				.getSerializable("sleepChart");

	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(updateChartReceiver, new IntentFilter(UPDATE_CHART));
		registerReceiver(syncChartReceiver, new IntentFilter(SYNC_CHART));
		sendBroadcast(new Intent(SleepAccelerometerService.POKE_SYNC_CHART));
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("sleepChart", sleepChart);
	}

	public void onTitleButton1Click(final View v) {

		sendBroadcast(new Intent(SleepAccelerometerService.STOP_AND_SAVE_SLEEP));
		finish();
	}

	public void onTitleButton2Click(final View v) {
		startActivity(new Intent(this, AlarmClock.class));
	}

	private void showWaitForSeriesDataIfNeeded() {
		if (sleepChart == null || !sleepChart.makesSenseToDisplay()) {
			if (waitForSeriesData == null || !waitForSeriesData.isShowing()) {
				waitForSeriesData = new WaitForSeriesDataProgressDialog(this);
				waitForSeriesData
						.setMessage(getText(R.string.dialog_wait_for_sleep_data_message));
				// waitForSeriesData.setContentView(R.layout.dialog_wait_for_data);
				waitForSeriesData.setButton(DialogInterface.BUTTON_NEGATIVE,
						getString(R.string.stop),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface arg0,
									final int arg1) {

								stopService(new Intent(SleepActivity.this,
										SleepAccelerometerService.class));
								SleepActivity.this.finish();
							}
						});
				waitForSeriesData.setButton(DialogInterface.BUTTON_NEUTRAL,
						getString(R.string.dismiss),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface arg0,
									final int arg1) {
								if (waitForSeriesData.isShowing()) {
									waitForSeriesData.dismiss();
								}
							}
						});
				waitForSeriesData.show();
			}
		}
	}
}
