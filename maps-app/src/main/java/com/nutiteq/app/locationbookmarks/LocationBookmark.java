package com.nutiteq.app.locationbookmarks;

public class LocationBookmark {

	private long id;
	private double lon;
	private double lat;
	private String description;
	private int red;
	private int green;
	private int blue;
	protected String firebaseNodeKey;

	public LocationBookmark(long id, double lon, double lat, String description,
			int red, int green, int blue, String firebaseNodeKey) {
		this.id = id;
		this.lon = lon;
		this.lat = lat;
		this.description = description;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.firebaseNodeKey = firebaseNodeKey;
	}

	public long getId() {
		return id;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public String getDescription() {
		return description;
	}

	public int getRed() {
		return red;
	}

	public int getGreen() {
		return green;
	}

	public int getBlue() {
		return blue;
	}

	public String firebaseNodeKey() {
		return firebaseNodeKey;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setRed(int red) {
		this.red = red;
	}

	public void setGreen(int green) {
		this.green = green;
	}

	public void setBlue(int blue) {
		this.blue = blue;
	}
}
