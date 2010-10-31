package com.androsz.electricsleepbeta.app;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.receiver.SaveSleepReceiver;

public class SaveSleepActivity extends CustomTitlebarActivity implements
		OnRatingBarChangeListener {

	public static final String SAVE_SLEEP = "com.androsz.electricsleepbeta.SAVE_SLEEP";

	private float rating = Float.NaN;

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_save_sleep;
	}

	@Override
	// @SuppressWarnings("unchecked")
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((RatingBar) findViewById(R.id.save_sleep_rating_bar))
				.setOnRatingBarChangeListener(this);
	}

	public void onDiscardClick(final View v) {
		finish();
	}

	@Override
	public void onRatingChanged(final RatingBar ratingBar, final float rating,
			final boolean fromUser) {
		if (fromUser) {
			this.rating = rating;
		}
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		rating = savedState.getFloat("rating");
		
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat("rating", rating);		
	}

	public void onResume() {
		super.onResume();
		registerReceiver(saveCompletedReceiver, new IntentFilter(
				SaveSleepReceiver.SAVE_SLEEP_COMPLETED));
	}

	public void onPause() {
		super.onPause();
		unregisterReceiver(saveCompletedReceiver);
	}

	ProgressDialog progress;

	public void onSaveClick(final View v) {

		if (Float.isNaN(rating)) {
			Toast.makeText(this, R.string.error_not_rated, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		getIntent().putExtra("rating", (int) rating);

		final Intent saveIntent = new Intent(SaveSleepActivity.SAVE_SLEEP);
		saveIntent.putExtras(getIntent().getExtras());

		v.setEnabled(false);
		progress  = new ProgressDialog(this);
		progress.setMessage(getString(R.string.saving_sleep));
		progress.show();
		sendBroadcast(saveIntent);
		finish();
	}

	private BroadcastReceiver saveCompletedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			progress.dismiss();
		}
	};
}
