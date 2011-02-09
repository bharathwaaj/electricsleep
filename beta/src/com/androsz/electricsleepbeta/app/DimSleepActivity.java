package com.androsz.electricsleepbeta.app;

import java.lang.reflect.Field;

import com.androsz.electricsleepbeta.R;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class DimSleepActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Window win = getWindow();
		final WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_FULLSCREEN;

		// set screen brightness to the lowest possible without turning screen off
		winParams.screenBrightness = 0.01f;

		if (Build.VERSION.SDK_INT < 8) {
			// hack for pre-froyo to set buttonBrightness off
			try {
				Field buttonBrightness = winParams.getClass().getField(
						"buttonBrightness");
				buttonBrightness.set(winParams,
						WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
		}

		//win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//		WindowManager.LayoutParams.FLAG_FULLSCREEN);
		win.setAttributes(winParams);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		LinearLayout blackness = new LinearLayout(this);
		blackness.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		blackness.setBackgroundColor(Color.BLACK);
		Toast.makeText(this,
				"Your screen is in dim mode. To exit, press the back button.",
				Toast.LENGTH_LONG).show();
		setContentView(blackness);
	}
}
