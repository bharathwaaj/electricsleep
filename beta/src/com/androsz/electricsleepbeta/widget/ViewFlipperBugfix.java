package com.androsz.electricsleepbeta.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

public class ViewFlipperBugfix extends ViewFlipper {

	public ViewFlipperBugfix(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public ViewFlipperBugfix(Context context, AttributeSet as) {
		super(context, as);
		// TODO Auto-generated constructor stub
	}
	
	//hack to prevent a crash on android 2.1 and 2.2 - http://daniel-codes.blogspot.com/2010/05/viewflipper-receiver-not-registered.html
	@Override
	public void onDetachedFromWindow() {
	    try {
	        super.onDetachedFromWindow();
	    }
	    catch (IllegalArgumentException e) {
	        stopFlipping();
	    }
	}

}
