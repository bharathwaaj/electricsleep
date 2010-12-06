package com.androsz.electricsleepbeta.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;

public abstract class CustomTitlebarTabActivity extends CustomTitlebarActivity {
	protected TabHost tabHost = null;

	public void addTab(final View v, final int resIndicator) {
		final TabHost.TabSpec spec = tabHost.newTabSpec(resIndicator + "");

		spec.setContent(new TabHost.TabContentFactory() {
			@Override
			public View createTabContent(final String tag) {
				return v;
			}
		});

		spec.setIndicator(buildIndicator(resIndicator));
		tabHost.addTab(spec);
	}

	/**
	 * Build a {@link View} to be used as a tab indicator, setting the requested
	 * string resource as its label.
	 */
	protected View buildIndicator(final int textRes) {
		final TextView indicator = (TextView) getLayoutInflater().inflate(
				R.layout.tab_indicator, tabHost.getTabWidget(), false);
		indicator.setText(textRes);
		return indicator;
	}

	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		tabHost.setCurrentTab(savedInstanceState.getInt("selectedTabIndex"));
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putInt("selectedTabIndex", tabHost.getCurrentTab());
		super.onSaveInstanceState(outState);
	}
}
