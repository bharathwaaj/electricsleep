package com.androsz.electricsleepbeta.view;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RatingBar;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.achartengine.ChartView;
import com.androsz.electricsleepbeta.achartengine.chart.AbstractChart;
import com.androsz.electricsleepbeta.achartengine.chart.TimeChart;
import com.androsz.electricsleepbeta.achartengine.model.XYMultipleSeriesDataset;
import com.androsz.electricsleepbeta.achartengine.model.XYSeries;
import com.androsz.electricsleepbeta.achartengine.renderer.XYMultipleSeriesRenderer;
import com.androsz.electricsleepbeta.achartengine.renderer.XYSeriesRenderer;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;

public class SleepChartView extends ChartView implements Serializable {

	private static final long serialVersionUID = -5692853786456847694L;

	public XYMultipleSeriesDataset xyMultipleSeriesDataset;

	public XYMultipleSeriesRenderer xyMultipleSeriesRenderer;

	public XYSeries xySeriesMovement;

	public XYSeriesRenderer xySeriesMovementRenderer;

	public int rating;

	public SleepChartView(final Context context) {
		super(context);
	}

	public SleepChartView(final Context context, final AttributeSet as) {
		super(context, as);
	}

	@Override
	public AbstractChart buildChart() {
		if (xySeriesMovement == null) {
			// set up sleep movement series/renderer
			xySeriesMovement = new XYSeries("sleep");
			xySeriesMovementRenderer = new XYSeriesRenderer();
			xySeriesMovementRenderer.setFillBelowLine(true);
			xySeriesMovementRenderer.setFillBelowLineColor(getResources()
					.getColor(R.color.primary1_transparent));
			xySeriesMovementRenderer.setColor(Color.TRANSPARENT);

			// add series to the dataset
			xyMultipleSeriesDataset = new XYMultipleSeriesDataset();
			xyMultipleSeriesDataset.addSeries(xySeriesMovement);

			// set up the dataset renderer
			xyMultipleSeriesRenderer = new XYMultipleSeriesRenderer();
			xyMultipleSeriesRenderer
					.addSeriesRenderer(xySeriesMovementRenderer);

			xyMultipleSeriesRenderer.setShowLegend(false);
			xyMultipleSeriesRenderer.setAxisTitleTextSize(17);
			xyMultipleSeriesRenderer.setLabelsTextSize(17);
			xyMultipleSeriesRenderer.setAntialiasing(true);
			// TODO move this?
			/*
			 * final Display defaultDisplay =
			 * super.getContext().getWindowManager() .getDefaultDisplay(); if
			 * (defaultDisplay.getWidth() > defaultDisplay.getHeight()) { //
			 * landscape xyMultipleSeriesRenderer.setXLabels(10); } else { //
			 * portrait xyMultipleSeriesRenderer.setXLabels(5); }
			 */
			xyMultipleSeriesRenderer.setXLabels(7);
			xyMultipleSeriesRenderer.setYLabels(0);
			xyMultipleSeriesRenderer.setYTitle(super.getContext().getString(
					R.string.movement_level_during_sleep));
			xyMultipleSeriesRenderer.setShowGrid(true);
			xyMultipleSeriesRenderer.setAxesColor(getResources().getColor(
					R.color.text));
			xyMultipleSeriesRenderer.setLabelsColor(xyMultipleSeriesRenderer
					.getAxesColor());
			final TimeChart timeChart = new TimeChart(xyMultipleSeriesDataset,
					xyMultipleSeriesRenderer);
			timeChart.setDateFormat("h:mm a");
			return timeChart;
		}
		return null;
	}

	public boolean makesSenseToDisplay() {
		return xySeriesMovement.getItemCount() > 1;
	}

	public void redraw(final double min, final double alarm) {
		if (makesSenseToDisplay()) {
			final double firstX = xySeriesMovement.mX.get(0);
			final double lastX = xySeriesMovement.mX.get(xySeriesMovement.mX
					.size() - 1);
			xyMultipleSeriesRenderer.setXAxisMin(firstX);
			xyMultipleSeriesRenderer.setXAxisMax(lastX);

			xyMultipleSeriesRenderer.setYAxisMin(min);
			xyMultipleSeriesRenderer.setYAxisMax(alarm);
			repaint();
		}
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (rating < 6 && rating > 0) {
			Drawable dStarOn = getResources().getDrawable(
					R.drawable.rate_star_small_on);
			Drawable dStarOff = getResources().getDrawable(
					R.drawable.rate_star_small_off);
			int width = dStarOn.getMinimumWidth();
			int height = dStarOn.getMinimumHeight();
			int numOffStars = 5 - rating;
			int centerThemDangStarz = (canvas.getWidth() - (width * 5)) / 2;
			for (int i = 0; i < rating; i++) {
				dStarOn.setBounds(width * (i) + centerThemDangStarz, height, width * (i) + width +centerThemDangStarz,
						height * 2);
				dStarOn.draw(canvas);
			}
			for (int i = 0; i < numOffStars; i++) {
				dStarOff.setBounds(width * (i + rating) + centerThemDangStarz, height, width
						* (i + rating) + width + centerThemDangStarz, height * 2);
				dStarOff.draw(canvas);
			}
		}
	}

	public void syncByAdding(final Double x, final Double y, final double min,
			final double alarm) {
		xySeriesMovement.mX.add(x);
		xySeriesMovement.mY.add(y);
		redraw(min, alarm);
	}

	@SuppressWarnings("unchecked")
	public void syncWithCursor(final Cursor cursor) {
		final String name = cursor
				.getString(cursor
						.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATE_TIME));

		try {

			xySeriesMovement.mX = (List<Double>) SleepHistoryDatabase
					.byteArrayToObject(cursor.getBlob(cursor
							.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATA_X)));

			xySeriesMovement.mY = (List<Double>) SleepHistoryDatabase
					.byteArrayToObject(cursor.getBlob(cursor
							.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATA_Y)));

		} catch (final StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final double min = cursor
				.getDouble(cursor
						.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATA_MIN));
		final double alarm = cursor
				.getDouble(cursor
						.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATA_ALARM));
		rating = cursor
				.getInt(cursor
						.getColumnIndexOrThrow(SleepHistoryDatabase.KEY_SLEEP_DATA_RATING));

		xyMultipleSeriesRenderer.setChartTitle(name);
		redraw(min, alarm);
	}
}
