package com.androsz.electricsleepbeta.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.androsz.electricsleepbeta.app.SettingsActivity;

public class SeekBarPreference extends DialogPreference {

	private final Context context;
	private SeekBar seekBar;
	private TextView textView;

	private final static float PRECISION = 100f;

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

	public SeekBarPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (positiveResult) {
			persistFloat(seekBar.getProgress() / PRECISION);
		}
	}

	@Override
	protected Object onGetDefaultValue(final TypedArray a, final int index) {
		return a.getFloat(index, 0);
	}

	@Override
	protected void onPrepareDialogBuilder(final Builder builder) {

		final LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setMinimumWidth(400);
		layout.setPadding(20, 20, 20, 20);

		textView = new TextView(context);
		textView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		syncTextViewText(getPersistedFloat(0));
		textView.setPadding(5, 5, 5, 5);
		layout.addView(textView);

		seekBar = new SeekBar(context);
		seekBar.setMax((int) (SettingsActivity.MAX_ALARM_SENSITIVITY * PRECISION));
		seekBar.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));

		seekBar.setProgress(Math.round((getPersistedFloat(0) * PRECISION)));
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(final SeekBar seekBar,
					final int progress, final boolean fromUser) {
				syncTextViewText(progress / PRECISION);
			}

			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(final SeekBar seekBar) {
			}
		});
		layout.addView(seekBar);

		builder.setView(layout);
		builder.setTitle(getTitle());

		super.onPrepareDialogBuilder(builder);
	}

	@Override
	protected void onSetInitialValue(final boolean restoreValue,
			final Object defaultValue) {
		final float temp = restoreValue ? getPersistedFloat(0)
				: (Float) defaultValue;
		if (!restoreValue) {
			persistFloat(temp);
		}
	}

	private void syncTextViewText(final float progress) {
		textView.setText(nf.format(progress));
	}
}
