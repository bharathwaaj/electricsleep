package com.androsz.electricsleepbeta.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.androsz.electricsleepbeta.app.SettingsActivity;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;

public class SaveSleepReceiver extends BroadcastReceiver {

	public static String SAVE_SLEEP_COMPLETED = "com.androsz.electricsleep.SAVE_SLEEP_COMPLETED";

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(final Context context, final Intent intent) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				final SleepHistoryDatabase shdb = new SleepHistoryDatabase(
						context);

				double min = Double.MAX_VALUE;

				final double alarm = intent.getDoubleExtra("alarm",
						SettingsActivity.DEFAULT_ALARM_SENSITIVITY);

				final String name = intent.getStringExtra("name");
				final int rating = intent.getIntExtra("rating", 5);
				final String note = intent.getStringExtra("note");

				List<Double> mX = (List<Double>) intent
						.getSerializableExtra("currentSeriesX");
				List<Double> mY = (List<Double>) intent
						.getSerializableExtra("currentSeriesY");

				final int numberOfPointsOriginal = mY.size();
				
				if(numberOfPointsOriginal == 0)
				{
					context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED));
					return;
				}

				final int numberOfDesiredGroupedPoints = 200;
				// numberOfDesiredGroupedPoints = numberOfPointsOriginal >
				// numberOfDesiredGroupedPoints ? numberOfDesiredGroupedPoints
				// : numberOfPointsOriginal;

				if (numberOfDesiredGroupedPoints <= numberOfPointsOriginal) {
					final int pointsPerGroup = numberOfPointsOriginal
							/ numberOfDesiredGroupedPoints + 1;
					final List<Double> lessDetailedX = new ArrayList<Double>(
							numberOfDesiredGroupedPoints);
					final List<Double> lessDetailedY = new ArrayList<Double>(
							numberOfDesiredGroupedPoints);
					int numberOfPointsInThisGroup = pointsPerGroup;
					double maxYForThisGroup;
					double totalForThisGroup;
					int numberOfSpikes = 0;
					int numberOfConsecutiveNonSpikes = 0;
					long timeOfFirstSleep = 0;
					for (int i = 0; i < numberOfDesiredGroupedPoints; i++) {
						maxYForThisGroup = 0;
						totalForThisGroup = 0;
						final int startIndexForThisGroup = i * pointsPerGroup;
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
							final int lastIndex = numberOfPointsOriginal - 1;
							lessDetailedX.add(mX.get(lastIndex));
							lessDetailedY.add(mY.get(lastIndex));
							mX = lessDetailedX;
							mY = lessDetailedY;
							break;
						} else {
							if (maxYForThisGroup < alarm) {
								maxYForThisGroup = averageForThisGroup;
								if (timeOfFirstSleep == 0
										&& ++numberOfConsecutiveNonSpikes > 4) {
									final int lastIndex = lessDetailedX.size() - 1;

									timeOfFirstSleep = Math.round(lessDetailedX
											.get(lastIndex));
								}
							} else {
								numberOfConsecutiveNonSpikes = 0;
								numberOfSpikes++;
							}
							if (maxYForThisGroup < min) {
								min = maxYForThisGroup;
							}
							lessDetailedX.add(mX.get(startIndexForThisGroup));
							lessDetailedY.add(maxYForThisGroup);
						}
					}

					final long endTime = Math.round(lessDetailedX
							.get(lessDetailedX.size() - 1));
					final long startTime = Math.round(lessDetailedX.get(0));

					try {
						shdb.addSleep(context, new SleepRecord(name,
								lessDetailedX, lessDetailedY, min, alarm,
								rating, endTime - startTime, numberOfSpikes,
								timeOfFirstSleep, note));
					} catch (final IOException e) {
						shdb.close();
						context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED)
								.putExtra("IOException", e.getMessage()));
						return;
					}
				} else {

					final long endTime = Math.round(mX.get(mX.size() - 1));
					final long startTime = Math.round(mX.get(0));

					int numberOfSpikes = 0;
					int numberOfConsecutiveNonSpikes = 0;
					long timeOfFirstSleep = endTime;
					final int size = mX.size();
					for (int i = 0; i < size; i++) {
						final double currentY = mY.get(i);
						if (currentY < alarm) {
							if (timeOfFirstSleep == endTime
									&& ++numberOfConsecutiveNonSpikes > 4) {
								final int lastIndex = mX.size() - 1;

								timeOfFirstSleep = Math.round(mX.get(lastIndex));
							}
						} else {
							numberOfConsecutiveNonSpikes = 0;
							numberOfSpikes++;
						}
						if (currentY < min) {
							min = currentY;
						}
					}
					try {
						shdb.addSleep(context, new SleepRecord(name, mX, mY,
								min, alarm, rating, endTime - startTime,
								numberOfSpikes, timeOfFirstSleep, note));
					} catch (final IOException e) {
						shdb.close();
						context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED)
								.putExtra("IOException", e.getMessage()));
						return;
					}
				}

				final Cursor c = shdb.getSleepMatches(name, new String[] {
						BaseColumns._ID, SleepRecord.KEY_TITLE });

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

				// context.startActivity(reviewSleepIntent);
				shdb.close();

				final Intent saveSleepCompletedIntent = new Intent(
						SAVE_SLEEP_COMPLETED);
				saveSleepCompletedIntent.putExtra("success", true);
				saveSleepCompletedIntent.putExtra("rowId",
						String.valueOf(rowId));
				context.sendBroadcast(saveSleepCompletedIntent);
			}
		}).start();
	}
}
