package com.androsz.electricsleepbeta.app;

import java.io.IOException;
import java.io.StreamCorruptedException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;
import com.androsz.electricsleepbeta.util.DeviceUtil;
import com.androsz.electricsleepbeta.widget.DailySleepComparisonChart;
import com.androsz.electricsleepbeta.widget.SleepHistoryCursorAdapter;

public class HistoryActivity extends CustomTitlebarTabActivity {

	public static final String SEARCH_FOR = "searchFor";

	private class DeleteSleepTask extends AsyncTask<Long, Void, Void> {

		@Override
		protected Void doInBackground(final Long... params) {
			final SleepHistoryDatabase shdb = new SleepHistoryDatabase(
					HistoryActivity.this);
			shdb.deleteRow(params[0]);
			shdb.close();
			return null;
		}

		@Override
		protected void onPostExecute(final Void results) {
			mListView.removeAllViewsInLayout();
			new QuerySleepTask().execute(null);
			Toast.makeText(HistoryActivity.this,
					getString(R.string.deleted_sleep_record),
					Toast.LENGTH_SHORT).show();

			if (progress != null && progress.isShowing()) {
				progress.dismiss();
			}
		}

		@Override
		protected void onPreExecute() {
			progress.setMessage(getString(R.string.deleting_sleep));
			progress.show();
		}
	}

	private class QuerySleepTask extends AsyncTask<Void, Void, Void> {
		Cursor cursor;

		@Override
		protected Void doInBackground(final Void... params) {
			if (searchFor == null) {
				searchFor = getString(R.string.to);
			}
			cursor = managedQuery(SleepContentProvider.CONTENT_URI, null, null,
					new String[] { searchFor }, SleepRecord.KEY_TITLE);
			return null;
		}

		@Override
		protected void onPostExecute(final Void results) {
			if (cursor == null) {
				// There are no results
				mTextView.setVisibility(View.VISIBLE);
				mTextView.setText(getString(R.string.no_results));
				mListView.setVisibility(View.GONE);
			} else {
				try {
					addChartView(cursor);
				} catch (final StreamCorruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mTextView.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);

				final SleepHistoryCursorAdapter sleepHistory = new SleepHistoryCursorAdapter(
						HistoryActivity.this, cursor);
				mListView.setAdapter(sleepHistory);
				// mListView.setWillNotCacheDrawing(true);
				if (DeviceUtil.getCpuClockSpeed() >= 600) {
					// anything faster than a
					// droid *should* be
					// able to handle smooth
					// scrolling
					mListView.setScrollingCacheEnabled(false);
				}

				mListView
						.setOnItemLongClickListener(new OnItemLongClickListener() {

							@Override
							public boolean onItemLongClick(
									final AdapterView<?> parent,
									final View view, final int position,
									final long rowId) {
								final AlertDialog.Builder dialog = new AlertDialog.Builder(
										HistoryActivity.this)
										.setMessage(
												getString(R.string.delete_sleep_record))
										.setPositiveButton(
												getString(R.string.ok),
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															final DialogInterface dialog,
															final int id) {

														new DeleteSleepTask()
																.execute(rowId,
																		null,
																		null);
													}
												})
										.setNegativeButton(
												getString(R.string.cancel),
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															final DialogInterface dialog,
															final int id) {
														dialog.cancel();
													}
												});
								dialog.show();
								return true;
							}

						});

				// Define the on-click listener for the list items
				mListView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(final AdapterView<?> parent,
							final View view, final int position, final long id) {
						// Build the Intent used to open WordActivity with a
						// specific word Uri
						final Intent reviewSleepIntent = new Intent(
								getApplicationContext(),
								ReviewSleepActivity.class);
						final Uri data = Uri.withAppendedPath(
								SleepContentProvider.CONTENT_URI,
								String.valueOf(id));
						reviewSleepIntent.setData(data);
						startActivity(reviewSleepIntent);
					}
				});
			}
			if (progress != null && progress.isShowing()) {
				progress.dismiss();
			}
		}

		@Override
		protected void onPreExecute() {
			progress.setMessage(getString(R.string.querying_sleep_database));
			progress.show();
		}
	}

	ProgressDialog progress;

	private TextView mTextView;

	private ListView mListView;

	private DailySleepComparisonChart sleepChart;

	private void addChartView(Cursor cursor) throws StreamCorruptedException,
			IllegalArgumentException, IOException, ClassNotFoundException {
		if (sleepChart != null) {
			sleepChart.xySeriesMovement.clear();
		}
		sleepChart = (DailySleepComparisonChart) findViewById(R.id.chart);

		if (cursor == null) {
			sleepChart.setVisibility(View.GONE);
		} else {
			sleepChart.setVisibility(View.VISIBLE);
			cursor.moveToFirst();
			do {
				final SleepRecord sleepRecord = new SleepRecord(cursor);
				sleepChart.sync((double) sleepRecord.getStartTime(),
						(double) sleepRecord.getSleepScore(), 0, 100);
			} while (cursor.moveToNext());
		}
	}

	@Override
	protected int getContentAreaLayoutId() {
		return R.layout.activity_history;
	}

	String searchFor = null;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		progress = new ProgressDialog(HistoryActivity.this);

		mTextView = (TextView) findViewById(R.id.text);
		mListView = (ListView) findViewById(R.id.list);
		((Spinner) findViewById(R.id.sleep_history_analysis_x_axis_spinner))
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(final AdapterView<?> parentView,
							final View selectedItemView, final int position,
							final long id) {
						Toast.makeText(
								HistoryActivity.this,
								"Value changing is not implemented yet; this currently only displays sleep score over time.",
								Toast.LENGTH_LONG).show();
					}

					@Override
					public void onNothingSelected(final AdapterView<?> arg0) {
					}
				});
		((Spinner) findViewById(R.id.sleep_history_analysis_y_axis_spinner))
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(final AdapterView<?> parentView,
							final View selectedItemView, final int position,
							final long id) {
						Toast.makeText(
								HistoryActivity.this,
								"Value changing is not implemented yet; this currently only displays sleep score over time.",
								Toast.LENGTH_LONG).show();
					}

					@Override
					public void onNothingSelected(final AdapterView<?> arg0) {
					}
				});
		mListView.setVerticalFadingEdgeEnabled(false);
		mListView.setScrollbarFadingEnabled(false);

		final Intent intent = getIntent();

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// handles a click on a search suggestion; launches activity to show
			// word
			final Intent reviewIntent = new Intent(this,
					ReviewSleepActivity.class);
			reviewIntent.setData(intent.getData());
			startActivity(reviewIntent);
			finish();
		} else {
			// set searchFor parameter if it exists
			searchFor = intent.getStringExtra(SEARCH_FOR);
			if (searchFor != null) {
				HistoryActivity.this.setTitle(HistoryActivity.this.getTitle()
						+ " " + searchFor);
			}
			new QuerySleepTask().execute(null);
		}

		addTab(R.id.sleep_history_list, R.string.list);
		addTab(R.id.sleep_history_analysis, R.string.analysis);
		// tabHost.setCurrentTab(1);
		// tabHost.setCurrentTab(0);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (progress != null && progress.isShowing()) {
			progress.dismiss();
		}
	}
}
