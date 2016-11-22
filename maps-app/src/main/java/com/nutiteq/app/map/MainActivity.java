package com.nutiteq.app.map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.carto.components.PanningMode;
import com.carto.core.BinaryData;
import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.MapRange;
import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.PackageManagerTileDataSource;
import com.carto.graphics.Color;
import com.carto.layers.TileSubstitutionPolicy;
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.layers.VectorTileRenderOrder;
import com.carto.packagemanager.CartoPackageManager;
import com.carto.packagemanager.PackageInfoVector;
import com.carto.projections.EPSG3857;
import com.carto.styles.LineJoinType;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.ui.MapView;
import com.carto.utils.AssetUtils;
import com.carto.utils.BitmapUtils;
import com.carto.vectorelements.Billboard;
import com.carto.vectorelements.Line;
import com.carto.vectorelements.Marker;
import com.carto.vectorelements.VectorElement;
import com.carto.styles.CompiledStyleSet;
import com.carto.vectortiles.MBVectorTileDecoder;
import com.carto.utils.ZippedAssetPackage;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.flurry.android.FlurryAgent;
import com.nutiteq.app.MapApplication;
import com.nutiteq.app.info.InfoActivity;
import com.nutiteq.app.locationbookmarks.LocationBookmark;
import com.nutiteq.app.locationbookmarks.LocationBookmarksDB;
import com.nutiteq.app.locationbookmarks.LocationBookmarksListActivity;
import com.nutiteq.app.map.hamburger.HamburgerAdapter;
import com.nutiteq.app.map.hamburger.HamburgerMenuGroupClickListener;
import com.nutiteq.app.map.listener.MyMapEventListener;
import com.nutiteq.app.map.listener.MyVectorElementEventListener;
import com.nutiteq.app.nutimap3d.dev.BuildConfig;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.app.search.SearchableActivity;
import com.nutiteq.app.settings.honeycomb.and.newer.SettingsActivity11;
import com.nutiteq.app.settings.froyo.and.gingerbread.SettingsActivity9;
import com.nutiteq.app.utils.Const;
import com.nutiteq.app.utils.LanguageUtils;
import com.nutiteq.nuticomponents.OrientationManager;
import com.nutiteq.nuticomponents.OrientationManager.OnChangedListener;
import com.nutiteq.nuticomponents.PackageSuggestion;
import com.nutiteq.nuticomponents.Utility;
import com.nutiteq.nuticomponents.customviews.AnimatedExpandableListView;
import com.nutiteq.nuticomponents.customviews.BottomView;
import com.nutiteq.nuticomponents.customviews.CompassView;
import com.nutiteq.nuticomponents.customviews.HamburgerMenuView;
import com.nutiteq.nuticomponents.customviews.LocationView;
import com.nutiteq.nuticomponents.customviews.LocationView.LocationButtonClickListener;
import com.nutiteq.nuticomponents.customviews.LocationView.LocationButtonGPSTrackingListener;
import com.nutiteq.nuticomponents.customviews.NutiteqCheckBox;
import com.nutiteq.nuticomponents.customviews.ScaleBarView;
import com.nutiteq.nuticomponents.customviews.SearchHamburgerView;
import com.nutiteq.nuticomponents.location.LocationCircle;
import com.nutiteq.nuticomponents.location.LocationSettingsSupportDialog;
import com.nutiteq.nuticomponents.locationtracking.LocationService;
import com.nutiteq.nuticomponents.locationtracking.LocationTrackingDB;
import com.nutiteq.nuticomponents.locationtracking.TrackData;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadListActivity;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerApplicationInterface;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Milan Ivankovic
 *         <p/>
 *         I use FragmentActivity otherwise I can't show DialogFragment which
 *         ask users to open GPS settings on device if they are disabled.
 */

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements OnChangedListener {

    // flag to know is app live
    public static boolean isLive = false;
    public static boolean shouldRefreshFavoriteLocations = false;
    public static long locationBookmarkId = -1;
    public static String primaryEmail = "";

    // use for preferences
    private static String measurementUnit = "";
    private static String mapLanguage = "";
    private static boolean shouldUpdateBaseLayer = false;
    public static boolean isNotDarkStyle = false;
    private static boolean buildings3D = false;

    private VectorTileLayer baseLayer;
    public MapView mapView;
    private EPSG3857 projection = new EPSG3857();
    private MBVectorTileDecoder vectorTileDecoder;
    private String vectorStyleLang = Const.MAP_LANGUAGE_AUTOMATIC; // default
    private Marker searchMarker;
    private LocalVectorDataSource searchVectorDataSource;
    private boolean shouldSearchMarkerFocus = false;
    private LocationView locationView;
    private LocationCircle locationCircle;
    private MapPos lastGPSlocation;
    private Boolean isMyPlaceShown = false; // flag for first GPS fix and focus
    private MyMapEventListener myMapEventListener;
    private MyVectorElementEventListener myVectorElementEventListener;
    private OrientationManager mOrientationManager;
    private CartoPackageManager packageManager;
    private CartoPackageManager packageManagerRouting;

    public  DrawerLayout hamburgerMenuLayout;
    public AnimatedExpandableListView hamburgerMenuList;

    private boolean isHamburgerMenuActivated;
    private HamburgerAdapter hamburgerAdapter;

    // flag, is map fixed on focus point
    private boolean isFixListener = false;
    private ScaleBarView scaleBarView;
    private PackageSuggestion packageSuggestion;
    private boolean isHamburgerClosed = true;

    public MainActivity mainActivity;

    // for package suggestions
    private File nutimapFolder = new File(Environment.getExternalStorageDirectory() + "/nutimaps/");
    private ArrayList<String> oldPackages = new ArrayList<String>();

    // private boolean isPackageListDownloaded = false;
    private boolean isFirstMsgCanceled = false;
    private LocationTrackingDB locationTrackingDB;
    private LocalVectorDataSource gpsTracksVectorDataSource;
    private Line lineLocationTrack;
    private LineStyleBuilder lineStyleBuilder;
    private MapPosVector linePoses;
    private LocalVectorDataSource bookmarkDataSource;
    private LocationManager locationManager;
    private SharedPreferences prefs;
    private LocationBookmarksDB locationBookmarkDB;
    public boolean isBrightStyle = true;

    // private static final String PANEL_OPEN = "panel_open";
    public SharedPreferences preferences;
    private LocalVectorDataSource routeDataSource;
    private VectorLayer gpsTrackingVectorLayer;
    private VectorLayer searchVectorLayer;
    private VectorLayer bookmarkVectorLayer;
    private VectorLayer routeVectorLayer;
    private boolean isNYC = true;
    private LocalVectorDataSource nycVectorDataSource;
    private VectorLayer nycVectorLayer;
    private BottomView bottomView;
    private RouteView routeView;
    private LocalVectorDataSource locationDataSource;
    private VectorLayer locationAnimationLayer;

    // to know if app need to disable GPS if app is not on home screen. if value
    // is true in onPause I disable GPS and enable it in onResume
    private boolean isFromMainActivity = true;

    private DisplayMetrics displayMetrics;

    private int selectedItem = -1;

    private boolean hasRouteInstructions = false;

    // for ballons popup, it's used on many places
    public static int strokeWidth;

    public static boolean isSupportedBySDK(String defaultLang) {
        if (defaultLang.equals(Const.MAP_LANGUAGE_ENGLISH)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_FRENCH)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_GERMAN)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_RUSSIAN)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_CHINESE)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_SPANISH)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_ITALIAN)) {
            return true;
        }

        if (defaultLang.equals(Const.MAP_LANGUAGE_ESTONIAN)) {
            return true;
        }

        return false;
    }

    public static void setMeasurementUnit(String s) {
        measurementUnit = s;
    }

    public static void setMapLanguage(String s) {
        mapLanguage = s;
    }

    public static void setShouldUpdateBaseLayer(boolean b) {
        shouldUpdateBaseLayer = b;
    }

    public static void setIsNotDarkStyle(boolean b) {
        isNotDarkStyle = b;
    }

    public static void setBuildings3D(boolean b) {
        buildings3D = b;
    }

    private void setHamburgerMenu() {
        String version = "";

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String versionCode = "" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

            version = getString(R.string.version) + " " + versionName + " " + versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // creating hamburger menu and sub menu
        List<GroupItem> items = new ArrayList<GroupItem>();

        GroupItem item = new GroupItem();
        item.image = getResources().getDrawable(R.drawable.logo);
        item.title = getString(R.string.app_name) + " " + version;
        items.add(item);

        item = new GroupItem();
        item.image = getResources().getDrawable(R.drawable.search_marker);
        item.title = getString(R.string.menu_search);
        items.add(item);

        item = new GroupItem();
        item.image = getResources().getDrawable(R.drawable.download_menu);
        item.imageHover = getResources().getDrawable(R.drawable.download_menu_hover);
        item.title = getString(R.string.download);
        items.add(item);

        item = new GroupItem();
        item.image = getResources().getDrawable(R.drawable.bookmarks_menu);
        item.imageHover = getResources().getDrawable(R.drawable.bookmarks_menu_hover);
        item.title = getString(R.string.bookmarks_menu);
        items.add(item);

        item = new GroupItem();
        item.image = getResources().getDrawable(R.drawable.tracks_menu);
        item.imageHover = getResources().getDrawable(R.drawable.tracks_menu_hover);
        item.title = getString(R.string.tracks);
        // items.add(item);

        item = new GroupItem();
        item.title = getString(R.string.bright_style);
        items.add(item);

        item = new GroupItem();
        item.title = getString(R.string.settings);
        items.add(item);

        item = new GroupItem();
        item.title = getString(R.string.about);
        items.add(item);

        hamburgerAdapter = new HamburgerAdapter(this);
        hamburgerAdapter.setData(items);

        hamburgerMenuList.setAdapter(hamburgerAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isLive = true;

        locationBookmarkDB = new LocationBookmarksDB(this);
        if (!locationBookmarkDB.isOpen()) {
            locationBookmarkDB.open();
        }

        displayMetrics = getResources().getDisplayMetrics();

        strokeWidth = (int) (displayMetrics.density * 1.0f);

        //http://stackoverflow.com/questions/2112965/how-to-get-the-android-devices-primary-e-mail-address
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                primaryEmail = account.name;
                break;
            }
        }

        primaryEmail.toLowerCase();

        // firebase doesn't accept this symbol
        primaryEmail = primaryEmail.replace(".", "D");

        // disable firebase save/share favourites if API can't get primary email
        if (!primaryEmail.equals("")) {
            // Get a reference to our posts
            Firebase firebase = new Firebase(Const.FIREBASE_URL);
            Firebase myFirebase = firebase.child("favorite").child(primaryEmail);

            if (myFirebase != null) {
                // Attach an listener to read the data
                myFirebase.addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            boolean shouldRefresh = false;

                            ArrayList<LocationBookmark> locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();
                            for (int i = 0; i < locationBookmarks.size(); i++) {
                                if (!snapshot.child(locationBookmarks.get(i).firebaseNodeKey()).exists()) {
                                    if (locationBookmarkDB.deleteLocationBookmark(locationBookmarks.get(i).getId())) {
                                        shouldRefresh = true;
                                    }
                                }
                            }

                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                LocationBookmark locationBookmark = new LocationBookmark(
                                        (long) dataSnapshot.child("id").getValue(),
                                        (double) dataSnapshot.child("lat").getValue(),
                                        (double) dataSnapshot.child("lon").getValue(),
                                        (String) dataSnapshot.child("description").getValue(),
                                        (int) (long) dataSnapshot.child("red").getValue(),
                                        (int) (long) dataSnapshot.child("green").getValue(),
                                        (int) (long) dataSnapshot.child("blue").getValue(),
                                        dataSnapshot.getKey());

                                LocationBookmark locationBookmark2 = locationBookmarkDB.getLocationBookmark(locationBookmark.getId());

                                if (locationBookmark2 == null) {
                                    long l = locationBookmarkDB.insertLocationBookmark(locationBookmark);

                                    if (l != -1) {
                                        shouldRefresh = true;
                                    }

                                } else if (!locationBookmark.getDescription().equals(locationBookmark2.getDescription())) {
                                    if (locationBookmarkDB.updateLocationBookmarkDescription(
                                            locationBookmark.getId(), locationBookmark.getDescription())) {
                                        shouldRefresh = true;
                                    }
                                }
                            }

                            if (shouldRefresh) {
                                myMapEventListener.refreshFavoriteLocationsOnMap();
                            }
                        } else {
                            if (locationBookmarkDB.deleteAllLocationBookmark()) {
                                myMapEventListener.refreshFavoriteLocationsOnMap();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });
            }
        }

        preferences = getSharedPreferences(Const.SHARED_PREFS, MODE_PRIVATE);

        boolean isFirstStart = preferences.getBoolean("isfirststart", true);
        boolean isFirstRoutingStart = preferences.getBoolean("isfirstroutingstart", true);

        isBrightStyle = preferences.getBoolean("isbrightstyle", true);
        isNYC = preferences.getBoolean("isnyc", false);

        // for hamburger menu which needs API 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setContentView(R.layout.activity_main_api_14);

            isHamburgerMenuActivated = true;

            hamburgerMenuList = (AnimatedExpandableListView) findViewById(R.id.hamburger_list);

            setHamburgerMenu();

            hamburgerMenuLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

            if (displayMetrics.heightPixels > displayMetrics.widthPixels) {
                hamburgerMenuList.getLayoutParams().width = (int) (displayMetrics.widthPixels * 0.85f);
            } else {
                hamburgerMenuList.getLayoutParams().width = (int) (displayMetrics.heightPixels * 0.85f);
            }

            // In order to show animations, we need to use a custom click
            // handler for our ExpandableListView.
            hamburgerMenuList.setOnGroupClickListener(new HamburgerMenuGroupClickListener(this));

            hamburgerMenuList.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    float density = displayMetrics.density;
                    int y = (int) event.getY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            if (y > (int) (density * (128f + 64f)) && y < (int) (density * (128f + 64f + 2 * 48f))) {
                                selectedItem = (y - (int) (density * (128f + 64f))) / (int) (density * 48f);
                            }

                            break;
                        case MotionEvent.ACTION_MOVE:
                            y = (int) event.getY();

                            if (selectedItem != -1) {
                                if (selectedItem != (y - (int) (density * (128f + 64f))) / (int) (density * 48f)) {
                                    selectedItem = -1;
                                    hamburgerAdapter.notifyDataSetChanged();
                                }
                            }

                            break;
                    }

                    return false;
                }
            });

            hamburgerMenuLayout
                    .setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                        @Override
                        public void onDrawerSlide(View drawerView,
                                                  float slideOffset) {
                            // Note: this is a workaround for Android 4.0
                            // devices, that
                            // have issues with DrawerLayout and GLSurfaceView
                            findViewById(R.id.empty_view).setVisibility(
                                    slideOffset > 0 ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onDrawerOpened(View drawerView) {
                            // Note: this is a workaround for Android 4.0
                            // devices, that
                            // have issues with DrawerLayout and GLSurfaceView
                            findViewById(R.id.empty_view).setVisibility(
                                    View.VISIBLE);

                            isHamburgerClosed = false;
                            invalidateOptionsMenu(); // creates call to
                            // onPrepareOptionsMenu()
                        }

                        @Override
                        public void onDrawerClosed(View drawerView) {
                            // Note: this is a workaround for Android 4.0
                            // devices, that
                            // have issues with DrawerLayout and GLSurfaceView
                            // See:
                            // http://stackoverflow.com/questions/23691012/drawerlayout-listview-not-drawn-with-glsurfaceview-as-content
                            findViewById(R.id.empty_view).setVisibility(
                                    View.GONE);

                            if (isHamburgerMenuActivated) {
                                hideKeyboard();
                            }

                            isHamburgerClosed = true;
                            invalidateOptionsMenu(); // creates call to
                            // onPrepareOptionsMenu()
                        }

                        @Override
                        public void onDrawerStateChanged(int newState) {

                        }
                    });

            HamburgerMenuView hamburgerMenuView = (HamburgerMenuView) findViewById(R.id.hamburger_menu);
            hamburgerMenuView.setHamburgerMenu(hamburgerMenuLayout,
                    hamburgerMenuList);
        } else {
            setContentView(R.layout.activity_main_api_9);
            isHamburgerMenuActivated = false;
        }

        locationView = (LocationView) findViewById(R.id.gps_button);

        gpsTracksVectorDataSource = new LocalVectorDataSource(projection);

        ((MapApplication) getApplication()).setReferences(this,
                gpsTracksVectorDataSource);

        locationTrackingDB = ((MapApplication) getApplication()).getLocationTrackingDB();

        // get default preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        measurementUnit = prefs.getString(Const.PREF_UNIT_KEY, Const.METRIC);
        String storageType = prefs.getString(Const.PREF_STORAGE_KEY, Const.EXTERNAL);
        vectorStyleLang = prefs.getString(Const.PREF_LANG_KEY, Const.LANG_AUTOMATIC);
        isNotDarkStyle = !prefs.getBoolean(Const.PREF_BRIGHT_STYLE, true);
        buildings3D = prefs.getBoolean(Const.PREF_BUILDINGS_3D_KEY, false);

        vectorStyleLang = LanguageUtils.getLanguage(vectorStyleLang);

        mapView = (MapView) this.findViewById(R.id.map_view);

        mapView.getOptions().setBaseProjection(projection);
        mapView.getOptions().setTileThreadPoolSize(2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            isNotDarkStyle = !isBrightStyle;
        }

        // Get package managers
        packageManager = ((MapApplication) getApplication()).getPackageManagerComponent().getPackageManager();
        packageManagerRouting = ((MapApplication) getApplication()).getPackageManagerComponent().getRoutingPackageManager();

        if (isFirstStart) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isfirststart", false);
            editor.commit();

            packageManager.startPackageListDownload();
        }

        if (isFirstRoutingStart) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isfirstroutingstart", false);
            editor.commit();

            packageManagerRouting.startPackageListDownload();
        }

        boolean isMapReady;

        try {
            // Check if initial package is imported
            boolean imported = false;
            PackageInfoVector packages = packageManager.getLocalPackages();
            for (int i = 0; i < packages.size(); i++) {
                if (packages.get(i).getPackageId()
                        .equals(Const.BASE_PACKAGE_ID)) {
                    imported = true;
                    break;
                }
            }

            if (!imported) {
                Log.i(Const.LOG_TAG, "Importing initial package");

                boolean isExternalStorageAvailable = !((MapApplication) getApplication())
                        .getPackageManagerComponent().isSetToDefaultInternal();

                File localDir;
                if (isExternalStorageAvailable) {
                    if (storageType.equals(Const.INTERNAL)) {
                        localDir = getFilesDir();
                    } else {
                        if (storageType.equals(Const.EXTERNAL)) {
                            localDir = getExternalFilesDir(null);
                        } else {
                            File[] files = getExternalFilesDirs(null);
                            localDir = files[Integer.parseInt(storageType
                                    .substring(storageType.indexOf(" ") + 1)) - 1];
                        }
                    }
                } else {
                    localDir = getFilesDir();
                }

                // it can be null, when SD card is mounted on PC or not
                // available, so check is must otherwise it will crash in this
                // situation
                if (localDir != null) {
                    if (isExternalStorageAvailable) {
                        if (storageType.equals(Const.INTERNAL)) {
                            // it will return false if there is no space or
                            // isn't
                            // available
                            isMapReady = Utility.copyAssetToInternalMemory(
                                    getAssets(), Const.BASE_PACKAGE_ASSET_NAME,
                                    localDir.getAbsolutePath());
                        } else {
                            // it will return false if there is no space or
                            // isn't
                            // available
                            isMapReady = Utility.copyAssetToSDCard(getAssets(),
                                    Const.BASE_PACKAGE_ASSET_NAME,
                                    localDir.getAbsolutePath());
                        }
                    } else {
                        // it will return false if there is no space or isn't
                        // available
                        isMapReady = Utility.copyAssetToInternalMemory(
                                getAssets(), Const.BASE_PACKAGE_ASSET_NAME,
                                localDir.getAbsolutePath());
                    }
                    if (isMapReady) {
                        isMapReady = packageManager.startPackageImport(
                                Const.BASE_PACKAGE_ID, 1, new File(localDir,
                                        Const.BASE_PACKAGE_ASSET_NAME)
                                        .getAbsolutePath());
                    }
                    // TODO: basepkg.mbtiles can be removed once import is
                    // complete
                    // (listener)
                    // nole: from app user interface it can't be removed, it can
                    // be delete with file manager but than above code will
                    // again import basepkg or maybe I missed something? :)
                } else {
                    isMapReady = false;
                }
            } else {
                isMapReady = true;
            }
        } catch (IOException e) {
            isMapReady = false;

            Log.e(Const.LOG_TAG, "mbTileFile isn't ready for use: " + Const.BASE_PACKAGE_ASSET_NAME);
        }

        if (isMapReady) {
            updateBaseLayer();

            double x = Double.parseDouble(preferences.getString("xpos", "0.0"));
            double y = Double.parseDouble(preferences.getString("ypos", "0.0"));
            float r = preferences.getFloat("rotation", 0.0f);
            float z = preferences.getFloat("zoom", 2.0f);
            float t = preferences.getFloat("tilt", 90.0f);

            // set last mapView states or default if it's a first start
            mapView.setFocusPos(new MapPos(x, y), 0);
            mapView.setZoom(z, 0);
            mapView.setMapRotation(r, 0);
            mapView.setTilt(t, 0);

            mapView.getOptions().setZoomRange(new MapRange(Const.MAP_ZOOM_MIN, Const.MAP_ZOOM_MAX));
            mapView.getOptions().setPanningMode(PanningMode.PANNING_MODE_STICKY_FINAL);
        } else {
            Toast.makeText(this, getString(R.string.map_error), Toast.LENGTH_LONG).show();
        }

        packageSuggestion = new PackageSuggestion(this, mapView, baseLayer, packageManager, packageManagerRouting,
                (Button) findViewById(R.id.suggestion_button));

        locationView
                .setLocationButtonClickListener(new LocationButtonClickListener() {
                    @Override
                    public Boolean onLocationStateOff() {
                        // nothing to do if GPS is already activated
                        if (!isMyPlaceShown) {
                            // check GPS settings and if it's disabled allow
                            // users to
                            // enable GPS settings on device
                            if (locationManager
                                    .isProviderEnabled(LocationManager.GPS_PROVIDER)
                                    || locationManager
                                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                                startGPS(isMyPlaceShown);

                                return true;
                            } else {
                                LocationSettingsSupportDialog gpsDialog = new LocationSettingsSupportDialog();
                                gpsDialog.show(getSupportFragmentManager(),
                                        "GPSFragmentDialog");

                                return false;
                            }
                        }

                        return true;
                    }

                    @Override
                    public Boolean onLocationStateOn() {
                        if (locationCircle.isFirstLocationSet()) {
                            // fix GPS to center of mapView
                            mapView.setFocusPos(lastGPSlocation, 1);
                            // flag touch listener so when user touch screen map
                            // looses focus point
                            isFixListener = true;

                            return true;
                        } else {
                            if (!LocationService.isLocationTrackingOn) {
                                locationView
                                        .setState(LocationView.LOCATION_STATE_OFF);

                                isMyPlaceShown = false;

                                stopGPS();

                                return false;
                            }

                            return false;
                        }
                    }

                    @Override
                    public Boolean onLocationStateFix() {
                        // exclude turn option if there isn't compass sensors
                        if (!mOrientationManager.hasCompassSensors()) {
                            locationView
                                    .setState(LocationView.LOCATION_STATE_OFF);

                            isFixListener = false;

                            isMyPlaceShown = false;

                            stopGPS();

                            return false;
                        }

                        return true;
                    }

                    @Override
                    public Boolean onLocationStateTurn() {
                        // listener is only needed for state 2 and 3
                        isFixListener = false;

                        isMyPlaceShown = false;

                        if (!LocationService.isLocationTrackingOn) {
                            stopGPS();
                        } else {
                            locationView.setState(LocationView.LOCATION_STATE_OFF);
                        }

                        return true;
                    }
                });

        locationView
                .setLocationButtonGPSTrackingListener(new LocationButtonGPSTrackingListener() {

                    @Override
                    public Boolean onTrackingOn() {
                        // check GPS settings and if it's disabled allow
                        // users to enable GPS settings on device
                        if (locationManager
                                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            if (!isMyPlaceShown) {
                                startGPS(isMyPlaceShown);
                                locationView.setState(LocationView.LOCATION_STATE_ON);
                            }

                            Intent intent = new Intent(getApplicationContext(),
                                    LocationService.class);

                            intent.putExtra(LocationService.START_TRACKING_FLAG, true);

                            startService(intent);

                            return true;
                        } else {
                            LocationSettingsSupportDialog gpsDialog = new LocationSettingsSupportDialog();
                            gpsDialog.show(getSupportFragmentManager(),
                                    "GPSFragmentDialog");

                            return false;
                        }
                    }

                    @Override
                    public Boolean onTrackingOff() {
                        Intent intent = new Intent(getApplicationContext(),
                                LocationService.class);

                        intent.putExtra(LocationService.STOP_TRACKING_FLAG, true);

                        startService(intent);

                        return true;
                    }
                });

        mapView.setOnTouchListener(new OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (isHamburgerMenuActivated) {
                    hideKeyboard();
                }

                // when map is in fix or turn state and user touch a screen I
                // must return to GPS ON state and set flag
                if (isFixListener) {
                    locationView.setState(LocationView.LOCATION_STATE_ON);
                    isFixListener = false;
                }

                return false;
            }
        });

        init();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {

        }
    }

    public void setLocation(Location location) {
        if (location.hasAccuracy()) {
            locationView.setIsFirstLocationFound();

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy();

            MapPos mapPos = new MapPos(latitude, longitude);
            mapPos = projection.fromWgs84(mapPos);

            myMapEventListener.setLocation(mapPos);

            lastGPSlocation = projection.fromWgs84(new MapPos(latitude, longitude));

            if (locationView.getState() == LocationView.LOCATION_STATE_ON && !isMyPlaceShown) {
                isMyPlaceShown = true;
                mapView.setFocusPos(lastGPSlocation, 1);

                locationView.setState(LocationView.LOCATION_STATE_FIX);
                isFixListener = true;
            } else if (locationView.getState() == LocationView.LOCATION_STATE_FIX
                    || locationView.getState() == LocationView.LOCATION_STATE_TURN) {
                mapView.setFocusPos(lastGPSlocation, 0.4f);
            }

            // set location and accuracy, so it can draw polygon circle
            // animation with accuracy radius if it can be seen on map

            if (locationView.isTrackingOn()) {
                locationCircle.setLocation(longitude, latitude, accuracy, false);
            } else {
                locationCircle.setLocation(longitude, latitude, accuracy, true);
            }
        }
    }

    public void stopLocationTracking() {
        locationView.setIsLongClickOn(false);
    }

    private void showPackageSuggestionDialog() {

        File[] files = nutimapFolder.listFiles();
        int index;

        Arrays.sort(files, new CustomComparator());

        for (int i = 0; i < files.length; i++) {
            index = Arrays.binarySearch(Const.oldPackageMap, files[i].getName());

            if (index >= 0 && !Const.newPackageMap[i].equals("-")) {
                if (!oldPackages.contains(Const.newPackageMap[index])) {
                    oldPackages.add(Const.newPackageMap[index]);
                }
            }
        }

    }

    public void updateBaseLayer() {
        String styleAssetName;

        if (isNotDarkStyle) {
            styleAssetName = Const.MAP_STYLE_GREY;
        } else {
            styleAssetName = Const.MAP_STYLE_BRIGHT;
        }

        BinaryData binaryData = AssetUtils.loadAsset(Const.MAP_STYLE_FILE);

        if (binaryData != null) {
            // Create style set
            CompiledStyleSet compiledStyleSet = new CompiledStyleSet(new ZippedAssetPackage(binaryData), styleAssetName);
            vectorTileDecoder = new MBVectorTileDecoder(compiledStyleSet);

            // get device default language
            String defaultLang = Locale.getDefault().getLanguage();
            String lang = vectorStyleLang;

            if (lang.equals(Const.MAP_LANGUAGE_AUTOMATIC)) {
                // if it's supported by SDK use it, otherwise use English as
                // default
                if (isSupportedBySDK(defaultLang)) {
                    lang = defaultLang;
                } else {
                    lang = Const.MAP_LANGUAGE_LOCAL;
                }
            }

            // Set language, language-specific texts from vector tiles will
            // be used
            vectorTileDecoder.setStyleParameter("lang", lang);

            vectorTileDecoder.setStyleParameter("buildings3d", buildings3D + "");
            vectorTileDecoder.setStyleParameter("texts3d", "1");
            vectorTileDecoder.setStyleParameter("markers3d", "1");

            // Create tile data source for vector tiles
            PackageManagerTileDataSource vectorTileDataSource = new PackageManagerTileDataSource(
                    ((MapApplication) getApplication()).getPackageManagerComponent().getPackageManager());

            if (nycVectorLayer != null) {
                mapView.getLayers().remove(nycVectorLayer);
            }

            if (gpsTrackingVectorLayer != null) {
                mapView.getLayers().remove(gpsTrackingVectorLayer);
            }

            if (searchVectorLayer != null) {
                mapView.getLayers().remove(searchVectorLayer);
            }

            if (bookmarkVectorLayer != null) {
                mapView.getLayers().remove(bookmarkVectorLayer);
            }

            if (routeVectorLayer != null) {
                mapView.getLayers().remove(routeVectorLayer);
            }

            if (locationAnimationLayer != null) {
                mapView.getLayers().remove(locationAnimationLayer);
            }

            if (baseLayer != null) {
                mapView.getLayers().remove(baseLayer);
            }

            baseLayer = new VectorTileLayer(vectorTileDataSource, vectorTileDecoder);
            baseLayer.setTileCacheCapacity(Const.TILE_CACHE_SIZE);
            baseLayer.setTileSubstitutionPolicy(TileSubstitutionPolicy.TILE_SUBSTITUTION_POLICY_VISIBLE);

            // show base map labels on top of other layers
            baseLayer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST);
            mapView.getLayers().add(baseLayer);

            if (locationAnimationLayer != null) {
                mapView.getLayers().add(locationAnimationLayer);
            }

            if (nycVectorLayer != null) {
                mapView.getLayers().add(nycVectorLayer);
            }

            if (gpsTrackingVectorLayer != null) {
                mapView.getLayers().add(gpsTrackingVectorLayer);
            }

            if (searchVectorLayer != null) {
                mapView.getLayers().add(searchVectorLayer);
            }

            if (bookmarkVectorLayer != null) {
                mapView.getLayers().add(bookmarkVectorLayer);
            }

            if (routeVectorLayer != null) {
                mapView.getLayers().add(routeVectorLayer);
            }
        } else {
            Log.e(Const.LOG_TAG, "map style file must be in project assets");
        }
    }

    private void init() {
        searchVectorDataSource = new LocalVectorDataSource(projection);
        bookmarkDataSource = new LocalVectorDataSource(projection);

        bottomView = (BottomView) findViewById(R.id.bottom_view);

        // set mapView reference to compass, so it can rotate map when you click
        // on it, gpsButton is also need to disable turn mode when user click on
        // compass
        CompassView compassView = (CompassView) findViewById(R.id.compass_view);
        compassView.setObjects(mapView, locationView);

        // set mapView reference to scale bar, so it can draw itself
        scaleBarView = (ScaleBarView) findViewById(R.id.scale_bar_view);
        scaleBarView.setMapView(mapView);

        routeView = (RouteView) findViewById(R.id.route_view);

        // Create and set a map event listener, it needs the data source for
        // balloons and compass to listen map rotation
        myMapEventListener = new MyMapEventListener(
                mapView, compassView, scaleBarView, packageSuggestion, searchVectorDataSource,
                bookmarkDataSource, routeDataSource, bottomView, findViewById(R.id.empty_view),
                locationView, routeView, ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getRoutingPackageManager());

        mapView.setMapEventListener(myMapEventListener);

        myVectorElementEventListener = new MyVectorElementEventListener(
                mapView, myMapEventListener, myMapEventListener.getLongClickPin(), bottomView,
                findViewById(R.id.empty_view), locationView, routeView);

        routeView.setMapView(mapView, myMapEventListener);

        lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new Color(0xFF00b483));// nutiteq

        // green
        // :)
        lineStyleBuilder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_ROUND);
        lineStyleBuilder.setStretchFactor(3);
        lineStyleBuilder.setWidth(7);

        // 1. Initialize a local vector data source
        nycVectorDataSource = new LocalVectorDataSource(projection);

        // Initialize a vector layer with the previous data source
        nycVectorLayer = new VectorLayer(nycVectorDataSource);

        // Add the previous vector layer to the map
        mapView.getLayers().add(nycVectorLayer);

        // Set visible zoom range for the vector layer
        nycVectorLayer.setVisibleZoomRange(new MapRange(0, 22));

        // add layers for GPS tracks
        gpsTrackingVectorLayer = new VectorLayer(gpsTracksVectorDataSource);
        mapView.getLayers().add(gpsTrackingVectorLayer);

        searchVectorLayer = new VectorLayer(searchVectorDataSource);
        searchVectorLayer.setVectorElementEventListener(myVectorElementEventListener);
        mapView.getLayers().add(searchVectorLayer);

        bookmarkVectorLayer = new VectorLayer(bookmarkDataSource);
        bookmarkVectorLayer.setVectorElementEventListener(myVectorElementEventListener);
        mapView.getLayers().add(bookmarkVectorLayer);

        routeDataSource = new LocalVectorDataSource(projection);
        routeVectorLayer = new VectorLayer(routeDataSource);
        mapView.getLayers().add(routeVectorLayer);

        Bitmap searchMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.search_marker);
        com.carto.graphics.Bitmap markerBitmap = BitmapUtils.createBitmapFromAndroidBitmap(searchMarkerBitmap);

        // Create marker style
        MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
        markerStyleBuilder.setBitmap(markerBitmap);
        markerStyleBuilder.setSize(36);
        MarkerStyle searchMarkerStyle = markerStyleBuilder.buildStyle();

        // add search marker and hide it until user pick up search result
        searchMarker = new Marker(new MapPos(0, 0), searchMarkerStyle);
        searchMarker.setVisible(false);
        searchVectorDataSource.add(searchMarker);

        bottomView.setOnDroppedPinViewClickListener(new BottomView.OnDroppedPinViewClickListener() {

            @Override
            public Boolean onLeftButtonClick() {
                return myMapEventListener.addFavorite(bottomView.getIsAddingFavoriteState());
            }

            @Override
            public Boolean onRightButtonClick() {
                if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
                    locationView.setState(LocationView.LOCATION_STATE_ON);
                    isFixListener = false;
                    isMyPlaceShown = true;
                }

                routeView.goUp();

                if (!hasRouteInstructions) {
                    myMapEventListener.startRouting(true);
                }

                return true;
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mOrientationManager = new OrientationManager(sensorManager,
                ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay());
        mOrientationManager.addOnChangedListener(this);
    }

    @Override
    public void onRestoreInstanceState(final Bundle bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(bundle);

        int state = bundle.getInt("GPS_STATE");
        final boolean isPlaceShown = bundle.getBoolean("isMyPlaceShown");

        mapView.setMapRotation(bundle.getFloat(Const.ROTATION), 0);
        mapView.setZoom(bundle.getFloat(Const.ZOOM), 0);
        mapView.setTilt(bundle.getFloat(Const.TILT), 0);
        mapView.setFocusPos(new MapPos(bundle.getDouble(Const.FOCUS_X), bundle.getDouble(Const.FOCUS_Y)), 0);

        searchMarker.setPos(new MapPos(bundle.getDouble(Const.SEARCH_X), bundle.getDouble(Const.SEARCH_Y)));

        if (bundle.getString(Const.SEARCH_TITLE) != null) {
            searchMarker.setMetaDataElement("title", new Variant(bundle.getString(Const.SEARCH_TITLE)));
        }
        if (bundle.getString(Const.SEARCH_DESC) != null) {
            searchMarker.setMetaDataElement("description", new Variant(bundle.getString(Const.SEARCH_DESC)));
        }

        shouldSearchMarkerFocus = bundle.getBoolean(Const.SEARCH_FOCUS);
        isHamburgerClosed = bundle.getBoolean(Const.IS_HAMBURGER);

        long l = bundle.getLong(Const.LOCATIONS_SIZE);

        if (l > 0) {
            linePoses = new MapPosVector();

            for (int i = 0; i < l; i++) {
                linePoses.add(new MapPos(bundle.getDouble(Const.LOCATION_X + i), bundle.getDouble(Const.LOCATION_Y + i)));
            }

            lineLocationTrack = new Line(linePoses, lineStyleBuilder.buildStyle());
            gpsTracksVectorDataSource.add(lineLocationTrack);
        }

        // add recorded line on map
        long l2 = bundle.getLong(Const.LOCATIONS_SIZE2);

        if (l2 > 0) {
            MapPosVector poses = new MapPosVector();

            for (int i = 0; i < l2; i++) {
                poses.add(new MapPos(bundle.getDouble(Const.LOCATION_X2 + i), bundle.getDouble(Const.LOCATION_Y2 + i)));
            }

            locationTrackingDB.addLine(poses);
        }

        if (state != LocationView.LOCATION_STATE_OFF) {

            locationView.setState(state);
            // it is off because activity is destroyed
            startGPS(isPlaceShown);

            locationView.refresh();

            if (state == LocationView.LOCATION_STATE_FIX || state == LocationView.LOCATION_STATE_TURN) {
                isFixListener = true;
            }
        }

        if (bundle.getBoolean(Const.DROPPED_VIEW_VISIBLE)) {
            new Handler(getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                    bottomView.show(
                            bundle.getString(Const.DROPPED_VIEW_TITLE),
                            bundle.getString(Const.DROPPED_VIEW_DESCRIPTION),
                            new MapPos(bundle.getDouble(Const.DROPPED_VIEW_X),
                            bundle.getDouble(Const.DROPPED_VIEW_Y)),
                            bundle.getBoolean(Const.DROPPED_VIEW_FAVORITE));
                    locationView.goUp();
                }
            });

            myMapEventListener.setFavoriteID(bundle.getString(Const.FAVORITE_ID));
            myMapEventListener.refreshFavoriteLocationsOnMap();
        }

        if (bundle.getBoolean(Const.LONG_PIN_VISIBLE)) {
            myMapEventListener.setLongClickPin(new MapPos(bundle.getDouble(Const.LONG_PIN_X), bundle.getDouble(Const.LONG_PIN_Y)));
        }

        if (bundle.getBoolean(Const.ROUTE_VIEW_VISIBLE)) {
            ArrayList<RouteInstruction> instructions = new ArrayList<RouteInstruction>();

            MapPos mapPos = new MapPos(bundle.getDouble(Const.ROUTE_VIEW_STOP_PIN_X), bundle.getDouble(Const.ROUTE_VIEW_STOP_PIN_Y));
            myMapEventListener.showStopPin(mapPos);

            hasRouteInstructions = true;

            int number = bundle.getInt(Const.ROUTE_VIEW_INSTRUCTION_NUMBER);

            Bitmap leftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.left_route);
            Bitmap rightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.right_route);
            Bitmap forwardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.forward_route);
            Bitmap startBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.start_route);
            Bitmap endBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.end_route);

            Bitmap bitmap = null;

            for (int i = 0; i < number; i++) {
                switch (bundle.getInt(Const.ROUTE_VIEW_BITMAP_TYPE + i)) {
                    case RouteInstruction.START_BITMAP_TYPE:
                        bitmap = startBitmap;
                        break;
                    case RouteInstruction.END_BITMAP_TYPE:
                        bitmap = endBitmap;
                        break;
                    case RouteInstruction.LEFT_BITMAP_TYPE:
                        bitmap = leftBitmap;
                        break;
                    case RouteInstruction.RIGHT_BITMAP_TYPE:
                        bitmap = rightBitmap;
                        break;
                    case RouteInstruction.FORWARD_BITMAP_TYPE:
                        bitmap = forwardBitmap;
                        break;
                }

                instructions.add(
                        new RouteInstruction(
                                bundle.getString(Const.ROUTE_VIEW_DESCRIPTION + i),
                                bundle.getDouble(Const.ROUTE_VIEW_DISTANCE + i),
                                bitmap,
                                bundle.getInt(Const.ROUTE_VIEW_BITMAP_TYPE + i),
                                new MapPos(bundle.getDouble(Const.ROUTE_VIEW_LOCATION_X + i),
                                bundle.getDouble(Const.ROUTE_VIEW_LOCATION_Y + i)),
                                (float) bundle.getDouble(Const.ROUTE_VIEW_AZIMUTH + i)
                        ));
            }

            long numberOfPoints = bundle.getLong(Const.ROUTE_VIEW_LINE_POINTS_NUMBER);

            MapPosVector routePoints = new MapPosVector();

            for (int i = 0; i < numberOfPoints; i++) {
                routePoints.add(
                        new MapPos(
                                bundle.getDouble(Const.ROUTE_VIEW_LINE_POINT_X + i),
                                bundle.getDouble(Const.ROUTE_VIEW_LINE_POINT_Y + i)
                        )
                );
            }

            myMapEventListener.setRoutePoints(routePoints);

            routeView.show(bundle.getDouble(Const.ROUTE_VIEW_FULL_DISTANCE, 0), bundle.getString(Const.ROUTE_VIEW_TIME, ""), instructions);

            if (bundle.getBoolean(Const.ROUTE_VIEW_IS_UP)) {
                routeView.goUp();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {

        bundle.putInt("GPS_STATE", locationView.getState());
        bundle.putBoolean("isMyPlaceShown", isMyPlaceShown);

        bundle.putFloat(Const.ROTATION, mapView.getMapRotation());
        bundle.putFloat(Const.ZOOM, mapView.getZoom());
        bundle.putFloat(Const.TILT, mapView.getTilt());
        bundle.putDouble(Const.FOCUS_X, mapView.getFocusPos().getX());
        bundle.putDouble(Const.FOCUS_Y, mapView.getFocusPos().getY());

        // search marker is shown and when orientation is changed
        bundle.putDouble(Const.SEARCH_X, searchMarker.getGeometry().getCenterPos().getX());
        bundle.putDouble(Const.SEARCH_Y, searchMarker.getGeometry().getCenterPos().getY());

        if (searchMarker.getMetaDataElement("title") != null && !searchMarker.getMetaDataElement("title").equals("")) {
            bundle.putString(Const.SEARCH_TITLE, searchMarker.getMetaDataElement("title").getString());
        }

        if (searchMarker.getMetaDataElement("description") != null && !searchMarker.getMetaDataElement("description").equals("")) {
            bundle.putString(Const.SEARCH_DESC, searchMarker.getMetaDataElement("description").getString());
        }

        bundle.putBoolean(Const.SEARCH_FOCUS, shouldSearchMarkerFocus);
        bundle.putBoolean(Const.IS_HAMBURGER, isHamburgerClosed);

        if (lineLocationTrack != null) {

            bundle.putLong(Const.LOCATIONS_SIZE, linePoses.size());

            for (int i = 0; i < linePoses.size(); i++) {
                bundle.putDouble(Const.LOCATION_X + i, linePoses.get(i).getX());
                bundle.putDouble(Const.LOCATION_Y + i, linePoses.get(i).getY());
            }
        }

        if (!locationTrackingDB.isGPSTrackingOn()) {

            MapPosVector poses = locationTrackingDB.getCurrentLinePoses();

            if (poses != null && poses.size() > 1) {
                bundle.putLong(Const.LOCATIONS_SIZE2, poses.size());

                for (int i = 0; i < poses.size(); i++) {
                    bundle.putDouble(Const.LOCATION_X2 + i, poses.get(i).getX());
                    bundle.putDouble(Const.LOCATION_Y2 + i, poses.get(i).getY());
                }
            }
        }

        if (bottomView.getVisibility() == View.VISIBLE) {
            bundle.putBoolean(Const.DROPPED_VIEW_VISIBLE, true);

            bundle.putString(Const.DROPPED_VIEW_TITLE, bottomView.getTitle());
            bundle.putString(Const.DROPPED_VIEW_DESCRIPTION, bottomView.getDescription());
            bundle.putDouble(Const.DROPPED_VIEW_X, bottomView.getLocation().getX());
            bundle.putDouble(Const.DROPPED_VIEW_Y, bottomView.getLocation().getY());
            bundle.putBoolean(Const.DROPPED_VIEW_FAVORITE, bottomView.getIsAddingFavorite());

            bundle.putBoolean(Const.LONG_PIN_VISIBLE, myMapEventListener.getLongPinVisibility());
            bundle.putDouble(Const.LONG_PIN_X, myMapEventListener.getLongClickPinMapPos().getX());
            bundle.putDouble(Const.LONG_PIN_Y, myMapEventListener.getLongClickPinMapPos().getY());

            bundle.putString(Const.FAVORITE_ID, myMapEventListener.getFavoriteID());
        } else {
            bundle.putBoolean(Const.DROPPED_VIEW_VISIBLE, false);
        }

        if (routeView.getVisibility() == View.VISIBLE) {
            bundle.putBoolean(Const.ROUTE_VIEW_VISIBLE, true);

            bundle.putDouble(Const.ROUTE_VIEW_STOP_PIN_X, myMapEventListener.getStopPinMapPos().getX());
            bundle.putDouble(Const.ROUTE_VIEW_STOP_PIN_Y, myMapEventListener.getStopPinMapPos().getY());

            bundle.putBoolean(Const.ROUTE_VIEW_IS_UP, routeView.isUp());

            bundle.putDouble(Const.ROUTE_VIEW_FULL_DISTANCE, routeView.getDistance());
            bundle.putString(Const.ROUTE_VIEW_TIME, routeView.getTime());

            ArrayList<RouteInstruction> instructions = routeView.getInstructions();

            bundle.putInt(Const.ROUTE_VIEW_INSTRUCTION_NUMBER, instructions.size());

            for (int i = 0; i < instructions.size(); i++) {
                bundle.putString(Const.ROUTE_VIEW_DESCRIPTION + i, instructions.get(i).getDescription());
                bundle.putDouble(Const.ROUTE_VIEW_DISTANCE + i, instructions.get(i).getDistance());
                bundle.putInt(Const.ROUTE_VIEW_BITMAP_TYPE + i, instructions.get(i).getBitmapType());

                if (instructions.get(i).getLocation() != null) {
                    bundle.putDouble(Const.ROUTE_VIEW_LOCATION_X + i, instructions.get(i).getLocation().getX());
                    bundle.putDouble(Const.ROUTE_VIEW_LOCATION_Y + i, instructions.get(i).getLocation().getY());
                }

                bundle.putDouble(Const.ROUTE_VIEW_AZIMUTH + i, instructions.get(i).getAzimuth());
            }

            MapPosVector routePoints = myMapEventListener.getRoutePoints();

            bundle.putLong(Const.ROUTE_VIEW_LINE_POINTS_NUMBER, routePoints.size());

            for (int i = 0; i < routePoints.size(); i++) {
                bundle.putDouble(Const.ROUTE_VIEW_LINE_POINT_X + i, routePoints.get(i).getX());
                bundle.putDouble(Const.ROUTE_VIEW_LINE_POINT_Y + i, routePoints.get(i).getY());
            }
        } else {
            bundle.putBoolean(Const.ROUTE_VIEW_VISIBLE, false);
        }

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(bundle);
    }

    // stop GPS animation thread and stop OrientationManager
    private void stopGPS() {

        locationCircle.stopLocationCircle();
        mOrientationManager.stop();

        Intent intent = new Intent(getApplicationContext(), LocationService.class);

        intent.putExtra(LocationService.STOP_LOCATION_FLAG, true);

        startService(intent);
    }

    private void startGPS(boolean isMyPlaceShown) {

        this.isMyPlaceShown = isMyPlaceShown;

        // start GPS and sensors
        mOrientationManager.start();

        Intent intent = new Intent(getApplicationContext(), LocationService.class);

        intent.putExtra(LocationService.START_LOCATION_FLAG, true);

        startService(intent);

        if (locationDataSource == null) {
            locationDataSource = new LocalVectorDataSource(projection);
            locationAnimationLayer = new VectorLayer(locationDataSource);
            mapView.getLayers().add(locationAnimationLayer);
        } else {
            mapView.getLayers().remove(locationAnimationLayer);
            mapView.getLayers().add(locationAnimationLayer);
        }

        if (locationCircle == null) {
            // start GPS animation with time duration, it does nothing until first
            // fix
            locationCircle = new LocationCircle(mapView, locationView, locationDataSource);
            locationCircle.start();
        } else {
            mapView.getLayers().remove(locationAnimationLayer);
            mapView.getLayers().add(locationAnimationLayer);

            locationCircle.refresh();
        }
    }

    // check does app crash last time and if it HOCKEY SDK give users option to
    // report this crash
    private void checkForCrashes() {
        CrashManager.register(this, Const.HOCKEYAPP_ID, new CrashManagerListener() {
                    public String getDescription() {
                        String description = "Crash in app";
                        /*
                         * // Try to read logs from logcat - not useful really
						 * try { Process process = Runtime.getRuntime().exec(
						 * "logcat -d HockeyApp:D *:S"); BufferedReader
						 * bufferedReader = new BufferedReader( new
						 * InputStreamReader(process .getInputStream()));
						 *
						 * StringBuilder log = new StringBuilder(); String line;
						 * while ((line = bufferedReader.readLine()) != null) {
						 * log.append(line);
						 * log.append(System.getProperty("line.separator")); }
						 * bufferedReader.close();
						 *
						 * description = log.toString(); } catch (IOException e)
						 * { }
						 */
                        return description;
                    }

                    public boolean shouldAutoUploadCrashes() {
                        return true;
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();

        FlurryAgent.onEndSession(this);

        if (isFromMainActivity) {
            if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
                mOrientationManager.stop();
                stopGPS();
            }
        }
    }

    private com.carto.graphics.Bitmap getBitmap (int resource) {
        return BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), resource));
    }

    @Override
    protected void onResume() {
        super.onResume();

        FlurryAgent.onStartSession(this);

        packageSuggestion.resetSuggestion();

        if (shouldRefreshFavoriteLocations) {
            myMapEventListener.refreshFavoriteLocationsOnMap();
            shouldRefreshFavoriteLocations = false;
        }

        if (locationBookmarkId != -1) {
            myMapEventListener.selectLocationBookmark(locationBookmarkId);
            locationBookmarkId = -1;
        }

        if (shouldUpdateBaseLayer) {
            updateBaseLayer();
            shouldUpdateBaseLayer = false;
        }

        if (!isNotDarkStyle) {
            mapView.getOptions().setBackgroundBitmap(getBitmap(R.drawable.white_tile));
        } else {
            mapView.getOptions().setBackgroundBitmap(getBitmap(R.drawable.dark_tile));
        }

        if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
            mOrientationManager.start();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    startGPS(isMyPlaceShown);
                }
            }, 500);
        }

        if (!measurementUnit.equals("")) {
            if (measurementUnit.equals(Const.METRIC)) {

                scaleBarView.setMeasurementUnit(ScaleBarView.METRIC_UNIT);
                routeView.setMeasurementUnit(RouteView.METRIC_UNIT);
                bottomView.setMeasurementUnit(BottomView.METRIC_UNIT);

            } else if (measurementUnit.equals(Const.IMPERIAL)) {

                scaleBarView.setMeasurementUnit(ScaleBarView.IMPERIAL_UNIT);
                routeView.setMeasurementUnit(RouteView.IMPERIAL_UNIT);
                bottomView.setMeasurementUnit(BottomView.IMPERIAL_UNIT);
            }

            measurementUnit = "";
        }

        if (!mapLanguage.equals("")) {
            vectorStyleLang = LanguageUtils.getLanguage(mapLanguage);
            updateBaseLayer();
            mapLanguage = "";
        }

        // bug for place with coordinate 0, 0 :)
        if (!searchMarker.isVisible()
                && searchMarker.getGeometry().getCenterPos().getX() != 0
                && searchMarker.getGeometry().getCenterPos().getY() != 0) {

            // clear last search marker or initial marker
            searchVectorDataSource.remove(searchMarker);
            myMapEventListener.removeSearchDataSource();

            searchMarker.setVisible(true);
            searchVectorDataSource.add(searchMarker);

            if (shouldSearchMarkerFocus) {
                final VectorElement vectorElement = searchMarker;

                final Billboard billboard = (Billboard) vectorElement;

                new Handler(getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        bottomView.show(
                                billboard.getMetaDataElement("title").getString(),
                                billboard.getMetaDataElement("description").getString(),
                                ((Billboard) vectorElement).getRootGeometry().getCenterPos(),
                                true
                        );
                        locationView.goUp();
                    }
                });

                // release fix or turn state of map and enable search marker to
                // be seen
                if (locationView.getState() == LocationView.LOCATION_STATE_FIX
                        || locationView.getState() == LocationView.LOCATION_STATE_TURN) {
                    locationView.setState(LocationView.LOCATION_STATE_ON);
                }

                mapView.moveToFitBounds(
                        new MapBounds(projection.fromWgs84(new MapPos(
                                Double.parseDouble(billboard
                                        .getMetaDataElement("x1").getString()), Double
                                .parseDouble(billboard
                                        .getMetaDataElement("y1").getString()))),

                                projection.fromWgs84(new MapPos(Double
                                        .parseDouble(billboard
                                                .getMetaDataElement("x2").getString()),
                                        Double.parseDouble(billboard
                                                .getMetaDataElement("y2").getString())))),

                        new ScreenBounds(
                                new ScreenPos(mapView.getWidth() / 10, mapView.getWidth() / 10),
                                new ScreenPos(mapView.getWidth() / 10 * 9, mapView.getHeight() / 10 * 9)), false, 1);

                shouldSearchMarkerFocus = false;
            }
        }

        isFromMainActivity = true;

        checkForCrashes();
    }

    @Override
    protected void onDestroy() {

        if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
            stopGPS();
        }

        shouldRefreshFavoriteLocations = false;
        locationBookmarkId = -1;

        // store current states for mapView
        SharedPreferences preferences = getSharedPreferences(Const.SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("xpos", Double.toString(mapView.getFocusPos().getX()));
        editor.putString("ypos", Double.toString(mapView.getFocusPos().getY()));
        editor.putFloat("tilt", mapView.getTilt());
        editor.putFloat("rotation", mapView.getMapRotation());
        editor.putFloat("zoom", mapView.getZoom());

        // commit to storage
        editor.commit();

        isLive = false;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        if (!isHamburgerMenuActivated) {
            inflater.inflate(R.menu.mainmenu_9, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (isHamburgerMenuActivated) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_search:
                isFromMainActivity = false;
                onSearchRequested();

                return true;
            case R.id.menu_download:
                isFromMainActivity = false;
                startActivity(new Intent(this, PackageDownloadListActivity.class));
                return true;
            case R.id.menu_bookmarks:
                isFromMainActivity = false;

                ArrayList<LocationBookmark> locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();

                // there is no location bookmars, so inform user with toast msg
                if (locationBookmarks.size() == 0) {
                    Toast.makeText(this, getString(R.string.bookmarks_zero), Toast.LENGTH_LONG).show();
                } else {
                    startActivityForResult(new Intent(this, LocationBookmarksListActivity.class), 3);
                }

                return true;

            case R.id.menu_info:
                isFromMainActivity = false;
                startActivity(new Intent(this, InfoActivity.class));

                return true;
            case R.id.menu_settings:

                isFromMainActivity = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    startActivity(new Intent(this, SettingsActivity11.class));
                } else {
                    startActivity(new Intent(this, SettingsActivity9.class));
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOrientationChanged(OrientationManager orientationManager) {

        float azimut = orientationManager.getHeading(); // orientation contains:

        // mapView angle rotation goes from -180 to 180, -180 is excluded
        // 0 means look north, 90 means west, -90 means east and 180 means
        // south
        if (azimut >= 180) {
            azimut = (360 - azimut);
        } else {
            azimut = -azimut;
        }

        if (mOrientationManager.hasCompassSensors()) {
            locationCircle.setRotation(azimut);

            if (locationView.getState() == LocationView.LOCATION_STATE_TURN) {
                mapView.setMapRotation(azimut, 0.5f);
            }
        }
    }

    @Override
    public void onAccuracyChanged(OrientationManager orientationManager) {
        // nothing to do
    }

    @Override
    public void onBackPressed() {
        if (isHamburgerMenuActivated) {
            if (isHamburgerClosed) {
                if (routeView.isUp()) {
                    routeView.goDown();
                } else if (bottomView.getVisibility() == View.VISIBLE) {
                    bottomView.hide();
                    routeView.setVisibility(View.GONE);
                    mapView.getOptions().setFocusPointOffset(new ScreenPos(0, 0));
                    searchVectorDataSource.remove(searchMarker);
                    myMapEventListener.removeSearchDataSource();
                    locationView.goDown();
                    myMapEventListener.clearRoute();
                    myMapEventListener.removeLongClickPin();
                    hasRouteInstructions = false;
                } else {
                    super.onBackPressed();
                }
            } else {
                hamburgerMenuLayout.closeDrawer(hamburgerMenuList);
            }
        } else {
            if (routeView.isUp()) {
                routeView.goDown();
            } else if (bottomView.getVisibility() == View.VISIBLE) {
                bottomView.hide();
                routeView.setVisibility(View.GONE);
                mapView.getOptions().setFocusPointOffset(new ScreenPos(0, 0));
                searchVectorDataSource.remove(searchMarker);
                myMapEventListener.removeSearchDataSource();
                locationView.goDown();
                myMapEventListener.clearRoute();
                myMapEventListener.removeLongClickPin();
                hasRouteInstructions = false;
            } else {
                finish();
            }
        }
    }

    // hamburger menu group clicks actions
    public void groupItemClick(int groupPosition) {
        switch (groupPosition) {
            case 2:
                isFromMainActivity = false;
                startActivity(new Intent(this, PackageDownloadListActivity.class));

                hamburgerAdapter.notifyDataSetChanged();

                break;
            case 3:
                hamburgerMenuLayout.closeDrawer(hamburgerMenuList);

                isFromMainActivity = false;

                ArrayList<LocationBookmark> locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();

                // there is no location bookmars, so inform user with toast msg
                if (locationBookmarks.size() == 0) {
                    Toast.makeText(this, getString(R.string.bookmarks_zero), Toast.LENGTH_LONG).show();
                } else {
                    startActivityForResult(new Intent(this, LocationBookmarksListActivity.class), 3);
                }

                hamburgerAdapter.notifyDataSetChanged();

                break;
            case 5:
                isFromMainActivity = false;
                startActivity(new Intent(this, SettingsActivity11.class));

                break;
            case 6:
                isFromMainActivity = false;
                startActivity(new Intent(this, InfoActivity.class));

                break;
        }
    }

    public void addtrackOnMap(TrackData trackData, boolean isTrackingOnForThisTrack) {

        int l = trackData.locations.length;

        if (l > 1) {
            linePoses = new MapPosVector();

            float x;
            float y;

            float xMin = 1000;
            float yMin = 1000;
            float xMax = -1000;
            float yMax = -1000;

            for (int i = 0; i < l; i++) {
                x = trackData.locations[i].x;
                y = trackData.locations[i].y;

                if (x < xMin) {
                    xMin = x;
                }
                if (x > xMax) {
                    xMax = x;
                }
                if (y < yMin) {
                    yMin = y;
                }
                if (y > yMax) {
                    yMax = y;
                }

                linePoses.add(projection.fromWgs84(new MapPos(x, y)));
            }

            if (lineLocationTrack != null) {
                gpsTracksVectorDataSource.remove(lineLocationTrack);
            }

            if (!isTrackingOnForThisTrack) {
                lineLocationTrack = new Line(linePoses, lineStyleBuilder.buildStyle());
                gpsTracksVectorDataSource.add(lineLocationTrack);
            }

            mapView.moveToFitBounds(
                    new MapBounds(projection.fromWgs84(new MapPos(xMin, yMin)),
                            projection.fromWgs84(new MapPos(xMax, yMax))),
                    new ScreenBounds(new ScreenPos(mapView.getWidth() / 10,
                            mapView.getWidth() / 10), new ScreenPos(mapView
                            .getWidth() / 10 * 9, mapView.getHeight() / 10 * 9)),
                    false, 1);
        } else {
            Toast.makeText(this, getString(R.string.no_locations), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (isHamburgerMenuActivated) {
                        new Handler(getMainLooper()).post(new Runnable() {

                            @Override
                            public void run() {
                                hamburgerMenuLayout.closeDrawer(hamburgerMenuList);
                            }
                        });
                    }

                    String result = data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS).get(0);

                    Intent intent = new Intent(Intent.ACTION_SEARCH);
                    intent.setClass(this, SearchableActivity.class);
                    intent.putExtra(SearchManager.QUERY, result);

                    startActivityForResult(intent, 2);
                }

                break;
            case 2:
                if (resultCode == Activity.RESULT_OK && data != null) {

                    if (isHamburgerMenuActivated) {
                        new Handler(getMainLooper()).post(new Runnable() {

                            @Override
                            public void run() {
                                hamburgerMenuLayout.closeDrawer(hamburgerMenuList);
                            }
                        });
                    }

                    searchMarker.setPos(new MapPos(
                            data.getDoubleExtra(Const.SEARCH_X, 0),
                            data.getDoubleExtra(Const.SEARCH_Y, 0))
                    );

                    if (data.getStringExtra(Const.SEARCH_TITLE) != null) {
                        searchMarker.setMetaDataElement("title", new Variant(data.getStringExtra(Const.SEARCH_TITLE)));
                    }
                    if (data.getStringExtra(Const.SEARCH_DESC) != null) {
                        searchMarker.setMetaDataElement("description", new Variant(data.getStringExtra(Const.SEARCH_DESC)));
                    }

                    if (data.getStringExtra("x1") != null) {
                        searchMarker.setMetaDataElement("x1", new Variant(data.getStringExtra("x1")));
                    }
                    if (data.getStringExtra("x2") != null) {
                        searchMarker.setMetaDataElement("x2", new Variant(data.getStringExtra("x2")));
                    }
                    if (data.getStringExtra("y1") != null) {
                        searchMarker.setMetaDataElement("y1", new Variant(data.getStringExtra("y1")));
                    }
                    if (data.getStringExtra("y2") != null) {
                        searchMarker.setMetaDataElement("y2", new Variant(data.getStringExtra("y2")));
                    }

                    searchMarker.setVisible(false);// flag for onResume
                    shouldSearchMarkerFocus = true;
                }

                break;
            case 3:

                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            // manually launch the real search activity
            Intent searchIntent = new Intent(Intent.ACTION_SEARCH);
            searchIntent.setClass(this, SearchableActivity.class);
            searchIntent.putExtra(SearchManager.QUERY, query);

            startActivityForResult(searchIntent, 2);
        }
    }

    public static class GroupItem {
        public Drawable image;
        public Drawable imageHover;
        public String title;
        public List<ChildItem> items = new ArrayList<ChildItem>();
    }

    public static class ChildItem {
        public String title;
    }

    public static class GroupHolder {
        public ImageView image;
        public ImageView image2;
        public TextView title;
        public SearchHamburgerView editText;
        public NutiteqCheckBox checkBox;
    }

    public static class ChildHolder {
        public TextView title;
    }

    private class CustomComparator implements Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

    public void startVoiceForm() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        mainActivity.startActivityForResult(intent, 1);
    }
}