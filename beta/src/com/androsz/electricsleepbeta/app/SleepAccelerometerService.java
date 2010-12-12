package com.androsz.electricsleepbeta.app;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.Alarm;
import com.androsz.electricsleepbeta.alarmclock.Alarms;

public class SleepAccelerometerService extends Service implements
		SensorEventListener {
	private static final String LOCK_TAG = "com.androsz.electricsleepbeta.app.SleepAccelerometerService";
	private static final int NOTIFICATION_ID = 0x1337a;
	public static final String POKE_SYNC_CHART = "com.androsz.electricsleepbeta.POKE_SYNC_CHART";

	public static final String SLEEP_STOPPED = "com.androsz.electricsleepbeta.SLEEP_STOPPED";

	public static final String STOP_AND_SAVE_SLEEP = "com.androsz.electricsleepbeta.STOP_AND_SAVE_SLEEP";
	private boolean airplaneMode = false;

	private final BroadcastReceiver alarmDoneReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			createSaveSleepNotification();
			stopSelf();
		}
	};

	private double alarmTriggerSensitivity = SettingsActivity.DEFAULT_ALARM_SENSITIVITY;
	private int alarmWindow = 30;

	private final ArrayList<Double> currentSeriesX = new ArrayList<Double>();
	private final ArrayList<Double> currentSeriesY = new ArrayList<Double>();
	private Date dateStarted;

	private long lastChartUpdateTime = 0;

	//private double mAccel = 0;

	//private double mAccelCurrent = 0;

	//private double mAccelLast = Double.POSITIVE_INFINITY;

	private double maxNetForce = 0;

	private double minNetForce = Double.MAX_VALUE;

	private WakeLock partialWakeLock;

	private final BroadcastReceiver pokeSyncChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final Intent i = new Intent(SleepActivity.SYNC_CHART);
			i.putExtra("currentSeriesX", currentSeriesX);
			i.putExtra("currentSeriesY", currentSeriesY);
			i.putExtra("min", minNetForce);
			i.putExtra("alarm", alarmTriggerSensitivity);
			i.putExtra("useAlarm", useAlarm);
			i.putExtra("forceScreenOn", forceScreenOn);
			sendBroadcast(i);
		}
	};

	private int testModeRate;

	public int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

	private final BroadcastReceiver stopAndSaveSleepReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final Intent saveIntent = addExtrasToSaveSleepIntent(new Intent(
					SleepAccelerometerService.this, SaveSleepActivity.class));
			startActivity(saveIntent);
			stopSelf();
		}
	};

	private final static int INTERVAL = 5000;
	private int updateInterval = INTERVAL;

	private boolean useAlarm = false;

	private boolean forceScreenOn = false;

	//private double averageForce = 0;

	//private int numberOfSamples = 0;

	private final float[] gravity = { 0, 0, 0 };

	private Intent addExtrasToSaveSleepIntent(final Intent saveIntent) {
		saveIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		saveIntent.putExtra("id", hashCode());
		saveIntent.putExtra("currentSeriesX", currentSeriesX);
		saveIntent.putExtra("currentSeriesY", currentSeriesY);
		saveIntent.putExtra("min", minNetForce);
		saveIntent.putExtra("alarm", alarmTriggerSensitivity);

		// send start/end time as well
		final DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT, Locale.getDefault());
		DateFormat sdf2 = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT, Locale.getDefault());
		final Date now = new Date();
		if (dateStarted.getDate() == now.getDate()) {
			sdf2 = DateFormat.getTimeInstance(DateFormat.SHORT);
		}
		saveIntent.putExtra("name", sdf.format(dateStarted) + " "
				+ getText(R.string.to) + " " + sdf2.format(now));
		return saveIntent;
	}

	private void createSaveSleepNotification() {
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		final int icon = R.drawable.home_btn_sleep_pressed;
		final CharSequence tickerText = getText(R.string.notification_save_sleep_ticker);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText,
				when);

		notification.flags = Notification.FLAG_AUTO_CANCEL;

		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.notification_save_sleep_title);
		final CharSequence contentText = getText(R.string.notification_save_sleep_text);
		final Intent notificationIntent = addExtrasToSaveSleepIntent(new Intent(
				this, SaveSleepActivity.class));
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		notificationManager.notify(this.hashCode(), notification);
		startActivity(notificationIntent);
	}

	private Notification createServiceNotification() {
		final int icon = R.drawable.icon_small;
		final CharSequence tickerText = getText(R.string.notification_sleep_ticker);
		final long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText,
				when);

		notification.flags = Notification.FLAG_ONGOING_EVENT;

		final Context context = getApplicationContext();
		final CharSequence contentTitle = getText(R.string.notification_sleep_title);
		final CharSequence contentText = getText(R.string.notification_sleep_text);
		final Intent notificationIntent = new Intent(this, SleepActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		return notification;
	}

	private void obtainWakeLock() {
		final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		partialWakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG);
		partialWakeLock.acquire();
		partialWakeLock.setReferenceCounted(false);
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
		// not used
	}

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		startForeground(NOTIFICATION_ID, createServiceNotification());

		lastChartUpdateTime = System.currentTimeMillis();

		registerReceiver(pokeSyncChartReceiver, new IntentFilter(
				POKE_SYNC_CHART));

		registerReceiver(stopAndSaveSleepReceiver, new IntentFilter(
				STOP_AND_SAVE_SLEEP));

		registerReceiver(alarmDoneReceiver, new IntentFilter(
				Alarms.ALARM_DONE_ACTION));

		dateStarted = new Date();

	}

	@Override
	public void onDestroy() {
		unregisterAccelerometerListener();

		if (partialWakeLock != null && partialWakeLock.isHeld()) {
			partialWakeLock.release();
		}

		unregisterReceiver(pokeSyncChartReceiver);
		unregisterReceiver(stopAndSaveSleepReceiver);
		unregisterReceiver(alarmDoneReceiver);

		// tell monitoring activities that sleep has ended
		sendBroadcast(new Intent(SLEEP_STOPPED));

		toggleAirplaneMode(false);

		stopForeground(true);

		final SharedPreferences.Editor ed = getSharedPreferences(
				"serviceIsRunning", Context.MODE_PRIVATE).edit();
		ed.putBoolean("serviceIsRunning", false);
		ed.commit();

		super.onDestroy();
	}

	@Override
	public synchronized void onSensorChanged(final SensorEvent event) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				final long currentTime = System.currentTimeMillis();
				final float alpha = 0.5f;

				/*if (Double.isInfinite(mAccelLast)) {

					gravity[0] = alpha * gravity[0] + (1 - alpha)
							* event.values[0];
					gravity[1] = alpha * gravity[1] + (1 - alpha)
							* event.values[1];
					gravity[2] = alpha * gravity[2] + (1 - alpha)
							* event.values[2];

					final double curX = event.values[0] - gravity[0];
					final double curY = event.values[1] - gravity[1];
					final double curZ = event.values[2] - gravity[2];
					mAccelCurrent = Math.sqrt(curX * curX + curY * curY + curZ
							* curZ);
					mAccelLast = mAccelCurrent;
					lastChartUpdateTime = currentTime;
					return;
				}*/

				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

				final double curX = event.values[0] - gravity[0];
				final double curY = event.values[1] - gravity[1];
				final double curZ = event.values[2] - gravity[2];

				//mAccelLast = mAccelCurrent;
				final double mAccelCurrent = Math.sqrt(curX * curX + curY * curY + curZ
						* curZ);
				// final double delta = mAccelCurrent - mAccelLast;
				// mAccel = mAccel * 0.1f + delta; // perform low-cut filter
				final double absAccel = Math.abs(mAccelCurrent);
				maxNetForce = absAccel > maxNetForce ? absAccel : maxNetForce;
				//averageForce += absAccel;
				//numberOfSamples++;

				// lastOnSensorChangedTime = currentTime;

				if (currentTime - lastChartUpdateTime >= updateInterval) {

					//averageForce /= numberOfSamples;

					final double x = currentTime;
					final double y = java.lang.Math
							.min(alarmTriggerSensitivity, maxNetForce);
									/*(maxNetForce >= alarmTriggerSensitivity) ? maxNetForce
											: averageForce);*/
					if (y < minNetForce) {
						minNetForce = y;
					}
					currentSeriesX.add(x);
					currentSeriesY.add(y);

					final Intent i = new Intent(SleepActivity.UPDATE_CHART);
					i.putExtra("x", x);
					i.putExtra("y", y);
					i.putExtra("min", minNetForce);
					i.putExtra("alarm", alarmTriggerSensitivity);
					sendBroadcast(i);

					// totalTimeBetweenSensorChanges = 0;

					lastChartUpdateTime = currentTime;
					maxNetForce = 0;
					//averageForce = 0;
					//numberOfSamples = 0;

					if (triggerAlarmIfNecessary(currentTime, y)) {
						unregisterAccelerometerListener();
					} else if (forceScreenOn) {
						final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

						final WakeLock forceScreenOnWakeLock = powerManager
								.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
										| PowerManager.ON_AFTER_RELEASE
										| PowerManager.ACQUIRE_CAUSES_WAKEUP,
										LOCK_TAG+"1");
						forceScreenOnWakeLock.acquire(3000);
					}
				}
			}
		}).start();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (intent != null && startId == 1) {
			testModeRate = intent
					.getIntExtra("testModeRate", Integer.MIN_VALUE);

			updateInterval = testModeRate == Integer.MIN_VALUE ? intent
					.getIntExtra("interval", INTERVAL) : testModeRate;

			sensorDelay = intent.getIntExtra("sensorDelay",
					SensorManager.SENSOR_DELAY_NORMAL);
			alarmTriggerSensitivity = intent.getDoubleExtra("alarm",
					SettingsActivity.MAX_ALARM_SENSITIVITY);

			useAlarm = intent.getBooleanExtra("useAlarm", false);
			alarmWindow = intent.getIntExtra("alarmWindow", 0);
			airplaneMode = intent.getBooleanExtra("airplaneMode", false);

			forceScreenOn = intent.getBooleanExtra("forceScreenOn", false);

			obtainWakeLock();
			registerAccelerometerListener();
			toggleAirplaneMode(true);

			final SharedPreferences.Editor ed = getSharedPreferences(
					"serviceIsRunning", Context.MODE_PRIVATE).edit();
			ed.putBoolean("serviceIsRunning", true);
			ed.commit();
		}
		return startId;
	}

	private void registerAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				sensorDelay);
	}

	private void toggleAirplaneMode(final boolean enabling) {
		if (airplaneMode) {
			Settings.System.putInt(getContentResolver(),
					Settings.System.AIRPLANE_MODE_ON, enabling ? 1 : 0);
			final Intent intent = new Intent(
					Intent.ACTION_AIRPLANE_MODE_CHANGED);
			intent.putExtra("state", enabling);
			sendBroadcast(intent);
		}
	}

	private boolean triggerAlarmIfNecessary(final long currentTime,
			final double y) {
		if (useAlarm) {
			final Alarm alarm = Alarms.calculateNextAlert(this);// adb.getNearestEnabledAlarm();
			if (alarm != null) {
				final Calendar alarmTime = Calendar.getInstance();
				alarmTime.setTimeInMillis(alarm.time);
				alarmTime.add(Calendar.MINUTE, alarmWindow * -1);
				final long alarmMillis = alarmTime.getTimeInMillis();
				if (currentTime >= alarmMillis && y >= alarmTriggerSensitivity) {
					// alarm.time = currentTime;
					partialWakeLock.release();
					Alarms.enableAlert(this, alarm, currentTime);

					return true;
				}
			}
		}
		return false;
	}

	private void unregisterAccelerometerListener() {
		final SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorManager.unregisterListener(this);
	}
}
