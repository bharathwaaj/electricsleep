package com.androsz.electricsleepbeta.service;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.alarmclock.AlarmAlertFullScreen;
import com.androsz.electricsleepbeta.alarmclock.AlarmReceiver;
import com.androsz.electricsleepbeta.alarmclock.Alarms;
import com.androsz.electricsleepbeta.app.CalibrationWizardActivity;
import com.androsz.electricsleepbeta.app.SaveSleepActivity;
import com.androsz.electricsleepbeta.app.SettingsActivity;
import com.androsz.electricsleepbeta.app.SleepActivity;
import com.androsz.electricsleepbeta.alarmclock.Alarm;

public class SleepAccelerometerService extends Service implements
		SensorEventListener {
	public static final String POKE_SYNC_CHART = "com.androsz.electricsleepbeta.POKE_SYNC_CHART";
	public static final String STOP_AND_SAVE_SLEEP = "com.androsz.electricsleepbeta.STOP_AND_SAVE_SLEEP";
	public static final String SLEEP_STOPPED = "com.androsz.electricsleepbeta.SLEEP_STOPPED";

	private static final int NOTIFICATION_ID = 0x1337a;

	private ArrayList<Double> currentSeriesX = new ArrayList<Double>();
	private ArrayList<Double> currentSeriesY = new ArrayList<Double>();

	private SensorManager sensorManager;

	private PowerManager powerManager;
	private WakeLock partialWakeLock;

	private long lastChartUpdateTime = System.currentTimeMillis();
	private long lastOnSensorChangedTime = System.currentTimeMillis();
	
	private double minSensitivity = SettingsActivity.DEFAULT_MIN_SENSITIVITY;
	private double alarmTriggerSensitivity = SettingsActivity.DEFAULT_ALARM_SENSITIVITY;

	private boolean airplaneMode = true;
	private boolean useAlarm = false;
	private int alarmWindow = 30;

	private int updateInterval = CalibrationWizardActivity.MINIMUM_CALIBRATION_TIME;

	private Date dateStarted;

	public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;

	private final BroadcastReceiver pokeSyncChartReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			// if (currentSeriesX.size() > 0 && currentSeriesY.size() > 0) {
			final Intent i = new Intent(SleepActivity.SYNC_CHART);
			i.putExtra("currentSeriesX", currentSeriesX);
			i.putExtra("currentSeriesY", currentSeriesY);
			i.putExtra("min", minSensitivity);
			i.putExtra("alarm", alarmTriggerSensitivity);
			sendBroadcast(i);
			// }
		}
	};

	private final BroadcastReceiver stopAndSaveSleepReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final Intent saveIntent = addExtrasToSaveSleepIntent(new Intent(
					SleepAccelerometerService.this, SaveSleepActivity.class));
			startActivity(saveIntent);
			stopSelf();
		}
	};

	private final BroadcastReceiver alarmDoneReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			createSaveSleepNotification();
			stopSelf();
		}
	};

	private Intent addExtrasToSaveSleepIntent(final Intent saveIntent) {
		saveIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		saveIntent.putExtra("currentSeriesX", currentSeriesX);
		saveIntent.putExtra("currentSeriesY", currentSeriesY);
		saveIntent.putExtra("min", minSensitivity);
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
	}

	private Notification createServiceNotification() {
		// notificationManager = (NotificationManager)
		// getSystemService(Context.NOTIFICATION_SERVICE);

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
		// notificationManager.notify(NOTIFICATION_ID, notification);
	}

	private void obtainWakeLock() {
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		partialWakeLock = powerManager.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, toString());
		partialWakeLock.acquire();
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

		registerReceiver(pokeSyncChartReceiver, new IntentFilter(
				POKE_SYNC_CHART));

		registerReceiver(stopAndSaveSleepReceiver, new IntentFilter(
				STOP_AND_SAVE_SLEEP));

		registerReceiver(alarmDoneReceiver, new IntentFilter(
				Alarms.ALARM_DONE_ACTION));
		registerAccelerometerListener();

		obtainWakeLock();

		dateStarted = new Date();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(pokeSyncChartReceiver);
		unregisterReceiver(stopAndSaveSleepReceiver);
		unregisterReceiver(alarmDoneReceiver);

		sensorManager.unregisterListener(SleepAccelerometerService.this);

		partialWakeLock.release();

		// tell monitoring activities that sleep has ended
		sendBroadcast(new Intent(SLEEP_STOPPED));

		currentSeriesX = new ArrayList<Double>();
		currentSeriesY = new ArrayList<Double>();

		toggleAirplaneMode(false);

		stopForeground(true);
	}

	double maxNetForce = 0;
	double lastX = 0;
	double lastY = 0;
	double lastZ = 0;

	@Override
	public void onSensorChanged(SensorEvent event) {
		final long currentTime = System.currentTimeMillis();

		double curX = event.values[0];
		double curY = event.values[1];
		double curZ = event.values[2];

		if (lastOnSensorChangedTime == 0) {
			lastOnSensorChangedTime = currentTime;
			lastX = curX;
			lastY = curY;
			lastZ = curZ;
			return;
		}

		final long timeSinceLastSensorChange = currentTime
				- lastOnSensorChangedTime;
		if (timeSinceLastSensorChange > 0) {
			double force = Math.abs(curX + curY + curZ - lastX - lastY - lastZ)
					/ timeSinceLastSensorChange;
			
			maxNetForce = (force > maxNetForce) ? force : maxNetForce;
			lastX = curX;
			lastY = curY;
			lastZ = curZ;
			lastOnSensorChangedTime = currentTime;
		}

		final long deltaTime = currentTime - lastChartUpdateTime;
		
		final double x = currentTime;
		final double y = java.lang.Math.max(minSensitivity,
				java.lang.Math.min(alarmTriggerSensitivity, maxNetForce));

		if (deltaTime >= updateInterval) {

			currentSeriesX.add(x);
			currentSeriesY.add(y);

			final Intent i = new Intent(SleepActivity.UPDATE_CHART);
			i.putExtra("x", x);
			i.putExtra("y", y);
			i.putExtra("min", minSensitivity);
			i.putExtra("alarm", alarmTriggerSensitivity);
			sendBroadcast(i);

			//totalTimeBetweenSensorChanges = 0;

			lastChartUpdateTime = currentTime;
			maxNetForce = 0;

			triggerAlarmIfNecessary(currentTime, y);
		} else if (triggerAlarmIfNecessary(currentTime, y)) {
			currentSeriesX.add(x);
			currentSeriesY.add(y);

			//totalTimeBetweenSensorChanges = 0;

			lastChartUpdateTime = currentTime;
			maxNetForce = 0;
		}
		lastOnSensorChangedTime = currentTime;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (intent != null && startId == 1) {
			updateInterval = intent.getIntExtra("interval",
					CalibrationWizardActivity.MINIMUM_CALIBRATION_TIME);
			minSensitivity = intent.getDoubleExtra("min",
					SettingsActivity.DEFAULT_MIN_SENSITIVITY);
			alarmTriggerSensitivity = intent.getDoubleExtra("alarm",
					SettingsActivity.DEFAULT_ALARM_SENSITIVITY);

			useAlarm = intent.getBooleanExtra("useAlarm", false);
			alarmWindow = intent.getIntExtra("alarmWindow", 0);
			airplaneMode = intent.getBooleanExtra("airplaneMode", false);
			toggleAirplaneMode(true);
		}
		return startId;
	}

	private void registerAccelerometerListener() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SENSOR_DELAY);
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
			// final AlarmDatabase adb = new
			// AlarmDatabase(getContentResolver());
			final Alarm alarm = Alarms.calculateNextAlert(this, -1);// adb.getNearestEnabledAlarm();
			if (alarm != null) {
				final Calendar alarmTime = Calendar.getInstance();
				alarmTime.setTimeInMillis(alarm.time);
				alarmTime.add(Calendar.MINUTE, alarmWindow * -1);
				final long alarmMillis = alarmTime.getTimeInMillis();
				if (currentTime >= alarmMillis && y >= alarmTriggerSensitivity) {
					alarm.time = currentTime;

					Alarms.enableAlert(this, alarm, currentTime);

					return true;
				}
			}
		}
		return false;
	}
}
