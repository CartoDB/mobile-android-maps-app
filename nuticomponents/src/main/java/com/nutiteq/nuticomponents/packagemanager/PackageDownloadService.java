package com.nutiteq.nuticomponents.packagemanager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import com.carto.packagemanager.CartoPackageManager;
import com.carto.packagemanager.PackageAction;
import com.carto.packagemanager.PackageErrorType;
import com.carto.packagemanager.PackageInfo;
import com.carto.packagemanager.PackageInfoVector;
import com.carto.packagemanager.PackageManagerListener;
import com.carto.packagemanager.PackageStatus;
import com.nutiteq.nuticomponents.Const;
import com.nutiteq.nuticomponents.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class PackageDownloadService extends Service {

    private CartoPackageManager packageManager;
    private CartoPackageManager packageManagerRouting;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder = null;
    private PackageListener packageListner = new PackageListener();
    private PackageRoutingListener packageRoutingListner = new PackageRoutingListener();

    private ArrayList<NotificationObject> jobs = new ArrayList<NotificationObject>();
    private ArrayList<Package> packageArray = new ArrayList<Package>();

    /**
     * Contain information is service live or not
     */
    public static boolean isLive = false;

    private boolean isStarted = false;

    private int storageType;
    private int storageNumber;

    private Handler handler;

    @Override
    public void onCreate() {
        storageType = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getStorageType();
        storageNumber = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getStorageNumber();

        handler = new Handler(getMainLooper());

        // Get package managers and set listener
        packageManager = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getPackageManager();
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().addPackageManagerListener(
                packageListner);

        packageManagerRouting = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getRoutingPackageManager();
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().addPackageManagerRoutingListener(
                packageRoutingListner);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        updatePackages();

        isLive = true;
    }

    @Override
    public void onDestroy() {
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().removePackageManagerListener(
                packageListner);
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().removePackageManagerRoutingListener(
                packageRoutingListner);

        isLive = false;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setIntent(intent);

        isStarted = true;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    private void setIntent(Intent intent) {
        if (intent != null) {
            String job = intent.getExtras().getString("job");

            if (job.equals("download")) {
                ArrayList<StackObject> stack = new ArrayList<StackObject>();
                int i = 0;
                String s = intent.getExtras().getString("stack" + i);
                int p = intent.getExtras().getInt("position" + i);
                while (s != null) {
                    stack.add(new StackObject(s, p));
                    i++;
                    s = intent.getExtras().getString("stack" + i);
                    p = intent.getExtras().getInt("position" + i);
                }

                jobs.add(new NotificationObject(jobs.size(), intent.getExtras()
                        .getString("package_id"), intent.getExtras().getInt(
                        "position"), intent.getExtras().getInt("level"), stack, intent.getExtras()
                        .getBoolean("routing_pkg")));
            } else if (job.equals("cancel")) {
                // find job that should be canceled and flag it
                for (int i = 0; i < jobs.size(); i++) {
                    if (intent.getExtras().getString("package_id")
                            .equals(jobs.get(i).packageId)) {
                        jobs.get(i).shouldCancel = true;
                        break;
                    }
                }
            } else if (job.equals("pause")) {
                // find job that should be paused
                for (int i = 0; i < jobs.size(); i++) {
                    if (intent.getExtras().getString("package_id")
                            .equals(jobs.get(i).packageId)) {
                        packageManager.setPackagePriority(intent.getExtras()
                                .getString("package_id"), -1);
                        if (jobs.get(i).isRoutingPkg) {
                            packageManagerRouting.setPackagePriority(intent.getExtras()
                                    .getString("package_id") + "", -1);
                        }

                        break;
                    }
                }
            } else if (job.equals("play")) {
                // find job that should be resumed
                boolean isFind = false;
                for (int i = 0; i < jobs.size(); i++) {
                    if (intent.getExtras().getString("package_id")
                            .equals(jobs.get(i).packageId)) {
                        packageManager.setPackagePriority(intent.getExtras()
                                .getString("package_id"), 0);

                        if (jobs.get(i).isRoutingPkg) {
                            packageManagerRouting.setPackagePriority(intent.getExtras()
                                    .getString("package_id") + "", 0);
                        }

                        isFind = true;

                        break;
                    }
                }
                // from some reason when app is killed, service is destroyed
                // because jobs size become 0 without known reason for now
                if (!isFind) {
                    ArrayList<StackObject> stack = new ArrayList<StackObject>();
                    int i = 0;
                    String s = intent.getExtras().getString("stack" + i);
                    int p = intent.getExtras().getInt("position" + i);
                    while (s != null) {
                        stack.add(new StackObject(s, p));
                        i++;
                        s = intent.getExtras().getString("stack" + i);
                        p = intent.getExtras().getInt("position" + i);
                    }

                    jobs.add(
                            new NotificationObject(
                                    jobs.size(),
                                    intent.getExtras().getString("package_id"),
                                    intent.getExtras().getInt("position"),
                                    intent.getExtras().getInt("level"),
                                    stack,
                                    intent.getExtras().getBoolean("routing_pkg")
                            )
                    );

                    packageManager.setPackagePriority(intent.getExtras().getString("package_id"), 0);

                    if (intent.getExtras().getBoolean("routing_pkg")) {
                        packageManagerRouting.setPackagePriority(intent.getExtras()
                                .getString("package_id") + "", 0);
                    }
                }
            }

            updatePackage(intent.getExtras().getString("package_id"));
            updateJobs();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private static class Package {

        final PackageInfo packageInfo;
        final PackageInfo packageRoutingInfo;

        PackageStatus packageStatus;
        PackageStatus packageRoutingStatus;

        Package(PackageInfo packageInfo, PackageStatus packageStatus, PackageInfo packageRoutingInfo, PackageStatus packageRoutingStatus) {
            this.packageInfo = packageInfo;
            this.packageStatus = packageStatus;
            this.packageRoutingInfo = packageRoutingInfo;
            this.packageRoutingStatus = packageRoutingStatus;
        }
    }

    private class PackageListener extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            updatePackages();
            updateJobs();
        }

        @Override
        public void onPackageListFailed() {
            updatePackages();
            updateJobs();
        }

        @Override
        public void onPackageStatusChanged(String packageId, int version, PackageStatus status) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageCancelled(String packageId, int version) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageUpdated(String packageId, int version) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageFailed(String packageId, int version, PackageErrorType errorType) {
            // find job that failed to download and flag it
            for (int i = 0; i < jobs.size(); i++) {
                if (packageId.equals(jobs.get(i).packageId)) {
                    jobs.get(i).isDownloadFailed = true;
                    break;
                }
            }
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }
    }

    private class PackageRoutingListener extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            updatePackages();
            updateJobs();
        }

        @Override
        public void onPackageListFailed() {
            updatePackages();
            updateJobs();
        }

        @Override
        public void onPackageStatusChanged(String packageId, int version, PackageStatus status) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageCancelled(String packageId, int version) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageUpdated(String packageId, int version) {
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }

        @Override
        public void onPackageFailed(String packageId, int version, PackageErrorType errorType) {
            // find job that failed to download and flag it
            for (int i = 0; i < jobs.size(); i++) {
                if (packageId.equals(jobs.get(i).packageId)) {
                    jobs.get(i).isDownloadFailed = true;
                    break;
                }
            }
            updatePackage(packageId);
            if (!shouldAllCancel) {
                updateJobs();
            }
        }
        
    }

    private ArrayList<Package> getPackages() {

        ArrayList<Package> pkgs = new ArrayList<Package>();
        PackageInfoVector packageInfoVector = packageManager.getServerPackages();

        for (int i = 0; i < packageInfoVector.size(); i++) {
            PackageInfo packageInfo = packageInfoVector.get(i);

            if (packageInfo.getPackageId().equals(Const.BASE_PACKAGE_ID)) {
                continue; // ignore base package
            }

            PackageInfo packageRoutingInfo = packageManagerRouting.getServerPackage(packageInfo.getPackageId() + "");
            PackageStatus packageRoutingStatus = null;

            if (packageRoutingInfo != null) {
                packageRoutingStatus = packageManagerRouting.getLocalPackageStatus(
                        packageInfo.getPackageId() + "",
                        packageRoutingInfo.getVersion());
            }

            Package pkg = new Package(
                    packageInfo,
                    packageManager.getLocalPackageStatus(packageInfo.getPackageId(), packageInfo.getVersion()),
                    packageRoutingInfo,
                    packageRoutingStatus
            );
            pkgs.add(pkg);
        }

        return pkgs;
    }

    private void updatePackage(final String id) {

        for (Package pkg : packageArray) {
            if (pkg.packageInfo.getPackageId().equals(id)) {
                pkg.packageStatus = packageManager.getLocalPackageStatus(
                        pkg.packageInfo.getPackageId(),
                        pkg.packageInfo.getVersion());

                if (pkg.packageRoutingInfo != null) {
                    pkg.packageRoutingStatus = packageManagerRouting.getLocalPackageStatus(
                            pkg.packageInfo.getPackageId() + "",
                            pkg.packageRoutingInfo.getVersion());
                }

                break;
            }
        }
    }

    private void updatePackages() {
        packageArray.clear();
        packageArray.addAll(getPackages());
    }

    private String getName(String name) {
        int i = name.lastIndexOf("/");
        if (i == -1) {
            return name;
        } else {
            return name.substring(i + 1);
        }
    }

    private Package pkg;

    private long freeStorage;
    private File file;

    private int numOfQueued = 0;
    private int numOfQueuedShowed = 0;

    private boolean shouldAllCancel = false;

    @SuppressLint("NewApi")
    private Runnable r = new Runnable() {

        public void run() {
            numOfQueued = 0;

            if (storageType == PackageManagerComponent.INTERNAL_STORAGE) {
                file = getFilesDir();
                if (file != null) {
                    freeStorage = file.getFreeSpace() / 1048576
                            - Const.INTERNAL_STORAGE_MIN;
                } else {
                    freeStorage = 0;// isn't available
                }
            } else if (storageType == PackageManagerComponent.EXTERNAL_STORAGE
                    && storageNumber == 1) {
                file = getExternalFilesDir(null);
                if (file != null) {
                    freeStorage = file.getFreeSpace() / 1048576
                            - Const.EXTERNAL_STORAGE_MIN;
                } else {
                    freeStorage = 0;
                }
            } else {
                file = getExternalFilesDirs(null)[storageNumber - 1];
                if (file != null) {
                    freeStorage = file.getFreeSpace()
                            - Const.EXTERNAL_STORAGE_MIN * 1048576;
                } else {
                    freeStorage = 0;
                }
            }

            // this is the easiest way to check is there enough space
            // also, it's checked before download start, but this is must if for
            // example one package is downloading and other one is queued, there
            // is
            // no guarantee that there will be enough storage for queued package
            if (freeStorage > 0) {

                if (jobs.size() > 0) {
                    // update all notifications
                    for (int i = 0; i < jobs.size(); i++) {
                        // find package
                        for (int j = 0; j < packageArray.size(); j++) {
                            if (packageArray.get(j).packageInfo.getPackageId()
                                    .equals(jobs.get(i).packageId)) {
                                pkg = packageArray.get(j);
                                break;
                            }
                        }

                        // remove jobs that is canceled or failed to download
                        if (jobs.get(i).shouldCancel) {
                            packageManager
                                    .cancelPackageTasks(jobs.get(i).packageId);
                            if (jobs.get(i).isRoutingPkg) {
                                packageManagerRouting
                                        .cancelPackageTasks(jobs.get(i).packageId + "");
                            }
                            mNotificationManager.cancel(jobs.get(i).id);
                            jobs.remove(i);
                            updateJobs();
                            break;
                        }
                        if (jobs.get(i).isDownloadFailed) {
                            if (PackageDownloadListActivity.isActivityOpen) {
                                mNotificationManager.cancel(jobs.get(i).id);
                            } else {
                                mNotificationManager
                                        .notify(jobs.get(i).id,
                                                buildNotificationFailedDownload(
                                                        getName(pkg.packageInfo
                                                                .getNames(
                                                                        Locale.getDefault()
                                                                                .getLanguage())
                                                                .get(0)), i));
                            }
                            jobs.remove(i);
                            updateJobs();
                            break;
                        }

                        // in some really rare and hard situation after
                        // pkg.packageStatus != null check, pkg.packageStatus
                        // can be null
                        try {
                            if (pkg != null && pkg.packageStatus != null) {
                                if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_READY) {
                                    if (PackageDownloadListActivity.isActivityOpen) {
                                        mNotificationManager
                                                .cancel(jobs.get(i).id);
                                    } else {
                                        mNotificationManager
                                                .notify(jobs.get(i).id,
                                                        buildNotificationForReady(
                                                                getName(pkg.packageInfo
                                                                        .getNames(
                                                                                Locale.getDefault()
                                                                                        .getLanguage())
                                                                        .get(0)),
                                                                i));
                                    }
                                    jobs.remove(i);
                                    updateJobs();
                                    break;
                                } else if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_WAITING) {
                                    numOfQueued++;
                                } else {
                                    String s = "";
                                    if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_COPYING) {
                                        s = getString(R.string.package_status_copying);
                                    } else if (pkg.packageStatus
                                            .getCurrentAction() == PackageAction.PACKAGE_ACTION_DOWNLOADING) {
                                        s = getString(R.string.package_status_downloading);
                                    } else if (pkg.packageStatus
                                            .getCurrentAction() == PackageAction.PACKAGE_ACTION_REMOVING) {
                                        s = getString(R.string.package_status_removing);
                                    }

                                    if (pkg.packageStatus.isPaused()) {
                                        if (!jobs.get(i).isPausedShow) {
                                            int progress;

                                            if (pkg.packageRoutingInfo == null) {
                                                progress = (int) pkg.packageStatus
                                                        .getProgress();
                                            } else {
                                                progress = (int) ((pkg.packageStatus
                                                        .getProgress() * pkg.packageInfo.getSize().longValue() + pkg.packageRoutingStatus.getProgress() * pkg.packageRoutingInfo.getSize().longValue()) / (pkg.packageInfo.getSize().longValue() + pkg.packageRoutingInfo.getSize().longValue()));
                                            }

                                            mNotificationManager
                                                    .notify(jobs.get(i).id,
                                                            buildNotificationForDownload(
                                                                    getName(pkg.packageInfo
                                                                            .getNames(
                                                                                    Locale.getDefault()
                                                                                            .getLanguage())
                                                                            .get(0)),
                                                                    progress,
                                                                    s
                                                                            + " ("
                                                                            + getString(R.string.package_status_paused)
                                                                            + ")",
                                                                    i));
                                            jobs.get(i).isPausedShow = true;
                                        }
                                    } else {
                                        int progress;

                                        if (pkg.packageRoutingInfo == null) {
                                            progress = (int) pkg.packageStatus.getProgress();
                                        } else {
                                            progress = (int) (
                                                    (pkg.packageStatus.getProgress() *
                                                    pkg.packageInfo.getSize().longValue() +
                                                    pkg.packageRoutingStatus.getProgress() *
                                                    pkg.packageRoutingInfo.getSize().longValue()) /
                                                    (pkg.packageInfo.getSize().longValue() +
                                                    pkg.packageRoutingInfo.getSize().longValue()));
                                        }

                                        mNotificationManager
                                                .notify(jobs.get(i).id,
                                                        buildNotificationForDownload(
                                                                getName(pkg.packageInfo
                                                                        .getNames(
                                                                                Locale.getDefault()
                                                                                        .getLanguage())
                                                                        .get(0)),
                                                                progress,
                                                                s, i));
                                        jobs.get(i).isPausedShow = false;
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                    }

                    if (numOfQueued > 0) {
                        if (numOfQueued != numOfQueuedShowed) {
                            mNotificationManager.notify(-1, buildNotificationForQueued(numOfQueued));
                            numOfQueuedShowed = numOfQueued;
                        }
                    } else {
                        mNotificationManager.cancel(-1);
                        numOfQueued = 0;
                        numOfQueuedShowed = 0;
                    }
                } else {
                    // stop service
                    if (isStarted) {
                        stopSelf();
                    }
                }
            } else {
                // cancel all packages because storage is very low
                shouldAllCancel = true;

                // first cancel queued notification with id -1
                mNotificationManager.cancel(-1);
                for (int i = 0; i < jobs.size(); i++) {

                    // find package
                    for (int j = 0; j < packageArray.size(); j++) {
                        if (packageArray.get(j).packageInfo.getPackageId().equals(jobs.get(i).packageId)) {
                            pkg = packageArray.get(j);
                            break;
                        }
                    }

                    packageManager.cancelPackageTasks(jobs.get(i).packageId);

                    if (jobs.get(i).isRoutingPkg) {
                        packageManagerRouting.cancelPackageTasks(jobs.get(i).packageId + "");
                    }

                    if (PackageDownloadListActivity.isActivityOpen) {
                        mNotificationManager.cancel(jobs.get(i).id);

                        new Handler(Looper.getMainLooper())
                                .post(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(
                                                PackageDownloadService.this,
                                                getString(R.string.toast_no_space_msg)
                                                        + " "
                                                        + getName(pkg.packageInfo
                                                        .getNames(
                                                                Locale.getDefault()
                                                                        .getLanguage())
                                                        .get(0)
                                                        + " "
                                                        + getString(R.string.toast_no_space_msg2)),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        mNotificationManager
                                .notify(jobs.get(i).id,
                                        buildNotificationForLowSpace(
                                                getName(pkg.packageInfo
                                                        .getNames(
                                                                Locale.getDefault()
                                                                        .getLanguage())
                                                        .get(0)), i));
                    }
                }

                // stop service
                stopSelf();
            }
        }
    };

    private void updateJobs() {
        // thread safe, package listener calls this method from different thread
        handler.post(r);
    }

    private synchronized NotificationCompat.Builder createNotificationBuilder(String title) {

        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(PackageDownloadService.this);
            mBuilder.setSmallIcon(R.drawable.notification);
        }

        mBuilder.setContentTitle(title);
        return mBuilder;
    }

    private synchronized Notification buildNotificationForDownload(String packageName, int progress, String status, int j) {
        // Creates an explicit intent for an Activity in app
        Intent resultIntent = new Intent(PackageDownloadService.this,
                ((PackageManagerApplicationInterface) getApplication())
                        .getMainActivityClass());

        resultIntent.putExtra("position", jobs.get(j).position);
        resultIntent.putExtra("level", jobs.get(j).level);

        for (int i = 0; i < jobs.get(j).stack.size(); i++) {
            resultIntent.putExtra("stack" + i, jobs.get(j).stack.get(i).name);
            resultIntent.putExtra("position" + i, jobs.get(j).stack.get(i).position);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder
                .create(PackageDownloadService.this);
        // Adds the back stack, defined in manifest
        stackBuilder.addParentStack(((PackageManagerApplicationInterface) getApplication())
                .getMainActivityClass());
        // Adds the Intent that starts the Activity to the top
        // of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                jobs.get(j).id, PendingIntent.FLAG_UPDATE_CURRENT);

        return createNotificationBuilder(
                status + " " + packageName + " " + progress + "%")
                .setContentIntent(resultPendingIntent)
                .setProgress(100, progress, false).build();
    }

    private synchronized Notification buildNotificationForReady(String packageName, int j) {

        // Creates an explicit intent for an Activity in app
        Intent resultIntent = new Intent(PackageDownloadService.this,
                ((PackageManagerApplicationInterface) getApplication())
                        .getMainActivityClass());

        resultIntent.putExtra("position", jobs.get(j).position);
        resultIntent.putExtra("level", jobs.get(j).level);

        for (int i = 0; i < jobs.get(j).stack.size(); i++) {
            resultIntent.putExtra("stack" + i, jobs.get(j).stack.get(i).name);
            resultIntent.putExtra("position" + i,
                    jobs.get(j).stack.get(i).position);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(PackageDownloadService.this);

        // Adds the back stack, defined in manifest
        stackBuilder.addParentStack(((PackageManagerApplicationInterface) getApplication())
                .getMainActivityClass());

        // Adds the Intent that starts the Activity to the top
        // of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                jobs.get(j).id, PendingIntent.FLAG_UPDATE_CURRENT);

        return createNotificationBuilder(
                packageName + " " + getString(R.string.notification_msg))
                .setContentIntent(resultPendingIntent).setAutoCancel(true)
                .setProgress(100, 100, false).build();
    }

    private synchronized Notification buildNotificationForQueued(int num) {
        // Creates an explicit intent for an Activity in app
        Intent resultIntent = new Intent(PackageDownloadService.this,
                ((PackageManagerApplicationInterface) getApplication())
                        .getMainActivityClass());

        resultIntent.putExtra("position", 0);
        resultIntent.putExtra("level", -1);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(PackageDownloadService.this);

        // Adds the back stack, defined in manifest
        stackBuilder.addParentStack(((PackageManagerApplicationInterface) getApplication())
                .getMainActivityClass());

        // Adds the Intent that starts the Activity to the top
        // of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(-1,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return createNotificationBuilder(
                getString(R.string.notification_msg3) + " " + num + " "
                        + getString(R.string.notification_msg4))
                .setContentIntent(resultPendingIntent)
                .setProgress(100, 0, true).build();
    }

    private synchronized Notification buildNotificationFailedDownload(String packageName, int j) {

        // Creates an explicit intent for an Activity in app
        Intent resultIntent = new Intent(PackageDownloadService.this,
                ((PackageManagerApplicationInterface) getApplication())
                        .getMainActivityClass());

        resultIntent.putExtra("position", jobs.get(j).position);
        resultIntent.putExtra("level", jobs.get(j).level);

        for (int i = 0; i < jobs.get(j).stack.size(); i++) {
            resultIntent.putExtra("stack" + i, jobs.get(j).stack.get(i).name);
            resultIntent.putExtra("position" + i, jobs.get(j).stack.get(i).position);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(PackageDownloadService.this);

        // Adds the back stack, defined in manifest
        stackBuilder.addParentStack(((PackageManagerApplicationInterface) getApplication()).getMainActivityClass());

        // Adds the Intent that starts the Activity to the top
        // of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                jobs.get(j).id, PendingIntent.FLAG_UPDATE_CURRENT);

        return createNotificationBuilder(
                getString(R.string.notification_msg2) + " " + packageName)
                .setContentIntent(resultPendingIntent).setAutoCancel(true)
                .setProgress(100, 0, false).build();
    }

    private synchronized Notification buildNotificationForLowSpace(String packageName, int j) {

        // Creates an explicit intent for an Activity in app
        Intent resultIntent = new Intent(PackageDownloadService.this,
                ((PackageManagerApplicationInterface) getApplication())
                        .getMainActivityClass());

        resultIntent.putExtra("position", jobs.get(j).position);
        resultIntent.putExtra("level", jobs.get(j).level);

        for (int i = 0; i < jobs.get(j).stack.size(); i++) {
            resultIntent.putExtra("stack" + i, jobs.get(j).stack.get(i).name);
            resultIntent.putExtra("position" + i,
                    jobs.get(j).stack.get(i).position);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(PackageDownloadService.this);

        // Adds the back stack, defined in manifest
        stackBuilder.addParentStack(((PackageManagerApplicationInterface) getApplication()).getMainActivityClass());

        // Adds the Intent that starts the Activity to the top
        // of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                jobs.get(j).id, PendingIntent.FLAG_UPDATE_CURRENT);

        return createNotificationBuilder(
                packageName + " "
                        + getString(R.string.notification_no_space_title))
                .setContentText(getString(R.string.notification_no_space_desc))
                .setContentIntent(resultPendingIntent).setAutoCancel(true)
                .setProgress(100, 0, false).build();
    }
}
