package com.androsz.electricsleepbeta.util;

import java.util.ArrayList;

import android.content.Context;
import android.os.PowerManager;

/**
 * Hold a wakelock that the whole process must use. Otherwise, interferences
 * happen.
 */
public class SharedWakeLock {

	private static PowerManager.WakeLock sCpuWakeLock;
	private static ArrayList<Integer> wakeLockFlags = new ArrayList<Integer>();

	public static void acquire(final Context context, int wakeLockFlags) {
		createWakeLock(context, wakeLockFlags);
		sCpuWakeLock.acquire();
	}

	public static void acquire(final Context context, int wakeLockFlags,
			int releaseAfter) {
		createWakeLock(context, wakeLockFlags);
		sCpuWakeLock.acquire(releaseAfter);
	}

	private static void createWakeLock(final Context context, int wakeLockFlags) {
		final PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		release();
		sCpuWakeLock = pm.newWakeLock(wakeLockFlags,
				SharedWakeLock.class.getSimpleName());
		sCpuWakeLock.setReferenceCounted(false);
	}

	public static void release() {
		if (isHeld()) {
			sCpuWakeLock.release();
		}
	}
	
	public static boolean isHeld() {
		return sCpuWakeLock != null && sCpuWakeLock.isHeld();
	}
}
