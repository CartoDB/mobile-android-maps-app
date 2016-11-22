package com.nutiteq.app.nutimap2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.nutiteq.app.locationbookmarks.LocationBookmarksListActivity;
import com.nutiteq.app.nutimap3d.dev.BuildConfig;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.app.search.SearchableActivity;
import com.nutiteq.app.settings.SettingsActivity11;
import com.nutiteq.app.settings.SettingsActivity9;
import com.nutiteq.components.PanningMode;
import com.nutiteq.core.MapBounds;
import com.nutiteq.core.MapPos;
import com.nutiteq.core.MapRange;
import com.nutiteq.core.ScreenBounds;
import com.nutiteq.core.ScreenPos;
import com.nutiteq.datasources.LocalVectorDataSource;
import com.nutiteq.datasources.PackageManagerTileDataSource;
import com.nutiteq.graphics.Color;
import com.nutiteq.layers.TileSubstitutionPolicy;
import com.nutiteq.layers.VectorLayer;
import com.nutiteq.layers.VectorTileLayer;
import com.nutiteq.nuticomponents.OrientationManager;
import com.nutiteq.nuticomponents.OrientationManager.OnChangedListener;
import com.nutiteq.nuticomponents.Utility;
import com.nutiteq.nuticomponents.customviews.AnimatedExpandableListView;
import com.nutiteq.nuticomponents.customviews.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import com.nutiteq.nuticomponents.customviews.CompassView;
import com.nutiteq.nuticomponents.customviews.LocationView;
import com.nutiteq.nuticomponents.customviews.LocationView.LocationButtonClickListener;
import com.nutiteq.nuticomponents.customviews.LocationView.LocationButtonGPSTrackingListener;
import com.nutiteq.nuticomponents.customviews.ScaleBarView;
import com.nutiteq.nuticomponents.customviews.search.HamburgerView;
import com.nutiteq.nuticomponents.customviews.search.SearchView;
import com.nutiteq.nuticomponents.customviews.search.VoiceView;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingDB;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingService;
import com.nutiteq.nuticomponents.locationtracking.TrackData;
import com.nutiteq.nuticomponents.location.LocationCircle;
import com.nutiteq.nuticomponents.location.LocationSettingsSupportDialog;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadListActivity;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadService;
import com.nutiteq.packagemanager.NutiteqPackageManager;
import com.nutiteq.packagemanager.PackageErrorType;
import com.nutiteq.packagemanager.PackageInfo;
import com.nutiteq.packagemanager.PackageManagerListener;
import com.nutiteq.packagemanager.PackageStatus;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.Projection;
import com.nutiteq.styles.BalloonPopupStyleBuilder;
import com.nutiteq.styles.LineJointType;
import com.nutiteq.styles.LineStyleBuilder;
import com.nutiteq.styles.MarkerStyle;
import com.nutiteq.styles.MarkerStyleBuilder;
import com.nutiteq.ui.MapView;
import com.nutiteq.utils.AssetUtils;
import com.nutiteq.utils.BitmapUtils;
import com.nutiteq.vectorelements.BalloonPopup;
import com.nutiteq.vectorelements.Billboard;
import com.nutiteq.vectorelements.Line;
import com.nutiteq.vectorelements.Marker;
import com.nutiteq.vectorelements.VectorElement;
import com.nutiteq.vectortiles.MBVectorTileDecoder;
import com.nutiteq.vectortiles.MBVectorTileStyleSet;
import com.nutiteq.wrappedcommons.MapPosVector;
import com.nutiteq.wrappedcommons.PackageInfoVector;
import com.nutiteq.wrappedcommons.UnsignedCharVector;

/**
 * @author Milan Ivankovic
 * 
 *         I use FragmentActivity otherwise I can't show DialogFragment which
 *         ask users to open GPS settings on device if they are disabled.
 */

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements
		OnChangedListener, ConnectionCallbacks, OnConnectionFailedListener,
		LocationListener {

	private VectorTileLayer baseLayer;
	private MapView mapView;
	private EPSG3857 projection = new EPSG3857();

	private static final int MAP_ZOOM_MIN = 0;
	private static final int MAP_ZOOM_MAX = 22;

	private static final String SHARED_PREFS = "com.nutiteq.offmaps";

	private MBVectorTileDecoder vectorTileDecoder;

	// Style parameters
	private String vectorStyleName = Const.MAP_STYLE; // default
														// style
														// name
	private String vectorStyleLang = Const.MAP_LANGUAGE_AUTOMATIC; // default
																	// language

	private LocationManager locationManager;// need to check if GPS
											// settings is enabled on device

	private Marker searchMarker;
	private LocalVectorDataSource searchVectorDataSource;

	private boolean shouldSearchMarkerFocus = false;

	private LocationView locationView;
	private LocationCircle locationCircle;

	private MapPos lastGPSlocation;

	private Boolean isMyPlaceShown = false; // flag for first GPS fix and focus

	private MyMapEventListener myMapEventListener;

	private OrientationManager mOrientationManager;

	private NutiteqPackageManager packageManager;

	private DrawerLayout hamburgerMenuLayout;
	private AnimatedExpandableListView hamburgerMenuList;

	private boolean isHamburgerMenuActivated;

	private MyAdapter adapter;

	// flag, is map fixed on focus point
	private boolean isFixListener = false;

	// use for preferences
	private static String measurementUnit = "";
	private static String mapLanguage = "";

	private ScaleBarView scaleBarView;

	private PackageSuggestion packageSuggestion;

	private static boolean shouldUpdateBaseLayer = false;

	private boolean ishamburgerClosed = true;

	private SearchView searchView;

	// for package suggestions
	private File nutimapFolder = new File(
			Environment.getExternalStorageDirectory() + "/nutimaps/");
	private ArrayList<String> oldPackages = new ArrayList<String>();
	private PackageListener packageListner;
	// private boolean isPackageListDownloaded = false;
	private boolean isFirstMsgCanceled = false;

	private GPSTrackingDB gpsTrackingDB;

	private LocalVectorDataSource tracksVectorDataSource;

	private Line lineGPSTrack;
	private LineStyleBuilder lineStyleBuilder;

	private MapPosVector linePoses;

	// flag to know from GPS tracking service is app live
	public static boolean isLive = false;

	private LocalVectorDataSource bookmarkDataSource;

	public static boolean shouldRefreshFavoriteLocations = false;

	public static long locationBookmarkId = -1;

	private void setHamburgerMenu() {
		String versionName = "";

		try {
			versionName = getString(R.string.version)
					+ " "
					+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// creating hamburger menu and sub menu
		List<GroupItem> items = new ArrayList<GroupItem>();

		GroupItem item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.logo);
		item.title = getString(R.string.app_name) + " " + versionName;
		items.add(item);

		item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.download_menu);
		item.title = getString(R.string.download);
		items.add(item);

		item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.bookmarks_menu);
		item.title = getString(R.string.bookmarks_menu);
		items.add(item);

		item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.tracks_menu);
		item.title = getString(R.string.tracks);
		items.add(item);

		item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.settings_menu);
		item.title = getString(R.string.settings);
		items.add(item);

		item = new GroupItem();
		item.image = getResources().getDrawable(R.drawable.info_menu);
		item.title = getString(R.string.about);
		items.add(item);

		adapter = new MyAdapter(this);
		adapter.setData(items);

		hamburgerMenuList.setAdapter(adapter);
	}

	private GoogleApiClient mGoogleApiClient;
	private LocationRequest mLocationRequest;

	// Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR = "dialog_error";
	// Bool to track whether the app is already resolving an error
	private boolean mResolvingError = false;

	private ImageButton bookmarkButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isLive = true;

		// Create a GoogleApiClient instance
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API).build();

		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(1000);
		mLocationRequest.setFastestInterval(1000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		// for hamburger menu which needs API 14
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			setContentView(R.layout.activity_main_api_14);

			isHamburgerMenuActivated = true;

			hamburgerMenuList = (AnimatedExpandableListView) findViewById(R.id.hamburger_list);

			setHamburgerMenu();

			hamburgerMenuLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

			DisplayMetrics metrics = getResources().getDisplayMetrics();
			if (metrics.heightPixels > metrics.widthPixels) {
				hamburgerMenuList.getLayoutParams().width = (int) (getResources()
						.getDisplayMetrics().widthPixels * 0.75f);
			} else {
				hamburgerMenuList.getLayoutParams().width = (int) (getResources()
						.getDisplayMetrics().heightPixels * 0.75f);
			}

			// In order to show animations, we need to use a custom click
			// handler for our ExpandableListView.
			hamburgerMenuList
					.setOnGroupClickListener(new HamburgerMenuGroupClickListener());

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

							ishamburgerClosed = false;
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

							ishamburgerClosed = true;
							invalidateOptionsMenu(); // creates call to
														// onPrepareOptionsMenu()
						}

						@Override
						public void onDrawerStateChanged(int newState) {

						}
					});

			searchView = (SearchView) findViewById(R.id.search_view);
			searchView.setObjects(this, SearchableActivity.class);

			HamburgerView hamburgerView = (HamburgerView) findViewById(R.id.hamburger_view);
			hamburgerView.setHamburgerMenu(hamburgerMenuLayout,
					hamburgerMenuList);
			hamburgerView.setSearchView(searchView);

			VoiceView voiceView = (VoiceView) findViewById(R.id.voice_view);
			voiceView.setMainActivity(this);
		} else {
			setContentView(R.layout.activity_main_api_9);
			isHamburgerMenuActivated = false;
		}

		locationView = (LocationView) findViewById(R.id.gps_button);

		tracksVectorDataSource = new LocalVectorDataSource(projection);

		((MapApplication) getApplication()).setReferences(this,
				tracksVectorDataSource, locationView);

		gpsTrackingDB = ((MapApplication) getApplication()).getGPSTrackingDB();

		// get default preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		measurementUnit = prefs.getString(Const.PREF_UNIT_KEY, Const.METRIC);
		String storageType = prefs.getString(Const.PREF_STORAGE_KEY,
				Const.EXTERNAL);
		vectorStyleLang = prefs.getString(Const.PREF_LANG_KEY,
				Const.LANG_AUTOMATIC);

		changeLang(vectorStyleLang);

		mapView = (MapView) this.findViewById(R.id.map_view);

		mapView.getOptions().setBaseProjection(projection);
		mapView.getOptions().setTileThreadPoolSize(2);

		SharedPreferences preferences = getSharedPreferences(SHARED_PREFS,
				MODE_PRIVATE);

		boolean isFirstStart = preferences.getBoolean("isfirststart", true);

		// Get package manager
		packageManager = ((MapApplication) getApplication())
				.getPackageManagerComponent().getNutiteqPackageManager();

		if (isFirstStart) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("isfirststart", false);
			editor.commit();

			packageManager.startPackageListDownload();
		}

		boolean isMapReady = false;

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

			Log.e(Const.LOG_TAG, "mbTileFile isn't ready for use: "
					+ Const.BASE_PACKAGE_ASSET_NAME);
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

			mapView.getOptions().setZoomRange(
					new MapRange(MAP_ZOOM_MIN, MAP_ZOOM_MAX));
			mapView.getOptions().setPanningMode(
					PanningMode.PANNING_MODE_STICKY_FINAL);
		} else {
			Toast.makeText(this, getString(R.string.map_error),
					Toast.LENGTH_LONG).show();
		}

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
							locationView
									.setState(LocationView.LOCATION_STATE_OFF);

							isMyPlaceShown = false;

							stopGPS();

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

						stopGPS();

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

							Intent intent = new Intent(getApplicationContext(),
									GPSTrackingService.class);

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
								GPSTrackingService.class);

						stopService(intent);

						return true;
					}
				});

		mapView.setOnTouchListener(new OnTouchListener() {

			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (isHamburgerMenuActivated) {
					searchView.hideKeyboard();
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

		packageSuggestion = new PackageSuggestion();
		packageSuggestion.setObjects(mapView, baseLayer, packageManager,
				(Button) findViewById(R.id.suggestion_button));

		init();

		checkForUpdates();
	}

	public void stopGPSTracking() {
		locationView.setIsLongClickOn(false);
	}

	public Projection getMapProjection() {
		return projection;
	}

	private void showPackageSuggestionDialog() {
		String[] oldPackageMap = { "ar_buenosaires_0_15", "ar_general_0_10",
				"at_general_0_12", "at_vienna_0_17", "au_graz_0_16",
				"au_innsbruck_0_16", "au_klagenfurt_0_17", "au_linz_0_16",
				"au_salzburg_0_16", "aus_general_0_9", "aus_melbourne_0_15",
				"aus_sydney_0_16", "ban_dhaka_0_17", "ban_general_0_12",
				"be_aalst_0_17", "be_antwerp_0_16", "be_brugge_0_17",
				"be_brussels_0_17", "be_chaleroi_0_17", "be_general_0_13",
				"be_gent_0_17", "be_leuven_0_17", "be_liege_0_17",
				"be_mons_0_17", "be_namur_0_17", "bel_general_0_11",
				"bl_minsk_0_16", "bos_general_0_12", "bos_sarejevo_0_17",
				"br_general_0_9", "br_rio_0_17", "br_saopaulo_0_16",
				"bul_general_0_12", "bul_sofia_0_17", "can_general_0_9",
				"can_montreal_0_16", "can_toronto_0_16", "chi_general_0_11",
				"chi_santiago_0_16", "chn_general_0_10", "chn_hk_0_15",
				"chn_peking_0_16", "chn_shanghai_0_15", "col_bogota_0_17",
				"col_general_0_11", "con_general_0_10", "cro_general_0_12",
				"cro_zagreb_0_17", "cz_brno_0_17", "cz_ceskebudejovice_0_17",
				"cz_general_0_12", "cz_hk_0_17", "cz_karlovyvary_0_17",
				"cz_olomouc_0_17", "cz_ostrava_0_17", "cz_pardubice_0_17",
				"cz_plzen_0_17", "cz_prague_0_17", "cz_ustinadlabem_0_17",
				"dk_Aalborg_0_17", "dk_Esbjerg_0_17", "dk_Horsens_0_17",
				"dk_Kolding_0_17", "dk_Odense_0_17", "dk_Roskilde_0_17",
				"dk_Vejle_0_17", "dk_arhus_0_17", "dk_copenhagen_0_17",
				"dk_general_0_12", "eg_cairo_0_16", "eg_general_0_11",
				"es_balearic_0_14", "es_bilbao_0_16", "es_canary_0_14",
				"es_laspalmasgrancanaria_0_17", "es_malaga_0_16",
				"es_murcia_0_16", "es_palma_0_16", "es_sevilla_0_16",
				"es_valencia_0_16", "es_zaragoza_0_16", "esp_barcelona_0_17",
				"esp_general_0_11", "esp_girona_0_17", "esp_madrid_0_17",
				"est_general", "est_tallinn", "est_tartu", "fi_general_0_10",
				"fi_helsinki_0_16", "fr_bordeaux_0_17", "fr_cannes_0_17",
				"fr_general_0_11", "fr_havre_0_17", "fr_lille_0_17",
				"fr_lyon_0_17", "fr_marseille_0_17", "fr_montpellier_0_17",
				"fr_nantes_0_17", "fr_nice_0_17", "fr_paris_0_16",
				"fr_reims_0_17", "fr_rennes_0_17", "fr_strasbourg_0_17",
				"fr_toulouse_0_17", "ger_berlin_0_16", "ger_bonn_0_17",
				"ger_bremen_0_17", "ger_frankfurtmain_0_17",
				"ger_general_0_11", "ger_hamburg_0_16", "ger_hannover_0_17",
				"ger_karlsruhe_0_17", "ger_koeln_0_17", "ger_leipzig_0_17",
				"ger_muenchen_0_17", "ger_nuernberg_0_17",
				"ger_stuttgart_0_17", "gr_athena_0_16", "gr_corfu_0_16",
				"gr_crete_0_14", "gr_general_0_12", "gr_iraklio_0_17",
				"gr_kos_0_16", "gr_larissa_0_17", "gr_patra_0_17",
				"gr_thessaloniki_0_16", "greater_london_0_16",
				"haiti_general_0_13", "haiti_portauprince_0_17",
				"hu_budapest_0_16", "hu_general_0_12", "il_general_0_13",
				"il_haifa_0_17", "il_jerusalem_0_16", "il_telaviv_0_16",
				"in_bangalore_0_16", "in_chennai_0_17", "in_delhi_0_16",
				"in_general_0_10", "in_kolkata_0_16", "in_mumbai_0_16",
				"ind_general_0_10", "ind_jakarta_0_16", "iq_baghdad_0_16",
				"iq_general_0_11", "ir_general_0_10", "ir_tehran_0_16",
				"ire_dublin_0_16", "ire_general_0_12", "it_general_0_11",
				"it_milano_0_17", "it_rome_0_17", "it_venezia_0_18",
				"jp_general_0_11", "jp_tokyo_0_15", "kenya_general_0_12",
				"kenya_nairobi_0_16", "kon_kinshasa_0_17", "kor_general_0_12",
				"kor_seoul_0_16", "ksa_general_0_11", "ksa_riyadh_0_16",
				"lat_general_0_13", "lat_riga_0_16", "london_central_0_17",
				"lt_general_0_12", "lt_kaunas_0_17", "lt_vilnius_0_17",
				"lux_general_0_17", "monaco_0_18", "mx_general_0_10",
				"mx_mexicocity_0_16", "my_general_0_11", "my_yangon_0_17",
				"ng_general_0_11", "ng_lagos_0_16", "nkor_general_0_12",
				"nkor_phoenyan_0_17", "nl_amsterdam_0_17", "nl_general_0_13",
				"no_Drammen_0_17", "no_Fredrikstad_0_17",
				"no_Kristiansand_0_17", "no_Stavanger_0_17", "no_Tromso_0_17",
				"no_bergen_0_17", "no_general_0_10", "no_greater_Oslo_0_15",
				"no_lillehammer_0_17", "no_oslo_0_17", "no_trondheim_0_17",
				"peru_general_0_11", "peru_lima_0_16", "ph_general_0_11",
				"ph_manila_0_17", "pk_general_0_11", "pk_karachi_0_16",
				"pk_lahore_0_16", "pl_Bydgoszcz_0_17", "pl_Katowice_0_17",
				"pl_Lublin_0_17", "pl_bialystok_0_17", "pl_gdansk_0_17",
				"pl_general_0_11", "pl_krakow_0_17", "pl_lodz_0_17",
				"pl_poznan_0_17", "pl_szczecin_0_17", "pl_warszawa_0_17",
				"pl_wroclaw_0_17", "pt_general_0_12", "pt_lisboa_0_17",
				"rom_bucharest_0_17", "rom_cluj_0_17", "rom_general_0_11",
				"rsa_capetown_0_16", "rsa_general_0_11",
				"rsa_johannesburg_0_16", "rsa_pretoria_0_16", "ru_europe_0_10",
				"ru_moskva_0_17", "ru_stpeterburg_0_16", "sch_general_0_13",
				"sch_geneve_0_17", "sch_zuerich_0_17", "sin_general_0_16",
				"sk_bratislava_0_17", "sk_general_0_12", "sk_kosishe_0_17",
				"slo_general_0_13", "slo_ljubliana_0_17", "sr_belgrad_0_17",
				"sr_general_0_12", "swe_Gothenburg_0_17", "swe_Gotland_0_17",
				"swe_general_0_10", "swe_greater_stockholm_0_16",
				"swe_helsingborg_0_17", "swe_kalmar_0_17",
				"swe_karlskrona_0_17", "swe_linkoping_0_17", "swe_lund_0_17",
				"swe_malmo_0_17", "swe_orebro_0_17", "swe_stockholm_0_17",
				"swe_uppsala_0_17", "swe_vasteras_0_17", "th_bangkok_0_17",
				"th_general_0_11", "tr_ankara_0_16", "tr_izmir_0_16",
				"tur_general_0_11", "tur_istanbul_0_17", "tw_general_0_13",
				"tw_taipei_0_16", "ua_kharkiv_0_17", "ua_odessa_0_17",
				"uk_birmingham_0_17", "uk_bradford_0_17", "uk_brighton_0_17",
				"uk_bristol_0_17", "uk_cambridge_0_17", "uk_cardiff_0_17",
				"uk_edinburgh_0_17", "uk_general_0_11", "uk_glasgow_0_17",
				"uk_isleofman_0_16", "uk_leeds_0_17", "uk_liverpool_0_17",
				"uk_manchester_0_17", "uk_newcastleupontyne_0_17",
				"uk_oxford_0_17", "uk_plymouth_0_17", "uk_portsmouth_0_17",
				"uk_preston_0_17", "uk_reading_0_17", "uk_southampton_0_17",
				"uk_strokeontent_0_17", "uk_sunderland_0_17",
				"ukr_general_0_11", "ukr_kiev_0_16", "us_ak_Anchorage_0_16",
				"us_az_phoenix_0_16", "us_ca_0_12", "us_ca_bayarea_0_15",
				"us_ca_losangeles_0_15", "us_ca_sandiego_0_16", "us_dc_0_16",
				"us_fl_0_11", "us_fl_Jacksonville_0_16", "us_fl_miami_0_16",
				"us_ga_atlanta_0_16", "us_general_0_9", "us_hi_0_14",
				"us_il_0_13", "us_il_chicago_0_16", "us_lasvegas_0_16",
				"us_ma_boston_0_16", "us_mi_detroit_0_16", "us_ny_0_11",
				"us_nyc_0_16", "us_oh_0_13", "us_oh_columbus_0_16",
				"us_pa_0_13", "us_pa_Philadelphia_0_16", "us_tx_0_11",
				"us_tx_dallas_0_16", "us_tx_houston_0_16",
				"us_tx_sanantonio_0_16", "us_wa_0_12", "us_wa_seatlle_0_16",
				"world" };

		String[] newPackageMap = { "AR", "AR", "AT", "AT", "AT", "AT", "AT",
				"AT", "AT", "AU", "AU", "AU", "BD", "BD", "BE", "BE", "BE",
				"BE", "BE", "BE", "BE", "BE", "BE", "BE", "BE", "BY", "BY",
				"BA", "BA", "BR", "BR", "BR", "BG", "BG", "-", "CA-QC",
				"CA-ON", "CL", "CL", "CN", "CN", "CN", "CN", "CO", "CO", "CD",
				"HR", "HR", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ",
				"CZ", "CZ", "CZ", "DK", "DK", "DK", "DK", "DK", "DK", "DK",
				"DK", "DK", "DK", "EG", "EG", "ES-IB", "ES", "ES-CN", "ES-CN",
				"ES", "ES", "ES", "ES", "ES", "ES", "ES-CT", "ES", "ES-CT",
				"ES", "EE", "EE", "EE", "FI", "FI", "FR-B", "FR-U", "FR",
				"FR-Q", "FR-O", "FR-V", "FR-U", "FR-K", "FR-R", "FR-U", "FR",
				"FR-G", "FR-E", "FR-A", "FR-N", "DE-BB", "DE-NW", "DE-HB",
				"DE-HE", "-", "DE-HH", "DE-NI", "DE-BW", "DE-NW", "DE-SN",
				"DE-BY", "DE-BY", "DE-BW", "GR", "GR", "GR", "GR", "GR", "GR",
				"GR", "GR", "GR", "GB-ENG", "HT", "HT", "HU", "HU", "IL", "IL",
				"IL", "IL", "IN", "IN", "IN", "IN", "IN", "IN", "IN", "IN",
				"IQ", "IQ", "IR", "IR", "IE", "IE", "IT", "IT", "IT", "IT",
				"JP", "JP", "KE", "KE", "CD", "KR", "KR", "SA", "SA", "LV",
				"LV", "GB-ENG", "LT", "LT", "LT", "LU", "MC", "MX", "MX", "MM",
				"MM", "NG", "NG", "KP", "KP", "NL", "NL", "NO", "NO", "NO",
				"NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "PE", "PE",
				"PH", "PH", "PK", "PK", "PK", "PL", "PL", "PL", "PL", "PL",
				"PL", "PL", "PL", "PL", "PL", "PL", "PL", "PT", "PT", "RO",
				"RO", "RO", "ZA", "ZA", "ZA", "ZA", "RU-CFD", "RU-CFD",
				"RU-NWFD", "CH", "CH", "CH", "SG", "SK", "SK", "SK", "SI",
				"SI", "RS", "RS", "SE", "SE", "SE", "SE", "SE", "SE", "SE",
				"SE", "SE", "SE", "SE", "SE", "SE", "SE", "TH", "TH", "TR",
				"TR", "TR", "TR", "TW", "TW", "UA", "UA", "GB", "GB", "GB",
				"GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB",
				"GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "UA",
				"UA", "US-AK", "US-AZ", "US-CA", "US-CA", "US-CA", "US-CA",
				"US-DC", "US-FL", "US-FL", "US-FL", "US-GA", "-", "US-HI",
				"US-IL", "US-IL", "US-NV", "US-MA", "US-MI", "US-NY", "US-NY",
				"US-OH", "US-OH", "US-PA", "US-PA", "US-TX", "US-TX", "US-TX",
				"US-TX", "US-WA", "US-WA", "-" };

		File[] files = nutimapFolder.listFiles();
		int index;

		Arrays.sort(files, new CustomComparator());

		for (int i = 0; i < files.length; i++) {
			index = Arrays.binarySearch(oldPackageMap, files[i].getName());

			if (index >= 0 && !newPackageMap[i].equals("-")) {
				if (!oldPackages.contains(newPackageMap[index])) {
					oldPackages.add(newPackageMap[index]);
				}
			}
		}

		if (oldPackages.size() > 0) {
			// show msg about packages
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					new ContextThemeWrapper(MainActivity.this, R.style.AppTheme));

			builder.setTitle(getString(R.string.app_name));
			builder.setIcon(R.drawable.icon);
			String msg = getString(R.string.old_package_msg) + "\n\n";

			PackageInfoVector piv = packageManager.getServerPackages();
			String pckName = "";

			for (String existingPackage : oldPackages) {
				for (int j = 0; j < piv.size(); j++) {
					PackageInfo packageInfo = piv.get(j);
					if (packageInfo.getPackageId().equals(existingPackage)) {
						pckName = packageInfo.getNames(
								Locale.getDefault().getLanguage()).get(0);
						break;
					}
				}

				msg += getName(pckName);
				msg += "\n";
			}

			msg += "\n";
			msg += getString(R.string.old_package_msg2);

			builder.setMessage(msg);

			builder.setPositiveButton(getString(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

							// register as Flurry event
							Map<String, String> parameters = new ArrayMap<String, String>();
							parameters.put("numPackages",
									String.valueOf(oldPackages.size()));
							FlurryAgent.logEvent("TRANSFER_PACKAGES",
									parameters);

							for (String existingPackage : oldPackages) {
								packageManager
										.startPackageDownload(existingPackage);

								Intent intent = new Intent(MainActivity.this,
										PackageDownloadService.class);

								intent.putExtra("job", "download");
								intent.putExtra("package_id", existingPackage);
								intent.putExtra("position", 0);
								intent.putExtra("level", -1);

								startService(intent);
							}
						}
					});
			builder.setNegativeButton(getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// nothing to do
						}
					});

			new Handler(getMainLooper()).post(new Runnable() {

				@Override
				public void run() {
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			});

			((MapApplication) getApplication()).getPackageManagerComponent()
					.removePackageManagerListener(packageListner);
		}
	}

	private class CustomComparator implements Comparator<File> {

		@Override
		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}
	}

	private String getName(String name) {
		int i = name.lastIndexOf("/");
		if (i == -1) {
			return name;
		} else {
			return name.substring(i + 1);
		}
	}

	private class PackageListener extends PackageManagerListener {

		@Override
		public void onPackageListUpdated() {
			// isPackageListDownloaded = true;

			if (isFirstMsgCanceled) {
				showPackageSuggestionDialog();
			}
		}

		@Override
		public void onPackageListFailed() {

		}

		@Override
		public void onPackageStatusChanged(String id, int version,
				PackageStatus status) {

		}

		@Override
		public void onPackageCancelled(String id, int version) {

		}

		@Override
		public void onPackageUpdated(String id, int version) {

		}

		@Override
		public void onPackageFailed(String id, int version,
				PackageErrorType errorType) {

		}
	}

	public void updateBaseLayer() {
		String styleAssetName = vectorStyleName + ".zip";

		if (vectorStyleName.equals("osmbright3d")) {
			styleAssetName = "osmbright.zip";
		}

		UnsignedCharVector styleBytes = AssetUtils.loadBytes(styleAssetName);

		if (styleBytes != null) {
			// Create style set
			MBVectorTileStyleSet vectorTileStyleSet = new MBVectorTileStyleSet(
					styleBytes);
			vectorTileDecoder = new MBVectorTileDecoder(vectorTileStyleSet);

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

			vectorTileDecoder.setStyleParameter("buildings3d", true);
			vectorTileDecoder.setStyleParameter("texts3d", 1);
			vectorTileDecoder.setStyleParameter("markers3d", 1);

			// Create tile data source for vector tiles
			PackageManagerTileDataSource vectorTileDataSource = new PackageManagerTileDataSource(
					((MapApplication) getApplication())
							.getPackageManagerComponent()
							.getNutiteqPackageManager());

			// Remove old base layer, create new base layer
			if (baseLayer != null) {
				mapView.getLayers().remove(baseLayer);
			}

			baseLayer = new VectorTileLayer(vectorTileDataSource,
					vectorTileDecoder);
			baseLayer.setTileCacheCapacity(Const.TILE_CACHE_SIZE);
			baseLayer
					.setTileSubstitutionPolicy(TileSubstitutionPolicy.TILE_SUBSTITUTION_POLICY_VISIBLE);
			mapView.getLayers().add(baseLayer);
		} else {
			Log.e(Const.LOG_TAG, "map style file must be in project assets: "
					+ vectorStyleName);
		}
	}

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

	private void init() {
		// add layers for GPS tracks
		VectorLayer vectorLayer = new VectorLayer(tracksVectorDataSource);
		mapView.getLayers().add(vectorLayer);

		lineStyleBuilder = new LineStyleBuilder();
		lineStyleBuilder.setColor(new Color(0xFF00b483));// nutiteq
															// green
															// :)
		lineStyleBuilder.setLineJointType(LineJointType.LINE_JOINT_TYPE_ROUND);
		lineStyleBuilder.setStretchFactor(3);
		lineStyleBuilder.setWidth(7);

		// set mapView reference to compass, so it can rotate map when you click
		// on it, gpsButton is also need to disable turn mode when user click on
		// compass
		CompassView compassView = (CompassView) findViewById(R.id.compass_view);
		compassView.setObjects(mapView, locationView);

		// set mapView reference to scale bar, so it can draw itself
		scaleBarView = (ScaleBarView) findViewById(R.id.scale_bar_view);
		scaleBarView.setMapView(mapView);

		searchVectorDataSource = new LocalVectorDataSource(projection);
		VectorLayer searchVectorLayer = new VectorLayer(searchVectorDataSource);
		mapView.getLayers().add(searchVectorLayer);

		bookmarkDataSource = new LocalVectorDataSource(projection);
		VectorLayer bookmarkVectorLayer = new VectorLayer(bookmarkDataSource);
		mapView.getLayers().add(bookmarkVectorLayer);

		Bitmap searchMarkerBitmap = BitmapFactory.decodeResource(
				getResources(), R.drawable.search_marker);
		com.nutiteq.graphics.Bitmap markerBitmap = BitmapUtils
				.createBitmapFromAndroidBitmap(searchMarkerBitmap);

		// Create marker style
		MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
		markerStyleBuilder.setBitmap(markerBitmap);
		markerStyleBuilder.setSize(36);
		MarkerStyle searchMarkerStyle = markerStyleBuilder.buildStyle();

		// add search marker and hide it until user pick up search result
		searchMarker = new Marker(new MapPos(0, 0), searchMarkerStyle);
		searchMarker.setVisible(false);
		searchVectorDataSource.add(searchMarker);

		bookmarkButton = (ImageButton) findViewById(R.id.bookmark_button);

		// Create and set a map event listener, it needs the data source for
		// balloons and compass to listen map rotation
		myMapEventListener = new MyMapEventListener(mapView, compassView,
				scaleBarView, packageSuggestion, searchVectorDataSource,
				bookmarkButton, bookmarkDataSource);
		mapView.setMapEventListener(myMapEventListener);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		mOrientationManager = new OrientationManager(sensorManager,
				((WindowManager) getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay());
		mOrientationManager.addOnChangedListener(this);
	}

	@Override
	public void onRestoreInstanceState(Bundle bundle) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(bundle);

		int state = bundle.getInt("GPS_STATE");
		boolean isPlaceShown = bundle.getBoolean("isMyPlaceShown");

		mapView.setMapRotation(bundle.getFloat(ROTATION), 0);
		mapView.setZoom(bundle.getFloat(ZOOM), 0);
		mapView.setTilt(bundle.getFloat(TILT), 0);
		mapView.setFocusPos(
				new MapPos(bundle.getDouble(FOCUS_X), bundle.getDouble(FOCUS_Y)),
				0);

		searchMarker.setPos(new MapPos(bundle.getDouble(SEARCH_X), bundle
				.getDouble(SEARCH_Y)));
		if (bundle.getString(SEARCH_TITLE) != null) {
			searchMarker.setMetaDataElement("title",
					bundle.getString(SEARCH_TITLE));
		}
		if (bundle.getString(SEARCH_DESC) != null) {
			searchMarker.setMetaDataElement("description",
					bundle.getString(SEARCH_DESC));
		}

		shouldSearchMarkerFocus = bundle.getBoolean(SEARCH_FOCUS);
		ishamburgerClosed = bundle.getBoolean(IS_HAMBURGER);

		long l = bundle.getLong(LOCATIONS_SIZE);

		if (l > 0) {
			linePoses = new MapPosVector();

			for (int i = 0; i < l; i++) {
				linePoses.add(new MapPos(bundle.getDouble(LOCATION_X + i),
						bundle.getDouble(LOCATION_Y + i)));
			}

			lineGPSTrack = new Line(linePoses, lineStyleBuilder.buildStyle());
			tracksVectorDataSource.add(lineGPSTrack);
		}

		// add recorded line on map
		long l2 = bundle.getLong(LOCATIONS_SIZE2);

		if (l2 > 0) {
			MapPosVector poses = new MapPosVector();

			for (int i = 0; i < l2; i++) {
				poses.add(new MapPos(bundle.getDouble(LOCATION_X2 + i), bundle
						.getDouble(LOCATION_Y2 + i)));
			}

			gpsTrackingDB.addLine(poses);
		}

		if (state != LocationView.LOCATION_STATE_OFF) {
			locationView.setState(state);
			// it is off because activity is destroyed
			startGPS(isPlaceShown);
			if (state == LocationView.LOCATION_STATE_FIX
					|| state == LocationView.LOCATION_STATE_TURN) {
				isFixListener = true;
			}
		}

		mResolvingError = bundle != null
				&& bundle.getBoolean(STATE_RESOLVING_ERROR, false);

		boolean isPanelOpen = bundle.getBoolean(PANEL_OPEN);

		if (isPanelOpen) {
			locationView.setPanelIsOpen();
		}

		boolean isTrackingOn = bundle.getBoolean(TRACKING_ON);

		if (isTrackingOn) {
			locationView.setSpeed(bundle.getString(TRACKING_SPEED));
			locationView.setDistance(bundle.getString(TRACKING_DISTANCE));
		}
	}

	private static final String ROTATION = "rotation";
	private static final String ZOOM = "zoom";
	private static final String TILT = "tilt";
	private static final String FOCUS_X = "focus_x";
	private static final String FOCUS_Y = "focus_y";

	public static final String SEARCH_X = "search_x";
	public static final String SEARCH_Y = "search_y";
	public static final String SEARCH_TITLE = "search_t";
	public static final String SEARCH_DESC = "search_d";

	private static final String IS_HAMBURGER = "hamburger";
	private static final String SEARCH_FOCUS = "search";

	private static final String LOCATION_X = "location_x";
	private static final String LOCATION_Y = "location_y";
	private static final String LOCATIONS_SIZE = "location_s";

	private static final String LOCATION_X2 = "location_x2";
	private static final String LOCATION_Y2 = "location_y2";
	private static final String LOCATIONS_SIZE2 = "location_s2";

	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	private static final String TRACKING_SPEED = "tracking_speed";
	private static final String TRACKING_DISTANCE = "tracking_distance";
	private static final String TRACKING_ON = "tracking_on";

	private static final String PANEL_OPEN = "panel_open";

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		bundle.putInt("GPS_STATE", locationView.getState());
		bundle.putBoolean("isMyPlaceShown", isMyPlaceShown);

		bundle.putFloat(ROTATION, mapView.getMapRotation());
		bundle.putFloat(ZOOM, mapView.getZoom());
		bundle.putFloat(TILT, mapView.getTilt());
		bundle.putDouble(FOCUS_X, mapView.getFocusPos().getX());
		bundle.putDouble(FOCUS_Y, mapView.getFocusPos().getY());

		// search marker is shown and when orientation is changed
		bundle.putDouble(SEARCH_X, searchMarker.getGeometry().getCenterPos()
				.getX());
		bundle.putDouble(SEARCH_Y, searchMarker.getGeometry().getCenterPos()
				.getY());
		if (searchMarker.getMetaDataElement("title") != null
				&& !searchMarker.getMetaDataElement("title").equals("")) {
			bundle.putString(SEARCH_TITLE,
					searchMarker.getMetaDataElement("title"));
		}
		if (searchMarker.getMetaDataElement("description") != null
				&& !searchMarker.getMetaDataElement("description").equals("")) {
			bundle.putString(SEARCH_DESC,
					searchMarker.getMetaDataElement("description"));
		}

		bundle.putBoolean(SEARCH_FOCUS, shouldSearchMarkerFocus);
		bundle.putBoolean(IS_HAMBURGER, ishamburgerClosed);

		if (lineGPSTrack != null) {
			bundle.putLong(LOCATIONS_SIZE, linePoses.size());

			for (int i = 0; i < linePoses.size(); i++) {
				bundle.putDouble(LOCATION_X + i, linePoses.get(i).getX());
				bundle.putDouble(LOCATION_Y + i, linePoses.get(i).getY());
			}
		}

		if (!gpsTrackingDB.isGPSTrackingOn()) {
			MapPosVector poses = gpsTrackingDB.getCurrentLinePoses();

			if (poses != null && poses.size() > 1) {
				bundle.putLong(LOCATIONS_SIZE2, poses.size());

				for (int i = 0; i < poses.size(); i++) {
					bundle.putDouble(LOCATION_X2 + i, poses.get(i).getX());
					bundle.putDouble(LOCATION_Y2 + i, poses.get(i).getY());
				}
			}
		}

		bundle.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);

		bundle.putBoolean(PANEL_OPEN, locationView.isPanelOpen());

		bundle.putBoolean(TRACKING_ON, locationView.isTrackingOn());

		if (locationView.isTrackingOn()) {
			bundle.putString(TRACKING_SPEED, locationView.getSpeed());
			bundle.putString(TRACKING_DISTANCE, locationView.getDistance());
		}

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(bundle);
	}

	private boolean isStartLocationUpdate = false;

	private void startLocationUpdates() {
		if (isConnected) {
			LocationServices.FusedLocationApi.requestLocationUpdates(
					mGoogleApiClient, mLocationRequest, this);
		}
		isStartLocationUpdate = true;
	}

	private void stopLocationUpdates() {
		if (isConnected && isStartLocationUpdate) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
					mGoogleApiClient, this);
		}
	}

	// stop GPS animation thread and stop OrientationManager
	private void stopGPS() {
		locationCircle.stopLocationCircle();
		mOrientationManager.stop();
		stopLocationUpdates();
		mGoogleApiClient.disconnect();
		isConnected = false;
	}

	private LocalVectorDataSource locationDataSource;
	private VectorLayer locationAnimationLayer;

	private void startGPS(boolean isMyPlaceShown) {
		this.isMyPlaceShown = isMyPlaceShown;

		if (!mResolvingError) {
			mGoogleApiClient.connect();
		}

		// start GPS and sensors
		mOrientationManager.start();

		startLocationUpdates();

		locationDataSource = new LocalVectorDataSource(projection);
		locationAnimationLayer = new VectorLayer(locationDataSource);
		mapView.getLayers().add(locationAnimationLayer);

		// start GPS animation with time duration, it does nothing until first
		// fix
		locationCircle = new LocationCircle(mapView, locationView,
				locationDataSource);
		locationCircle.start();
	}

	// check does app crash last time and if it HOCKEY SDK give users option to
	// report this crash
	private void checkForCrashes() {
		CrashManager.register(this, Const.HOCKEYAPP_ID,
				new CrashManagerListener() {
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
				});
	}

	private void checkForUpdates() {
		if (BuildConfig.DEBUG) {
			UpdateManager.register(this, Const.HOCKEYAPP_ID);
		}
	}

	// to know if app need to disable GPS if app is not on home screen. if value
	// is true in onPause I disable GPS and enable it in onResume
	private boolean isFromMainActivity = true;

	// TODO if you leave app from activity which is not main activity GPS will
	// not be disabled it will continue to drain battery!

	@Override
	protected void onPause() {
		super.onPause();

		FlurryAgent.onEndSession(this);

		if (isFromMainActivity) {
			if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
				mOrientationManager.stop();
				stopLocationUpdates();
			}
		}

		UpdateManager.unregister();
	}

	@Override
	protected void onResume() {
		super.onResume();

		FlurryAgent.onStartSession(this);

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

		if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
			mOrientationManager.start();
			startLocationUpdates();
		}

		if (!measurementUnit.equals("")) {
			if (measurementUnit.equals(Const.METRIC)) {
				scaleBarView.setMeasurementUnit(ScaleBarView.METRIC_UNIT);
			} else if (measurementUnit.equals(Const.IMPERIAL)) {
				scaleBarView.setMeasurementUnit(ScaleBarView.IMPERIAL_UNIT);
			}
			measurementUnit = "";
		}

		if (!mapLanguage.equals("")) {
			changeLanguage(mapLanguage);
			mapLanguage = "";
		}

		// bug for place with coordinate 0, 0 :)
		if (!searchMarker.isVisible()
				&& searchMarker.getGeometry().getCenterPos().getX() != 0
				&& searchMarker.getGeometry().getCenterPos().getY() != 0) {

			// clear last search marker or initial marker
			searchVectorDataSource.removeAll();

			if (bookmarkButton.getVisibility() == View.VISIBLE) {
				bookmarkButton.setVisibility(View.GONE);
			}

			searchMarker.setVisible(true);
			searchVectorDataSource.add(searchMarker);

			if (shouldSearchMarkerFocus) {
				BalloonPopup clickPopup = null;

				BalloonPopupStyleBuilder balloonPopupStyleBuilder = myMapEventListener
						.getBalloonPopupStyleBuilder();

				VectorElement vectorElement = searchMarker;

				Billboard billboard = (Billboard) vectorElement;
				clickPopup = new BalloonPopup(billboard,
						balloonPopupStyleBuilder.buildStyle(),
						billboard.getMetaDataElement("title"),
						billboard.getMetaDataElement("description"));

				searchVectorDataSource.add(clickPopup);
				myMapEventListener.setOldClickLabelWhenSearch(clickPopup,
						searchMarker);

				// release fix or turn state of map and enable search marker to
				// be seen
				if (locationView.getState() == LocationView.LOCATION_STATE_FIX
						|| locationView.getState() == LocationView.LOCATION_STATE_TURN) {
					locationView.setState(LocationView.LOCATION_STATE_ON);
				}

				mapView.moveToFitBounds(
						new MapBounds(projection.fromWgs84(new MapPos(
								Double.parseDouble(billboard
										.getMetaDataElement("x1")), Double
										.parseDouble(billboard
												.getMetaDataElement("y1")))),
								projection.fromWgs84(new MapPos(Double
										.parseDouble(billboard
												.getMetaDataElement("x2")),
										Double.parseDouble(billboard
												.getMetaDataElement("y2"))))),
						new ScreenBounds(new ScreenPos(mapView.getWidth() / 10,
								mapView.getWidth() / 10), new ScreenPos(mapView
								.getWidth() / 10 * 9,
								mapView.getHeight() / 10 * 9)), false, 1);

				shouldSearchMarkerFocus = false;
			}
		}

		isFromMainActivity = true;

		checkForCrashes();
	}

	// use to proper set vectorStyleLang on start of app
	private void changeLang(String lang) {
		if (lang.equals(Const.LANG_AUTOMATIC)) {
			vectorStyleLang = Const.MAP_LANGUAGE_AUTOMATIC;
			return;
		}

		if (lang.equals(Const.LANG_LOCAL)) {
			vectorStyleLang = Const.MAP_LANGUAGE_LOCAL;
			return;
		}

		if (lang.equals(Const.LANG_ENGLISH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ENGLISH;
			return;
		}

		if (lang.equals(Const.LANG_GERMAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_GERMAN;
			return;
		}

		if (lang.equals(Const.LANG_FRENCH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_FRENCH;
			return;
		}

		if (lang.equals(Const.LANG_RUSSIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_RUSSIAN;
			return;
		}

		if (lang.equals(Const.LANG_CHINESE)) {
			vectorStyleLang = Const.MAP_LANGUAGE_CHINESE;
			return;
		}

		if (lang.equals(Const.LANG_SPANISH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_SPANISH;
			return;
		}

		if (lang.equals(Const.LANG_ITALIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ITALIAN;
			return;
		}

		if (lang.equals(Const.LANG_ESTONIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ESTONIAN;
			return;
		}
	}

	private void changeLanguage(String lang) {
		if (lang.equals(Const.LANG_AUTOMATIC)) {
			vectorStyleLang = Const.MAP_LANGUAGE_AUTOMATIC;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_LOCAL)) {
			vectorStyleLang = Const.MAP_LANGUAGE_LOCAL;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_ENGLISH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ENGLISH;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_GERMAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_GERMAN;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_FRENCH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_FRENCH;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_RUSSIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_RUSSIAN;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_CHINESE)) {
			vectorStyleLang = Const.MAP_LANGUAGE_CHINESE;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_SPANISH)) {
			vectorStyleLang = Const.MAP_LANGUAGE_SPANISH;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_ITALIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ITALIAN;
			updateBaseLayer();
			return;
		}

		if (lang.equals(Const.LANG_ESTONIAN)) {
			vectorStyleLang = Const.MAP_LANGUAGE_ESTONIAN;
			updateBaseLayer();
			return;
		}
	}

	@Override
	protected void onDestroy() {
		if (locationView.getState() != LocationView.LOCATION_STATE_OFF) {
			stopGPS();
		}

		shouldRefreshFavoriteLocations = false;
		locationBookmarkId = -1;

		// store current states for mapView
		SharedPreferences preferences = getSharedPreferences(SHARED_PREFS,
				MODE_PRIVATE);
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
			startActivityForResult(new Intent(this,
					LocationBookmarksListActivity.class), 3);

			return true;
		case R.id.menu_tracks:
			isFromMainActivity = false;
			startActivity(new Intent(this, TracksActivity.class));

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
			if (ishamburgerClosed) {
				super.onBackPressed();
			} else {
				hamburgerMenuLayout.closeDrawer(hamburgerMenuList);
			}
		} else {
			finish();
		}
	}

	/*
	 * The group click listener for AnimatedExpandableListView in the hamburger
	 * menu
	 */
	private class HamburgerMenuGroupClickListener implements
			ExpandableListView.OnGroupClickListener {

		@Override
		public boolean onGroupClick(ExpandableListView parent, View v,
				int groupPosition, long id) {
			if (groupPosition > 5) {
				// We call collapseGroupWithAnimation(int) and
				// expandGroupWithAnimation(int) to animate
				// group expansion/collapse.
				if (hamburgerMenuList.isGroupExpanded(groupPosition)) {
					hamburgerMenuList.collapseGroupWithAnimation(groupPosition);
				} else {
					hamburgerMenuList.expandGroupWithAnimation(groupPosition);
				}
			} else {
				// click actions
				groupItemClick(groupPosition);
			}

			return true;
		}
	}

	// hamburger menu group clicks actions
	private void groupItemClick(int groupPosition) {
		switch (groupPosition) {
		case 1:
			isFromMainActivity = false;
			startActivity(new Intent(this, PackageDownloadListActivity.class));

			break;
		case 2:
			hamburgerMenuLayout.closeDrawer(hamburgerMenuList);

			isFromMainActivity = false;

			startActivityForResult(new Intent(this,
					LocationBookmarksListActivity.class), 3);

			break;
		case 3:
			hamburgerMenuLayout.closeDrawer(hamburgerMenuList);

			isFromMainActivity = false;

			startActivity(new Intent(this, TracksActivity.class));

			break;
		case 4:
			isFromMainActivity = false;
			startActivity(new Intent(this, SettingsActivity11.class));

			break;
		case 5:
			isFromMainActivity = false;
			startActivity(new Intent(this, InfoActivity.class));

			break;
		}
	}

	private static class GroupItem {
		Drawable image;
		String title;
		List<ChildItem> items = new ArrayList<ChildItem>();
	}

	private static class ChildItem {
		String title;
	}

	private static class GroupHolder {
		ImageView image;
		TextView title;
	}

	private static class ChildHolder {
		TextView title;
	}

	/**
	 * Adapter for our list of {@link GroupItem}s.
	 */
	private class MyAdapter extends AnimatedExpandableListAdapter {
		private LayoutInflater inflater;

		private List<GroupItem> items;

		public MyAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void setData(List<GroupItem> items) {
			this.items = items;
		}

		@Override
		public ChildItem getChild(int groupPosition, int childPosition) {
			return items.get(groupPosition).items.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getRealChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			ChildHolder holder;
			ChildItem item = getChild(groupPosition, childPosition);
			if (convertView == null) {
				holder = new ChildHolder();
				convertView = inflater.inflate(R.layout.hamburger_submenu_item,
						parent, false);

				holder.title = (TextView) convertView
						.findViewById(R.id.textTitle);

				convertView.setTag(holder);
			} else {
				holder = (ChildHolder) convertView.getTag();
			}

			holder.title.setText(item.title);

			return convertView;
		}

		@Override
		public int getRealChildrenCount(int groupPosition) {
			return items.get(groupPosition).items.size();
		}

		@Override
		public GroupItem getGroup(int groupPosition) {
			return items.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return items.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			GroupHolder holder;
			GroupItem item = getGroup(groupPosition);
			if (convertView == null) {
				holder = new GroupHolder();
				if (groupPosition == 0) {
					convertView = inflater.inflate(R.layout.hamburger_logo,
							parent, false);
				} else {
					convertView = inflater.inflate(
							R.layout.hamburger_menu_item, parent, false);
				}
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				holder.title = (TextView) convertView
						.findViewById(R.id.textTitle);
				convertView.setTag(holder);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}

			holder.image.setImageDrawable(item.image);
			holder.title.setText(item.title);

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}
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

	public void addtrackOnMap(TrackData trackData,
			boolean isTrackingOnForThisTrack) {
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

			if (lineGPSTrack != null) {
				tracksVectorDataSource.remove(lineGPSTrack);
			}

			if (!isTrackingOnForThisTrack) {
				lineGPSTrack = new Line(linePoses,
						lineStyleBuilder.buildStyle());

				tracksVectorDataSource.add(lineGPSTrack);
			}

			mapView.moveToFitBounds(
					new MapBounds(projection.fromWgs84(new MapPos(xMin, yMin)),
							projection.fromWgs84(new MapPos(xMax, yMax))),
					new ScreenBounds(new ScreenPos(mapView.getWidth() / 10,
							mapView.getWidth() / 10), new ScreenPos(mapView
							.getWidth() / 10 * 9, mapView.getHeight() / 10 * 9)),
					false, 1);
		} else {
			Toast.makeText(this, getString(R.string.no_locations),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case 1:
			if (resultCode == Activity.RESULT_OK && data != null) {
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
				searchMarker.setPos(new MapPos(
						data.getDoubleExtra(SEARCH_X, 0), data.getDoubleExtra(
								SEARCH_Y, 0)));

				if (data.getStringExtra(SEARCH_TITLE) != null) {
					searchMarker.setMetaDataElement("title",
							data.getStringExtra(SEARCH_TITLE));
				}
				if (data.getStringExtra(SEARCH_DESC) != null) {
					searchMarker.setMetaDataElement("description",
							data.getStringExtra(SEARCH_DESC));
				}

				if (data.getStringExtra("x1") != null) {
					searchMarker.setMetaDataElement("x1",
							data.getStringExtra("x1"));
				}
				if (data.getStringExtra("x2") != null) {
					searchMarker.setMetaDataElement("x2",
							data.getStringExtra("x2"));
				}
				if (data.getStringExtra("y1") != null) {
					searchMarker.setMetaDataElement("y1",
							data.getStringExtra("y1"));
				}
				if (data.getStringExtra("y2") != null) {
					searchMarker.setMetaDataElement("y2",
							data.getStringExtra("y2"));
				}

				searchMarker.setVisible(false);// flag for onResume
				shouldSearchMarkerFocus = true;
			}

			break;
		case 3:

			break;
		case REQUEST_RESOLVE_ERROR:
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				// Make sure the app is not already connected or attempting to
				// connect
				if (!mGoogleApiClient.isConnecting()
						&& !mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
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

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (mResolvingError) {
			// Already attempting to resolve an error.
			return;
		} else if (result.hasResolution()) {
			try {
				mResolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (SendIntentException e) {
				// There was an error with the resolution intent. Try again.
				mGoogleApiClient.connect();
			}
		} else {
			// Show dialog using GooglePlayServicesUtil.getErrorDialog()
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

	private boolean isConnected = false;

	@Override
	public void onConnected(Bundle bundle) {
		isConnected = true;
		if (isStartLocationUpdate) {
			startLocationUpdates();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getSupportFragmentManager(), "errordialog");
	}

	/* Called from ErrorDialogFragment when the dialog is dismissed. */
	public void onDialogDismissed() {
		mResolvingError = false;
	}

	/* A fragment to display an error dialog */
	public static class ErrorDialogFragment extends DialogFragment {
		public ErrorDialogFragment() {
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode,
					this.getActivity(), REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((MainActivity) getActivity()).onDialogDismissed();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d(Const.LOG_TAG, "onLocationChanged to " + location);
		if (location.hasAccuracy()) {
			lastGPSlocation = projection.fromWgs84(new MapPos(location
					.getLongitude(), location.getLatitude()));

			if (locationView.getState() == LocationView.LOCATION_STATE_ON
					&& !isMyPlaceShown) {
				isMyPlaceShown = true;
				mapView.setFocusPos(lastGPSlocation, 1);

			} else if (locationView.getState() == LocationView.LOCATION_STATE_FIX
					|| locationView.getState() == LocationView.LOCATION_STATE_TURN) {
				mapView.setFocusPos(lastGPSlocation, 0.4f);
			}

			// set location and accuracy, so it can draw polygon circle
			// animation with accuracy radius if it can be seen on map
			locationCircle.setLocation(location.getLongitude(),
					location.getLatitude(), location.getAccuracy());
		}
	}
}