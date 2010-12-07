package com.androsz.electricsleepbeta.app;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.content.SaveSleepReceiver;
import com.androsz.electricsleepbeta.db.SleepContentProvider;

public class SaveSleepActivity extends CustomTitlebarActivity implements
		OnRatingBarChangeListener {

	public static final String SAVE_SLEEP = "com.androsz.electricsleepbeta.SAVE_SLEEP";

	private float rating = Float.NaN;

	ProgressDialog progress;

	EditText noteEdit;

	private final BroadcastReceiver saveCompletedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {

			final Intent reviewSleepIntent = new Intent(context,
					ReviewSleepActivity.class);
			if (!intent.getBooleanExtra("success", false)) {
				Toast.makeText(
						context,
						R.string.could_not_save_this_sleep_properly_it_was_likely_too_brief_to_analyze_,
						Toast.LENGTH_LONG).show();
				progress.dismiss();
				finish();
				return;
			}
			final String rowId = intent.getStringExtra("rowId");
			if (rowId != null) {
				final Uri uri = Uri.withAppendedPath(
						SleepContentProvider.CONTENT_URI, rowId);
				reviewSleepIntent.setData(uri);
			}

			reviewSleepIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_NEW_TASK);

			startActivity(reviewSleepIntent);
			progress.dismiss();
			finish();
		}
	};

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
		noteEdit = (EditText) findViewById(R.id.save_sleep_note_edit);
	}

	public void onDiscardClick(final View v) {
		finish();
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(getIntent().getExtras().getInt("id"));
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(saveCompletedReceiver);
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
	public void onResume() {
		super.onResume();
		registerReceiver(saveCompletedReceiver, new IntentFilter(
				SaveSleepReceiver.SAVE_SLEEP_COMPLETED));
	}

	public void onSaveClick(final View v) {

		if (Float.isNaN(rating)) {
			Toast.makeText(this, R.string.error_not_rated, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		final Intent saveIntent = new Intent(SaveSleepActivity.SAVE_SLEEP);
		saveIntent.putExtra("note", noteEdit.getText().toString());
		saveIntent.putExtra("rating", (int) rating);
		saveIntent.putExtras(getIntent().getExtras()); // add the sleep history
														// data

		v.setEnabled(false);
		progress = new ProgressDialog(this);
		progress.setMessage(getString(R.string.saving_sleep));
		progress.show();
		sendBroadcast(saveIntent);
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(getIntent().getExtras().getInt("id"));
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat("rating", rating);
	}
}
