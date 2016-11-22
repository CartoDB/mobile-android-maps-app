package com.nutiteq.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.carto.datasources.LocalVectorDataSource;
import com.carto.projections.EPSG3857;
import com.carto.ui.MapView;
import com.firebase.client.Firebase;
import com.flurry.android.FlurryAgent;
import com.nutiteq.app.utils.Const;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.nutimap3d.dev.BuildConfig;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.app.utils.Utils;
import com.nutiteq.nuticomponents.locationtracking.LocationTrackingApplicationInterface;
import com.nutiteq.nuticomponents.locationtracking.LocationTrackingDB;
import com.nutiteq.nuticomponents.locationtracking.TrackData;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerApplicationInterface;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerComponent;

public class MapApplication extends Application implements LocationTrackingApplicationInterface, PackageManagerApplicationInterface {

	private PackageManagerComponent packageManager;

	private LocationTrackingDB locationTrackingDB;

	private MainActivity mainActivity;

	private int unit;

	static {
		System.loadLibrary("carto_mobile_sdk");
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();

		com.carto.utils.Log.setShowInfo(true);
		com.carto.utils.Log.setShowDebug(true);

		Firebase.setAndroidContext(this);

		// configure Flurry
		FlurryAgent.setLogEnabled(false);
		if (BuildConfig.DEBUG) {
			FlurryAgent.setCaptureUncaughtExceptions(false);
		}
		// init Flurry
		FlurryAgent.init(this, "SHGHCJXK3ZXRNJ8B5JFZ");

		MapView.registerLicense(Const.LICENSE_KEY, getApplicationContext());

		// init default values only on first start
		PreferenceManager.setDefaultValues(this, R.xml.preferences9, false);

		// get default preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String s = prefs.getString(Const.PREF_STORAGE_KEY, Const.EXTERNAL);

		int storageType = PackageManagerComponent.INTERNAL_STORAGE;
		int multiStorageNumber = 1;

		if (s.equals(Const.INTERNAL)) {
			storageType = PackageManagerComponent.INTERNAL_STORAGE;
			multiStorageNumber = 1;
		} else if (s.equals(Const.EXTERNAL)) {
			storageType = PackageManagerComponent.EXTERNAL_STORAGE;
			multiStorageNumber = 1;
		} else {
			storageType = PackageManagerComponent.EXTERNAL_STORAGE;
			multiStorageNumber = Integer
					.parseInt(s.substring(s.indexOf(" ") + 1));
		}

		packageManager = new PackageManagerComponent(getApplicationContext(),
				storageType, multiStorageNumber);

		s = prefs.getString(Const.PREF_UNIT_KEY, Const.METRIC);
		unit = LocationTrackingDB.METRIC;

		if (s.equals(Const.METRIC)) {
			unit = LocationTrackingDB.METRIC;
		} else if (s.equals(Const.IMPERIAL)) {
			unit = LocationTrackingDB.IMPERIAL;
		}
	}

	public void setReferences(MainActivity mainActivity, LocalVectorDataSource tracksDataSource) {
		this.mainActivity = mainActivity;

		locationTrackingDB = new LocationTrackingDB(getApplicationContext(), unit,
				new EPSG3857(), this.mainActivity, tracksDataSource);

		if (!locationTrackingDB.isOpen()) {
			locationTrackingDB.open();
		}
	}

	@Override
	public LocationTrackingDB getLocationTrackingDB() {
		return locationTrackingDB;
	}

	@Override
	public PackageManagerComponent getPackageManagerComponent() {
		return packageManager;
	}

	@Override
	public Class getMainActivityClass() {
		return MainActivity.class;
	}

	@Override
	public boolean isMapViewLive() {
		return MainActivity.isLive;
	}

	@Override
	public void stopLocationTracking() {
		mainActivity.stopLocationTracking();
	}

	@Override
	public void addTrackOnMap(TrackData trackData,
			boolean isTrackingOnForThisTrack) {
		mainActivity.addtrackOnMap(trackData, isTrackingOnForThisTrack);
	}

	@Override
	public void setLocation(Location location) {
		mainActivity.setLocation(location);
	}
}
