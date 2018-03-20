package com.nutiteq.app.map.listener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.MapRange;
import com.carto.core.ScreenPos;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.graphics.Color;
import com.carto.layers.VectorLayer;
import com.carto.packagemanager.PackageManager;
import com.carto.projections.Projection;
import com.carto.routing.PackageManagerRoutingService;
import com.carto.routing.RoutingAction;
import com.carto.routing.RoutingInstruction;
import com.carto.routing.RoutingInstructionVector;
import com.carto.routing.RoutingRequest;
import com.carto.routing.RoutingResult;
import com.carto.routing.RoutingService;
import com.carto.styles.BalloonPopupMargins;
import com.carto.styles.BalloonPopupStyleBuilder;
import com.carto.styles.BillboardOrientation;
import com.carto.styles.LineJoinType;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.ui.ClickType;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.utils.BitmapUtils;
import com.carto.vectorelements.BalloonPopup;
import com.carto.vectorelements.Billboard;
import com.carto.vectorelements.Line;
import com.carto.vectorelements.Marker;
import com.carto.vectorelements.VectorElement;
import com.carto.vectorelements.VectorElementVector;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.nutiteq.app.locationbookmarks.LocationBookmark;
import com.nutiteq.app.locationbookmarks.LocationBookmarksDB;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.map.RouteInstruction;
import com.nutiteq.app.map.RouteView;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.app.utils.Const;
import com.nutiteq.nuticomponents.NominatimService;
import com.nutiteq.nuticomponents.PackageSuggestion;
import com.nutiteq.nuticomponents.customviews.BottomView;
import com.nutiteq.nuticomponents.customviews.CompassView;
import com.nutiteq.nuticomponents.customviews.LocationView;
import com.nutiteq.nuticomponents.customviews.ScaleBarView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MyMapEventListener extends MapEventListener {

    private MapView mapView;
    private CompassView compassView;
    private ScaleBarView scaleBarView;
    private PackageSuggestion packageSuggestion;
    private LocalVectorDataSource searchVectorDataSource;
    private LocalVectorDataSource bookmarkDataSource;
    private BottomView bottomView;
    private View emptyView;
    private LocationView locationView;
    private RouteView routeView;

    private ArrayList<LocationBookmark> locationBookmarks;
    private ArrayList<Marker> pins;

    private BalloonPopup oldClickLabel;

    private int strokeWidth;

    private Projection projection;
    private Context context;
    private Handler handler;

    private LocationBookmarksDB locationBookmarkDB;

    private com.carto.graphics.Bitmap bookmarkBitmap;

    private VectorElement vectorElement;
    private Billboard billboard;

    private float angle = Float.MAX_VALUE;

    private Random rand = new Random();

    private VectorElement selectedVectorElement;

    private MarkerStyleBuilder markerLongStyleBuilder;

    private MarkerStyleBuilder markerFavoriteStyleBuilder;

    private BalloonPopupStyleBuilder balloonPopupStyleBuilder;

    private Firebase myFirebase;

    private String firebaseNodeKey;

    private Marker longClickPin;

    private MapPos tempMapPos;

    private boolean isFirebase = true;

    // contains information about selected location bookmark id and vector
    // element, it's init when user click on location bookmarks pin
    private String favoriteID;

    private Bitmap leftBitmap;
    private Bitmap rightBitmap;
    private Bitmap forwardBitmap;
    private Bitmap startBitmap;
    private Bitmap endBitmap;

    private MapPosVector routePoints;
    private Line routeLine;

    private VectorElementVector routeDirections = new VectorElementVector();
    private VectorElementVector bookmarks = new VectorElementVector();

    public MyMapEventListener(MapView mapView, CompassView compassView,
                              ScaleBarView scalBarView, PackageSuggestion packageSuggestion,
                              LocalVectorDataSource searchVectorDataSource,
                              LocalVectorDataSource bookmarkDataSource, LocalVectorDataSource routeDataSource, BottomView bottomView, View emptyView, LocationView locationView, RouteView routeView, PackageManager packageRoutingManager) {

        this.mapView = mapView;
        this.compassView = compassView;
        this.scaleBarView = scalBarView;
        this.packageSuggestion = packageSuggestion;
        this.searchVectorDataSource = searchVectorDataSource;
        this.bookmarkDataSource = bookmarkDataSource;
        this.bottomView = bottomView;
        this.emptyView = emptyView;
        this.locationView = locationView;
        this.routeView = routeView;

        this.routeDataSource = routeDataSource;

        handler = new Handler(mapView.getContext().getMainLooper());

        angle = mapView.getMapRotation();

        DisplayMetrics metrics = mapView.getContext().getResources()
                .getDisplayMetrics();
        strokeWidth = MainActivity.strokeWidth;

        projection = mapView.getOptions().getBaseProjection();
        context = mapView.getContext();

        leftBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.left_route);
        rightBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.right_route);
        forwardBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.forward_route);
        startBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.start_route);
        endBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.end_route);

        Bitmap longMarkerBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.long_pin);
        com.carto.graphics.Bitmap markerBitmap = BitmapUtils
                .createBitmapFromAndroidBitmap(longMarkerBitmap);

        markerLongStyleBuilder = new MarkerStyleBuilder();
        markerLongStyleBuilder.setBitmap(markerBitmap);
        markerLongStyleBuilder.setSize(20);
        MarkerStyle longMarkerStyle = markerLongStyleBuilder.buildStyle();

        // add search marker and hide it until user pick up search result
        longClickPin = new Marker(new MapPos(0, 0), longMarkerStyle);
        longClickPin.setVisible(false);

        this.searchVectorDataSource.add(longClickPin);

        Bitmap favMarkerBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.location_bookmark);
        com.carto.graphics.Bitmap favBitmap = BitmapUtils
                .createBitmapFromAndroidBitmap(favMarkerBitmap);

        markerFavoriteStyleBuilder = new MarkerStyleBuilder();
        markerFavoriteStyleBuilder.setBitmap(favBitmap);
        markerFavoriteStyleBuilder.setSize(16);

        locationBookmarkDB = new LocationBookmarksDB(context);
        if (!locationBookmarkDB.isOpen()) {
            locationBookmarkDB.open();
        }

        if (MainActivity.primaryEmail.equals("")) {
            isFirebase = false;
        }

        if (isFirebase) {
            myFirebase = new Firebase(Const.FIREBASE_URL);
        }

        Bitmap searchMarkerBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.location_bookmark);
        bookmarkBitmap = BitmapUtils
                .createBitmapFromAndroidBitmap(searchMarkerBitmap);

        MarkerStyleBuilder markerStyleBuilder2 = new MarkerStyleBuilder();
        markerStyleBuilder2.setBitmap(bookmarkBitmap);

        markerStyleBuilder2.setHideIfOverlapped(false);
        markerStyleBuilder2.setSize(32);

        balloonPopupStyleBuilder = new BalloonPopupStyleBuilder();
        balloonPopupStyleBuilder.setCornerRadius(2);
        balloonPopupStyleBuilder.setTriangleHeight(9);
        balloonPopupStyleBuilder.setRightMargins(new BalloonPopupMargins(10,
                10, 10, 10));
        balloonPopupStyleBuilder.setLeftMargins(new BalloonPopupMargins(10, 10,
                10, 10));
        balloonPopupStyleBuilder.setTitleMargins(new BalloonPopupMargins(10, 10,
                0, 0));
        balloonPopupStyleBuilder.setDescriptionMargins(new BalloonPopupMargins(
                10, 0, 0, 10));
        balloonPopupStyleBuilder.setLeftColor(new Color((short) 15,
                (short) 59, (short) 130, (short) 255));
        balloonPopupStyleBuilder.setTitleColor(new com.carto.graphics.Color(
                (short) 48, (short) 48, (short) 48, (short) 255));
        balloonPopupStyleBuilder.setDescriptionColor(new Color((short) 133,
                (short) 133, (short) 133, (short) 255));
        balloonPopupStyleBuilder.setStrokeColor(new Color((short) 208,
                (short) 208, (short) 208, (short) 108));
        balloonPopupStyleBuilder.setStrokeWidth(strokeWidth);
        balloonPopupStyleBuilder.setPlacementPriority(1);

        refreshFavoriteLocationsOnMap();

        offlineRoutingService = new PackageManagerRoutingService(packageRoutingManager);

        // define layer and datasource for route start and stop markers
        routeDirectionDataSource = new LocalVectorDataSource(projection);
        // Initialize a vector layer with the previous data source
        VectorLayer vectorLayer = new VectorLayer(routeDirectionDataSource);
        // Add the previous vector layer to the map
        mapView.getLayers().add(vectorLayer);
        // Set visible zoom range for the vector layer
        vectorLayer.setVisibleZoomRange(new MapRange(0, 22));

        // create markers for start & end, and a layer for them
        markerStyleBuilder2 = new MarkerStyleBuilder();
        markerStyleBuilder2.setBitmap(BitmapUtils
                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.pin_blue)));
        markerStyleBuilder2.setHideIfOverlapped(false);
        markerStyleBuilder2.setSize(20);

        stopPin = new Marker(new MapPos(0, 0), markerStyleBuilder2.buildStyle());
        stopPin.setVisible(false);
        routeDirectionDataSource.add(stopPin);
        routeDirections.add(stopPin);

        markerStyleBuilder2.setColor(new com.carto.graphics.Color(android.graphics.Color.WHITE));
        markerStyleBuilder2.setBitmap(BitmapUtils
                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.end)));
        markerStyleBuilder2.setSize(24);
        instructionEnd = markerStyleBuilder2.buildStyle();

        markerStyleBuilder2.setOrientationMode(BillboardOrientation.BILLBOARD_ORIENTATION_GROUND);
        markerStyleBuilder2.setAnchorPointX(0);
        markerStyleBuilder2.setAnchorPointY(0);

        markerStyleBuilder2.setBitmap(BitmapUtils
                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.forward)));
        instructionUp = markerStyleBuilder2.buildStyle();

        markerStyleBuilder2.setBitmap(BitmapUtils
                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.left)));
        instructionLeft = markerStyleBuilder2.buildStyle();

        markerStyleBuilder2.setBitmap(BitmapUtils
                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.right)));
        instructionRight = markerStyleBuilder2.buildStyle();
    }

    @Override
    public void onMapMoved() {
        // notify compass only when map rotation is changed
        if (angle != mapView.getMapRotation()) {
            angle = mapView.getMapRotation();
            compassView.notifyCompass();
        }

        // notify scale bar always when map moves, I can exclude x movement but
        // I didn't, not a big improvement :)
        scaleBarView.notifyScaleBar();

        // Suggest package to download
        packageSuggestion.notifyMapMoved();
    }

    @Override
    public void onMapIdle() {
        // store current states for mapView
        SharedPreferences preferences = context.getSharedPreferences(Const.SHARED_PREFS,
                context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("xpos", Double.toString(mapView.getFocusPos().getX()));
        editor.putString("ypos", Double.toString(mapView.getFocusPos().getY()));
        editor.putFloat("tilt", mapView.getTilt());
        editor.putFloat("rotation", mapView.getMapRotation());
        editor.putFloat("zoom", mapView.getZoom());

        // commit to storage
        editor.commit();
    }

    @Override
    public void onMapClicked(final MapClickInfo mapClickInfo) {
        // Remove old click label
        if (oldClickLabel != null) {
            searchVectorDataSource.remove(oldClickLabel);
            oldClickLabel = null;
        }

        if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DOUBLE) {
            mapView.zoom(1.5f, mapClickInfo.getClickPos(), 0.6f);
        } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DUAL) {
            mapView.zoom(-1.5f, 0.6f);
        } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
            longClick(mapClickInfo.getClickPos());
        } else {
            if (routePoints == null) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (bottomView.getVisibility() == View.VISIBLE) {
                            longClickPin.setVisible(false);

                            bottomView.hide();

                            routeView.setVisibility(View.GONE);

                            mapView.getOptions().setFocusPointOffset(new ScreenPos(0, 0));

                            locationView.goDown();

                            emptyView.setVisibility(View.GONE);

                            removeSearchDataSource();
                            searchVectorDataSource.add(longClickPin);
                        }
                    }
                });
            }
        }
    }

    public Marker getLongClickPin() {
        return longClickPin;
    }

    public void removeOldClickLabel(BalloonPopup clickPopup) {
        if (oldClickLabel != null) {
            searchVectorDataSource.remove(oldClickLabel);
            oldClickLabel = null;
        }

        if (clickPopup != null) {
            searchVectorDataSource.add(clickPopup);
            oldClickLabel = clickPopup;
        }
    }

    public void removeSearchDataSource() {
        if (longClickPin != null) {
            searchVectorDataSource.remove(longClickPin);
        }
        if (oldClickLabel != null) {
            searchVectorDataSource.remove(oldClickLabel);
        }
        if (vectorElement != null) {
            searchVectorDataSource.remove(vectorElement);
        }
    }

    public boolean addFavorite(boolean isAddingFavorite) {
        if (isAddingFavorite) {
            return insertLocationBookmarkInDB();
        } else {
            firebaseNodeKey = null;

            int id;

            try {
                id = Integer.parseInt(favoriteID.trim());
                firebaseNodeKey = locationBookmarkDB.getFirebaseNodeKey(id);
            } catch (Exception e) {
                id = -1;
            }

            boolean isDone = locationBookmarkDB
                    .deleteLocationBookmark(id);

            if (isDone && isFirebase) {
                if (firebaseNodeKey != null) {
                    myFirebase.child("favorite").child(MainActivity.primaryEmail).child(firebaseNodeKey).removeValue();
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(
                                context,
                                context.getString(R.string.location_bookmark_delete),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });

                if (selectedVectorElement != null) {
                    bookmarkDataSource.remove(selectedVectorElement);
                }

                return true;
            } else {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(
                                context,
                                context.getString(R.string.bookmark_error2),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                });

                return false;
            }
        }
    }

    public boolean getLongPinVisibility() {
        return longClickPin.isVisible();
    }

    public MapPos getLongClickPinMapPos() {
        return longClickPin.getGeometry().getCenterPos();
    }

    public void setLongClickPin(MapPos mapPos) {
        longClickPin.setPos(mapPos);
        longClickPin.setVisible(true);
    }

    public String getFavoriteID() {
        return favoriteID;
    }

    public void setFavoriteID(String favoriteID) {
        this.favoriteID = favoriteID;
    }

    public void refreshFavoriteLocationsOnMap() {
        bookmarkDataSource.removeAll(bookmarks);
        bookmarks.clear();

        locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();

        pins = new ArrayList<Marker>();

        for (int i = 0; i < locationBookmarks.size(); i++) {
            int red = locationBookmarks.get(i).getRed();
            int green = locationBookmarks.get(i).getGreen();
            int blue = locationBookmarks.get(i).getBlue();

            markerFavoriteStyleBuilder.setColor(new Color(android.graphics.Color.rgb(
                    red, green, blue)));

            Marker marker = new Marker(new MapPos(locationBookmarks.get(i).getLon(),
                    locationBookmarks.get(i).getLat()),
                    markerFavoriteStyleBuilder.buildStyle());

            String location = locationBookmarks.get(i).getDescription();

            marker.setMetaDataElement("title", new Variant(""));
            marker.setMetaDataElement("description", new Variant(location));
            marker.setMetaDataElement("id", new Variant(locationBookmarks.get(i).getId() + ""));

            if (favoriteID != null && favoriteID.equals(locationBookmarks.get(i).getId() + "")) {
                selectedVectorElement = marker;
            }

            bookmarkDataSource.add(marker);
            bookmarks.add(marker);

            pins.add(marker);
        }
    }

    public void selectLocationBookmark(long id) {
        locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();

        for (int i = 0; i < locationBookmarks.size(); i++) {
            if (locationBookmarks.get(i).getId() == id) {
                final MapPos mapPos = new MapPos(locationBookmarks.get(i).getLon(),
                        locationBookmarks.get(i).getLat());

                final String location = locationBookmarks.get(i).getDescription();

                mapView.setFocusPos(mapPos, 1);

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        bottomView.show(location, mapPos, false);
                        locationView.goUp();
                    }
                });

                break;
            }
        }
    }

    public void removeLongClickPin() {
        longClickPin.setVisible(false);
    }

    private boolean insertLocationBookmarkInDB() {
        int red = randInt(0, 255);
        int green = randInt(0, 255);
        int blue = randInt(0, 255);

        String location = bottomView.getDescription();

        double x = bottomView.getLocation().getX();
        double y = bottomView.getLocation().getY();

        String firebaseKey = "";
        Firebase firebasePush = null;

        if (isFirebase) {
            Firebase firebase = myFirebase.child("favorite").child(MainActivity.primaryEmail);
            firebasePush = firebase.push();

            firebaseKey = firebasePush.getKey();
        }

        long id = locationBookmarkDB.insertLocationBookmark(x, y, location, red, green,
                blue, firebaseKey);

        LocationBookmark locationBookmark = new LocationBookmark(id, y, x, location,
                red, green, blue, firebaseKey);

        if (id != -1 && isFirebase && firebasePush != null) {
            firebasePush.setValue(locationBookmark, new Firebase.CompletionListener() {

                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if (firebaseError != null) {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(context,
                                        context.getString(R.string.firebase_saving_error),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });

            markerFavoriteStyleBuilder.setColor(new Color(android.graphics.Color.rgb(
                    red, green, blue)));

            Marker marker = new Marker(bottomView.getLocation(), markerFavoriteStyleBuilder.buildStyle());

            marker.setMetaDataElement("title", new Variant(""));
            marker.setMetaDataElement("description", new Variant(location));
            marker.setMetaDataElement("id", new Variant(id + ""));

            favoriteID = id + "";

            if (oldClickLabel != null) {
                searchVectorDataSource.remove(oldClickLabel);
            }

            if (vectorElement != null) {
                searchVectorDataSource.remove(vectorElement);
            }

            oldClickLabel = null;

            bookmarkDataSource.add(marker);
            bookmarks.add(marker);

            selectedVectorElement = marker;

            longClickPin.setVisible(false);

            handler.post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(
                            context,
                            context.getString(R.string.location_bookmark_added),
                            Toast.LENGTH_SHORT).show();
                }
            });

            return true;
        } else {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(context,
                            context.getString(R.string.bookmark_error),
                            Toast.LENGTH_SHORT).show();
                }
            });

            return false;
        }
    }

    private int randInt(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }

    private void doReverseGeocoding(final MapPos wgs) {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                NominatimService nominatium = new NominatimService();

                try {
                    final String location = nominatium.reverseGeocode(wgs.getX(),
                            wgs.getY());

                    if (location == null) {
                        // check if it's same balloon
                        if (bottomView.getLocation() != null
                                && bottomView.getLocation().equals(tempMapPos)) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    bottomView.setDescription(context
                                            .getString(R.string.location));
                                }
                            });
                        }
                    } else {
                        // check if it's same baloon
                        if (bottomView.getLocation() != null
                                && bottomView.getLocation().equals(tempMapPos)) {
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    bottomView.setDescription(location);
                                }
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void longClick(final MapPos clickPoint) {
        longClickPin.setPos(clickPoint);
        longClickPin.setVisible(true);

        selectedVectorElement = longClickPin;

        tempMapPos = clickPoint;

        handler.post(new Runnable() {

            @Override
            public void run() {
                emptyView.setVisibility(View.VISIBLE);
                bottomView.show("", clickPoint, true);
                locationView.goUp();
            }
        });

        doReverseGeocoding(projection.toWgs84(clickPoint));

        startRouting(false);
    }

    public void setLocation(MapPos mapPos) {
        startPos = mapPos;
    }

    private MapPos startPos = null;
    private MapPos stopPos = null;

    private RoutingService offlineRoutingService;

    private boolean shortestPathRunning;

    private Marker stopPin;
    private MarkerStyle instructionUp;
    private MarkerStyle instructionLeft;
    private MarkerStyle instructionRight;
    private MarkerStyle instructionEnd;
    private LocalVectorDataSource routeDataSource;
    private LocalVectorDataSource routeDirectionDataSource;
    private ArrayList<RouteInstruction> routeInstructions;

    public void startRouting(boolean showErrorMsg) {
        if (startPos == null) {
            if (showErrorMsg) {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.no_start), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } else {
            // set stop and calculate
            if (selectedVectorElement == null) {
                stopPos = bottomView.getLocation();
            } else {
                stopPos = selectedVectorElement.getGeometry().getCenterPos();
            }

            stopPin.setPos(stopPos);

            showRoute(startPos, stopPos);
        }
    }

    public MapPos getStopPinMapPos() {
        return stopPin.getRootGeometry().getCenterPos();
    }

    // when orientation changed
    public void showStopPin(MapPos mapPos) {
        stopPin.setPos(mapPos);
        stopPin.setVisible(true);
    }

    public void clearRoute() {
        if (routeLine != null) {
            routeDataSource.remove(routeLine);
        }
        routeDirectionDataSource.removeAll(routeDirections);
        routeDirections.clear();
    }

    public MapPosVector getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(MapPosVector routePoints) {
        this.routePoints = routePoints;

        routeDataSource.add(createPolyline());
    }

    public void addDirection(RouteInstruction instruction) {
        routeDirectionDataSource.removeAll(routeDirections);
        routeDirections.clear();
        routeDirectionDataSource.add(stopPin);
        routeDirections.add(stopPin);

        MarkerStyle style = null;

        switch (instruction.getBitmapType()) {
            case RouteInstruction.START_BITMAP_TYPE:
                style = instructionUp;

                break;
            case RouteInstruction.LEFT_BITMAP_TYPE:
                style = instructionLeft;

                break;
            case RouteInstruction.RIGHT_BITMAP_TYPE:
                style = instructionRight;

                break;
            case RouteInstruction.FORWARD_BITMAP_TYPE:
                style = instructionUp;

                break;
            case RouteInstruction.END_BITMAP_TYPE:
                style = instructionEnd;

                break;
            default:
                style = instructionUp;
        }

        Marker marker = new Marker(instruction.getLocation(), style);
        if (style != instructionEnd) {
            marker.setRotation(360 - instruction.getAzimuth());
        }

        routeDirectionDataSource.add(marker);
        routeDirections.add(marker);
    }

    private void showRoute(final MapPos startPos, final MapPos stopPos) {
        Log.d(Const.LOG_TAG, "calculating path " + startPos + " to " + stopPos);

        @SuppressLint("StaticFieldLeak")
        AsyncTask<Void, Void, RoutingResult> task = new AsyncTask<Void, Void, RoutingResult>() {

            public long timeStart;

            protected RoutingResult doInBackground(Void... v) {
                timeStart = System.currentTimeMillis();
                MapPosVector poses = new MapPosVector();
                poses.add(startPos);
                poses.add(stopPos);
                RoutingRequest request = new RoutingRequest(projection, poses);
                RoutingResult result;

                try {
                    result = offlineRoutingService.calculateRoute(request);
                } catch (IOException e) {
                    e.printStackTrace();
                    result = null;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    result = null;
                }

                return result;
            }

            protected void onPostExecute(RoutingResult result) {
                if (result == null) {
                    bottomView.setTitle(context.getString(R.string.dropped_pin));

                    shortestPathRunning = false;

                    stopPin.setVisible(false);

                    return;
                }

                if(routeLine!=null) {
                    routeDataSource.remove(routeLine);
                }
                routeDirectionDataSource.removeAll(routeDirections);
                routeDirections.clear();

                longClickPin.setVisible(false);

                stopPin.setVisible(true);

                routeDirectionDataSource.add(stopPin);
                routeDirections.add(stopPin);

                routePoints = result.getPoints();

                routeDataSource.add(createPolyline());

                routeInstructions = new ArrayList<RouteInstruction>();

                // add instruction markers
                RoutingInstructionVector instructions = result.getInstructions();
                boolean first = true;
                for (int i = 0; i < instructions.size(); i++) {
                    RoutingInstruction instruction = instructions.get(i);
                    if (first) {
                        Log.d(Const.LOG_TAG, "first instruction");
                        // set car to first instruction position
                        first = false;
                        MapPos firstInstructionPos = result.getPoints().get(instruction.getPointIndex());

                        // rotate car based on first instruction leg azimuth
                        float azimuth = instruction.getAzimuth();

                        RouteInstruction routeInstruction = new RouteInstruction(context.getString(R.string.my_location), instruction.getDistance(), startBitmap, RouteInstruction.START_BITMAP_TYPE, firstInstructionPos, azimuth);

                        routeInstructions.add(routeInstruction);
                    } else {
                        // Log.d(Const.LOG_TAG, instruction.toString());
                        createRoutePoint(result.getPoints().get(instruction.getPointIndex()), instruction.getStreetName(),
                                instruction.getTime(), instruction.getDistance(), instruction.getAction(), instruction.getAzimuth());
                    }
                }

                bottomView.setTitle(result.getTotalDistance(),
                        " (" + secondsToHours((int) result.getTotalTime()) + ")");

                routeView.show(result.getTotalDistance(), secondsToHours((int) result.getTotalTime()), routeInstructions);

                shortestPathRunning = false;
            }
        };

        if (!shortestPathRunning) {
            shortestPathRunning = true;
            task.execute();
        }
    }

    protected String secondsToHours(int sec) {
        int hours = sec / 3600,
                remainder = sec % 3600,
                minutes = remainder / 60,
                seconds = remainder % 60;

        String result = "";

        if (hours > 0) {
            result += hours + " h";
        }
        if (minutes > 0) {
            if (hours > 0) {
                result += " ";
            }

            result += minutes + " min";
        }
        if (seconds > 0 && hours == 0) {
            if (minutes > 0) {
                result += " ";
            }

            result += " " + seconds + " sec";
        }

        return result;
    }

    private double continueDistance = 0;
    private boolean shouldAddContinueDistance = false;

    private void continueDistance() {
        if (continueDistance > 0) {
            shouldAddContinueDistance = true;
        }
    }

    protected void createRoutePoint(MapPos location, String name,
                                    double time, double distance, RoutingAction action, float azimuth) {

        MarkerStyle style = instructionUp;
        String str = "";
        Bitmap bitmap = forwardBitmap;
        int bitmapType = 0;

        switch (action) {
            case ROUTING_ACTION_HEAD_ON:
                str = "head on";
                bitmap = forwardBitmap;
                style = instructionUp;
                bitmapType = RouteInstruction.FORWARD_BITMAP_TYPE;
                continueDistance();

                break;
            case ROUTING_ACTION_FINISH:
                str = "finish";
                style = instructionEnd;
                bitmap = endBitmap;
                bitmapType = RouteInstruction.END_BITMAP_TYPE;
                continueDistance();

                break;
            case ROUTING_ACTION_TURN_LEFT:
                style = instructionLeft;
                bitmap = leftBitmap;
                str = "turn left";
                bitmapType = RouteInstruction.LEFT_BITMAP_TYPE;
                continueDistance();

                break;
            case ROUTING_ACTION_TURN_RIGHT:
                style = instructionRight;
                bitmap = rightBitmap;
                str = "turn right";
                bitmapType = RouteInstruction.RIGHT_BITMAP_TYPE;
                continueDistance();

                break;
            case ROUTING_ACTION_UTURN:
                str = "u turn";
                continueDistance();

                break;
            case ROUTING_ACTION_NO_TURN:
            case ROUTING_ACTION_GO_STRAIGHT:
//                style = instructionUp;
//                str = "continue";
//                bitmap = forwardBitmap;
//                bitmapType = RouteInstruction.FORWARD_BITMAP_TYPE;
                continueDistance += distance;

                break;
            case ROUTING_ACTION_REACH_VIA_LOCATION:
                style = instructionUp;
                bitmap = forwardBitmap;
                str = "stopover";
                bitmapType = RouteInstruction.FORWARD_BITMAP_TYPE;
                continueDistance();

                break;
            case ROUTING_ACTION_ENTER_AGAINST_ALLOWED_DIRECTION:
                str = "enter against allowed direction";
                continueDistance();

                break;
            case ROUTING_ACTION_LEAVE_AGAINST_ALLOWED_DIRECTION:
                break;
            case ROUTING_ACTION_ENTER_ROUNDABOUT:
                str = "enter roundabout";
                continueDistance();

                break;
            case ROUTING_ACTION_STAY_ON_ROUNDABOUT:
                str = "stay on roundabout";
                continueDistance();

                break;
            case ROUTING_ACTION_LEAVE_ROUNDABOUT:
                str = "leave roundabout";
                continueDistance();

                break;
            case ROUTING_ACTION_START_AT_END_OF_STREET:
                str = "start at end of street";
                continueDistance();

                break;
        }

        if (!str.equals("")) {
            if (!name.equals("")) {
                str += " " + context.getString(R.string.in) + " " + name;
            }

            if (shouldAddContinueDistance) {
                routeInstructions.get(routeInstructions.size() - 1).setDistance(routeInstructions.get(routeInstructions.size() - 1).getDistance() + continueDistance);
                continueDistance = 0;
                shouldAddContinueDistance = false;
            }

            routeInstructions.add(new RouteInstruction(str, distance, bitmap, bitmapType, location, azimuth));
        }
    }

    // creates Nutiteq line from GraphHopper response
    protected Line createPolyline() {
        LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new com.carto.graphics.Color((short) 121, (short) 187, (short) 235, (short) 204));
        lineStyleBuilder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_ROUND);
        lineStyleBuilder.setStretchFactor(3);
        lineStyleBuilder.setWidth(8);

        routeLine = new Line(routePoints, lineStyleBuilder.buildStyle());

        return routeLine;
    }
}
