package com.androsz.electricsleepbeta.app;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.widget.CalibrationSleepChart;
import com.androsz.electricsleepbeta.widget.DecimalSeekBar;

public class CalibrateAlarmActivity extends CalibrateForResultActivity {

	CalibrationSleepChart sleepChart;

	private final BroadcastReceiver updateChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			/*CalibrateAlarmActivity.this
					.setResult(
							CALIBRATION_SUCCEEDED,
							new Intent().putExtra("y",
									sleepChart.getCalibrationLevel()));*/
			if (sleepChart != null) {
				final DecimalSeekBar seekBar = (DecimalSeekBar) findViewById(R.id.calibration_level_seekbar);
				seekBar.setProgress((float) sleepChart.getCalibrationLevel());
				sleepChart.sync(intent.getDoubleExtra("x", 0), intent
						.getDoubleExtra("y", 0), intent.getDoubleExtra("min",
						SettingsActivity.DEFAULT_MIN_SENSITIVITY), sleepChart
						.getCalibrationLevel());
			}
		}
	};

	private final BroadcastReceiver syncChartReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(final Context context, final Intent intent) {

			sleepChart = (CalibrationSleepChart) findViewById(R.id.calibration_sleep_chart);

			// inlined for efficiency
			sleepChart.xySeriesMovement.mX = (List<Double>) intent
					.getSerializableExtra("currentSeriesX");
			sleepChart.xySeriesMovement.mY = (List<Double>) intent
					.getSerializableExtra("currentSeriesY");
			sleepChart.reconfigure(intent.getDoubleExtra("min",
					SettingsActivity.DEFAULT_MIN_SENSITIVITY), intent
					.getDoubleExtra("alarm",
							SettingsActivity.DEFAULT_ALARM_SENSITIVITY));
			sleepChart.repaint();
		}
	};

	@Override
	protected Intent getAssociatedServiceIntent() {
		return new Intent(this, SleepAccelerometerService.class);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_calibrate_alarm);

		sleepChart = (CalibrationSleepChart) findViewById(R.id.calibration_sleep_chart);

		final DecimalSeekBar seekBar = (DecimalSeekBar) findViewById(R.id.calibration_level_seekbar);
		seekBar.setMax((int) SettingsActivity.MAX_ALARM_SENSITIVITY);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(final SeekBar seekBar,
					final int progress, final boolean fromUser) {
				if (fromUser) {
					sleepChart.setCalibrationLevel(progress
							/ DecimalSeekBar.PRECISION);
				}
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
			}
		});

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void onDoneClick(final View v) {
		CalibrateAlarmActivity.this
		.setResult(
				CALIBRATION_SUCCEEDED,
				new Intent().putExtra("y",
						sleepChart.getCalibrationLevel()));
		finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(updateChartReceiver);
		unregisterReceiver(syncChartReceiver);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {

		try {
			super.onRestoreInstanceState(savedState);
		} catch (final java.lang.RuntimeException rte) {
			// sendBroadcast(new
			// Intent(SleepAccelerometerService.POKE_SYNC_CHART));
		}
		sleepChart = (CalibrationSleepChart) savedState
				.getSerializable("sleepChart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(updateChartReceiver, new IntentFilter(
				SleepActivity.UPDATE_CHART));
		registerReceiver(syncChartReceiver, new IntentFilter(
				SleepActivity.SYNC_CHART));
		sendBroadcast(new Intent(SleepAccelerometerService.POKE_SYNC_CHART));
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("sleepChart", sleepChart);
	}

}