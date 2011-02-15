/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androsz.electricsleepbeta.widget.calendar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.format.Time;
import android.util.Log;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event implements Comparable<Event>, Cloneable {

	// The indices for the projection array above.
	private static final int PROJECTION_TITLE_INDEX = 0;
	private static final int PROJECTION_LOCATION_INDEX = 1;
	private static final int PROJECTION_ALL_DAY_INDEX = 2;
	private static final int PROJECTION_COLOR_INDEX = 3;
	private static final int PROJECTION_TIMEZONE_INDEX = 4;
	private static final int PROJECTION_EVENT_ID_INDEX = 5;
	private static final int PROJECTION_BEGIN_INDEX = 6;
	private static final int PROJECTION_END_INDEX = 7;
	private static final int PROJECTION_START_DAY_INDEX = 9;
	private static final int PROJECTION_END_DAY_INDEX = 10;
	private static final int PROJECTION_START_MINUTE_INDEX = 11;
	private static final int PROJECTION_END_MINUTE_INDEX = 12;
	private static final int PROJECTION_HAS_ALARM_INDEX = 13;
	private static final int PROJECTION_RRULE_INDEX = 14;
	private static final int PROJECTION_RDATE_INDEX = 15;
	private static final int PROJECTION_SELF_ATTENDEE_STATUS_INDEX = 16;
	private static final int PROJECTION_ORGANIZER_INDEX = 17;
	private static final int PROJECTION_GUESTS_CAN_INVITE_OTHERS_INDEX = 18;

	/**
	 * Computes a position for each event. Each event is displayed as a
	 * non-overlapping rectangle. For normal events, these rectangles are
	 * displayed in separate columns in the week view and day view. For all-day
	 * events, these rectangles are displayed in separate rows along the top. In
	 * both cases, each event is assigned two numbers: N, and Max, that specify
	 * that this event is the Nth event of Max number of events that are
	 * displayed in a group. The width and position of each rectangle depend on
	 * the maximum number of rectangles that occur at the same time.
	 * 
	 * @param eventsList
	 *            the list of events, sorted into increasing time order
	 */
	static void computePositions(ArrayList<Event> eventsList) {
		if (eventsList == null)
			return;

		doComputePositions(eventsList);
	}

	private static void doComputePositions(ArrayList<Event> eventsList) {
		final ArrayList<Event> activeList = new ArrayList<Event>();
		final ArrayList<Event> groupList = new ArrayList<Event>();

		long colMask = 0;
		int maxCols = 0;
		for (final Event event : eventsList) {

			final long start = event.getStartMillis();

			// Remove the inactive events. An event on the active list
			// becomes inactive when its end time is less than or equal to
			// the current event's start time.
			final Iterator<Event> iter = activeList.iterator();
			while (iter.hasNext()) {
				final Event active = iter.next();
				if (active.getEndMillis() <= start) {
					colMask &= ~(1L << active.getColumn());
					iter.remove();
				}
			}

			// If the active list is empty, then reset the max columns, clear
			// the column bit mask, and empty the groupList.
			if (activeList.isEmpty()) {
				for (final Event ev : groupList) {
					ev.setMaxColumns(maxCols);
				}
				maxCols = 0;
				colMask = 0;
				groupList.clear();
			}

			// Find the first empty column. Empty columns are represented by
			// zero bits in the column mask "colMask".
			int col = findFirstZeroBit(colMask);
			if (col == 64) {
				col = 63;
			}
			colMask |= (1L << col);
			event.setColumn(col);
			activeList.add(event);
			groupList.add(event);
			final int len = activeList.size();
			if (maxCols < len) {
				maxCols = len;
			}
		}
		for (final Event ev : groupList) {
			ev.setMaxColumns(maxCols);
		}
	}

	public static int findFirstZeroBit(long val) {
		for (int ii = 0; ii < 64; ++ii) {
			if ((val & (1L << ii)) == 0)
				return ii;
		}
		return 64;
	}

	/**
	 * Loads <i>days</i> days worth of instances starting at <i>start</i>.
	 */
	public static void loadEvents(Context context, ArrayList<Event> events,
			long start, int days, int requestId, AtomicInteger sequenceNumber) {

		Cursor c = null;

		events.clear();
		try {
			final Time local = new Time();
			int count;

			local.set(start);
			final int startDay = Time.getJulianDay(start, local.gmtoff);
			final int endDay = startDay + days;

			local.monthDay += days;
			local.normalize(true /* ignore isDst */);

			// Widen the time range that we query by one day on each end
			// so that we can catch all-day events. All-day events are
			// stored starting at midnight in UTC but should be included
			// in the list of events starting at midnight local time.
			// This may fetch more events than we actually want, so we
			// filter them out below.
			//
			// The sort order is: events with an earlier start time occur
			// first and if the start times are the same, then events with
			// a later end time occur first. The later end time is ordered
			// first so that long rectangles in the calendar views appear on
			// the left side. If the start and end times of two events are
			// the same then we sort alphabetically on the title. This isn't
			// required for correctness, it just adds a nice touch.

			// String orderBy = "";//Instances.SORT_CALENDAR_VIEW;
			final SleepHistoryDatabase shdb = new SleepHistoryDatabase(context);
			// TODO: hook this into sleep db

			c = shdb.getSleepMatches(context.getString(R.string.to),
					new String[] { BaseColumns._ID, SleepRecord.KEY_TITLE });

			shdb.close();

			if (c == null) {
				Log.e("Cal", "loadEvents() returned null cursor!");
				return;
			}

			// Check if we should return early because there are more recent
			// load requests waiting.
			if (requestId != sequenceNumber.get())
				return;

			count = c.getCount();

			if (count == 0)
				return;

			context.getResources();
			while (c.moveToNext()) {
				final Event e = new Event();

				e.title = c.getString(c
						.getColumnIndexOrThrow(SleepRecord.KEY_TITLE));

				final long eStart = c.getLong(PROJECTION_BEGIN_INDEX);
				final long eEnd = c.getLong(PROJECTION_END_INDEX);

				e.startMillis = eStart;
				e.startTime = c.getInt(PROJECTION_START_MINUTE_INDEX);
				e.startDay = c.getInt(PROJECTION_START_DAY_INDEX);

				e.endMillis = eEnd;
				e.endTime = c.getInt(PROJECTION_END_MINUTE_INDEX);
				e.endDay = c.getInt(PROJECTION_END_DAY_INDEX);

				if (e.startDay > endDay || e.endDay < startDay) {
					continue;
				}

				events.add(e);
			}

			computePositions(events);
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	public static final Event newInstance() {
		final Event e = new Event();

		e.title = null;
		e.startDay = 0;
		e.endDay = 0;
		e.startTime = 0;
		e.endTime = 0;
		e.startMillis = 0;
		e.endMillis = 0;

		return e;
	}

	public CharSequence title;
	public int startDay; // start Julian day
	public int endDay; // end Julian day
	public int startTime; // Start and end time are in minutes since midnight

	public int endTime;
	public long startMillis; // UTC milliseconds since the epoch
	public long endMillis; // UTC milliseconds since the epoch
	private int mColumn;

	private int mMaxColumns;

	// The coordinates of the event rectangle drawn on the screen.
	public float left;

	public float right;

	public float top;

	public float bottom;

	@Override
	public final Object clone() throws CloneNotSupportedException {
		super.clone();
		final Event e = new Event();

		e.title = title;
		e.startDay = startDay;
		e.endDay = endDay;
		e.startTime = startTime;
		e.endTime = endTime;
		e.startMillis = startMillis;
		e.endMillis = endMillis;

		return e;
	}

	/**
	 * Compare string a with string b, but if either string is null, then treat
	 * it (the null) as if it were the empty string ("").
	 * 
	 * @param a
	 *            the first string
	 * @param b
	 *            the second string
	 * @return the result of comparing a with b after replacing null strings
	 *         with "".
	 */
	private int compareStrings(CharSequence a, CharSequence b) {
		String aStr, bStr;
		if (a != null) {
			aStr = a.toString();
		} else {
			aStr = "";
		}
		if (b != null) {
			bStr = b.toString();
		} else {
			bStr = "";
		}
		return aStr.compareTo(bStr);
	}

	/**
	 * Compares this event to the given event. This is just used for checking if
	 * two events differ. It's not used for sorting anymore.
	 */
	@Override
	public final int compareTo(Event obj) {
		// The earlier start day and time comes first
		if (startDay < obj.startDay)
			return -1;
		if (startDay > obj.startDay)
			return 1;
		if (startTime < obj.startTime)
			return -1;
		if (startTime > obj.startTime)
			return 1;

		// The later end time comes first (in order to put long strips on
		// the left).
		if (endDay < obj.endDay)
			return 1;
		if (endDay > obj.endDay)
			return -1;
		if (endTime < obj.endTime)
			return 1;
		if (endTime > obj.endTime)
			return -1;

		// If two events have the same time range, then sort them in
		// alphabetical order based on their titles.
		final int cmp = compareStrings(title, obj.title);
		if (cmp != 0)
			return cmp;

		return 0;
	}

	public final void copyTo(Event dest) {
		dest.title = title;
		dest.startDay = startDay;
		dest.endDay = endDay;
		dest.startTime = startTime;
		dest.endTime = endTime;
		dest.startMillis = startMillis;
		dest.endMillis = endMillis;
	}

	public int getColumn() {
		return mColumn;
	}

	public long getEndMillis() {
		return endMillis;
	}

	public int getMaxColumns() {
		return mMaxColumns;
	}

	public long getStartMillis() {
		return startMillis;
	}

	/**
	 * Returns the event title and location separated by a comma. If the
	 * location is already part of the title (at the end of the title), then
	 * just the title is returned.
	 * 
	 * @return the event title and location as a String
	 */
	public String getTitleAndLocation() {
		final String text = title.toString();

		return text;
	}

	public final boolean intersects(int julianDay, int startMinute,
			int endMinute) {
		if (endDay < julianDay)
			return false;

		if (startDay > julianDay)
			return false;

		if (endDay == julianDay) {
			if (endTime < startMinute)
				return false;
			// An event that ends at the start minute should not be considered
			// as intersecting the given time span, but don't exclude
			// zero-length (or very short) events.
			if (endTime == startMinute
					&& (startTime != endTime || startDay != endDay))
				return false;
		}

		if (startDay == julianDay && startTime > endMinute)
			return false;

		return true;
	}

	public void setColumn(int column) {
		mColumn = column;
	}

	public void setEndMillis(long endMillis) {
		this.endMillis = endMillis;
	}

	public void setMaxColumns(int maxColumns) {
		mMaxColumns = maxColumns;
	}

	public void setStartMillis(long startMillis) {
		this.startMillis = startMillis;
	}
}
