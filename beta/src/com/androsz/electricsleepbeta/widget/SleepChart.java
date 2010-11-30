package com.androsz.electricsleepbeta.widget;

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
import android.view.View;
import android.view.animation.Animation;
import android.widget.ProgressBar;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.achartengine.ChartView;
import com.androsz.electricsleepbeta.achartengine.chart.AbstractChart;
import com.androsz.electricsleepbeta.achartengine.chart.TimeChart;
import com.androsz.electricsleepbeta.achartengine.model.XYMultipleSeriesDataset;
import com.androsz.electricsleepbeta.achartengine.model.XYSeries;
import com.androsz.electricsleepbeta.achartengine.renderer.XYMultipleSeriesRenderer;
import com.androsz.electricsleepbeta.achartengine.renderer.XYSeriesRenderer;
import com.androsz.electricsleepbeta.db.SleepHistoryDatabase;
import com.androsz.electricsleepbeta.db.SleepRecord;

public class SleepChart extends ChartView implements Serializable {

	private static final long serialVersionUID = -5692853786456847694L;

	public XYMultipleSeriesDataset xyMultipleSeriesDataset;

	public XYMultipleSeriesRenderer xyMultipleSeriesRenderer;

	public XYSeries xySeriesMovement;

	public XYSeriesRenderer xySeriesMovementRenderer;

	public int rating;

	public SleepChart(final Context context) {
		super(context);
	}

	public SleepChart(final Context context, final AttributeSet as) {
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

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		if (rating < 6 && rating > 0) {
			final Drawable dStarOn = getResources().getDrawable(
					R.drawable.rate_star_small_on);
			final Drawable dStarOff = getResources().getDrawable(
					R.drawable.rate_star_small_off);
			final int width = dStarOn.getMinimumWidth();
			final int height = dStarOn.getMinimumHeight();
			final int numOffStars = 5 - rating;
			final int centerThemDangStarz = (canvas.getWidth() - width * 5) / 2;
			for (int i = 0; i < rating; i++) {
				dStarOn.setBounds(width * i + centerThemDangStarz, height,
						width * i + width + centerThemDangStarz, height * 2);
				dStarOn.draw(canvas);
			}
			for (int i = 0; i < numOffStars; i++) {
				dStarOff.setBounds(width * (i + rating) + centerThemDangStarz,
						height, width * (i + rating) + width
								+ centerThemDangStarz, height * 2);
				dStarOff.draw(canvas);
			}
		}
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

	public void syncByAdding(final Double x, final Double y, final double min,
			final double alarm) {
		xySeriesMovement.mX.add(x);
		xySeriesMovement.mY.add(y);
		redraw(min, alarm);
	}

	public void syncWithCursor(final Cursor cursor)
			throws StreamCorruptedException, IllegalArgumentException,
			IOException, ClassNotFoundException {
		syncWithRecord(new SleepRecord(cursor));
	}

	public void syncWithRecord(final SleepRecord sleepRecord) {
		xySeriesMovement.mX = sleepRecord.chartDataX;

		xySeriesMovement.mY = sleepRecord.chartDataY;

		rating = sleepRecord.rating;

		xyMultipleSeriesRenderer.setChartTitle(sleepRecord.title);
		redraw(sleepRecord.min, sleepRecord.alarm);
	}
}
