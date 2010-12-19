package com.androsz.electricsleepbeta.content;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.androsz.electricsleepbeta.app.SettingsActivity;
import com.androsz.electricsleepbeta.app.SleepAccelerometerService;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;
import com.androsz.electricsleepbeta.util.PointD;

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

				FileInputStream fis;
				byte[] buffer = new byte[10000000];
				try {
					fis = context
							.openFileInput(SleepAccelerometerService.SLEEP_DATA_FILE);
					int readNum = fis.read(buffer);
					readNum++;
					fis.close();
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}

				List<PointD> originalData = null;
				try {
					originalData = (List<PointD>) (SleepRecord
							.byteArrayToObject(buffer));
				} catch (StreamCorruptedException e1) {
				} catch (IOException e1) {
				} catch (ClassNotFoundException e1) {
				}
				
				if(originalData == null) //if something went wrong with reading the file, load from the intent.
				{
					originalData = (List<PointD>)(intent.getSerializableExtra("sleepData"));
				}

				final int numberOfPointsOriginal = originalData.size();

				// List<Double> mX = (List<Double>) intent
				// .getSerializableExtra("currentSeriesX");
				// List<Double> mY = (List<Double>) intent
				// .getSerializableExtra("currentSeriesY");

				if (numberOfPointsOriginal == 0) {
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
					final List<PointD> lessDetailedData = new ArrayList<PointD>(
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
								final double currentY = originalData
										.get(startIndexForThisGroup + j).y;
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
							lessDetailedData.add(originalData.get(lastIndex));
							break;
						} else {
							if (maxYForThisGroup < alarm) {
								maxYForThisGroup = averageForThisGroup;
								if (timeOfFirstSleep == 0
										&& ++numberOfConsecutiveNonSpikes > 4) {
									final int lastIndex = lessDetailedData
											.size() - 1;

									timeOfFirstSleep = Math
											.round(lessDetailedData
													.get(lastIndex).x);
								}
							} else {
								numberOfConsecutiveNonSpikes = 0;
								numberOfSpikes++;
							}
							if (maxYForThisGroup < min) {
								min = maxYForThisGroup;
							}
							lessDetailedData.add(new PointD(originalData
									.get(startIndexForThisGroup).x,
									maxYForThisGroup));
						}
					}

					final long endTime = Math.round(lessDetailedData
							.get(lessDetailedData.size() - 1).x);
					final long startTime = Math.round(lessDetailedData.get(0).x);

					try {
						shdb.addSleep(context, new SleepRecord(name,
								lessDetailedData, min, alarm, rating, endTime
										- startTime, numberOfSpikes,
								timeOfFirstSleep, note));
					} catch (final IOException e) {
						shdb.close();
						context.sendBroadcast(new Intent(SAVE_SLEEP_COMPLETED)
								.putExtra("IOException", e.getMessage()));
						return;
					}
				} else {

					final long endTime = Math.round(originalData
							.get(numberOfPointsOriginal - 1).x);
					final long startTime = Math.round(originalData.get(0).x);

					int numberOfSpikes = 0;
					int numberOfConsecutiveNonSpikes = 0;
					long timeOfFirstSleep = endTime;
					for (int i = 0; i < numberOfPointsOriginal; i++) {
						final double currentY = originalData.get(i).y;
						if (currentY < alarm) {
							if (timeOfFirstSleep == endTime
									&& ++numberOfConsecutiveNonSpikes > 4) {
								final int lastIndex = originalData.size() - 1;

								timeOfFirstSleep = Math.round(originalData
										.get(lastIndex).x);
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
						shdb.addSleep(context, new SleepRecord(name,
								originalData, min, alarm, rating, endTime
										- startTime, numberOfSpikes,
								timeOfFirstSleep, note));
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
