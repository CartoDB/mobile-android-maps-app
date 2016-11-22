package com.nutiteq.nuticomponents.packagemanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.nutiteq.nuticomponents.Const;
import com.nutiteq.nuticomponents.R;
import com.carto.packagemanager.CartoPackageManager;
import com.carto.packagemanager.PackageErrorType;
import com.carto.packagemanager.PackageManagerListener;
import com.carto.packagemanager.PackageStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Milan Ivankovic, Nole
 *         <p/>
 *         PackageManager class manage all NutiteqPackageManager needs for an
 *         app
 */
public class PackageManagerComponent {

    private Context context;

    public static final int INTERNAL_STORAGE = 0;
    public static final int EXTERNAL_STORAGE = 1;

    private int storageType;
    private int multiStorageNumber;

    private CartoPackageManager packageManager;
    private CartoPackageManager packageManagerRouting;
    private List<PackageManagerListener> listeners = new ArrayList<PackageManagerListener>();
    private List<PackageManagerListener> listenersRouting = new ArrayList<PackageManagerListener>();

    private int currentStorageType;
    private int currentStorageNumber;

    private boolean isSetToDefaultInternal = false;

    private Handler handler = new Handler();
    private int i;

    /**
     * PackageManagerComponent constructor
     *
     * @param Context context, int storageType, int multiStorageNumber storageType
     *                can be INTERNAL_STORAGE or EXTERNAL_STORAGE,
     *                multiStorageNumber for INTERNAL_STORAGE doesn't have any
     *                effect and for EXTERNAL_STORAGE starting from android 4.4
     *                devices can have multiply external storage and you must set
     *                which storage do you want to use, for example 1 or 2 with two
     *                external storage. If external storage isn't available than
     *                internal will be used and you can check this with method
     *                isSetToDefaultInternal()
     */
    @SuppressLint("NewApi")
    public PackageManagerComponent(Context context, int storageType, int multiStorageNumber) {

        this.context = context;
        this.currentStorageType = storageType;
        this.currentStorageNumber = multiStorageNumber;
        this.storageType = storageType;
        this.multiStorageNumber = multiStorageNumber;

        init();
    }

    private void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // if there is external storage use map storage by user preferences,
            // otherwise internal is always available and use internal
            if (storageType == INTERNAL_STORAGE) {
                setPackageManagerToInternal();
            } else {
                File[] files = context.getExternalFilesDirs(null);

                int n = multiStorageNumber;

                if (files[n - 1] == null) {
                    if (files[0] == null) {
                        setPackageManagerToInternal();
                        isSetToDefaultInternal = true;
                    } else {
                        setPackageManagerToExternal(1);
                    }
                } else {
                    setPackageManagerToExternal(n);
                }
            }
        } else {
            if (storageType == INTERNAL_STORAGE) {
                setPackageManagerToInternal();
            } else {
                if (Environment.MEDIA_MOUNTED.equals(Environment
                        .getExternalStorageState())) {
                    setPackageManagerToExternal(1);
                } else {
                    setPackageManagerToInternal();
                    isSetToDefaultInternal = true;
                }
            }
        }
    }

    private void setPackageManagerToInternal() {

        File packageFolderInternal = new File(context.getFilesDir(), "mappackages");
        File packageFolderInternalRouting = new File(context.getFilesDir(), "maprouting");

        if (!(packageFolderInternal.mkdirs() || packageFolderInternal.isDirectory())) {
            Log.e(Const.LOG_TAG, "Could not create package folder : "
                    + packageFolderInternal);
        }
        if (!(packageFolderInternalRouting.mkdirs() || packageFolderInternalRouting.isDirectory())) {
            Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderInternalRouting);
        }

        Log.i(Const.LOG_TAG, "using package folder " + packageFolderInternal.getAbsolutePath());
        try {
            packageManager = new CartoPackageManager(Const.NUTITEQ_SOURCE_ID, packageFolderInternal.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        packageManager.setPackageManagerListener(new PackageListenerHub());
        packageManager.start();

        Log.i(Const.LOG_TAG,
                "using package folder " + packageFolderInternalRouting.getAbsolutePath());
        try {
            packageManagerRouting = new CartoPackageManager(
                    Const.NUTITEQ_ROUTING_SOURCE_ID,
                    packageFolderInternalRouting.getAbsolutePath()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        packageManagerRouting.setPackageManagerListener(new PackageRoutingListenerHub());
        packageManagerRouting.start();

        currentStorageType = INTERNAL_STORAGE;
        currentStorageNumber = 1;
    }

    @SuppressLint("NewApi")
    private void setPackageManagerToExternal(int multiStorageNumber) {

        File packageFolder;
        File packageFolderRouting;

        if (multiStorageNumber == 1) {
            packageFolder = new File(context.getExternalFilesDir(null), "mappackages");
            if (!(packageFolder.mkdirs() || packageFolder.isDirectory())) {
                Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolder);
            }

            packageFolderRouting = new File(context.getExternalFilesDir(null), "maprouting");
            if (!(packageFolderRouting.mkdirs() || packageFolderRouting.isDirectory())) {
                Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRouting);
            }
        } else {
            packageFolder = new File(context.getExternalFilesDirs(null)[multiStorageNumber - 1], "mappackages");
            if (!(packageFolder.mkdirs() || packageFolder.isDirectory())) {
                Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolder);
            }

            packageFolderRouting = new File(context.getExternalFilesDirs(null)[multiStorageNumber - 1], "maprouting");
            if (!(packageFolderRouting.mkdirs() || packageFolderRouting.isDirectory())) {
                Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRouting);
            }
        }

        Log.i(Const.LOG_TAG, "using package folder " + packageFolder.getAbsolutePath());

        try {
            packageManager = new CartoPackageManager(Const.NUTITEQ_SOURCE_ID, packageFolder.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        packageManager.setPackageManagerListener(new PackageListenerHub());
        packageManager.start();

        Log.i(Const.LOG_TAG, "using package routing folder " + packageFolderRouting.getAbsolutePath());

        try {
            packageManagerRouting = new CartoPackageManager(Const.NUTITEQ_ROUTING_SOURCE_ID, packageFolderRouting.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        packageManagerRouting.setPackageManagerListener(new PackageRoutingListenerHub());
        packageManagerRouting.start();

        currentStorageType = EXTERNAL_STORAGE;
        currentStorageNumber = multiStorageNumber;
    }

    @SuppressLint("NewApi")
    private class MoveFromTo extends AsyncTask<Void, Void, Boolean> {
        /**
         * The system calls this to perform work in a worker thread and delivers
         * it the parameters given to AsyncTask.execute()
         *
         * @return
         */
        protected Boolean doInBackground(Void... params) {
            // first stop it
            packageManager.stop(true);
            packageManagerRouting.stop(true);

            File packageFolderFrom;
            File packageFolderTo;

            File packageFolderRoutingFrom;
            File packageFolderRoutingTo;

            File worldMBtileFileFrom;
            File worldMBtileFileTo;

            boolean isMovingFinished = false;

            // same location
            if (from == to && fromStorageNumber == toStorageNumber) {
                return isMovingFinished;
            }

            if (from == INTERNAL_STORAGE) {
                packageFolderFrom = new File(context.getFilesDir(), "mappackages");
                packageFolderRoutingFrom = new File(context.getFilesDir(), "maprouting");

                worldMBtileFileFrom = new File(context.getFilesDir(), Const.BASE_PACKAGE_ASSET_NAME);

                if (!(packageFolderFrom.mkdirs() || packageFolderFrom.isDirectory())) {
                    Log.e(Const.LOG_TAG, "Could not create package folder : "
                            + packageFolderFrom);
                }

                if (!(packageFolderRoutingFrom.mkdirs() || packageFolderRoutingFrom.isDirectory())) {
                    Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingFrom);
                }
            } else if (from == EXTERNAL_STORAGE) {
                if (fromStorageNumber == 1) {
                    packageFolderFrom = new File(context.getExternalFilesDir(null), "mappackages");
                    packageFolderRoutingFrom = new File(context.getExternalFilesDir(null), "maprouting");

                    worldMBtileFileFrom = new File(context.getExternalFilesDir(null), Const.BASE_PACKAGE_ASSET_NAME);

                    if (!(packageFolderFrom.mkdirs() || packageFolderFrom.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolderFrom);
                    }

                    if (!(packageFolderRoutingFrom.mkdirs() || packageFolderRoutingFrom.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingFrom);
                    }
                } else {
                    packageFolderFrom = new File(context.getExternalFilesDirs(null)[fromStorageNumber - 1], "mappackages");
                    packageFolderRoutingFrom = new File(context.getExternalFilesDirs(null)[fromStorageNumber - 1], "maprouting");

                    worldMBtileFileFrom = new File(context.getExternalFilesDirs(null)[fromStorageNumber - 1], Const.BASE_PACKAGE_ASSET_NAME);

                    if (!(packageFolderFrom.mkdirs() || packageFolderFrom.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolderFrom);
                    }

                    if (!(packageFolderRoutingFrom.mkdirs() || packageFolderRoutingFrom.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingFrom);
                    }
                }
            } else {
                return isMovingFinished;
            }

            if (to == INTERNAL_STORAGE) {
                packageFolderTo = new File(context.getFilesDir(), "mappackages");
                packageFolderRoutingTo = new File(context.getFilesDir(), "maprouting");

                worldMBtileFileTo = new File(context.getFilesDir(), Const.BASE_PACKAGE_ASSET_NAME);

                if (!(packageFolderTo.mkdirs() || packageFolderTo.isDirectory())) {
                    Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolderTo);
                }

                if (!(packageFolderRoutingTo.mkdirs() || packageFolderRoutingTo.isDirectory())) {
                    Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingTo);
                }
            } else if (to == EXTERNAL_STORAGE) {
                if (toStorageNumber == 1) {
                    packageFolderTo = new File(context.getExternalFilesDir(null), "mappackages");
                    packageFolderRoutingTo = new File(context.getExternalFilesDir(null), "maprouting");

                    worldMBtileFileTo = new File(context.getExternalFilesDir(null), Const.BASE_PACKAGE_ASSET_NAME);

                    if (!(packageFolderTo.mkdirs() || packageFolderTo.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolderTo);
                    }

                    if (!(packageFolderRoutingTo.mkdirs() || packageFolderRoutingTo.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingTo);
                    }
                } else {
                    packageFolderTo = new File(context.getExternalFilesDirs(null)[toStorageNumber - 1], "mappackages");
                    packageFolderRoutingTo = new File(context.getExternalFilesDirs(null)[toStorageNumber - 1], "maprouting");

                    worldMBtileFileTo = new File(context.getExternalFilesDirs(null)[toStorageNumber - 1], Const.BASE_PACKAGE_ASSET_NAME);

                    if (!(packageFolderTo.mkdirs() || packageFolderTo.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package folder : " + packageFolderTo);
                    }

                    if (!(packageFolderRoutingTo.mkdirs() || packageFolderRoutingTo.isDirectory())) {
                        Log.e(Const.LOG_TAG, "Could not create package routing folder : " + packageFolderRoutingTo);
                    }
                }
            } else {
                return isMovingFinished;
            }

            // move all files
            try {
                final File[] f = packageFolderFrom.listFiles();
                final File[] f2 = packageFolderRoutingFrom.listFiles();

                for (i = 0; i < f.length; i++) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.setMessage(context
                                    .getString(R.string.move)
                                    + " "
                                    + (i + 1)
                                    + "/" + (f.length + 1 + f2.length));
                        }
                    });
                    copyFile(new FileInputStream(f[i]), new FileOutputStream(new File(packageFolderTo, f[i].getName())));
                    f[i].delete();
                }

                for (i = 0; i < f2.length; i++) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.setMessage(context
                                    .getString(R.string.move)
                                    + " "
                                    + (i + 1 + f.length)
                                    + "/" + (f.length + 1 + f2.length));
                        }
                    });
                    copyFile(new FileInputStream(f2[i]), new FileOutputStream(
                            new File(packageFolderRoutingTo, f2[i].getName())));
                    f2[i].delete();
                }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        progressDialog.setMessage(context
                                .getString(R.string.move)
                                + " "
                                + (f.length + 1 + f2.length) + "/" + (f.length + 1 + f2.length));
                    }
                });
                copyFile(new FileInputStream(worldMBtileFileFrom),
                        new FileOutputStream(worldMBtileFileTo));
                worldMBtileFileFrom.delete();

                isMovingFinished = true;
            } catch (IOException e) {
                Log.e(Const.LOG_TAG, "Error moving files from: " + from
                        + " to " + to + " memory");
                isMovingFinished = false;
            }

            Log.i(Const.LOG_TAG, "using package folder " + packageFolderTo.getAbsolutePath());

            try {
                packageManager = new CartoPackageManager(Const.NUTITEQ_SOURCE_ID, packageFolderTo.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            packageManager.setPackageManagerListener(new PackageListenerHub());
            packageManager.start();

            Log.i(Const.LOG_TAG,
                    "using package routing folder " + packageFolderRoutingTo.getAbsolutePath());

            try {
                packageManagerRouting = new CartoPackageManager(Const.NUTITEQ_ROUTING_SOURCE_ID,
                        packageFolderRoutingTo.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            packageManagerRouting.setPackageManagerListener(new PackageRoutingListenerHub());
            packageManagerRouting.start();

            return isMovingFinished;
        }

        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(Boolean b) {
            // unlock orientation
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            progressDialog.dismiss();

            if (b) {
                currentStorageType = to;
                currentStorageNumber = toStorageNumber;

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        r1.run();
                    }
                });
            } else {
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        r2.run();
                    }
                });
            }
        }
    }

    private int from;
    private int to;
    private int fromStorageNumber;
    private int toStorageNumber;
    private ProgressDialog progressDialog;
    private Runnable r1;
    private Runnable r2;
    private Activity activity;

    /**
     * Call this method to move downloaded maps from any storage to any storage
     * type (internal to external, external to internal or if multiply external
     * exist for example external1 to external2 or some other combination). You
     * must set from which storage to move and also fromStorageNumber.
     * fromStorageNumber for internal storage is 1 and it's ignored because
     * there is always just one internal. If you set from to external storage
     * and device has two external storage you can set 1 or 2 for
     * fromStorageNumber. Also, you must set to where to move files. For
     * toStorageNumber you set external storage number or 1 for internal
     * storage. For example if device has only one external storage set it to 1,
     * if multiply set it on which you want to move. You must set Runnable r1
     * which will run if moving files finish successful and Runnable r2 if
     * moving files finish unsuccessful. Also you must set Activity from which
     * you call this method, so progress dialog can be attached.
     */
    public void movePackageManagerFromTo(int from, int fromStorageNumber,
                                         int to, int toStorageNumber, Runnable r1, Runnable r2,
                                         Activity activity) {
        this.from = from;
        this.fromStorageNumber = fromStorageNumber;
        this.to = to;
        this.toStorageNumber = toStorageNumber;
        this.r1 = r1;
        this.r2 = r2;
        this.activity = activity;

        // lock orientation
        lockOrientation(this.activity);

        handler.post(new Runnable() {

            @Override
            public void run() {
                // show spinning
                progressDialog = new ProgressDialog(
                        PackageManagerComponent.this.activity);
                progressDialog.setMessage(context.getString(R.string.move));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        });

        new MoveFromTo().execute();
    }

    private class PackageListenerHub extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageListUpdated();
            }
        }

        @Override
        public void onPackageListFailed() {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageListFailed();
            }
        }

        @Override
        public void onPackageStatusChanged(String id, int version,
                                           PackageStatus status) {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageStatusChanged(id, version, status);
            }
        }

        @Override
        public void onPackageCancelled(String id, int version) {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageCancelled(id, version);
            }
        }

        @Override
        public void onPackageUpdated(String id, int version) {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageUpdated(id, version);
            }
        }

        @Override
        public void onPackageFailed(String id, int version,
                                    PackageErrorType errorType) {
            for (PackageManagerListener listener : getPackageManagerListeners()) {
                listener.onPackageFailed(id, version, errorType);
            }
        }
    }

    private class PackageRoutingListenerHub extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageListUpdated();
            }
        }

        @Override
        public void onPackageListFailed() {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageListFailed();
            }
        }

        @Override
        public void onPackageStatusChanged(String id, int version,
                                           PackageStatus status) {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageStatusChanged(id, version, status);
            }
        }

        @Override
        public void onPackageCancelled(String id, int version) {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageCancelled(id, version);
            }
        }

        @Override
        public void onPackageUpdated(String id, int version) {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageUpdated(id, version);
            }
        }

        @Override
        public void onPackageFailed(String id, int version,
                                    PackageErrorType errorType) {
            for (PackageManagerListener listener : getPackageManagerRoutingListeners()) {
                listener.onPackageFailed(id, version, errorType);
            }
        }
    }

    public List<PackageManagerListener> getPackageManagerListeners() {
        synchronized (listeners) {
            return new ArrayList<PackageManagerListener>(listeners);
        }
    }

    public void addPackageManagerListener(PackageManagerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removePackageManagerListener(PackageManagerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void removePackageManagerRoutingListener(PackageManagerListener listener) {
        synchronized (listenersRouting) {
            listenersRouting.remove(listener);
        }
    }

    public List<PackageManagerListener> getPackageManagerRoutingListeners() {
        synchronized (listenersRouting) {
            return new ArrayList<PackageManagerListener>(listenersRouting);
        }
    }

    public void addPackageManagerRoutingListener(PackageManagerListener listener) {
        synchronized (listenersRouting) {
            listenersRouting.add(listener);
        }
    }

    public CartoPackageManager getPackageManager() {
        return packageManager;
    }

    public CartoPackageManager getRoutingPackageManager() {
        return packageManagerRouting;
    }

    public int getStorageType() {
        return currentStorageType;
    }

    public int getStorageNumber() {
        return currentStorageNumber;
    }

    public boolean isSetToDefaultInternal() {
        return isSetToDefaultInternal;
    }

    private static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static void lockOrientation(Activity activity) {

        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = 0;

        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
        }

        activity.setRequestedOrientation(orientation);
    }
}
