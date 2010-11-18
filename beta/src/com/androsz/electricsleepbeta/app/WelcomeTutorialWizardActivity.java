package com.androsz.electricsleepbeta.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.androsz.electricsleepbeta.R;

public class WelcomeTutorialWizardActivity extends CustomTitlebarWizardActivity {

	private boolean required = false;

	private void enforceCalibrationBeforeStartingSleep() {

		final SharedPreferences userPrefs = getSharedPreferences(
				getString(R.string.prefs_version), Context.MODE_PRIVATE);
		final int prefsVersion = userPrefs.getInt(
				getString(R.string.prefs_version), 0);
		String message = "";
		if (prefsVersion == 0) {
			message = getString(R.string.message_not_calibrated);
		} else if (prefsVersion != getResources().getInteger(
				R.integer.prefs_version)) {
			message = getString(R.string.message_prefs_not_compatible);
		}

		if (message.length() > 0) {
			message += getString(R.string.message_recommend_calibration);
			final AlertDialog.Builder dialog = new AlertDialog.Builder(
					WelcomeTutorialWizardActivity.this)
					.setMessage(message)
					.setCancelable(false)
					.setPositiveButton(getString(R.string.calibrate),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(
											WelcomeTutorialWizardActivity.this,
											CalibrationWizardActivity.class));
									finish();
								}
							})
					.setNeutralButton(getString(R.string.manual),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									startActivity(new Intent(
											WelcomeTutorialWizardActivity.this,
											SettingsActivity.class));
									finish();
								}
							})
					.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.cancel();
								}
							});
			dialog.show();
		} else {
			finish();
		}
	}
	
	@Override
	protected void setupNavigationButtons()
	{
		super.setupNavigationButtons();
		final Button leftButton = (Button) findViewById(R.id.leftButton);
		if (viewFlipper.getDisplayedChild() == 0) {
			leftButton.setText(R.string.skip_tutorial);
		}
	}

	@Override
	protected int getWizardLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.wizard_welcome;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		required = getIntent().hasExtra("required");
		if (required) {
			setHomeButtonAsLogo();
		}
	}

	@Override
	protected void onFinishWizardActivity() {
		enforceCalibrationBeforeStartingSleep();
	}

	@Override
	public void onHomeClick(final View v) {
		if (required) {
			// do nothing b/c home is home!
		} else {
			super.onHomeClick(v);
		}
	}

	@Override
	public void onLeftButtonClick(final View v) {
		if (viewFlipper.getDisplayedChild() == 0) {
			onFinishWizardActivity();
		} else {
			super.onLeftButtonClick(v);
		}
	}

	@Override
	protected void onPrepareLastSlide() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		required = savedState.getBoolean("required");
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("required", required);
	}

	@Override
	protected boolean onWizardActivity() {
		// TODO Auto-generated method stub
		return false;
	}
}
