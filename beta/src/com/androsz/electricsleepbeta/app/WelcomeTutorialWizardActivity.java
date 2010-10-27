package com.androsz.electricsleepbeta.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import com.androsz.electricsleepbeta.R;

public class WelcomeTutorialWizardActivity extends CustomTitlebarWizardActivity {

	@Override
	protected int getWizardLayoutId() {
		// TODO Auto-generated method stub
		return R.layout.wizard_welcome;
	}

	@Override
	protected void onFinishWizardActivity() {
		enforceCalibrationBeforeStartingSleep();
	}
	
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
									startActivity(new Intent(WelcomeTutorialWizardActivity.this,
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
									startActivity(new Intent(WelcomeTutorialWizardActivity.this,
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
		}
		else
		{
			finish();
		}
	}
	
	public void onLeftButtonClick(View v)
	{
		if(viewFlipper.getDisplayedChild() == 0)
		{
			return;
		}
		else
			super.onLeftButtonClick(v);
	}

	@Override
	protected void onPrepareLastSlide() {
		// TODO Auto-generated method stub
	}

	public void onSkipTutorialClick(final View v) {
		onFinishWizardActivity();
	}

	@Override
	protected boolean onWizardActivity() {
		// TODO Auto-generated method stub
		return false;
	}
}
