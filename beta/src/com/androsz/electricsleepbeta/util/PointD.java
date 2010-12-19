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
}
