package com.androsz.electricsleepbeta.app;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

import com.androsz.electricsleepbeta.R;

public class AboutActivity extends CustomTitlebarActivity {
	@Override
	protected int getContentAreaLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.activity_about;
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			((TextView) findViewById(R.id.about_version_text))
					.setText(getPackageManager()
							.getPackageInfo(this.getPackageName(),
									PackageManager.GET_META_DATA).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
