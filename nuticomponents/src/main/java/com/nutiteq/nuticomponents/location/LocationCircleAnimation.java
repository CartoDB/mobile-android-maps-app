package com.nutiteq.nuticomponents.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.ScreenPos;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.graphics.Color;
import com.carto.projections.Projection;
import com.carto.styles.BillboardOrientation;
import com.carto.styles.LineJoinType;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.ui.MapView;
import com.carto.utils.BitmapUtils;
import com.carto.vectorelements.Marker;
import com.carto.vectorelements.Polygon;
import com.nutiteq.nuticomponents.R;
import com.nutiteq.nuticomponents.Utility;
import com.nutiteq.nuticomponents.customviews.LocationView;

public class LocationCircleAnimation extends Thread {

    private MapView mapView;
    private int duration;
    private LocationView locationView;

    private Boolean isLive = true;

    private static final int SIZE_OF_CIRCLE_LOCATION_MARKER = 50;
    private static final int SIZE_OF_ARROW_LOCATION_MARKER = 50;
    private static final float MIN_CIRCLE_ACCURACY_PX = 40.0f;

    private static final int NUMBER_OF_CIRCLE_POINTS = 72;

    private Marker locationMarker;

    private boolean isFirstLocationSet = false;
    private boolean isLocationMarkerNewLocationSet = false;

    private boolean isLocationMarkerStyleChanged = false;

    private Projection projection;

    private double locationX;
    private double locationY;
    private double lastLocationX;
    private double lastLocationY;
    private double animLocationX;
    private double animLocationY;
    private float accuracy;
    private float animAccurancy; // use for animation and set new value from
    // accurancy on start of animation, so
    // animation is always smooth, same for
    // animLocationX and animLocationY

    private Polygon polygon;

    private MapPosVector polygonPoses;
    private PolygonStyleBuilder polygonStyleBuilder;
    private LineStyleBuilder lineStyleBuilder;

    private short r = 252;// red
    private short g = 61;// green
    private short b = 19;// blue
    private short ALFA = 200;// initial and max transparency
    private short a = ALFA; // goes from alfa to zero and again

    private static final String SHARED_PREFS = "com.nutiteq.nuticomponents.locationanimation";

    private MarkerStyle locationCircleMarkerStyle;
    private MarkerStyle locationArrowMarkerStyle;

    private float p = 1f; // percent of elapsed time of animation

    private LocalVectorDataSource locationDataSource;

    private boolean isSmoothMoveFinished = true;

    private boolean shouldDoSmoothMove = false;

    private Runnable smoothMove = new Runnable() {

        @Override
        public void run() {
            if (shouldDoSmoothMove) {
                double deltaX = locationX - lastLocationX;
                double deltaY = locationY - lastLocationY;

                for (int i = 1; i <= 50; i++) {
                    locationMarker.setPos(mapView
                            .getOptions()
                            .getBaseProjection()
                            .fromWgs84(
                                    new MapPos(lastLocationX + deltaX / 50 * i,
                                            lastLocationY + deltaY / 50 * i)));

                    updateCirclePoints(new MapPos(lastLocationX + deltaX / 50 * i,
                                    lastLocationY + deltaY / 50 * i), animAccurancy * p,
                            NUMBER_OF_CIRCLE_POINTS);

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                locationMarker.setPos(mapView
                        .getOptions()
                        .getBaseProjection()
                        .fromWgs84(
                                new MapPos(locationX,
                                        locationY)));

                updateCirclePoints(new MapPos(locationX,
                                locationY), animAccurancy * p,
                        NUMBER_OF_CIRCLE_POINTS);
            }

            isSmoothMoveFinished = true;
            isLocationMarkerNewLocationSet = true;
        }
    };

    /**
     * @author Milan Ivankovic, Nole
     * <p/>
     * LocationAnimation is thread and it shows animated circle on map
     * which has radius of location accuracy and user location with
     * marker. You must call method setLocation to update user location
     * and circle radius and setRotation which updates user marker
     * direction. On end you must call stopLocationAnimation to stop
     * this thread. Duration is in ms is time spent for start animation
     * where radius = 0 to end of animation where radius = location
     * accuracy
     */

    public LocationCircleAnimation(MapView mapView, int duration,
                                   LocationView locationView, LocalVectorDataSource locationDataSource) {
        super();

        this.mapView = mapView;
        this.duration = duration;
        this.locationView = locationView;
        this.locationDataSource = locationDataSource;

        this.projection = this.mapView.getOptions().getBaseProjection();

        Bitmap locationCircleMarkerBitmap = BitmapFactory.decodeResource(
                mapView.getContext().getResources(),
                R.drawable.location_circle_marker);
        com.carto.graphics.Bitmap markerCircleBitmap = BitmapUtils
                .createBitmapFromAndroidBitmap(locationCircleMarkerBitmap);

        // Create marker styles, with circle and arrow bitmaps
        MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
        markerStyleBuilder.setBitmap(markerCircleBitmap);
        markerStyleBuilder.setSize(SIZE_OF_CIRCLE_LOCATION_MARKER);
        markerStyleBuilder
                .setOrientationMode(BillboardOrientation.BILLBOARD_ORIENTATION_FACE_CAMERA_GROUND);
        markerStyleBuilder.setAnchorPoint(0, 0);
        locationCircleMarkerStyle = markerStyleBuilder.buildStyle();

        Bitmap locationArrowMarkerBitmap = BitmapFactory.decodeResource(mapView
                .getContext().getResources(), R.drawable.location_arrow_marker);
        com.carto.graphics.Bitmap markerArrowBitmap = BitmapUtils
                .createBitmapFromAndroidBitmap(locationArrowMarkerBitmap);
        markerStyleBuilder.setBitmap(markerArrowBitmap);
        markerStyleBuilder.setSize(SIZE_OF_ARROW_LOCATION_MARKER);
        locationArrowMarkerStyle = markerStyleBuilder.buildStyle();

        // get last location
        SharedPreferences preferences = mapView.getContext()
                .getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        MapPos lastLocation = new MapPos(Double.parseDouble(preferences
                .getString("xpos", "0.0")), Double.parseDouble(preferences
                .getString("ypos", "0.0")));

        // add location marker with circle bitmaps until sensors returns compass
        // information
        locationMarker = new Marker(mapView.getOptions().getBaseProjection()
                .fromWgs84(lastLocation), locationCircleMarkerStyle);

        locationMarker.setVisible(false);

        locationDataSource.add(locationMarker);

        // Create polygon style and poses
        polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(r, g, b, a));

        lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new com.carto.graphics.Color(r, g, b, (short) 255));
        lineStyleBuilder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_ROUND);
        lineStyleBuilder.setStretchFactor(2);
        lineStyleBuilder.setWidth(1);

        polygonStyleBuilder.setLineStyle(lineStyleBuilder.buildStyle());

        polygonPoses = new MapPosVector();
        polygonPoses.add(new MapPos(0, 0));
        polygonPoses.add(new MapPos(0, 0));
        polygonPoses.add(new MapPos(0, 0));

        // Add polygon
        polygon = new Polygon(polygonPoses, polygonStyleBuilder.buildStyle());
        locationDataSource.add(polygon);
    }

    /**
     * generates round polygon (circle) based on center, radius and number of
     * polygon points
     *
     * @param center defines lon, lat in WGS84 of center
     * @param radius in meters
     * @param points number of required points, 360/points should be integer
     */
    private void updateCirclePoints(MapPos center, float radius, int points) {
        double earthsRadius = 6378137;
        double d2r = Math.PI / 180;
        double r2d = 180 / Math.PI;

        double rLatitude = r2d * (radius / earthsRadius);
        double rLongitude = rLatitude / Math.cos(d2r * center.getY());

        polygonPoses.clear();

        for (int i = 0; i <= points + 1; i++) {
            double theta = Math.PI * ((double) i / ((double) points / 2));
            double pLong = center.getX() + (rLongitude * Math.cos(theta));
            double pLat = center.getY() + (rLatitude * Math.sin(theta));
            polygonPoses.add(projection.fromWgs84(new MapPos(pLong, pLat)));
        }

        polygon.setPoses(polygonPoses);

        // set new alfa
        polygonStyleBuilder.setColor(new Color(r, g, b, a));
        lineStyleBuilder.setColor(new com.carto.graphics.Color(r, g, b, (short) 255));
        polygonStyleBuilder.setLineStyle(lineStyleBuilder.buildStyle());

        polygon.setStyle(polygonStyleBuilder.buildStyle());
    }

    @Override
    public void run() {
        long t = 0; // start time of animation in ms

        MapPos mp1;
        MapPos mp2;
        float onePxInMercatorMeters;
        double lat;
        double cos;
        float onePxInMeters;

        DisplayMetrics metrics = mapView.getContext().getResources()
                .getDisplayMetrics();

        while (isLive) {
            // check if there is a first location fix
            if (isFirstLocationSet) {
                mp1 = mapView.screenToMap(new ScreenPos(mapView.getWidth() / 2,
                        mapView.getHeight() / 2));
                mp2 = mapView.screenToMap(new ScreenPos(
                        mapView.getWidth() / 2 + 1, mapView.getHeight() / 2));

                onePxInMercatorMeters = (float) Utility.distanceBetweenPoints(mp1, mp2);

                lat = mapView.getOptions().getBaseProjection()
                        .toWgs84(mapView.getFocusPos()).getY();
                cos = Math.cos(lat * Math.PI / 180.0);
                onePxInMeters = (float) (onePxInMercatorMeters * cos);

                // do animation if it can be seen on map
                if (accuracy / onePxInMeters > MIN_CIRCLE_ACCURACY_PX
                        * metrics.density) {
                    p = 1.0f * (System.currentTimeMillis() - t) / duration;

                    if (p > 1f) {
                        t = System.currentTimeMillis();
                        p = 0f;
                        animLocationX = locationX;
                        animLocationY = locationY;
                        animAccurancy = accuracy; // it's always smooth,
                        // animation only get radius
                        // and location
                        // on the begin

                        if (!isLocationMarkerNewLocationSet) {
                            if (isSmoothMoveFinished) {
                                isSmoothMoveFinished = false;
                                Thread thread = new Thread(smoothMove);
                                thread.start();
                            }

                            locationMarker.setVisible(true);
                        }
                    }

                    a = (short) ((1 - p) * ALFA); // goes from ALFA to 0

                    if (isSmoothMoveFinished) {
                        updateCirclePoints(new MapPos(animLocationX,
                                        animLocationY), animAccurancy * p,
                                NUMBER_OF_CIRCLE_POINTS);
                    }

                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // clear polygon first time it's not visible on
                    // screen
                    // TODO finish animation and than stop it
                    if (polygonPoses.size() > 3) {
                        polygonPoses.clear();

                        polygonPoses.add(new MapPos(0, 0));
                        polygonPoses.add(new MapPos(0, 0));
                        polygonPoses.add(new MapPos(0, 0));

                        polygon.setPoses(polygonPoses);
                    }

                    if (!isLocationMarkerNewLocationSet) {
                        if (isSmoothMoveFinished) {
                            isSmoothMoveFinished = false;
                            Thread thread = new Thread(smoothMove);
                            thread.start();
                        }

                        locationMarker.setVisible(true);
                    }

                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Set WGS84 x, y for location marker and also accuracy in meters which is
     * used for radius of location circle.
     */
    public void setLocation(double x, double y, float a, boolean shouldDoSmoothMove) {
        this.shouldDoSmoothMove = shouldDoSmoothMove;

        if (isSmoothMoveFinished) {
            if (isFirstLocationSet) {
                lastLocationX = locationX;
                lastLocationY = locationY;
            } else {
                lastLocationX = x;
                lastLocationY = y;
            }

            locationX = x;
            locationY = y;
            accuracy = a;

            isFirstLocationSet = true;
            isLocationMarkerNewLocationSet = false;
        }
    }

    /**
     * Set rotation for location marker. You must use sensors for this rotation
     * information.
     */
    public void setRotation(float rotation) {
        if (!isLocationMarkerStyleChanged) {
            if (isFirstLocationSet) {
                locationMarker.setRotation(rotation - mapView.getMapRotation());
                locationMarker.setStyle(locationArrowMarkerStyle);
                isLocationMarkerStyleChanged = true;
            }
        }

        if (locationView.getState() != LocationView.LOCATION_STATE_TURN) {
            locationMarker.setRotation(rotation - mapView.getMapRotation());
        } else {
            /*
			 * In location turn mode location marker arrow must always have 0
			 * rotation, if I just set normal rotation as when it isn't location
			 * turn mode, location marker arrow has almost 0 rotation but not
			 * exactly 0. Also, rotation to 0 is smooth :)
			 */
            if (!(locationMarker.getRotation() > -2 && locationMarker
                    .getRotation() < 2)) {
                locationMarker.setRotation(rotation - mapView.getMapRotation());
            } else if (locationMarker.getRotation() != 0) {
                locationMarker.setRotation(0);
            }
        }
    }

    /**
     * Stop LocationCircle thread
     */
    public void stopLocationAnimation() {
        isLive = false;

        // store current states for location marker
        SharedPreferences preferences = mapView.getContext()
                .getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("xpos", Double.toString(locationX));
        editor.putString("ypos", Double.toString(locationY));

        // commit to storage
        editor.commit();

        locationDataSource.remove(locationMarker);
        locationDataSource.remove(polygon);
    }

    /**
     * Returns if first location is set.
     */
    public boolean isFirstLocationSet() {
        return isFirstLocationSet;
    }

    /**
     * Set location circle color with RGB and alfa
     */
    public void setLocationColor(short r, short g, short b, short a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.ALFA = a;
    }
}
