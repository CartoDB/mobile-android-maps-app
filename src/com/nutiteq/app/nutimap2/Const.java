package com.nutiteq.app.nutimap2;

import com.nutiteq.app.nutimap3d.dev.BuildConfig;

public class Const {

	public static final String LOG_TAG = "OfflineMaps3D";

	// offline vector map data source from assets
	public static final String BASE_PACKAGE_ASSET_NAME = "world_0_5.mbtiles";
	public static final String BASE_PACKAGE_ID = "basepkg";
	public static final int BASE_PACKAGE_ZOOM_LEVELS = 6;
	public static final int TILEMASK_ZOOM_LEVELS = 12;

	// Tile cache size. Larger number will give better performance but higher
	// memory usage
	public static final int TILE_CACHE_SIZE = 32 * 1024 * 1024;

	// names of map styles
	public static final String MAP_STYLE = "nutibright";

	// names of map language
	public static final String MAP_LANGUAGE_AUTOMATIC = "automatic";
	public static final String MAP_LANGUAGE_ENGLISH = "en";
	public static final String MAP_LANGUAGE_GERMAN = "de";
	public static final String MAP_LANGUAGE_FRENCH = "fr";
	public static final String MAP_LANGUAGE_RUSSIAN = "ru";
	public static final String MAP_LANGUAGE_CHINESE = "zh";
	public static final String MAP_LANGUAGE_SPANISH = "es";
	public static final String MAP_LANGUAGE_ITALIAN = "it";
	public static final String MAP_LANGUAGE_ESTONIAN = "et";
	public static final String MAP_LANGUAGE_LOCAL = "";

	// ID for HOCKEY SDK
	public static final String HOCKEYAPP_ID = BuildConfig.DEBUG ? "2a90c57f65f6f8821e857247416f5475"
			: "94d69b5ee0f22c85af784ba041109921";

	// value in MB which indicate unreachable storage, for example if 1030 MB is
	// free only 1000 is reachable, I use this because on my device when there
	// is below 30MB free external storage, package manager crash if it's
	// downloading package, I think 30 is good value for all devices
	// http://developer.android.com/training/basics/data-storage/files.html#GetFreeSpace
	public static final int EXTERNAL_STORAGE_MIN = 30;
	public static final int INTERNAL_STORAGE_MIN = 30;

	public static final String LICENSE_KEY = "XTUN3Q0ZBYlg2d2twYU5FQ3dIdzZGNW9McGVqdWl3SEhBaFFtRWlEUWNtNHFPTXlFV2ttMTVVZ085aTgwNkE9PQoKcHJvZHVjdHM9c2RrLWFuZHJvaWQtMy4qLHNkay1naXNleHRlbnNpb24KcGFja2FnZU5hbWU9Y29tLm51dGl0ZXEuYXBwLm51dGltYXAzZC4qCndhdGVybWFyaz1jdXN0b20KdXNlcktleT0yZjFmM2JlYjQ1NmU4YTZhNWE5Nzk4NWE4NTgyY2U0ZAo=";

	// from colors.xml it doesn't works for action bar background
	public static final String NUTITEQ_GREEN = "#00b483";

	// values for preferences, please don't change it values
	public static final String INTERNAL = "Internal";
	public static final String EXTERNAL = "External";
	public static final String METRIC = "Metric";
	public static final String IMPERIAL = "Imperial";
	public static final String LANG_AUTOMATIC = "Automatic";
	public static final String LANG_LOCAL = "Local";
	public static final String LANG_ENGLISH = "English";
	public static final String LANG_GERMAN = "German";
	public static final String LANG_FRENCH = "French";
	public static final String LANG_RUSSIAN = "Russian";
	public static final String LANG_ITALIAN = "Italian";
	public static final String LANG_SPANISH = "Spanish";
	public static final String LANG_CHINESE = "Chinese";
	public static final String LANG_ESTONIAN = "Estonian";

	public static final String PREF_UNIT_KEY = "pref_unit_key";
	public static final String PREF_STORAGE_KEY = "pref_storage_key";
	public static final String PREF_LANG_KEY = "pref_lang_key";
}
