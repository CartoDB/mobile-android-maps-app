package com.nutiteq.app.locationbookmarks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class LocationBookmarksDB {

    private LocationBookmarksDBHelper dbHelper;
    private SQLiteDatabase database;

    private boolean isOpen = false;

    public LocationBookmarksDB(Context context) {
        dbHelper = new LocationBookmarksDBHelper(context);
    }

    /**
     * Open DB.
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
     */
    public void close() {
        dbHelper.close();
        isOpen = false;
    }

    /**
     * Returns if DB is open.
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Insert location bookmark into DB and return ID for this location bookmark
     * or -1.
     */
    public long insertLocationBookmark(LocationBookmark locationBookmark) {
        return insertLocationBookmark(
                locationBookmark.getLon(),
                locationBookmark.getLat(),
                locationBookmark.getDescription(),
                locationBookmark.getRed(),
                locationBookmark.getGreen(),
                locationBookmark.getBlue(),
                locationBookmark.firebaseNodeKey
        );
    }

    /**
     * Insert location bookmark into DB and return ID for this location bookmark
     * or -1.
     */
    public long insertLocationBookmark(double lon, double lat, String location, int red, int green, int blue, String firebaseNodeKey) {

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

            values.put(LocationBookmarksDBHelper.COLUMN_NAME_DESCRIPTION, location);

            values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_RED, red);
            values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_GREEN, green);
            values.put(LocationBookmarksDBHelper.COLUMN_NAME_PIN_COLOR_BLUE, blue);

            values.put(LocationBookmarksDBHelper.COLUMN_NAME_FIREBASE_NODE_KEY, firebaseNodeKey);

            long l = database.insert(LocationBookmarksDBHelper.TRACKS_TABLE_NAME, null, values);

            if (l == -1) {
                return l;
            } else {
                return id + 1;
            }
        }

        return -1;
    }

    public String getFirebaseNodeKey(long id) {
        String result = "";

        if (isOpen) {
            Cursor c = database.rawQuery("select " + LocationBookmarksDBHelper.COLUMN_NAME_FIREBASE_NODE_KEY + " from "
                    + LocationBookmarksDBHelper.TRACKS_TABLE_NAME + " where " + LocationBookmarksDBHelper.COLUMN_NAME_ID + " = " + id, null);

            c.moveToFirst();

            int l = c.getCount();

            if (l > 0) {
                result = c.getString(0);
            }

            c.close();
        }

        return result;
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
            String f;

            while (!c.isAfterLast()) {
                id = c.getInt(0);
                x = c.getDouble(1);
                y = c.getDouble(2);
                location = c.getString(3);
                r = c.getInt(4);
                g = c.getInt(5);
                b = c.getInt(6);
                f = c.getString(7);

                locationBookmarks.add(new LocationBookmark(id, x, y, location,
                        r, g, b, f));

                c.moveToNext();
            }
            c.close();

            return locationBookmarks;
        }

        return locationBookmarks;
    }

    /**
     * return location bookmark by id
     */
    public LocationBookmark getLocationBookmark(long favId) {
        if (isOpen) {
            Cursor c = database.rawQuery("select * from "
                    + LocationBookmarksDBHelper.TRACKS_TABLE_NAME + " where " + LocationBookmarksDBHelper.COLUMN_NAME_ID + " = " + favId, null);

            if (c.moveToFirst()) {
                long id;
                double x;
                double y;
                String location;
                int r;
                int g;
                int b;
                String f;

                id = c.getInt(0);
                x = c.getDouble(1);
                y = c.getDouble(2);
                location = c.getString(3);
                r = c.getInt(4);
                g = c.getInt(5);
                b = c.getInt(6);
                f = c.getString(7);

                c.close();

                return new LocationBookmark(id, x, y, location,
                        r, g, b, f);
            }
        }

        return null;
    }

    /**
     * Delete location bookmark from DB.
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
     * Delete all location bookmark from DB.
     */
    public boolean deleteAllLocationBookmark() {
        if (isOpen) {
            int l = database.delete(
                    LocationBookmarksDBHelper.TRACKS_TABLE_NAME,
                    "", null);

            return l > 0;
        } else {
            return false;
        }
    }

    /**
     * Update location bookmark in DB.
     */
    public boolean updateLocationBookmarkDescription(long id, String description) {
        if (isOpen) {
            ContentValues values = new ContentValues();
            values.put(LocationBookmarksDBHelper.COLUMN_NAME_DESCRIPTION,
                    description);

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

        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "LocationBookmarks.db";

        private static final String TRACKS_TABLE_NAME = "locations";

        private static final String COLUMN_NAME_ID = "_ID";
        private static final String COLUMN_NAME_LONGITUDE = "lon";
        private static final String COLUMN_NAME_LATITUDE = "lat";
        private static final String COLUMN_NAME_DESCRIPTION = "location";
        private static final String COLUMN_NAME_PIN_COLOR_RED = "red";
        private static final String COLUMN_NAME_PIN_COLOR_GREEN = "green";
        private static final String COLUMN_NAME_PIN_COLOR_BLUE = "blue";
        private static final String COLUMN_NAME_FIREBASE_NODE_KEY = "firebase";

        private static final String SQL_CREATE_TRACKS = "CREATE TABLE "
                + TRACKS_TABLE_NAME + " (" + COLUMN_NAME_ID
                + " INTEGER PRIMARY KEY, " + COLUMN_NAME_LONGITUDE + " REAL, "
                + COLUMN_NAME_LATITUDE + " REAL, " + COLUMN_NAME_DESCRIPTION
                + " TEXT, " + COLUMN_NAME_PIN_COLOR_RED + " INTEGER, "
                + COLUMN_NAME_PIN_COLOR_GREEN + " INTEGER, "
                + COLUMN_NAME_PIN_COLOR_BLUE + " INTEGER, " + COLUMN_NAME_FIREBASE_NODE_KEY + " INTEGER" + ")";

        private static final String SQL_DELETE_LOCATIONS = "DROP TABLE IF EXISTS " + TRACKS_TABLE_NAME;

        public LocationBookmarksDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TRACKS);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL(SQL_DELETE_LOCATIONS);

                onCreate(db);
            } else {
                // TODO with next DB changes
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
