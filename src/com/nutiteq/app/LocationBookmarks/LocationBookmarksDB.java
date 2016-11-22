package com.nutiteq.app.locationbookmarks;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocationBookmarksDB {

	private LocationBookmarksDBHelper dbHelper;
	private SQLiteDatabase database;

	private boolean isOpen = false;

	public LocationBookmarksDB(Context context) {
		dbHelper = new LocationBookmarksDBHelper(context);
	}

	/**
	 * Open DB.
	 * 
	 */
	public void open() {
		try {
			database = dbHelper.getWritableDatabase();
			isOpen = true;
		} catch (SQLException e) {
			isOpen = false;
		}
	}

	/**
	 * Close DB.
	 * 
	 */
	public void close() {
		dbHelper.close();
		isOpen = false;
	}

	/**
	 * Returns if DB is open.
	 * 
	 */
	public boolean isOpen() {
		return isOpen;
	}

	/**
	 * Insert location bookmark into DB and return ID for this location bookmark
	 * or -1.
	 * 
	 */
	public long insertLocationBookmark(double lon, double lat, String location,
			int red, int green, int blue) {
		if (isOpen) {
			Cursor c = database.rawQuery("select "
					+ LocationBookmarksDBHelper.COLUMN_NAME_ID + " from "
					+ LocationBookmarksDBHelper.TRACKS_TABLE_NAME, null);

			int id;

			if (c.moveToLast()) {
				id = c.getInt(0);
			} else {
				id = 0;
			}

			c.close();

			ContentValues values = new ContentValues();

			values.put(LocationBookmarksDBHelper.COLUMN_NAME_ID, id + 1);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_LONGITUDE, lon);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_LATITUDE, lat);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_LOCATION, location);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_RED, red);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_GREEN,
					green);
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_BLUE,
					blue);

			long l = database.insert(
					LocationBookmarksDBHelper.TRACKS_TABLE_NAME, null, values);

			return l;
		}

		return -1;
	}

	public ArrayList<LocationBookmark> getAllLocationBookmarks() {
		ArrayList<LocationBookmark> locationBookmarks = new ArrayList<LocationBookmark>();

		if (isOpen) {
			Cursor c = database.rawQuery("select * from "
					+ LocationBookmarksDBHelper.TRACKS_TABLE_NAME, null);

			c.moveToFirst();

			long id;
			double x;
			double y;
			String location;
			int r;
			int g;
			int b;

			while (!c.isAfterLast()) {
				id = c.getInt(0);
				x = c.getDouble(1);
				y = c.getDouble(2);
				location = c.getString(3);
				r = c.getInt(4);
				g = c.getInt(5);
				b = c.getInt(6);

				locationBookmarks.add(new LocationBookmark(id, x, y, location,
						r, g, b));

				c.moveToNext();
			}
			c.close();

			return locationBookmarks;
		}

		return locationBookmarks;
	}

	/**
	 * Delete location bookmark from DB.
	 * 
	 */
	public boolean deleteLocationBookmark(long id) {
		if (isOpen) {
			int l = database.delete(
					LocationBookmarksDBHelper.TRACKS_TABLE_NAME,
					LocationBookmarksDBHelper.COLUMN_NAME_ID + "=" + id, null);

			return l > 0;
		} else {
			return false;
		}
	}

	/**
	 * Update location bookmark in DB.
	 * 
	 */
	public boolean updateLocationBookmark(long id, String newLocationName) {
		if (isOpen) {
			ContentValues values = new ContentValues();
			values.put(LocationBookmarksDBHelper.COLUMN_NAME_LOCATION,
					newLocationName);

			int l = database
					.update(LocationBookmarksDBHelper.TRACKS_TABLE_NAME,
							values, LocationBookmarksDBHelper.COLUMN_NAME_ID
									+ " = " + id, null);

			return l > 0;
		} else {
			return false;
		}
	}

	private class LocationBookmarksDBHelper extends SQLiteOpenHelper {

		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "LocationBookmarks.db";

		private static final String TRACKS_TABLE_NAME = "locations";

		private static final String COLUMN_NAME_ID = "_ID";
		private static final String COLUMN_NAME_LONGITUDE = "lon";
		private static final String COLUMN_NAME_LATITUDE = "lat";
		private static final String COLUMN_NAME_LOCATION = "location";
		private static final String COLUMN_NAME_PIN_COLOR_RED = "red";
		private static final String COLUMN_NAME_PIN_COLOR_GREEN = "green";
		private static final String COLUMN_NAME_PIN_COLOR_BLUE = "blue";

		private static final String SQL_CREATE_TRACKS = "CREATE TABLE "
				+ TRACKS_TABLE_NAME + " (" + COLUMN_NAME_ID
				+ " INTEGER PRIMARY KEY, " + COLUMN_NAME_LONGITUDE + " REAL, "
				+ COLUMN_NAME_LATITUDE + " REAL, " + COLUMN_NAME_LOCATION
				+ " TEXT, " + COLUMN_NAME_PIN_COLOR_RED + " INTEGER, "
				+ COLUMN_NAME_PIN_COLOR_GREEN + " INTEGER, "
				+ COLUMN_NAME_PIN_COLOR_BLUE + " INTEGER" + ")";

		public LocationBookmarksDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_TRACKS);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// not upgraded yet
		}

		public void onDowngrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}
	}
}
