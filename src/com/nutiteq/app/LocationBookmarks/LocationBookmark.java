package com.nutiteq.app.locationbookmarks;

public class LocationBookmark {

	public long id;
	public double lon;
	public double lat;
	public String location;
	public int red;
	public int green;
	public int blue;

	public LocationBookmark(long id, double lon, double lat, String location,
			int red, int green, int blue) {
		super();

		this.id = id;
		this.lon = lon;
		this.lat = lat;
		this.location = location;
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
}
