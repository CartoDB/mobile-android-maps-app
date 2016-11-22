package com.nutiteq.nuticomponents.locationtracking;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.PointF;
import android.location.Location;

import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.graphics.Color;
import com.carto.projections.Projection;
import com.carto.styles.LineJoinType;
import com.carto.styles.LineStyleBuilder;
import com.carto.vectorelements.Line;
import com.nutiteq.nuticomponents.NominatimService;
import com.nutiteq.nuticomponents.R;
import com.nutiteq.nuticomponents.Utility;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class LocationTrackingDB {

    private LocationTrackDBHelper dbHelper;
    private SQLiteDatabase database;

    private Context context;

    private Projection projection;

    private boolean isOpen = false;

    private LocalVectorDataSource tracksVectorDataSource;
    private LineStyleBuilder lineStyleBuilder;
    private MapPosVector linePoses;
    private Line line;

    private Activity mapActivity;

    private boolean isFirstLocation = true;
    private double lastX = 0;
    private double lastY = 0;

    // to know track ID that is right now recording or -1
    private long curentTrackID = -1;

    private int measurementUnit;

    public static final int METRIC = 0;
    public static final int IMPERIAL = 1;

    public LocationTrackingDB(Context context, int measurementUnit,
                         Projection projection, Activity mapActivity,
                         LocalVectorDataSource tracksVectorDataSource) {
        dbHelper = new LocationTrackDBHelper(context);

        this.measurementUnit = measurementUnit;
        this.projection = projection;
        this.context = context;
        this.mapActivity = mapActivity;

        this.tracksVectorDataSource = tracksVectorDataSource;

        lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new Color(0xFF00b483));// nutiteq green :)
        lineStyleBuilder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_ROUND);
        lineStyleBuilder.setStretchFactor(3);
        lineStyleBuilder.setWidth(7);

        // if tracking is on don't destroy line points
        if (!LocationService.isLocationTrackingOn) {
            linePoses = new MapPosVector();
        }

        // if line is not null than user exit app with tracking on, so I just
        // add line and user see it when start app again
        if (line != null) {
            line.setPoses(linePoses);
            this.tracksVectorDataSource.add(line);
        }
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
     * Returns measurement unit. Can be GPSTrackingDB.METRIC or
     * GPSTrackingDB.IMPERIAL.
     */
    public int getMeasurementUnit() {
        return measurementUnit;
    }

    /**
     * Set measurement unit with one of those values GPSTrackingDB.METRIC or
     * GPSTrackingDB.IMPERIAL.
     */
    public void setMeasurementUnit(int measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    /**
     * Returns if GPS tracking is turned on.
     */
    public boolean isGPSTrackingOn() {
        return LocationService.isLocationTrackingOn;
    }

    /**
     * Returns MapPosVector which contains line poses of current GPS tracking.
     */
    public MapPosVector getCurrentLinePoses() {
        return linePoses;
    }

    /**
     * Returns MapBounds for all GPS tracks in DB or null if no tracks.
     */
    public MapBounds getAllTracksMapBounds() {
        if (isOpen) {
            Cursor c = database.rawQuery("select * from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME, null);

            double xMin = Double.MAX_VALUE;
            double yMin = Double.MIN_VALUE;
            double xMax = Double.MIN_VALUE;
            double yMax = Double.MAX_VALUE;

            double x1;
            double y1;
            double x2;
            double y2;

            c.moveToFirst();

            boolean istracksInDB = false;

            // visit each tracks and there map bounds
            while (!c.isAfterLast()) {
                istracksInDB = true;

                x1 = c.getDouble(3);
                y1 = c.getDouble(4);
                x2 = c.getDouble(5);
                y2 = c.getDouble(6);

                if (x1 < xMin) {
                    xMin = x1;
                }
                if (y1 > yMin) {
                    yMin = y1;
                }
                if (x2 > xMax) {
                    xMax = x2;
                }
                if (y2 < yMax) {
                    yMax = y2;
                }

                c.moveToNext();
            }

            c.close();

            if (istracksInDB) {
                // map bounds isn't set they are default 0
                if (xMin == Double.MAX_VALUE && yMin == Double.MIN_VALUE
                        && xMax == Double.MIN_VALUE && yMax == Double.MAX_VALUE) {
                    return null;
                } else {
                    return new MapBounds(new MapPos(xMin, yMin), new MapPos(
                            xMax, yMax));
                }
            } else {
                return null;
            }
        }

        return null;
    }

    /**
     * Returns MapBounds for one GPS track.
     */
    public MapBounds getTrackMapBounds(long id) {
        if (isOpen) {
            Cursor c = database.rawQuery("select * from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME + " where "
                    + LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);

            c.moveToFirst();

            if (c.getCount() == 1) {
                double x1 = c.getDouble(3);
                double y1 = c.getDouble(4);
                double x2 = c.getDouble(5);
                double y2 = c.getDouble(6);

                c.close();

                if (x1 == Double.MAX_VALUE && y1 == Double.MIN_VALUE
                        && x2 == Double.MIN_VALUE && y2 == Double.MAX_VALUE) {
                    return null;
                } else {
                    return new MapBounds(new MapPos(x1, y1), new MapPos(x2, y2));
                }
            } else {
                c.close();

                return null;
            }
        }

        return null;
    }

    /**
     * Add line on LocalVectorDataSource
     */
    public void addLine(MapPosVector linePoses) {
        this.linePoses = linePoses;
        line = new Line(this.linePoses, lineStyleBuilder.buildStyle());
        tracksVectorDataSource.add(line);
    }

    /**
     * Stop GPS tracking session.
     */
    public void stop(final long id) {
        // just for 100% sure, because it's called from service
        if (((LocationTrackingApplicationInterface) mapActivity.getApplication())
                .isMapViewLive()) {
            ((LocationTrackingApplicationInterface) mapActivity.getApplication())
                    .stopLocationTracking();
        }

        if (id != -1) {
            if (lastX != 0 && lastY != 0) {
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        NominatimService nominatium = new NominatimService();

                        try {
                            String location = nominatium.reverseGeocode(lastX,
                                    lastY);

                            setEndLocation(id, location);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.run();

                if (linePoses.size() > 1) {
                    updateBBox(id);
                }
            }
        }

        isFirstLocation = true;

        lastX = 0;
        lastY = 0;

        linePoses.clear();

        curentTrackID = -1;
    }

    /**
     * Remove last GPS track from MapView.
     */
    protected void removeLastGPSTrack() {
        if (line != null) {
            tracksVectorDataSource.remove(line);
        }
        line = null;
        // jaak send me crash log and because I really don't know how is
        // possible that lineposes be null I made this check and it will work :)
        if (linePoses == null) {
            linePoses = new MapPosVector();
        } else {
            linePoses.clear();
        }
    }

    /**
     * Returns if there is GPS tracks in DB.
     */
    public boolean isTracksInDB() {
        if (isOpen) {
            return getTracksCount() > 0;
        } else {
            return false;
        }
    }

    /**
     * Returns current ID for GPS tracking session.
     */
    public long getCurentTrackID() {
        return curentTrackID;
    }

    /**
     * Returns total number of GPS tracks in DB.
     */
    public int getTracksCount() {
        if (isOpen) {
            Cursor c = database.rawQuery("select count(*) from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME, null);

            int count = 0;

            if (c.moveToFirst()) {
                count = c.getInt(0);
            }

            c.close();

            return count;
        } else {
            return 0;
        }
    }

    /**
     * Insert track into DB and return ID for this track.
     */
    public long insertTrack() {
        if (isOpen) {
            Cursor c = database.rawQuery("select "
                    + LocationTrackDBHelper.COLUMN_NAME_ID + " from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME, null);

            int id;

            if (c.moveToLast()) {
                id = c.getInt(0);
            } else {
                id = 0;
            }

            c.close();

            ContentValues values = new ContentValues();
            values.put(LocationTrackDBHelper.COLUMN_NAME_ID, id + 1);
            values.put(LocationTrackDBHelper.COLUMN_NAME_START, "");
            values.put(LocationTrackDBHelper.COLUMN_NAME_END, "");
            values.put(LocationTrackDBHelper.COLUMN_NAME_X_MIN, Double.MAX_VALUE);
            values.put(LocationTrackDBHelper.COLUMN_NAME_Y_MIN, Double.MIN_VALUE);
            values.put(LocationTrackDBHelper.COLUMN_NAME_X_MAX, Double.MIN_VALUE);
            values.put(LocationTrackDBHelper.COLUMN_NAME_Y_MAX, Double.MAX_VALUE);
            values.put(LocationTrackDBHelper.COLUMN_NAME_IS_ON_MAP, 1);

            long l = database.insert(LocationTrackDBHelper.TRACKS_TABLE_NAME, null,
                    values);

            curentTrackID = l;

            return l;
        }

        return -1;
    }

    /**
     * Insert track location and timestamp for track id
     */
    public long insertTrackLocation(final long id, final Location location) {
        if (isOpen) {
            ContentValues values = new ContentValues();

            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_ID, id);
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_X,
                    location.getLongitude());
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_Y,
                    location.getLatitude());
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_TIME,
                    location.getTime() + "");
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_ELEVATION,
                    location.getAltitude());
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_DIRECTION,
                    location.getBearing());
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_ACCURACY,
                    location.getAccuracy());
            values.put(LocationTrackDBHelper.COLUMN_NAME_TRACK_SPEED,
                    location.getSpeed());

            long l = database.insert(LocationTrackDBHelper.TRACKPOINTS_TABLE_NAME,
                    null, values);

            if (l != -1) {
                // jaak send me crash log and because I really don't know how is
                // possible that lineposes be null I made this check and it will
                // work :)
                if (linePoses == null) {
                    linePoses = new MapPosVector();
                }

                linePoses.add(projection.fromWgs84(new MapPos(location
                        .getLongitude(), location.getLatitude())));

                if (((LocationTrackingApplicationInterface) mapActivity
                        .getApplication()).isMapViewLive()) {
                    if (line == null && linePoses.size() > 1) {
                        line = new Line(linePoses,
                                lineStyleBuilder.buildStyle());
                        tracksVectorDataSource.add(line);
                    } else {
                        if (line != null) {
                            line.setPoses(linePoses);
                            tracksVectorDataSource.notifyElementsChanged();
                        }
                    }
                }

                if (isFirstLocation) {
                    isFirstLocation = false;

                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            NominatimService nominatium = new NominatimService();

                            try {
                                String loc = nominatium.reverseGeocode(
                                        location.getLongitude(),
                                        location.getLatitude());

                                setStartLocation(id, loc);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.run();
                }

                lastX = location.getLongitude();
                lastY = location.getLatitude();

                if (linePoses.size() > 1) {
                    updateBBox(id);
                }
            }

            return l;
        }

        return -1;
    }

    /**
     * Delete track from DB.
     */
    public boolean deleteTrack(long id) {
        if (isOpen) {
            database.delete(LocationTrackDBHelper.TRACKPOINTS_TABLE_NAME,
                    LocationTrackDBHelper.COLUMN_NAME_TRACK_ID + "=" + id, null);

            int l = database.delete(LocationTrackDBHelper.TRACKS_TABLE_NAME,
                    LocationTrackDBHelper.COLUMN_NAME_ID + "=" + id, null);

            return l > 0;
        } else {
            return false;
        }
    }

    /**
     * Returns all tracks id.
     */
    public int[] getTracksID() {
        if (isOpen) {
            Cursor c = database.rawQuery("select "
                    + LocationTrackDBHelper.COLUMN_NAME_ID + " from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME, null);

            c.moveToFirst();

            int[] id = new int[c.getCount()];
            int i = 0;

            while (!c.isAfterLast()) {
                id[i++] = c.getInt(0);
                c.moveToNext();
            }

            c.close();

            return id;
        } else {
            return new int[0];
        }
    }

    /**
     * Set is track visible on map.
     */
    public int setIsTrackVisibleOnMap(long id, boolean isVisibleOnMap) {
        if (isOpen) {
            int v = 1;

            if (isVisibleOnMap) {
                v = 1;
            } else {
                v = 0;
            }

            ContentValues values = new ContentValues();
            values.put(LocationTrackDBHelper.COLUMN_NAME_IS_ON_MAP, v);

            return database.update(LocationTrackDBHelper.TRACKS_TABLE_NAME, values,
                    LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);
        }

        return -1;
    }

    /**
     * Returns TrackData object for track id, which contains all information
     * about track.
     */
    public TrackData getTrack(long id) {
        if (isOpen) {
            Cursor c = database.rawQuery("select * from "
                    + LocationTrackDBHelper.TRACKPOINTS_TABLE_NAME + " where "
                    + LocationTrackDBHelper.COLUMN_NAME_TRACK_ID + " = " + id, null);

            c.moveToFirst();

            TrackData trackData = new TrackData();
            int i = 0;
            int l = c.getCount();

            if (l > 0) {
                trackData.locations = new PointF[l];

                long distance = 0;
                long duration = 0;
                long startTime = 0;
                long endTime = 0;

                float x = 0;
                float y = 0;

                boolean hasLastPoints = false;

                while (!c.isAfterLast()) {
                    if (i == 0) {
                        startTime = Long.parseLong(c.getString(4));
                    }

                    if (i == l - 1) {
                        endTime = Long.parseLong(c.getString(4));
                        duration = (endTime - startTime) / 1000;
                    }

                    trackData.locations[i++] = new PointF(c.getFloat(2),
                            c.getFloat(3));

                    if (hasLastPoints) {
                        distance += Utility.calculateDistance(y, x,
                                c.getFloat(3), c.getFloat(2));

                        x = c.getFloat(2);
                        y = c.getFloat(3);
                    } else {
                        x = c.getFloat(2);
                        y = c.getFloat(3);

                        hasLastPoints = true;
                    }

                    c.moveToNext();
                }

                c.close();

                trackData.distance = formatDistance(distance);
                trackData.duration = formatTimeDuration(duration);
                trackData.startTime = Utility
                        .formatTimestampToDateFormat(startTime);
                trackData.endTime = Utility
                        .formatTimestampToDateFormat(endTime);
                trackData.id = id;
            } else {
                trackData.locations = new PointF[0];
                trackData.distance = "";
                trackData.duration = "";
                trackData.startTime = "";
                trackData.endTime = "";
                trackData.id = id;
            }

            c = database.rawQuery("select * from "
                    + LocationTrackDBHelper.TRACKS_TABLE_NAME + " where "
                    + LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);

            c.moveToFirst();

            l = c.getCount();

            if (l > 0) {
                trackData.startLocation = c.getString(1);
                trackData.endLocation = c.getString(2);
                trackData.minX = c.getDouble(3);
                trackData.minY = c.getDouble(4);
                trackData.maxX = c.getDouble(5);
                trackData.maxY = c.getDouble(6);
                if (c.getInt(7) == 0) {
                    trackData.isOnMap = false;
                } else {
                    trackData.isOnMap = true;
                }
            } else {
                trackData.startLocation = "";
                trackData.endLocation = "";
                trackData.minX = 0;
                trackData.minY = 0;
                trackData.maxX = 0;
                trackData.maxY = 0;
                trackData.isOnMap = true;
            }

            c.close();

            return trackData;
        } else {
            return null;
        }
    }

    private int setStartLocation(long id, String location) {
        if (isOpen) {
            ContentValues values = new ContentValues();
            values.put(LocationTrackDBHelper.COLUMN_NAME_START, location);

            return database.update(LocationTrackDBHelper.TRACKS_TABLE_NAME, values,
                    LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);
        }

        return 0;
    }

    private int setEndLocation(long id, String location) {
        if (isOpen) {
            ContentValues values = new ContentValues();
            values.put(LocationTrackDBHelper.COLUMN_NAME_END, location);

            return database.update(LocationTrackDBHelper.TRACKS_TABLE_NAME, values,
                    LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);
        }

        return 0;
    }

    private void updateBBox(long id) {
        if (isOpen) {
            double x;
            double y;

            double xMin = Double.MAX_VALUE;
            double yMin = Double.MIN_VALUE;
            double xMax = Double.MIN_VALUE;
            double yMax = Double.MAX_VALUE;

            long l = linePoses.size();

            for (int i = 0; i < l; i++) {
                x = linePoses.get(i).getX();
                y = linePoses.get(i).getY();

                if (x < xMin) {
                    xMin = x;
                }
                if (y > yMin) {
                    yMin = y;
                }
                if (x > xMax) {
                    xMax = x;
                }
                if (y < yMax) {
                    yMax = y;
                }
            }

            ContentValues values = new ContentValues();
            values.put(LocationTrackDBHelper.COLUMN_NAME_X_MIN, xMin);
            values.put(LocationTrackDBHelper.COLUMN_NAME_Y_MIN, yMin);
            values.put(LocationTrackDBHelper.COLUMN_NAME_X_MAX, xMax);
            values.put(LocationTrackDBHelper.COLUMN_NAME_Y_MAX, yMax);

            database.update(LocationTrackDBHelper.TRACKS_TABLE_NAME, values,
                    LocationTrackDBHelper.COLUMN_NAME_ID + " = " + id, null);
        }
    }

    private String formatTimeDuration(long timeDuration) {
        if (timeDuration < 60) {
            return String.format(Locale.getDefault(), "%s %s", timeDuration,
                    context.getString(R.string.second));
        } else if (timeDuration < 3600) {
            long m = timeDuration / 60;
            long s = timeDuration - m * 60;

            return String.format(Locale.getDefault(), "%s %s %s %s", m,
                    context.getString(R.string.minute), s,
                    context.getString(R.string.second));
        } else {
            long h = timeDuration / 3600;
            long m = (timeDuration - h * 3600) / 60;
            long s = timeDuration - (m * 60 + h * 3600);

            return String.format(Locale.getDefault(), "%s %s %s %s %s %s", h,
                    context.getString(R.string.hour), m,
                    context.getString(R.string.minute), s,
                    context.getString(R.string.second));
        }
    }

    private String formatDistance(long length) {
        if (measurementUnit == LocationTrackingDB.METRIC) {
            if (length < 1000) {
                return length + " m";
            } else {
                float km = length / 1000f;

                return String.format(Locale.getDefault(), "%.2f %s", km, " km");
            }
        } else if (measurementUnit == LocationTrackingDB.IMPERIAL) {
            // meters to ft
            length *= 3.28084f;

            if (length < 528) {
                return length + " ft";
            } else {
                float mi = length / 5280.0f;

                return String.format(Locale.getDefault(), "%.2f %s", mi, " mi");
            }
        } else {
            return "";
        }
    }

    private class LocationTrackDBHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION = 9;
        public static final String DATABASE_NAME = "GPSTracks.db";

        private static final String TRACKS_TABLE_NAME = "gps_tracks";
        private static final String TRACKPOINTS_TABLE_NAME = "gps_tracks_locations";

        private static final String COLUMN_NAME_ID = "_ID";
        private static final String COLUMN_NAME_START = "_start";
        private static final String COLUMN_NAME_END = "_end";
        private static final String COLUMN_NAME_X_MIN = "x1";
        private static final String COLUMN_NAME_Y_MIN = "y1";
        private static final String COLUMN_NAME_X_MAX = "x2";
        private static final String COLUMN_NAME_Y_MAX = "y2";
        private static final String COLUMN_NAME_IS_ON_MAP = "visible";

        private static final String COLUMN_NAME_TRACK_ID = "track_id";
        private static final String COLUMN_NAME_TRACK_X = "track_x";
        private static final String COLUMN_NAME_TRACK_Y = "track_y";
        private static final String COLUMN_NAME_TRACK_TIME = "time";

        private static final String COLUMN_NAME_TRACK_ELEVATION = "elevation";
        private static final String COLUMN_NAME_TRACK_DIRECTION = "direction";
        private static final String COLUMN_NAME_TRACK_ACCURACY = "accuracy";
        private static final String COLUMN_NAME_TRACK_SPEED = "speed";

        private static final String SQL_CREATE_TRACKS = "CREATE TABLE "
                + TRACKS_TABLE_NAME + " (" + COLUMN_NAME_ID
                + " INTEGER PRIMARY KEY, " + COLUMN_NAME_START + " TEXT, "
                + COLUMN_NAME_END + " TEXT, " + COLUMN_NAME_X_MIN + " REAL, "
                + COLUMN_NAME_Y_MIN + " REAL, " + COLUMN_NAME_X_MAX + " REAL, "
                + COLUMN_NAME_Y_MAX + " REAL, " + COLUMN_NAME_IS_ON_MAP
                + " INTEGER" + ")";

        private static final String SQL_CREATE_TRACKPOINTS = "CREATE TABLE "
                + TRACKPOINTS_TABLE_NAME + " (" + COLUMN_NAME_ID
                + " INTEGER PRIMARY KEY, " + COLUMN_NAME_TRACK_ID
                + " INTEGER, " + COLUMN_NAME_TRACK_X + " INTEGER, "
                + COLUMN_NAME_TRACK_Y + " REAL, " + COLUMN_NAME_TRACK_TIME
                + " TEXT, " + COLUMN_NAME_TRACK_ELEVATION + " REAL, "
                + COLUMN_NAME_TRACK_DIRECTION + " REAL, "
                + COLUMN_NAME_TRACK_ACCURACY + " REAL, "
                + COLUMN_NAME_TRACK_SPEED + " REAL " + ")";

        private static final String SQL_DELETE_TRACKS = "DROP TABLE IF EXISTS "
                + TRACKS_TABLE_NAME;
        private static final String SQL_DELETE_TRACKPOINTS = "DROP TABLE IF EXISTS "
                + TRACKPOINTS_TABLE_NAME;

        private static final String SQL_ALTER_TRACKPOINTS8_1 = "ALTER TABLE "
                + TRACKPOINTS_TABLE_NAME + " ADD COLUMN "
                + COLUMN_NAME_TRACK_ELEVATION + " REAL";
        private static final String SQL_ALTER_TRACKPOINTS8_2 = "ALTER TABLE "
                + TRACKPOINTS_TABLE_NAME + " ADD COLUMN "
                + COLUMN_NAME_TRACK_DIRECTION + " REAL";
        private static final String SQL_ALTER_TRACKPOINTS8_3 = "ALTER TABLE "
                + TRACKPOINTS_TABLE_NAME + " ADD COLUMN "
                + COLUMN_NAME_TRACK_ACCURACY + " REAL";
        private static final String SQL_ALTER_TRACKPOINTS8_4 = "ALTER TABLE "
                + TRACKPOINTS_TABLE_NAME + " ADD COLUMN "
                + COLUMN_NAME_TRACK_SPEED + " REAL";

        public LocationTrackDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TRACKS);
            db.execSQL(SQL_CREATE_TRACKPOINTS);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 8) {
                db.execSQL(SQL_DELETE_TRACKS);
                db.execSQL(SQL_DELETE_TRACKPOINTS);

                onCreate(db);
            } else if (oldVersion == 8 && newVersion == 9) {
                db.execSQL(SQL_ALTER_TRACKPOINTS8_1);
                db.execSQL(SQL_ALTER_TRACKPOINTS8_2);
                db.execSQL(SQL_ALTER_TRACKPOINTS8_3);
                db.execSQL(SQL_ALTER_TRACKPOINTS8_4);
            } else {
                // TODO with next DB changes
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion,
                                int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
