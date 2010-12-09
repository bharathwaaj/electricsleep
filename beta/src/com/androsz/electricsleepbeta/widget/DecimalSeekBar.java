package com.androsz.electricsleepbeta.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class DecimalSeekBar extends SeekBar {

	public final static float PRECISION = 100f;

	private static java.text.NumberFormat nf = java.text.NumberFormat
			.getInstance();

	static {
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(("" + (int) Math.pow(PRECISION, 0.5))
				.length());
		nf.setMinimumFractionDigits(("" + (int) Math.pow(PRECISION, 0.5))
				.length());
	}
	
	public DecimalSeekBar(Context context, AttributeSet as) {
		super(context, as);
		setMax(Math.round((10 * PRECISION)));
	}

	public DecimalSeekBar(Context context) {
		super(context);
	}
	
	public float getFloatProgress() {
		return super.getProgress() / PRECISION;
	}

	public void setMax(int max)
	{
		super.setMax(Math.round((max * PRECISION)));
	}
	
	public void setProgress(int progress) {
		super.setProgress(Math.round(progress * PRECISION));
	}
	
	public void setMax(float max)
	{
		super.setMax(Math.round((max * PRECISION)));
	}
	
	public void setProgress(float progress) {
		super.setProgress(Math.round(progress * PRECISION));
	}
}
