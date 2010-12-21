package com.androsz.electricsleepbeta.util;

import java.io.Serializable;

public class PointD implements Serializable {
	private static final long serialVersionUID = -7526147553632397385L;

	public PointD(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double x;
	public double y;

	public static PointD fromByteArray(byte[] data) {
		byte[] temp = new byte[8];
		double x;
		double y;
		System.arraycopy(data, 0, temp, 0, 8);
		x = toDouble(temp);
		System.arraycopy(data, 8, temp, 0, 8);
		y = toDouble(temp);
		return new PointD(x, y);
	}

	private static double toDouble(byte[] data) {
		if (data == null || data.length != 8)
			return 0x0;
		// ---------- simple:
		return Double.longBitsToDouble(toLong(data));
	}

	private static long toLong(byte[] data) {
		if (data == null || data.length != 8)
			return 0x0;
		// ----------
		return (long) (
		// (Below) convert to longs before shift because digits
		// are lost with ints beyond the 32-bit limit
		(long) (0xff & data[0]) << 56 | (long) (0xff & data[1]) << 48
				| (long) (0xff & data[2]) << 40 | (long) (0xff & data[3]) << 32
				| (long) (0xff & data[4]) << 24 | (long) (0xff & data[5]) << 16
				| (long) (0xff & data[6]) << 8 | (long) (0xff & data[7]) << 0);
	}

	public static byte[] toByteArray(PointD point) {
		byte[] bytes = new byte[16];
		System.arraycopy(toByta(point.x), 0, bytes, 0, 8);
		System.arraycopy(toByta(point.y), 0, bytes, 8, 8);
		return bytes;
	}

	private static byte[] toByta(long data) {
		return new byte[] { (byte) ((data >> 56) & 0xff),
				(byte) ((data >> 48) & 0xff), (byte) ((data >> 40) & 0xff),
				(byte) ((data >> 32) & 0xff), (byte) ((data >> 24) & 0xff),
				(byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
				(byte) ((data >> 0) & 0xff), };
	}

	private static byte[] toByta(double data) {
		return toByta(Double.doubleToRawLongBits(data));
	}
}
