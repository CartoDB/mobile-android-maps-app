package com.nutiteq.nuticomponents.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
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
import com.nutiteq.nuticomponents.customviews.LocationView;

public class LocationCircle extends Thread {

    private MapView mapView;
    private LocationView locationView;

    private Boolean isLive = true;

    private static final int SIZE_OF_CIRCLE_LOCATION_MARKER = 50;
    private static final int SIZE_OF_ARROW_LOCATION_MARKER = 50;

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
    private float lastAccuracy = 0f;
    private float accuracy;
    private float deltaAccuracy = 0f;

    private float currentAccuracy = 0f;

    private double animLocationX;
    private double animLocationY;
    private float animAccurancy;
    private float deltaAnimAccurancy;// use for animation and set new value from
    // accurancy on start of animation, so
    // animation is always smooth, same for
    // animLocationX and animLocationY

    private Polygon polygon;

    private MapPosVector polygonPoses;
    private PolygonStyleBuilder polygonStyleBuilder;
    private LineStyleBuilder lineStyleBuilder;

    private short r = 171;// red
    private short g = 227;// green
    private short b = 207;// blue
    private short a = 200; // alfa

    private static final String SHARED_PREFS = "com.nutiteq.nuticomponents.locationcircle";

    private MarkerStyle locationCircleMarkerStyle;
    private MarkerStyle locationArrowMarkerStyle;

    private boolean isAnimFinished = true;

    private float p = 1f; // percent of elapsed time of animation

    private boolean isFirstAccuracySet = false;

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

                    if (p >= 1f) {
                        updateCirclePoints(new MapPos(lastLocationX + deltaX / 50
                                        * i, lastLocationY + deltaY / 50 * i),
                                currentAccuracy, NUMBER_OF_CIRCLE_POINTS);
                    } else {
                        updateCirclePoints(new MapPos(lastLocationX + deltaX / 50
                                        * i, lastLocationY + deltaY / 50 * i),
                                animAccurancy + p * deltaAnimAccurancy,
                                NUMBER_OF_CIRCLE_POINTS);
                    }

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

                if (p >= 1f) {
                    updateCirclePoints(new MapPos(locationX, locationY),
                            currentAccuracy, NUMBER_OF_CIRCLE_POINTS);
                } else {
                    updateCirclePoints(new MapPos(locationX, locationY),
                            animAccurancy + p * deltaAnimAccurancy,
                            NUMBER_OF_CIRCLE_POINTS);
                }
            }

            isSmoothMoveFinished = true;
            isLocationMarkerNewLocationSet = true;
        }
    };

    /**
     * @author Milan Ivankovic, Nole
     * <p/>
     * LocationCircle is thread and it shows circle on map which has
     * radius of location accuracy and user location with marker. You
     * must call method setLocation to update user location and circle
     * radius and setRotation which updates user marker direction. On
     * end you must call stopLocationCircle to stop this thread.
     */
    public LocationCircle(MapView mapView, LocationView locationView, LocalVectorDataSource locationDataSource) {
        super();

        this.mapView = mapView;
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

        this.locationDataSource.add(locationMarker);

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
        this.locationDataSource.add(polygon);
    }

    /**
     * Refresh location marker and polygon circle so both are on top
     */
    public void refresh() {
        this.locationDataSource.remove(locationMarker);
        this.locationDataSource.add(locationMarker);

        this.locationDataSource.remove(polygon);
        this.locationDataSource.add(polygon);
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
        long duration = 1000;

        while (isLive) {
            // check if there is a first location fix
            if (isFirstLocationSet) {
                if (!isFirstAccuracySet) {
                    isFirstAccuracySet = true;
                    currentAccuracy = accuracy;
                    lastAccuracy = accuracy;
                }

                if (deltaAccuracy != 0 && isAnimFinished) {
                    isAnimFinished = false;

                    t = System.currentTimeMillis();
                    p = 0f;

                    animLocationX = locationX;
                    animLocationY = locationY;
                    deltaAnimAccurancy = deltaAccuracy;
                    animAccurancy = currentAccuracy; // it's always
                    // smooth,
                    // animation only get radius
                    // and location
                    // on the begin

                    currentAccuracy += deltaAccuracy;

                    if (isSmoothMoveFinished) {
                        isSmoothMoveFinished = false;
                        Thread thread = new Thread(smoothMove);
                        thread.start();
                    }

                    locationMarker.setVisible(true);
                }

                if (isAnimFinished && !isLocationMarkerNewLocationSet) {
                    locationMarker.setVisible(true);

                    if (isSmoothMoveFinished) {
                        isSmoothMoveFinished = false;
                        Thread thread = new Thread(smoothMove);
                        thread.start();
                    }
                } else {
                    p = 1.0f * (System.currentTimeMillis() - t) / duration;
                }

                if (p >= 1f) {
                    if (!isAnimFinished) {
                        if (isSmoothMoveFinished) {
                            updateCirclePoints(new MapPos(animLocationX,
                                            animLocationY), currentAccuracy,
                                    NUMBER_OF_CIRCLE_POINTS);
                            deltaAccuracy = 0;
                            isAnimFinished = true;
                        }
                    } else {
                        if (isSmoothMoveFinished) {
                            updateCirclePoints(
                                    new MapPos(locationX, locationY),
                                    currentAccuracy, NUMBER_OF_CIRCLE_POINTS);
                        }
                    }

                    try {
                        sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (isSmoothMoveFinished) {
                        updateCirclePoints(new MapPos(animLocationX,
                                animLocationY), animAccurancy + p
                                * deltaAnimAccurancy, NUMBER_OF_CIRCLE_POINTS);
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

            if (isAnimFinished && isFirstAccuracySet) {
                // only set delta if it's than 10% worse
                if (Math.abs(1 - accuracy / lastAccuracy) > 0.1f) {
                    deltaAccuracy = accuracy - lastAccuracy;
                } else {
                    deltaAccuracy = 0;
                }
            }

            lastAccuracy = accuracy;

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
    public void stopLocationCircle() {
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
    }
}
