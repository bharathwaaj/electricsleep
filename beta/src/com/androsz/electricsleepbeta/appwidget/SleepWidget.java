package com.androsz.electricsleepbeta.appwidget;

import com.androsz.electricsleepbeta.R;
import com.androsz.electricsleepbeta.receiver.StartSleepReceiver;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class SleepWidget extends AppWidgetProvider {
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final RemoteViews updateViews = new RemoteViews(
				context.getPackageName(), R.layout.appwidget_sleep);

		final PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
				0, new Intent(StartSleepReceiver.START_SLEEP), 0);

		updateViews.setOnClickPendingIntent(R.id.appwidget_btn_sleep,
				pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetIds, updateViews);
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}