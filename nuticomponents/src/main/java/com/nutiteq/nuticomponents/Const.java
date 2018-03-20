package com.nutiteq.nuticomponents;

public class Const {

    public static final String LOG_TAG = "Nuticomponents";

    public static final String BASE_PACKAGE_ID = "basepkg";
    public static final String BASE_PACKAGE_ASSET_NAME = "world_0_5.mbtiles";

    public static final String NUTITEQ_SOURCE_ID = "carto.streets";

    public static final String NUTITEQ_ROUTING_SOURCE_ID = "routing:" + NUTITEQ_SOURCE_ID;

    public static final String NUTITEQ_GREEN = "#00b483";

    // NUTITEQ key for geocoding
    public static final String NUTITEQ_KEY = "9192c473df10007fa7ae027a87c72649";

    public static final int PACKAGELIST_CHECK_PERIOD = 24 * 60 * 60; // in seconds

    // value in MB which indicate unreachable storage, for example if 1030 MB is
    // free only 1000 is reachable, I use this because on my device when there
    // is below 30MB free external storage, package manager crash if it's
    // downloading package, I think 30 is good value for all devices
    // http://developer.android.com/training/basics/data-storage/files.html#GetFreeSpace
    public static final int EXTERNAL_STORAGE_MIN = 30;
    public static final int INTERNAL_STORAGE_MIN = 30;

    public static final float BASE_PACKAGE_ZOOM_LEVELS = 6;
    public static final int TILEMASK_ZOOM_LEVELS = 12;
}
