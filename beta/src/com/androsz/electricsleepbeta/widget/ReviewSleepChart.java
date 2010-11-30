package com.androsz.electricsleepbeta.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TabHost;

public class ReviewSleepChart extends RelativeLayout {

	SleepChart sleepChart;
	private TabHost tabHost;
	
	public ReviewSleepChart(Context context) {
		super(context);
	}
	
	public ReviewSleepChart(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
}