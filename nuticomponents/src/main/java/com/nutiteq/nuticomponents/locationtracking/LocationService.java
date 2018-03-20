package com.nutiteq.nuticomponents.locationtracking;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.nutiteq.nuticomponents.R;

public class LocationService extends Service {

    // flag to know is tracking ON
    public static boolean isLocationTrackingOn = false;

    // flag to know is location listener ON
    public static boolean isLocationListenerOn = false;

    private LocationTrackingDB locationTrackingDB;
    private long tracksID;

    private static int MAX_ALLOWED_ACCURACY = 50;

    public static final String START_TRACKING_FLAG = "start_tracking";
    public static final String START_LOCATION_FLAG = "start_location";
    public static final String STOP_TRACKING_FLAG = "stop_tracking";
    public static final String STOP_LOCATION_FLAG = "stop_location";

    private Location lastTrackingLocation = null;
    private long lastInsertedTrackingLocationTimestamp;

    private long timestamp;

    private LocationManager locationManager;
    private android.location.LocationListener locationListener;

    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int percent = (level * 100) / scale;

            if (percent < 10) {
                locationTrackingDB.stop(tracksID);

                Toast.makeText(context, getString(R.string.tracks_battery),
                        Toast.LENGTH_LONG).show();

                LocationService.this.stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        locationTrackingDB = ((LocationTrackingApplicationInterface) getApplication())
                .getLocationTrackingDB();

        locationManager = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);

        locationListener = new android.location.LocationListener() {

            public void onLocationChanged(final Location location) {
                if (isLocationListenerOn && location != null && location.hasAccuracy()) {
                    ((LocationTrackingApplicationInterface) getApplication())
                            .setLocation(location);
                }

                if (isLocationTrackingOn && location.hasAccuracy()) {
                    timestamp = System.currentTimeMillis();

                    if (lastTrackingLocation != null) {
                        if (location.getAccuracy() / 2 >= lastTrackingLocation.getAccuracy()) {
                            if (timestamp - lastInsertedTrackingLocationTimestamp > 30000) {
                                insertGPSTrack(location, timestamp);
                            }
                        } else {
                            insertGPSTrack(location, timestamp);
                        }
                    } else {
                        insertGPSTrack(location, timestamp);
                    }

                    lastTrackingLocation = location;
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int finePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int granted = PackageManager.PERMISSION_GRANTED;

        if (intent != null) {
            if (intent.getBooleanExtra(START_TRACKING_FLAG, false)) {
                tracksID = locationTrackingDB.insertTrack();

                if (tracksID == -1) {
                    locationTrackingDB.stop(tracksID);

                    Toast.makeText(this, getString(R.string.tracks_error),
                            Toast.LENGTH_LONG).show();
                } else {
                    isLocationTrackingOn = true;

                    if (finePermission == granted && coarsePermission == granted) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                1000, 0, locationListener);
                    } else {
                        Toast.makeText(this, "Network permission not granted", Toast.LENGTH_SHORT).show();
                    }

                    if (finePermission == granted && coarsePermission == granted) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                1000, 0, locationListener);
                    } else {
                        Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_SHORT).show();
                    }

                    registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                }
            } else if (intent.getBooleanExtra(START_LOCATION_FLAG, false)) {
                isLocationListenerOn = true;

                if (finePermission == granted && coarsePermission == granted) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            1000, 0, locationListener);
                } else {
                    Toast.makeText(this, "Network permission not granted", Toast.LENGTH_SHORT).show();
                }

                if (finePermission == granted && coarsePermission == granted) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            1000, 0, locationListener);
                } else {
                    Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_SHORT).show();
                }

            } else if (intent.getBooleanExtra(STOP_LOCATION_FLAG, false)) {
                if (!isLocationTrackingOn) {
                    locationManager.removeUpdates(locationListener);
                    isLocationListenerOn = false;

                    stopSelf();
                } else {
                    isLocationListenerOn = false;
                }
            } else if (intent.getBooleanExtra(STOP_TRACKING_FLAG, false)) {
                locationTrackingDB.stop(tracksID);

                isLocationTrackingOn = false;
            }
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListener);

        if (isLocationTrackingOn) {
            unregisterReceiver(batteryInfoReceiver);
        }

        isLocationListenerOn = false;
        isLocationTrackingOn = false;

        super.onDestroy();
    }

    private void insertGPSTrack(Location location, long timestamp) {
        if (location.getAccuracy() <= MAX_ALLOWED_ACCURACY) {
            locationTrackingDB.insertTrackLocation(tracksID, location);
            lastInsertedTrackingLocationTimestamp = timestamp;
        }
    }
}
