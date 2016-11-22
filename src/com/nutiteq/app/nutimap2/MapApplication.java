package com.nutiteq.app.nutimap2;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flurry.android.FlurryAgent;
import com.nutiteq.app.nutimap3d.dev.BuildConfig;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.datasources.LocalVectorDataSource;
import com.nutiteq.nuticomponents.customviews.LocationView;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingApplicationInterface;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingDB;
import com.nutiteq.nuticomponents.locationtracking.TrackData;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerApplicationInterface;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerComponent;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.ui.MapView;

public class MapApplication extends Application implements
		GPSTrackingApplicationInterface, PackageManagerApplicationInterface {

	private PackageManagerComponent packageManager;

	private GPSTrackingDB gpsTrackingDB;

	private MainActivity mainActivity;

	private int unit;

	static {
		System.loadLibrary("nutiteq_maps_sdk");
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();

		// configure Flurry
		FlurryAgent.setLogEnabled(false);
		if (BuildConfig.DEBUG) {
			FlurryAgent.setCaptureUncaughtExceptions(false);
		}
		// init Flurry
		FlurryAgent.init(this, "SHGHCJXK3ZXRNJ8B5JFZ");

		MapView.registerLicense(Const.LICENSE_KEY, getApplicationContext());

		// init default values only on first start
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// get default preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
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
		unit = GPSTrackingDB.METRIC;

		if (s.equals(Const.METRIC)) {
			unit = GPSTrackingDB.METRIC;
		} else if (s.equals(Const.IMPERIAL)) {
			unit = GPSTrackingDB.IMPERIAL;
		}
	}

	public void setReferences(MainActivity mainActivity,
			LocalVectorDataSource tracksDataSource, LocationView locationView) {
		this.mainActivity = mainActivity;

		gpsTrackingDB = new GPSTrackingDB(getApplicationContext(), unit,
				new EPSG3857(), this.mainActivity, tracksDataSource,
				locationView);

		if (!gpsTrackingDB.isOpen()) {
			gpsTrackingDB.open();
		}
	}

	@Override
	public GPSTrackingDB getGPSTrackingDB() {
		return gpsTrackingDB;
	}

	@Override
	public PackageManagerComponent getPackageManagerComponent() {
		return packageManager;
	}

	@Override
	public boolean isMapViewLive() {
		return MainActivity.isLive;
	}

	@Override
	public void stopGPSTracking() {
		mainActivity.stopGPSTracking();
	}

	@Override
	public void addTrackOnMap(TrackData trackData,
			boolean isTrackingOnForThisTrack) {
		mainActivity.addtrackOnMap(trackData, isTrackingOnForThisTrack);
	}
}
