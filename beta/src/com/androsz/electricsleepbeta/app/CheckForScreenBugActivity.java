package com.androsz.electricsleepbeta.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.androsz.electricsleepbeta.R;

public class CheckForScreenBugActivity extends CalibrateForResultActivity {

	private final BroadcastReceiver bugNotPresentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			CheckForScreenBugActivity.this
					.setResult(
							CALIBRATION_SUCCEEDED,
							new Intent(
									CheckForScreenBugAccelerometerService.BUG_NOT_PRESENT));
			finish();
		}
	};

	private final BroadcastReceiver bugPresentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			CheckForScreenBugActivity.this.setResult(CALIBRATION_SUCCEEDED,
					new Intent(
							CheckForScreenBugAccelerometerService.BUG_PRESENT));
			finish();
		}
	};

	@Override
	protected Intent getAssociatedServiceIntent() {
		return new Intent(this, CheckForScreenBugAccelerometerService.class);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_check_for_screen_bug);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(bugPresentReceiver);
		unregisterReceiver(bugNotPresentReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(bugPresentReceiver, new IntentFilter(
				CheckForScreenBugAccelerometerService.BUG_PRESENT));
		registerReceiver(bugNotPresentReceiver, new IntentFilter(
				CheckForScreenBugAccelerometerService.BUG_NOT_PRESENT));
	}

}
