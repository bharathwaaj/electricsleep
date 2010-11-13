package com.androsz.electricsleepbeta.receiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.widget.Toast;

import com.androsz.electricsleepbeta.app.ReviewSleepActivity;
import com.androsz.electricsleepbeta.app.SettingsActivity;
import com.androsz.electricsleepbeta.db.SleepContentProvider;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;

public class SaveSleepReceiver extends BroadcastReceiver {

	public static String SAVE_SLEEP_COMPLETED = "com.androsz.electricsleep.SAVE_SLEEP_COMPLETED";

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(final Context context, final Intent intent) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				final SleepHistoryDatabase shdb = new SleepHistoryDatabase(
						context);

				double min = Double.MAX_VALUE;

				final double alarm = intent.getDoubleExtra("alarm",
						SettingsActivity.DEFAULT_ALARM_SENSITIVITY);

				final String name = intent.getStringExtra("name");
				final int rating = intent.getIntExtra("rating", 5);

				List<Double> mX = (List<Double>) intent
						.getSerializableExtra("currentSeriesX");
				List<Double> mY = (List<Double>) intent
						.getSerializableExtra("currentSeriesY");
				final int count = mY.size();

				int numberOfDesiredGroupedPoints = 200;
				numberOfDesiredGroupedPoints = count > numberOfDesiredGroupedPoints ? numberOfDesiredGroupedPoints
						: count;

				try {
					if (numberOfDesiredGroupedPoints < count) {
						final int pointsPerGroup = count
								/ numberOfDesiredGroupedPoints + 1;
						final List<Double> lessDetailedX = new ArrayList<Double>(
								numberOfDesiredGroupedPoints);
						final List<Double> lessDetailedY = new ArrayList<Double>(
								numberOfDesiredGroupedPoints);
						int numberOfPointsInThisGroup = pointsPerGroup;
						double maxYForThisGroup;
						double totalForThisGroup;
						for (int i = 0; i < numberOfDesiredGroupedPoints; i++) {
							maxYForThisGroup = 0;
							totalForThisGroup = 0;
							final int startIndexForThisGroup = i
									* pointsPerGroup;
							for (int j = 0; j < pointsPerGroup; j++) {
								try {
									final double currentY = mY
											.get(startIndexForThisGroup + j);
									if (currentY > maxYForThisGroup) {
										maxYForThisGroup = currentY;
									}
									totalForThisGroup += currentY;
								} catch (final IndexOutOfBoundsException ioobe) {
									// lower the number of points
									// (and thereby signify that we are done)
									numberOfPointsInThisGroup = j - 1;
									break;
								}
							}
							final double averageForThisGroup = totalForThisGroup
									/ numberOfPointsInThisGroup;
							if (numberOfPointsInThisGroup < pointsPerGroup) {
								// we are done
								final int lastIndex = mX.size() - 1;
								lessDetailedX.add(mX.get(lastIndex));
								lessDetailedY.add(mY.get(lastIndex));
								mX = lessDetailedX;
								mY = lessDetailedY;
								break;
							} else {
								if (maxYForThisGroup < alarm) {
									maxYForThisGroup = averageForThisGroup;
								}
								if (maxYForThisGroup < min) {
									min = maxYForThisGroup;
								}
								lessDetailedX.add(mX
										.get(startIndexForThisGroup));
								lessDetailedY.add(maxYForThisGroup);
							}
						}
						shdb.addSleep(context, name, lessDetailedX,
								lessDetailedY, min, alarm, rating);
					} else {
						min = intent.getDoubleExtra("min", Double.MAX_VALUE);
						shdb.addSleep(context, name, mX, mY, min, alarm, rating);
					}
				} catch (final IOException e) {
					context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED));
					return;
				}

				final Cursor c = shdb.getSleepMatches(name, new String[] {
						BaseColumns._ID,
						SleepHistoryDatabase.KEY_SLEEP_DATE_TIME });

				if (c == null) {
					/*
					 * Toast.makeText( context,
					 * "Could not find the recently saved sleep in the sleep database- report this!"
					 * , Toast.LENGTH_LONG).show();
					 */
					shdb.close();
					context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED));
					return;
				} else if (!c.moveToFirst()) {
					/*
					 * Toast.makeText( context,
					 * "Could not move to the recently saved sleep in the sleep database- report this!"
					 * , Toast.LENGTH_LONG).show();
					 */
					shdb.close();
					c.close();
					context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED));
					return;
				}
				final long rowId = c.getLong(0);
				c.close();

				final Intent reviewSleepIntent = new Intent(context,
						ReviewSleepActivity.class);
				final Uri uri = Uri.withAppendedPath(
						SleepContentProvider.CONTENT_URI, String.valueOf(rowId));
				reviewSleepIntent.setData(uri);

				reviewSleepIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK);

				context.startActivity(reviewSleepIntent);
				shdb.close();
				context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED));
			}
		}).start();
	}

}
