/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androsz.electricsleepbeta.alarmclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.androsz.electricsleepbeta.R;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play if
 * another activity overrides the AlarmAlert dialog.
 */
public class AlarmKlaxon extends Service {

	/** Play alarm up to 10 minutes before silencing */
	private static final int ALARM_TIMEOUT_SECONDS = 10 * 60;

	private static final long[] sVibratePattern = new long[] { (long) (java.lang.Math.random()* 500), (long) (java.lang.Math.random()* 500) };

	private boolean mPlaying = false;
	private Vibrator mVibrator;
	private MediaPlayer mMediaPlayer;
	private Alarm mCurrentAlarm;
	private long mStartTime;
	private TelephonyManager mTelephonyManager;
	private int mInitialCallState;

	// Internal messages
	private static final int KILLER = 1000;
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case KILLER:
				if (Log.LOGV) {
					Log.v("*********** Alarm killer triggered ***********");
				}
				sendKillBroadcast((Alarm) msg.obj);
				stopSelf();
				break;
			}
		}
	};

	private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(final int state, final String ignored) {
			// The user might already be in a call when the alarm fires. When
			// we register onCallStateChanged, we get the initial in-call state
			// which kills the alarm. Check against the initial call state so
			// we don't kill the alarm during a call.
			if (state != TelephonyManager.CALL_STATE_IDLE
					&& state != mInitialCallState) {
				sendKillBroadcast(mCurrentAlarm);
				stopSelf();
			}
		}
	};

	// Volume suggested by media team for in-call alarms.
	private static final float IN_CALL_VOLUME = 0.125f;

	private void disableKiller() {
		mHandler.removeMessages(KILLER);
	}

	/**
	 * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm won't run all
	 * day.
	 * 
	 * This just cancels the audio, but leaves the notification popped, so the
	 * user will know that the alarm tripped.
	 */
	private void enableKiller(final Alarm alarm) {
		mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
				1000 * ALARM_TIMEOUT_SECONDS);
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		// Listen for incoming calls to kill the alarm.
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyManager.listen(mPhoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);
		AlarmAlertWakeLock.acquireCpuWakeLock(this);
	}

	@Override
	public void onDestroy() {
		stop();
		// Stop listening for incoming calls.
		mTelephonyManager.listen(mPhoneStateListener, 0);
		AlarmAlertWakeLock.releaseCpuLock();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		// No intent, tell the system not to restart us.
		if (intent == null) {
			stopSelf();
			return START_NOT_STICKY;
		}

		final Alarm alarm = intent
				.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);

		if (alarm == null) {
			Log.v("AlarmKlaxon failed to parse the alarm from the intent");
			stopSelf();
			return START_NOT_STICKY;
		}

		if (startId != 1) {
			return START_NOT_STICKY;
		}

		if (mCurrentAlarm != null) {
			sendKillBroadcast(mCurrentAlarm);
		}

		play(alarm);
		mCurrentAlarm = alarm;
		// Record the initial call state here so that the new alarm has the
		// newest state.
		mInitialCallState = mTelephonyManager.getCallState();

		return START_STICKY;
	}

	private void play(final Alarm alarm) {
		// stop() checks to see if we are already playing.
		stop();

		if (Log.LOGV) {
			Log.v("AlarmKlaxon.play() " + alarm.id + " alert " + alarm.alert);
		}

		if (!alarm.silent) {
			Uri alert = alarm.alert;
			// Fall back on the default alarm if the database does not have an
			// alarm stored.
			if (alert == null) {
				alert = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_ALARM);
				if (Log.LOGV) {
					Log.v("Using default alarm: " + alert.toString());
				}
			}

			// TODO: Reuse mMediaPlayer instead of creating a new one and/or use
			// RingtoneManager.
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setOnErrorListener(new OnErrorListener() {
				@Override
				public boolean onError(final MediaPlayer mp, final int what,
						final int extra) {
					Log.e("Error occurred while playing audio.");
					mp.stop();
					mp.release();
					mMediaPlayer = null;
					return true;
				}
			});

			try {
				// Check if we are in a call. If we are, use the in-call alarm
				// resource at a low volume to not disrupt the call.
				if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
					Log.v("Using the in-call alarm");
					mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
					setDataSourceFromResource(getResources(), mMediaPlayer,
							R.raw.fallbackring);
				} else {
					mMediaPlayer.setDataSource(this, alert);
				}
				startAlarm(mMediaPlayer);
			} catch (final Exception ex) {
				Log.v("Using the fallback ringtone");
				// The alert may be on the sd card which could be busy right
				// now. Use the fallback ringtone.
				try {
					// Must reset the media player to clear the error state.
					mMediaPlayer.reset();
					setDataSourceFromResource(getResources(), mMediaPlayer,
							R.raw.fallbackring);
					startAlarm(mMediaPlayer);
				} catch (final Exception ex2) {
					// At this point we just don't play anything.
					Log.e("Failed to play fallback ringtone", ex2);
				}
			}
		}

		/* Start the vibrator after everything is ok with the media player */
		if (alarm.vibrate) {
			mVibrator.vibrate(sVibratePattern, 0);
		} else {
			mVibrator.cancel();
		}

		enableKiller(alarm);
		mPlaying = true;
		mStartTime = System.currentTimeMillis();
	}

	private void sendKillBroadcast(final Alarm alarm) {
		final long millis = System.currentTimeMillis() - mStartTime;
		final int minutes = (int) Math.round(millis / 60000.0);
		final Intent alarmKilled = new Intent(Alarms.ALARM_KILLED);
		alarmKilled.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
		alarmKilled.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
		sendBroadcast(alarmKilled);
	}

	private void setDataSourceFromResource(final Resources resources,
			final MediaPlayer player, final int res) throws java.io.IOException {
		final AssetFileDescriptor afd = resources.openRawResourceFd(res);
		if (afd != null) {
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
					afd.getLength());
			afd.close();
		}
	}

	// Do the common stuff when starting the alarm.
	private void startAlarm(final MediaPlayer player)
			throws java.io.IOException, IllegalArgumentException,
			IllegalStateException {
		final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// do not play alarms if stream volume is 0
		// (typically because ringer mode is silent).
		if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
			player.setAudioStreamType(AudioManager.STREAM_ALARM);
			player.setLooping(true);
			//TODO: prepareAsync should be used here.
			player.prepare();
			player.start();
		}
	}

	/**
	 * Stops alarm audio and disables alarm if it not snoozed and not repeating
	 */
	public void stop() {
		if (Log.LOGV) {
			Log.v("AlarmKlaxon.stop()");
		}
		if (mPlaying) {
			mPlaying = false;

			final Intent alarmDone = new Intent(Alarms.ALARM_DONE_ACTION);
			sendBroadcast(alarmDone);

			// Stop audio playing
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}

			// Stop vibrator
			mVibrator.cancel();
		}
		disableKiller();
	}

}
