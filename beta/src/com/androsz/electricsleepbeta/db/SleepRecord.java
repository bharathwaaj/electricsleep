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
import java.util.TimeZone;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.androsz.electricsleepbeta.R;

public class SleepRecord {

	public static final HashMap<String, String> COLUMN_MAP = buildColumnMap();

	// The columns we'll include in the dictionary table
	// DATABASE_VERSION = 3
	public static final String KEY_TITLE = SearchManager.SUGGEST_COLUMN_TEXT_1;

	public static final String KEY_X = "sleep_data_x";
	public static final String KEY_Y = "sleep_data_y";
	public static final String KEY_MIN = "sleep_data_min";
	public static final String KEY_ALARM = "sleep_data_alarm";
	public static final String KEY_RATING = "sleep_data_rating";
	// DATABASE_VERSION = 4
	public static final String KEY_DURATION = "KEY_SLEEP_DATA_DURATION";
	public static final String KEY_SPIKES = "KEY_SLEEP_DATA_SPIKES";
	public static final String KEY_TIME_FELL_ASLEEP = "KEY_SLEEP_DATA_TIME_FELL_ASLEEP";
	public static final String KEY_NOTE = "KEY_SLEEP_DATA_NOTE";

	/**
	 * Builds a map for all columns that may be requested, which will be given
	 * to the SQLiteQueryBuilder. This is a good way to define aliases for
	 * column names, but must include all columns, even if the value is the key.
	 * This allows the ContentProvider to request columns w/o the need to know
	 * real column names and create the alias itself.
	 */
	private static HashMap<String, String> buildColumnMap() {

		final HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_TITLE, KEY_TITLE);
		map.put(KEY_X, KEY_X);
		map.put(KEY_Y, KEY_Y);
		map.put(KEY_MIN, KEY_MIN);
		map.put(KEY_ALARM, KEY_ALARM);
		map.put(KEY_RATING, KEY_RATING);
		map.put(KEY_DURATION, KEY_DURATION);
		map.put(KEY_SPIKES, KEY_SPIKES);
		map.put(KEY_TIME_FELL_ASLEEP, KEY_TIME_FELL_ASLEEP);
		map.put(KEY_NOTE, KEY_NOTE);

		map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		return map;
	}

	public static Object byteArrayToObject(final byte[] bytes)
			throws StreamCorruptedException, IOException,
			ClassNotFoundException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final ObjectInputStream ois = new ObjectInputStream(bais);

		return ois.readObject();
	}

	//

	private static byte[] objectToByteArray(final Object obj)
			throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);

		return baos.toByteArray();
	}

	public final String title;
	public final List<Double> chartDataX;
	public final List<Double> chartDataY;
	public final double min;
	public final double alarm;
	public final int rating;

	public final long duration;
	public final int spikes;
	public final long fellAsleep;
	public final String note;

	@SuppressWarnings("unchecked")
	public SleepRecord(final Cursor cursor) throws StreamCorruptedException,
			IllegalArgumentException, IOException, ClassNotFoundException {

		title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE));

		chartDataX = (List<Double>) byteArrayToObject(cursor.getBlob(cursor
				.getColumnIndexOrThrow(KEY_X)));

		chartDataY = (List<Double>) byteArrayToObject(cursor.getBlob(cursor
				.getColumnIndexOrThrow(KEY_Y)));

		min = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_MIN));
		alarm = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_ALARM));
		rating = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_RATING));

		duration = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION));
		spikes = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SPIKES));
		fellAsleep = cursor.getLong(cursor
				.getColumnIndexOrThrow(KEY_TIME_FELL_ASLEEP));
		note = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE));
	}

	public SleepRecord(final String title, final List<Double> chartDataX,
			final List<Double> chartDataY, final double min,
			final double alarm, final int rating, final long duration,
			final int spikes, final long fellAsleep, final String note) {
		this.title = title;
		this.chartDataX = chartDataX;
		this.chartDataY = chartDataY;
		this.min = min;
		this.alarm = alarm;
		this.rating = rating;
		this.duration = duration;
		this.spikes = spikes;
		this.fellAsleep = fellAsleep;
		this.note = note;
	}

	public CharSequence getDurationText(final Resources res) {
		final Calendar duration = getTimeDiffCalendar(this.duration);
		final int hours = Math.min(24, duration.get(Calendar.HOUR_OF_DAY));
		final int minutes = duration.get(Calendar.MINUTE);
		return res.getQuantityString(R.plurals.hour, hours, hours) + " "
				+ res.getQuantityString(R.plurals.minute, minutes, minutes);
	}

	public CharSequence getFellAsleepText(final Resources res) {
		final Calendar fellAsleep = getTimeDiffCalendar(this.fellAsleep
				- getStartTime());
		final int hours = Math.min(24, fellAsleep.get(Calendar.HOUR_OF_DAY));
		final int minutes = fellAsleep.get(Calendar.MINUTE);
		return res.getQuantityString(R.plurals.hour, hours, hours) + " "
				+ res.getQuantityString(R.plurals.minute, minutes, minutes);
	}

	public int getSleepScore() {
		int score = 0;
		final float ratingPct = rating / 5f;
		final float deepPct = Math.min(1, 20f / spikes);
		final float diffFrom8HoursPct = 1 - Math
				.abs((duration - 28800000) / 28800000f);
		final float timeToFallAsleepPct = 1000 * 60 * 15f / Math.max(fellAsleep
				- getStartTime(), 1000 * 60 * 15);
		// ratingPct *= 1;
		// deepPct *= 1;
		// diffFrom8HoursPct *= 1.4;
		// timeToFallAsleepPct *= 0.6;

		score = Math
				.round((ratingPct + deepPct + diffFrom8HoursPct + timeToFallAsleepPct) / 4 * 100);

		return score;
	}

	public long getStartTime() {
		return Math.round(chartDataX.get(0));
	}

	private Calendar getTimeDiffCalendar(final long time) {
		// set calendar to GMT +0
		final Calendar timeDiffCalendar = Calendar.getInstance(TimeZone
				.getTimeZone(TimeZone.getAvailableIDs(0)[0]));
		timeDiffCalendar.setTimeInMillis(time);
		return timeDiffCalendar;
	}

	public long insertIntoDb(final SQLiteDatabase db) throws IOException {
		long insertResult = -1;
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TITLE, title);

		initialValues.put(KEY_X, objectToByteArray(chartDataX));
		initialValues.put(KEY_Y, objectToByteArray(chartDataY));

		initialValues.put(KEY_MIN, min);
		initialValues.put(KEY_ALARM, alarm);
		initialValues.put(KEY_RATING, rating);
		initialValues.put(KEY_DURATION, duration);
		initialValues.put(KEY_SPIKES, spikes);
		initialValues.put(KEY_TIME_FELL_ASLEEP, fellAsleep);
		initialValues.put(KEY_NOTE, note);

		insertResult = db.insert(SleepHistoryDatabase.FTS_VIRTUAL_TABLE, null,
				initialValues);
		db.close();
		return insertResult;
	}
}
