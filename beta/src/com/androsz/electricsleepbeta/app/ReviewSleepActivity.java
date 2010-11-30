package com.androsz.electricsleepbeta.app;

import java.io.IOException;
import java.io.StreamCorruptedException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;
import com.androsz.electricsleepbeta.widget.SleepChart;

public class ReviewSleepActivity extends CustomTitlebarActivity {

	private class DeleteSleepTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog progress;

		@Override
		protected Void doInBackground(final Void... params) {
			final SleepHistoryDatabase shdb = new SleepHistoryDatabase(
					ReviewSleepActivity.this);
			shdb.deleteRow(rowId);
			shdb.close();
			return null;
		}

		@Override
		protected void onPostExecute(final Void results) {
			Toast.makeText(ReviewSleepActivity.this,
					getString(R.string.deleted_sleep_record),
					Toast.LENGTH_SHORT).show();
			progress.dismiss();
			finish();
		}

		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(ReviewSleepActivity.this);
			progress.setMessage(getString(R.string.deleting_sleep));
			progress.show();
		}
	}

	private SleepChart sleepChart;

	private long rowId;

	private void addChartView() throws StreamCorruptedException, IllegalArgumentException, IOException, ClassNotFoundException {
		sleepChart = (SleepChart) findViewById(R.id.sleep_movement_chart);

		final Uri uri = getIntent().getData();
		Cursor cursor;
		if (uri == null) {
			final long uriEnding = getIntent().getLongExtra("position", -1);
			cursor = managedQuery(SleepContentProvider.CONTENT_URI, null, null,
					new String[] { getString(R.string.to) },
					SleepRecord.KEY_SLEEP_DATE_TIME + " DESC");
			if (cursor == null) {
				finish();
			} else {
				cursor.moveToPosition((int) uriEnding);
				rowId = cursor.getPosition();
				sleepChart.syncWithCursor(cursor);
			}
		} else {
			cursor = managedQuery(uri, null, null, null, null);

			if (cursor == null) {
				finish();
			} else {
				rowId = Long.parseLong(uri.getLastPathSegment());
				showTitleButton1(android.R.drawable.ic_menu_delete);
				cursor.moveToFirst();
				sleepChart.syncWithCursor(cursor);
			}
		}
	}

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_sleep;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {
		try {
			super.onRestoreInstanceState(savedState);
			sleepChart = (SleepChart) savedState
					.getSerializable("sleepChart");
		} catch (final RuntimeException re) {

		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			addChartView();
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("sleepChart", sleepChart);
	}

	public void onTitleButton1Click(final View v) {
		final AlertDialog.Builder dialog = new AlertDialog.Builder(
				ReviewSleepActivity.this)
				.setMessage(getString(R.string.delete_sleep_record))
				.setPositiveButton(getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int id) {
								new DeleteSleepTask().execute(null, null, null);
							}
						})
				.setNegativeButton(getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();
							}
						});
		dialog.show();
	}
}
