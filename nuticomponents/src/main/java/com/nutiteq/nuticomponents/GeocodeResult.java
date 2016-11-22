package com.nutiteq.nuticomponents;

import com.carto.core.MapBounds;

public class GeocodeResult {

	public String line1;
	public String line2;
	public double lon;
	public double lat;
	public MapBounds boundingBox;

	public GeocodeResult(String line1, String line2, double lon, double lat, MapBounds boundingBox) {

		this.line1 = line1;
		this.line2 = line2;
		this.lon = lon;
		this.lat = lat;
		this.boundingBox = boundingBox;
	}
}
