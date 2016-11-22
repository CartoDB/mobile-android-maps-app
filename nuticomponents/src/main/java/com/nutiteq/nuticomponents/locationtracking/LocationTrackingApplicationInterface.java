package com.nutiteq.nuticomponents.locationtracking;

import android.location.Location;

public interface LocationTrackingApplicationInterface {

	public LocationTrackingDB getLocationTrackingDB();

	public boolean isMapViewLive();

	public void stopLocationTracking();

	public void addTrackOnMap(TrackData trackData,
			boolean isTrackingOnForThisTrack);

	public void setLocation(Location location);
}
