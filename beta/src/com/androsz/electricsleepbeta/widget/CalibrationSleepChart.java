package com.androsz.electricsleepbeta.widget;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.achartengine.chart.AbstractChart;
import com.androsz.electricsleepbeta.achartengine.chart.TimeChart;
import com.androsz.electricsleepbeta.achartengine.model.XYMultipleSeriesDataset;
import com.androsz.electricsleepbeta.achartengine.model.XYSeries;
import com.androsz.electricsleepbeta.achartengine.renderer.XYMultipleSeriesRenderer;
import com.androsz.electricsleepbeta.achartengine.renderer.XYSeriesRenderer;
import com.androsz.electricsleepbeta.app.SettingsActivity;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

public class CalibrationSleepChart extends SleepChart {

	private static final long serialVersionUID = -3856680457687369240L;
	public XYSeries xySeriesCalibration;
	public XYSeriesRenderer xySeriesCalibrationRenderer;

	public double getCalibrationLevel() {
		return calibrationLevel;
	}

	public void setCalibrationLevel(double calibrationLevel) {
		this.calibrationLevel = calibrationLevel;
	}

	private double calibrationLevel = 0.5d;

	public CalibrationSleepChart(Context context) {
		super(context);
	}

	public CalibrationSleepChart(Context context, AttributeSet as) {
		super(context, as);
	}

	protected AbstractChart buildChart() {
		if (xySeriesMovement == null) {
			// set up sleep movement series/renderer
			xySeriesMovement = new XYSeries("Movement");
			xySeriesMovementRenderer = new XYSeriesRenderer();
			xySeriesMovementRenderer.setFillBelowLine(true);
			xySeriesMovementRenderer.setFillBelowLineColor(getResources()
					.getColor(R.color.primary1_transparent));
			xySeriesMovementRenderer.setColor(getResources().getColor(
					R.color.primary1));

			xySeriesCalibration = new XYSeries("Light Sleep Trigger");
			xySeriesCalibrationRenderer = new XYSeriesRenderer();
			xySeriesCalibrationRenderer.setFillBelowLine(true);
			xySeriesCalibrationRenderer.setFillBelowLineColor(getResources()
					.getColor(R.color.background_transparent_lighten));
			xySeriesCalibrationRenderer.setColor(getResources()
					.getColor(R.color.white));

			// add series to the dataset
			xyMultipleSeriesDataset = new XYMultipleSeriesDataset();
			xyMultipleSeriesDataset.addSeries(xySeriesMovement);
			xyMultipleSeriesDataset.addSeries(xySeriesCalibration);

			// set up the dataset renderer
			xyMultipleSeriesRenderer = new XYMultipleSeriesRenderer();
			xyMultipleSeriesRenderer
					.addSeriesRenderer(xySeriesMovementRenderer);
			xyMultipleSeriesRenderer
					.addSeriesRenderer(xySeriesCalibrationRenderer);

			xyMultipleSeriesRenderer.setShowLegend(true);
			xyMultipleSeriesRenderer.setLegendTextSize(24);
			xyMultipleSeriesRenderer.setAxisTitleTextSize(17);
			xyMultipleSeriesRenderer.setLabelsTextSize(17);

			xyMultipleSeriesRenderer.setXLabels(0);
			xyMultipleSeriesRenderer.setYLabels(4);
			//xyMultipleSeriesRenderer.setYTitle(super.getContext().getString(
			//		R.string.movement_level_during_sleep));
			xyMultipleSeriesRenderer.setShowGrid(true);
			xyMultipleSeriesRenderer.setAxesColor(getResources().getColor(
					R.color.text));
			xyMultipleSeriesRenderer.setLabelsColor(xyMultipleSeriesRenderer
					.getAxesColor());
			final TimeChart timeChart = new TimeChart(xyMultipleSeriesDataset,
					xyMultipleSeriesRenderer);
			timeChart.setDateFormat("");
			return timeChart;
		}
		return null;
	}

	public void reconfigure(final double min, final double alarm) {
		super.reconfigure(0d, 2d);
		if (makesSenseToDisplay()) {
			// reconfigure the calibration line..
			xySeriesCalibration.clear();

			xySeriesCalibration.add(firstX, calibrationLevel);
			xySeriesCalibration.add(lastX, calibrationLevel);
		}
	}
}
