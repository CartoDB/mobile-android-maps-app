package com.nutiteq.nuticomponents.locationtracking;

import android.graphics.PointF;

public class TrackData {

	public long id;
	public String startTime; // in HH:MM:SS
	public String endTime; // in HH:MM:SS
	public String duration; // in HH:MM:SS
	public String distance; // in meters or miles based on settings
	public PointF[] locations; // WGS84
	public String startLocation;
	public String endLocation;
	public double minX;
	public double minY;
	public double maxX;
	public double maxY;
	public boolean isOnMap;
}
