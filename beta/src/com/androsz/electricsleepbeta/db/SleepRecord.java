package com.androsz.electricsleepbeta.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class SleepRecord {

	public static Object byteArrayToObject(final byte[] bytes)
			throws StreamCorruptedException, IOException,
			ClassNotFoundException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final ObjectInputStream ois = new ObjectInputStream(bais);

		return ois.readObject();
	}

	private static byte[] objectToByteArray(final Object obj)
			throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);

		return baos.toByteArray();
	}

	public static final HashMap<String, String> COLUMN_MAP = buildColumnMap();
	// The columns we'll include in the dictionary table
	public static final String KEY_SLEEP_DATE_TIME = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_SLEEP_DATA_X = "sleep_data_x";
	public static final String KEY_SLEEP_DATA_Y = "sleep_data_y";
	public static final String KEY_SLEEP_DATA_MIN = "sleep_data_min";
	public static final String KEY_SLEEP_DATA_ALARM = "sleep_data_alarm";
	public static final String KEY_SLEEP_DATA_RATING = "sleep_data_rating";


	/**
	 * Builds a map for all columns that may be requested, which will be given
	 * to the SQLiteQueryBuilder. This is a good way to define aliases for
	 * column names, but must include all columns, even if the value is the key.
	 * This allows the ContentProvider to request columns w/o the need to know
	 * real column names and create the alias itself.
	 */
	private static HashMap<String, String> buildColumnMap() {

		final HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_SLEEP_DATE_TIME, KEY_SLEEP_DATE_TIME);
		map.put(KEY_SLEEP_DATA_X, KEY_SLEEP_DATA_X);
		map.put(KEY_SLEEP_DATA_Y, KEY_SLEEP_DATA_Y);
		map.put(KEY_SLEEP_DATA_MIN, KEY_SLEEP_DATA_MIN);
		map.put(KEY_SLEEP_DATA_ALARM, KEY_SLEEP_DATA_ALARM);
		map.put(KEY_SLEEP_DATA_RATING, KEY_SLEEP_DATA_RATING);

		map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		return map;
	}

	public final String title;
	public final List<Double> chartDataX;
	public final List<Double> chartDataY;
	public final double min;
	public final double alarm;
	public final int rating;

	public SleepRecord(final String title, final List<Double> chartDataX,
			final List<Double> chartDataY, final double min,
			final double alarm, final int rating) {
		this.title = title;
		this.chartDataX = chartDataX;
		this.chartDataY = chartDataY;
		this.min = min;
		this.alarm = alarm;
		this.rating = rating;
	}

	@SuppressWarnings("unchecked")
	public SleepRecord(final Cursor cursor) throws StreamCorruptedException,
			IllegalArgumentException, IOException, ClassNotFoundException {

		title = cursor.getString(cursor
				.getColumnIndexOrThrow(KEY_SLEEP_DATE_TIME));

		chartDataX = (List<Double>) SleepRecord.byteArrayToObject(cursor
				.getBlob(cursor.getColumnIndexOrThrow(KEY_SLEEP_DATA_X)));

		chartDataY = (List<Double>) SleepRecord.byteArrayToObject(cursor
				.getBlob(cursor.getColumnIndexOrThrow(KEY_SLEEP_DATA_Y)));

		min = cursor.getDouble(cursor
				.getColumnIndexOrThrow(SleepRecord.KEY_SLEEP_DATA_MIN));
		alarm = cursor.getDouble(cursor
				.getColumnIndexOrThrow(SleepRecord.KEY_SLEEP_DATA_ALARM));
		rating = cursor.getInt(cursor
				.getColumnIndexOrThrow(SleepRecord.KEY_SLEEP_DATA_RATING));
	}

	public long insertIntoDb(SQLiteDatabase db) throws IOException {
		long insertResult = -1;
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SLEEP_DATE_TIME, title);

		initialValues.put(KEY_SLEEP_DATA_X, objectToByteArray(chartDataX));
		initialValues.put(KEY_SLEEP_DATA_Y, objectToByteArray(chartDataY));

		initialValues.put(KEY_SLEEP_DATA_MIN, min);
		initialValues.put(KEY_SLEEP_DATA_ALARM, alarm);
		initialValues.put(KEY_SLEEP_DATA_RATING, rating);

		insertResult = db.insert(SleepHistoryDatabase.FTS_VIRTUAL_TABLE, null,
				initialValues);
		db.close();
		return insertResult;
	}

	public long getStartTime() {
		return Math.round(chartDataX.get(0));
	}

	public long getEndTime() {
		return Math.round(chartDataX.get(chartDataX.size() - 1));
	}

	public long getDuration() {
		return getEndTime() - getStartTime();
	}

	public String getDurationString() {
		final Calendar duration = Calendar.getInstance();
		duration.setTimeInMillis(getDuration());
		int hours = Math.max(24, duration.get(Calendar.HOUR_OF_DAY));
		int minutes = duration.get(Calendar.MINUTE);
		if (minutes >= 30) {
			hours++;
		}
		return hours + " hr.";
	}
}
