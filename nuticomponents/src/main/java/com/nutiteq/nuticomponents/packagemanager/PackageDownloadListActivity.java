package com.nutiteq.nuticomponents.packagemanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.carto.packagemanager.CartoPackageManager;
import com.carto.packagemanager.PackageAction;
import com.carto.packagemanager.PackageErrorType;
import com.carto.packagemanager.PackageInfo;
import com.carto.packagemanager.PackageInfoVector;
import com.carto.packagemanager.PackageManagerListener;
import com.carto.packagemanager.PackageStatus;
import com.flurry.android.FlurryAgent;
import com.nutiteq.nuticomponents.Const;
import com.nutiteq.nuticomponents.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.support.v4.util.ArrayMap;

public class PackageDownloadListActivity extends ListActivity {

    private static class Package {

        final String packageId;
        final PackageInfo packageInfo;
        final PackageInfo packageRoutingInfo;
        PackageStatus packageStatus;
        PackageStatus packageRoutingStatus;
        final boolean hasUpdate;
        final boolean hasRoutingUpdate;
        final String label;
        final boolean canBeDownloaded;

        Package(PackageInfo packageInfo, PackageInfo packageRoutingInfo, PackageStatus packageStatus,
                PackageStatus packageRoutingStatus, String label,
                boolean canbeDownloaded, boolean hasUpdate, boolean hasRoutingUpdate) {

            this.packageId = (packageInfo != null ? packageInfo.getPackageId() : null);

            this.packageInfo = packageInfo;
            this.packageRoutingInfo = packageRoutingInfo;
            this.packageStatus = packageStatus;
            this.packageRoutingStatus = packageRoutingStatus;
            this.label = label;
            this.canBeDownloaded = canbeDownloaded;
            this.hasUpdate = hasUpdate;
            this.hasRoutingUpdate = hasRoutingUpdate;
        }

        @Override
        public String toString() {
            return "Package | Id: " + packageId + "; Label: " + label;
        }
    }

    private static class PackageHolder {

        TextView nameView;
        TextView statusView;
        Button actionButton;

        boolean isClickable;
    }

    private class PackageAdapter extends ArrayAdapter<Package> {

        Context context;
        int layoutResourceId;
        ArrayList<Package> packages;
        String space;

        public PackageAdapter(Context context, int layoutResourceId,
                              ArrayList<Package> packages) {
            super(context, layoutResourceId, packages);

            this.context = context;
            this.layoutResourceId = layoutResourceId;
            this.packages = packages;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final Package pkg = packages.get(position);

            if (level == 0 && position == 0 && hasDownloadedPackages) {
                View row = convertView;
                PackageHolder holder = null;

                if (row == null) {
                    LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                    row = inflater.inflate(layoutResourceId, parent, false);

                    holder = new PackageHolder();

                    holder.nameView = (TextView) row.findViewById(R.id.package_name);
                    holder.statusView = (TextView) row.findViewById(R.id.package_status);
                    holder.actionButton = (Button) row.findViewById(R.id.package_action);

                    holder.statusView.setVisibility(View.GONE);
                    holder.isClickable = true;

                    row.setTag(holder);
                } else {
                    holder = (PackageHolder) row.getTag();

                    holder.statusView.setVisibility(View.GONE);
                    holder.isClickable = true;
                }

                final String nameView = getString(R.string.downloaded);

                holder.nameView.setText(nameView);
                holder.actionButton.setBackgroundResource(R.drawable.arrow);
                holder.actionButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clickItem(position, nameView);
                    }
                });

                return row;
            }

            if (pkg.canBeDownloaded) {
                View row = convertView;
                PackageHolder holder = null;

                if (row == null) {
                    LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                    row = inflater.inflate(layoutResourceId, parent, false);

                    holder = new PackageHolder();

                    holder.nameView = (TextView) row.findViewById(R.id.package_name);
                    holder.statusView = (TextView) row.findViewById(R.id.package_status);
                    holder.actionButton = (Button) row.findViewById(R.id.package_action);

                    holder.statusView.setVisibility(View.VISIBLE);
                    holder.actionButton.setVisibility(View.VISIBLE);
                    holder.isClickable = false;

                    row.setTag(holder);
                } else {
                    holder = (PackageHolder) row.getTag();

                    holder.statusView.setVisibility(View.VISIBLE);
                    holder.statusView.setTextColor(getResources().getColor(R.color.search_desc));
                    holder.actionButton.setVisibility(View.VISIBLE);
                    holder.isClickable = false;
                }

                holder.nameView.setText(pkg.label);
                String status = getString(R.string.package_status_available);

                long pkgSize = pkg.packageInfo.getSize().longValue();

                long routingPkgSize = 0;
                if (pkg.packageRoutingInfo != null) {
                    routingPkgSize = pkg.packageRoutingInfo.getSize().longValue();
                }

                if ((pkgSize + routingPkgSize) < 1024 * 1024) {
                    status += " (" + (pkgSize + routingPkgSize)
                            / 1024 + " KB)"; // doesn't need res string
                    space = (pkgSize + routingPkgSize) / 1024
                            + " KB";
                } else if ((pkgSize + routingPkgSize) / 1024 / 1024 < 1024) {
                    status += " (" + (pkgSize + routingPkgSize)
                            / 1024 / 1024 + " MB)"; // doesn't need res string
                    space = (pkgSize + routingPkgSize) / 1024 / 1024
                            + " MB";
                } else {
                    String size = String.format(Locale.US, "%.2f", (pkgSize + routingPkgSize) * 1.0f / 1024 / 1024 / 1024);
                    status += " (" + size + " GB)"; // doesn't need res string

                    space = size + " GB";
                }

                if (pkg.packageStatus != null) {

                    if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_READY) {
                        if (pkg.hasUpdate || pkg.hasRoutingUpdate) {
                            status = getString(R.string.pck_update);
                        } else {
                            status = getString(R.string.package_status_ready);
                        }

                        status += " (" + space + ")";

                        holder.statusView.setTextColor(getResources().getColor(
                                R.color.nutiteq_green));
                        if (pkg.hasUpdate || pkg.hasRoutingUpdate) {
                            holder.actionButton.setBackgroundResource(R.drawable.download);
                        } else {
                            holder.actionButton.setBackgroundResource(R.drawable.remove);
                        }
                        holder.actionButton.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        readyPackage(pkg);
                                    }
                                });
                    } else if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_WAITING) {
                        status = getString(R.string.package_status_waiting);
                        holder.actionButton.setBackgroundResource(R.drawable.remove);
                        holder.actionButton.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        packageWaiting(pkg);
                                    }
                                });
                    } else {
                        if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_COPYING) {
                            status = getString(R.string.package_status_copying);
                        } else if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_DOWNLOADING) {
                            status = getString(R.string.package_status_downloading);
                        } else if (pkg.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_REMOVING) {
                            status = getString(R.string.package_status_removing);
                        }

                        if (pkg.packageRoutingStatus == null) {
                            status += " ("
                                    + Integer.toString((int) pkg.packageStatus
                                    .getProgress()) + "% "
                                    + getString(R.string.of) + " " + space + ")";
                        } else {
                            int progress = (int)(
                                    (pkg.packageStatus.getProgress() * pkgSize + pkg.packageRoutingStatus.getProgress() * routingPkgSize)
                                            / (pkgSize + routingPkgSize));
                            status += " ("
                                    + progress + "% "
                                    + getString(R.string.of) + " " + space + ")";
                        }

                        holder.statusView.setTextColor(getResources().getColor(R.color.orange));

                        if (pkg.packageStatus.isPaused()) {
                            if (pkg.packageRoutingStatus == null) {
                                status = getString(R.string.package_status_paused)
                                        + " ("
                                        + Integer.toString((int) pkg.packageStatus
                                        .getProgress()) + "% "
                                        + getString(R.string.of) + " " + space
                                        + ")";
                            } else {
                                int progress = (int) ((pkg.packageStatus
                                        .getProgress() * pkgSize + pkg.packageRoutingStatus.getProgress() * routingPkgSize) / (pkgSize + routingPkgSize));
                                status = getString(R.string.package_status_paused)
                                        + " ("
                                        + progress + "% "
                                        + getString(R.string.of) + " " + space
                                        + ")";
                            }

                            holder.actionButton.setBackgroundResource(R.drawable.play);
                            holder.actionButton.setOnClickListener(new OnClickListener() {

                                        @Override
                                        public void onClick(View v) {
                                            resumeDownloadingPackage(pkg);
                                        }
                                    });
                        } else {
                            holder.actionButton.setBackgroundResource(R.drawable.pause);
                            holder.actionButton.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            pausePackage(pkg);
                                        }
                                    });
                        }
                    }
                } else {
                    if (freeSpace > pkg.packageInfo.getSize().longValue()) {
                        holder.actionButton.setBackgroundResource(R.drawable.download);
                        holder.actionButton.setOnClickListener(new OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        startDownloadingPackage(pkg);
                                    }
                                });
                    } else {
                        holder.actionButton.setBackgroundResource(R.drawable.no_space);
                        holder.actionButton.setOnClickListener(new OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        displayToast(getString(R.string.no_space_msg));
                                    }
                                });
                    }
                }
                holder.statusView.setText(status);

                return row;

            } else {
                View row = convertView;
                PackageHolder holder = null;

                if (row == null) {
                    LayoutInflater inflater = ((Activity) context)
                            .getLayoutInflater();
                    row = inflater.inflate(layoutResourceId, parent, false);

                    holder = new PackageHolder();

                    holder.nameView = (TextView) row.findViewById(R.id.package_name);
                    holder.statusView = (TextView) row.findViewById(R.id.package_status);
                    holder.actionButton = (Button) row.findViewById(R.id.package_action);

                    holder.statusView.setVisibility(View.GONE);
                    holder.isClickable = true;

                    row.setTag(holder);
                } else {
                    holder = (PackageHolder) row.getTag();

                    holder.statusView.setVisibility(View.GONE);
                    holder.isClickable = true;
                }

                holder.nameView.setText(pkg.label);
                holder.actionButton.setBackgroundResource(R.drawable.arrow);
                holder.actionButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clickItem(position, pkg.label);
                    }
                });

                return row;
            }
        }
    }

    private void packageWaiting(final Package pkg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                PackageDownloadListActivity.this);

        builder.setTitle(getString(R.string.pck_cancel));
        builder.setIcon(R.drawable.icon);
        builder.setMessage(getString(R.string.pck_cancel_msg));

        builder.setPositiveButton(getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        boolean routingPkg = false;

                        if (pkg.packageRoutingInfo != null) {
                            routingPkg = true;
                        }

                        Intent intent = new Intent(
                                PackageDownloadListActivity.this,
                                PackageDownloadService.class);
                        intent.putExtra("job", "cancel");
                        intent.putExtra("routing_pkg", routingPkg);
                        intent.putExtra("package_id", pkg.packageId);

                        startService(intent);
                    }
                });
        builder.setNegativeButton(getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // nothing to do
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void pausePackage(final Package pkg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                PackageDownloadListActivity.this);

        builder.setTitle(getString(R.string.pause_cancel));
        builder.setIcon(R.drawable.icon);
        builder.setMessage(getString(R.string.pause_cancel_msg));

        builder.setPositiveButton(getString(R.string.pause),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        boolean routingPkg = false;

                        if (pkg.packageRoutingInfo != null) {
                            routingPkg = true;
                        }

                        Intent intent = new Intent(
                                PackageDownloadListActivity.this,
                                PackageDownloadService.class);
                        intent.putExtra("job", "pause");
                        intent.putExtra("routing_pkg", routingPkg);
                        intent.putExtra("package_id", pkg.packageId);

                        startService(intent);
                    }
                });
        builder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        boolean routingPkg = false;

                        if (pkg.packageRoutingInfo != null) {
                            routingPkg = true;
                        }

                        Intent intent = new Intent(
                                PackageDownloadListActivity.this,
                                PackageDownloadService.class);
                        intent.putExtra("job", "cancel");
                        intent.putExtra("routing_pkg", routingPkg);
                        intent.putExtra("package_id", pkg.packageId);

                        startService(intent);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void readyPackage(final Package pkg) {

        if (pkg.hasUpdate || pkg.hasRoutingUpdate) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PackageDownloadListActivity.this);

            builder.setTitle(getString(R.string.pck_update_title));
            builder.setIcon(R.drawable.icon);
            builder.setMessage(getString(R.string.pck_update_msg));

            builder.setPositiveButton(getString(R.string.update),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // flurry event
                            Map<String, String> parameters = new ArrayMap<String, String>();
                            parameters.put("package_id", pkg.packageInfo.getPackageId());

                            FlurryAgent.logEvent("PACKAGEUPDATING_START", parameters);

                            // can be done
                            // without
                            // service
                            if (pkg.hasUpdate) {
                                packageManager.startPackageRemove(pkg.packageInfo.getPackageId());
                                packageManager.startPackageDownload(pkg.packageId);
                            }

                            boolean routingPkg = false;

                            if (pkg.hasRoutingUpdate) {
                                if (pkg.packageRoutingInfo != null) {
                                    packageManagerRouting.startPackageRemove(pkg.packageInfo
                                            .getPackageId() + "-routing");
                                    packageManagerRouting.startPackageDownload(pkg.packageId + "-routing");

                                    routingPkg = true;
                                }
                            }

                            Intent intent = new Intent(
                                    PackageDownloadListActivity.this,
                                    PackageDownloadService.class);

                            intent.putExtra("job", "download");
                            intent.putExtra("package_id", pkg.packageId);
                            intent.putExtra("routing_pkg", routingPkg);
                            intent.putExtra("position", position);
                            intent.putExtra("level", level);

                            for (int i = 0; i < stack.size(); i++) {
                                intent.putExtra("stack" + i, stack.get(i).name);
                                intent.putExtra("position" + i, stack.get(i).position);
                            }

                            startService(intent);

                            updatePackage(pkg.packageInfo.getPackageId());
                        }
                    });
            builder.setNegativeButton(getString(R.string.delete),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // can be done
                            // without
                            // service
                            packageManager.startPackageRemove(pkg.packageInfo
                                    .getPackageId());
                            if (pkg.packageRoutingInfo != null) {
                                packageManagerRouting.startPackageRemove(pkg.packageId + "-routing");
                            }

                            if (level == -1) {
                                updatePackages();
                            }
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(PackageDownloadListActivity.this);

            builder.setTitle(getString(R.string.package_remove));
            builder.setIcon(R.drawable.icon);
            builder.setMessage(getString(R.string.package_remove_msg));

            builder.setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // can be done
                            // without
                            // service
                            packageManager.startPackageRemove(pkg.packageInfo.getPackageId());
                            if (pkg.packageRoutingInfo != null) {
                                packageManagerRouting.startPackageRemove(pkg.packageId + "-routing");
                            }

                            if (level == -1) {
                                updatePackages();
                            }
                        }
                    });
            builder.setNegativeButton(getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // nothing to do
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void resumeDownloadingPackage(Package pkg) {
        Intent intent = new Intent(this, PackageDownloadService.class);

        boolean routingPkg = false;

        if (pkg.packageRoutingInfo != null) {
            routingPkg = true;
        }

        intent.putExtra("job", "play");
        intent.putExtra("package_id", pkg.packageId);
        intent.putExtra("routing_pkg", routingPkg);
        intent.putExtra("position", position);
        intent.putExtra("level", level);

        for (int i = 0; i < stack.size(); i++) {
            intent.putExtra("stack" + i, stack.get(i).name);
            intent.putExtra("position" + i, stack.get(i).position);
        }

        startService(intent);
    }

    private void startDownloadingPackage(Package pkg) {
        // flurry event
        Map<String, String> parameters = new ArrayMap<String, String>();
        parameters.put("package_id", pkg.packageId);

        FlurryAgent.logEvent("PACKAGEDOWNLOAD_START", parameters);

        boolean routingPkg = false;

        packageManager.startPackageDownload(pkg.packageId);

        if (pkg.packageRoutingInfo != null) {
            packageManagerRouting.startPackageDownload(pkg.packageId + "-routing");
            routingPkg = true;
        }

        Intent intent = new Intent(this, PackageDownloadService.class);

        intent.putExtra("job", "download");
        intent.putExtra("package_id", pkg.packageId);
        intent.putExtra("routing_pkg", routingPkg);
        intent.putExtra("position", position);
        intent.putExtra("level", level);

        for (int i = 0; i < stack.size(); i++) {
            intent.putExtra("stack" + i, stack.get(i).name);
            intent.putExtra("position" + i, stack.get(i).position);
        }

        startService(intent);

        updatePackage(pkg.packageInfo.getPackageId());
    }

    private class PackageListener extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            updatePackages();
        }

        @Override
        public void onPackageListFailed() {
            updatePackages();
            displayToast(getString(R.string.package_list_failed));
        }

        @Override
        public void onPackageStatusChanged(String id, int version, PackageStatus status) {
            updatePackage(id);
        }

        @Override
        public void onPackageCancelled(String id, int version) {
            updatePackage(id);
        }

        @Override
        public void onPackageUpdated(String id, int version) {
            updatePackage(id);
        }

        @Override
        public void onPackageFailed(String id, int version, PackageErrorType errorType) {
            updatePackage(id);
            for (Package pkg : packageArray) {
                if (pkg != null && pkg.packageId != null
                        && pkg.packageId.equals(id) && pkg.packageInfo != null) {
                    displayToast(getString(R.string.package_failed_download)
                            + " " + getName(pkg.packageInfo.getName()));
                    break;
                }
            }
        }
    }

    private class PackageRoutingListener extends PackageManagerListener {

        @Override
        public void onPackageListUpdated() {
            updatePackages();
        }

        @Override
        public void onPackageListFailed() {
            updatePackages();
            displayToast(getString(R.string.package_routing_list_failed));
        }

        @Override
        public void onPackageStatusChanged(String id, int version, PackageStatus status) {
            updatePackage(id);
        }

        @Override
        public void onPackageCancelled(String id, int version) {
            updatePackage(id);
        }

        @Override
        public void onPackageUpdated(String id, int version) {
            updatePackage(id);
        }

        @Override
        public void onPackageFailed(String id, int version,
                                    PackageErrorType errorType) {
            updatePackage(id);

            for (Package pkg : packageArray) {
                if (pkg != null && pkg.packageId != null
                        && (pkg.packageId + "-routing").equals(id) && pkg.packageRoutingInfo != null) {
                    displayToast(getString(R.string.package_routing_failed_download)
                            + " " + getName(pkg.packageRoutingInfo.getName()));
                    break;
                }
            }
        }
    }

    private CartoPackageManager packageManager;
    private CartoPackageManager packageManagerRouting;
    private ArrayList<Package> packageArray = new ArrayList<Package>();
    private ArrayAdapter<Package> packageAdapter;
    private PackageListener packageListner = new PackageListener();
    private PackageRoutingListener packageRoutingListner = new PackageRoutingListener();
    private long freeSpace;

    private Handler handler = new Handler();

    private int position = 0;

    private ArrayList<StackObject> stack = new ArrayList<StackObject>();
    private int level = 0; // current hierarchical level

    private boolean hasActionBar;

    private boolean hasDownloadedPackages = false;

    // to know from notification service is activity open or not
    public static boolean isActivityOpen = false;

    private int storageType;
    private int storageNumber;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FlurryAgent.logEvent("PACKAGELIST_START");

        // Get package managers
        packageManager = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getPackageManager();
        packageManagerRouting = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getRoutingPackageManager();

        storageType = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getStorageType();
        storageNumber = ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().getStorageNumber();

        Boolean isExternal2Avaiable = false;
        if ((storageType == PackageManagerComponent.EXTERNAL_STORAGE)
                && storageNumber > 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File f = getExternalFilesDirs(null)[storageNumber - 1];
                if (f != null) {
                    isExternal2Avaiable = true;
                }
            }
        }

        // check if sd card is mounted, packageManager needs data from sd card
        // or if internal than it's always avaiable
        if ((storageType == PackageManagerComponent.EXTERNAL_STORAGE && Environment.MEDIA_MOUNTED
                .equals(Environment.getExternalStorageState()))
                || storageType == PackageManagerComponent.INTERNAL_STORAGE
                || isExternal2Avaiable) {
            // enable progress circle
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                hasActionBar = true;
            } else {
                hasActionBar = false;
            }

            if (getIntent().getExtras() != null) {
                level = getIntent().getExtras().getInt("level");
                getIntent().removeExtra("level");

                int i = 0;
                String s = getIntent().getExtras().getString("stack" + i);
                int p = getIntent().getExtras().getInt("position" + i);
                while (s != null) {
                    stack.add(new StackObject(s, p));

                    getIntent().removeExtra("stack" + i);
                    getIntent().removeExtra("position" + i);

                    i++;

                    s = getIntent().getExtras().getString("stack" + i);
                    p = getIntent().getExtras().getInt("position" + i);
                }
                position = getIntent().getExtras().getInt("position");
                getIntent().removeExtra("position");

                setTitle();
            }

            // Initialize list view
            setContentView(R.layout.package_list);
            packageAdapter = new PackageAdapter(this, R.layout.package_item_download, packageArray);
            getListView().setAdapter(packageAdapter);

            getListView().setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View v, int pos,
                                        long id) {

                    if (((PackageHolder) v.getTag()).isClickable) {
                        clickItem(pos, ((PackageHolder) v.getTag()).nameView.getText().toString().trim());
                    } else {
                        Package p = packageArray.get(pos);
                        if (p != null) {
                            if (p.packageStatus == null) {
                                startDownloadingPackage(p);
                            } else if (p.packageStatus.isPaused()) {
                                resumeDownloadingPackage(p);
                            } else if (p.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_READY) {
                                readyPackage(p);
                            } else if (p.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_COPYING
                                    || p.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_DOWNLOADING
                                    || p.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_REMOVING) {
                                pausePackage(p);
                            } else if (p.packageStatus.getCurrentAction() == PackageAction.PACKAGE_ACTION_WAITING) {
                                packageWaiting(p);
                            }
                        }
                    }
                }
            });

            // this must be called after setContentView, I lost few hours on
            // trying to see progress circle in right corner and this was above
            // setContentView :)
            setProgressBarIndeterminateVisibility(true);
        } else {
            Toast.makeText(this, getString(R.string.external_storage_error2),
                    Toast.LENGTH_LONG).show();
        }

        getListView().setDivider(null);
        getListView().setDividerHeight(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));
            getListView().setSelector(R.xml.selector);
            getListView().setBackgroundColor(Color.WHITE);

            getActionBar().setHomeButtonEnabled(true);

            getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.download_title) + "</b>"));
        }
    }

    @SuppressLint("NewApi")
    private void clickItem(int pos, String name) {

        if (level == 0 && pos == 0 && hasDownloadedPackages) {
            level = -1;
            updatePackages();

            if (hasActionBar) {
                getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.downloaded_title) + "</b>"));
            } else {
                setTitle(R.string.downloaded);
            }

            return;
        }

        level++;
        position = 0;
        stack.add(new StackObject(name, getListView().getFirstVisiblePosition()));
        updatePackages();

        if (hasActionBar) {
            getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + stack.get(stack.size() - 1).name + "</b>"));
        } else {
            setTitle(stack.get(stack.size() - 1).name);
        }
    }

    @Override
    public void onBackPressed() {
        onBack();
    }

    @SuppressLint("NewApi")
    private void setTitle() {
        if (level == -1) {
            if (hasActionBar) {
                getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.downloaded_title) + "</b>"));
            } else {
                setTitle(getString(R.string.downloaded));
            }

            return;
        }

        if (hasActionBar) {
            if (stack.size() > 0) {
                getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + stack.get(stack.size() - 1).name + "</b>"));
            } else {
                getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.download_title) + "</b>"));
            }
        } else {
            if (stack.size() > 0) {
                setTitle(stack.get(stack.size() - 1).name);
            } else {
                setTitle(getString(R.string.download_title));
            }
        }
    }

    private void onBack() {

        if (level == -1) {
            level = 0;
            position = 0;
            updatePackages();
            setTitle();
            return;
        }

        if (stack.size() == 0) {
            finish();
        } else {
            position = stack.get(stack.size() - 1).position;
            stack.remove(stack.size() - 1);
            level--;
            updatePackages();
            setTitle();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(bundle);

        level = bundle.getInt("level");

        int i = 0;

        String s = bundle.getString("stack" + i);
        int p = bundle.getInt("position" + i);

        while (s != null) {
            stack.add(new StackObject(s, p));
            i++;
            s = bundle.getString("stack" + i);
            p = bundle.getInt("position" + i);
        }

        setTitle();

        position = bundle.getInt("list_position");
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt("level", level);
        for (int i = 0; i < stack.size(); i++) {
            bundle.putString("stack" + i, stack.get(i).name);
            bundle.putInt("position" + i, stack.get(i).position);
        }

        bundle.putInt("list_position", getListView().getFirstVisiblePosition());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onResume() {
        isActivityOpen = true;

        // Start the package manager
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().addPackageManagerListener(
                packageListner);
        ((PackageManagerApplicationInterface) getApplication())
                .getPackageManagerComponent().addPackageManagerRoutingListener(
                packageRoutingListner);

        if (packageManager.getServerPackageListAge() > Const.PACKAGELIST_CHECK_PERIOD) {
            packageManager.startPackageListDownload();
            packageManagerRouting.startPackageListDownload();
        } else {
            packageListner.onPackageListUpdated();
            packageRoutingListner.onPackageListUpdated();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        isActivityOpen = false;

        // remove listeners
        if (packageManager.getPackageManagerListener() != null) {
            ((PackageManagerApplicationInterface) getApplication())
                    .getPackageManagerComponent().removePackageManagerListener(
                    packageListner);
        }

        if (packageManagerRouting.getPackageManagerListener() != null) {
            ((PackageManagerApplicationInterface) getApplication())
                    .getPackageManagerComponent().removePackageManagerRoutingListener(
                    packageRoutingListner);
        }

        super.onPause();
    }

    private static class Region {

        String label;
        boolean canBeDownloaded;

        public Region(String label, boolean canBeDownloaded) {
            super();

            this.label = label;
            this.canBeDownloaded = canBeDownloaded;
        }
    }

    // return region based on current and previous level or null if there is no
    // one for this criteria in packageName
    private Region getRegion(String packageName) {

        if (level == 0) {
            int index = packageName.indexOf("/");

            if (index == -1) {
                return new Region(packageName, true);
            } else {
                return new Region(packageName.substring(0, index), false);
            }
        }

        if (level == 1) {
            int index2 = packageName.indexOf(stack.get(level - 1).name);
            if (index2 == -1) {
                return null;
            }

            packageName = packageName.substring(index2);
            int index = packageName.indexOf("/");

            if (index == -1) {
                return null;
            }

            packageName = packageName.substring(index + 1);
            index = packageName.indexOf("/");

            if (index == -1) {
                return new Region(packageName, true);
            } else {
                packageName = packageName.substring(index + 1);
                index = packageName.indexOf("/");

                if (index == -1) {
                    return new Region(packageName, true);
                } else {
                    return new Region(packageName.substring(0, index), false);
                }
            }
        } else {
            int index2 = packageName.indexOf(stack.get(level - 1).name);
            if (index2 == -1) {
                return null;
            }

            packageName = packageName.substring(index2);
            int index = packageName.indexOf("/");

            if (index == -1) {
                return null;
            }

            packageName = packageName.substring(index + 1);
            index = packageName.indexOf("/");

            if (index == -1) {
                return new Region(packageName, true);
            } else {
                return new Region(packageName.substring(0, index), false);
            }
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

    private ArrayList<Package> getServerPackages() {

        ArrayList<Package> pkgsServer = new ArrayList<Package>();
        PackageInfoVector packageInfoVectorServer = packageManager.getServerPackages();

        PackageInfoVector packageInfoVectorLocal = packageManager.getLocalPackages();
        PackageInfoVector packageRoutingInfoVectorLocal = packageManagerRouting.getLocalPackages();

        hasDownloadedPackages = false;

        for (int i = 0; i < packageInfoVectorLocal.size(); i++) {
            PackageInfo packageInfoLocal = packageInfoVectorLocal.get(i);

            if (packageManager.getLocalPackageStatus(
                    packageInfoLocal.getPackageId(),
                    packageInfoLocal.getVersion()) != null) {
                hasDownloadedPackages = true;

                break;
            }
        }

        boolean hasUpdate = false;
        boolean hasRoutingUpdate = false;

        for (int i = 0; i < packageInfoVectorServer.size(); i++) {
            PackageInfo packageInfoServer = packageInfoVectorServer.get(i);

            String routingId = packageInfoServer.getPackageId() + "-routing";
            PackageInfo packageRoutingInfoServer = packageManagerRouting.getServerPackage(routingId);

            if (packageInfoServer.getPackageId().equals(Const.BASE_PACKAGE_ID)) {
                // ignore base package
                continue;
            }

            // check is there update for downloaded pck
            hasUpdate = false;

            for (int j = 0; j < packageInfoVectorLocal.size(); j++) {
                PackageInfo packageInfoLocal = packageInfoVectorLocal.get(j);

                if (packageInfoServer.getPackageId().equals(packageInfoLocal.getPackageId())
                        && packageInfoServer.getVersion() > packageInfoLocal.getVersion()) {
                    hasUpdate = true;

                    break;
                }
            }

            if (packageRoutingInfoServer != null) {
                // check is there update for downloaded routing pck
                hasRoutingUpdate = false;
                for (int j = 0; j < packageRoutingInfoVectorLocal.size(); j++) {
                    PackageInfo packageRoutingInfoLocal = packageRoutingInfoVectorLocal.get(j);

                    if (packageRoutingInfoServer.getPackageId().equals(
                            packageRoutingInfoLocal.getPackageId())
                            && packageRoutingInfoServer.getVersion() > packageRoutingInfoLocal
                            .getVersion()) {
                        hasRoutingUpdate = true;

                        break;
                    }
                }
            }

            if (level == -1) {
                PackageStatus localPackageStatus = packageManager.getLocalPackageStatus(packageInfoServer.getPackageId(), -1);
                PackageStatus localPackageRoutingStatus = packageManagerRouting.getLocalPackageStatus(routingId, -1);

                if (localPackageStatus != null) {
                    pkgsServer.add(
                            new Package(packageInfoServer,
                                    packageManagerRouting.getServerPackage(routingId),
                                    localPackageStatus, localPackageRoutingStatus,
                                    getName(packageInfoServer.getNames(Locale.getDefault().getLanguage()).get(0)),
                                    true,
                                    hasUpdate,
                                    hasRoutingUpdate
                            )
                    );
                }

                continue;
            }

            Region region = getRegion(packageInfoServer.getNames(Locale.getDefault().getLanguage()).get(0));

            if (region != null) {

                if (region.canBeDownloaded) {
                    pkgsServer.add(
                            new Package(
                                    packageInfoServer,
                                    packageManagerRouting.getServerPackage(routingId),
                                    packageManager.getLocalPackageStatus(packageInfoServer.getPackageId(), -1),
                                    packageManagerRouting.getLocalPackageStatus(routingId, -1),
                                    region.label,
                                    true,
                                    hasUpdate,
                                    hasRoutingUpdate
                            )
                    );
                } else {
                    boolean isAlreadyAdded = false;

                    // it can't be downloaded, so I must check if it's
                    // already added
                    int j;

                    for (j = 0; j < pkgsServer.size(); j++) {
                        if (pkgsServer.get(j).label.equals(region.label)) {
                            isAlreadyAdded = true;
                            break;
                        }
                    }

                    if (!isAlreadyAdded) {
                        pkgsServer.add(
                                new Package(
                                        packageInfoServer,
                                        packageManagerRouting.getServerPackage(routingId),
                                        null,
                                        null,
                                        region.label,
                                        false,
                                        hasUpdate,
                                        hasRoutingUpdate
                                )
                        );
                    } else {
                        if (pkgsServer.get(j).canBeDownloaded) {

                            isAlreadyAdded = false;

                            for (j = 0; j < pkgsServer.size(); j++) {
                                if (pkgsServer.get(j).label.equals(region.label) && !pkgsServer.get(j).canBeDownloaded) {
                                    isAlreadyAdded = true;
                                    break;
                                }
                            }

                            if (!isAlreadyAdded) {
                                pkgsServer.add(
                                        new Package(
                                                packageInfoServer,
                                                packageManagerRouting.getServerPackage(routingId),
                                                null,
                                                null,
                                                region.label,
                                                false,
                                                hasUpdate,
                                                hasRoutingUpdate
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(pkgsServer, new CustomComparator());

        if (level == 0 && hasDownloadedPackages) {
            pkgsServer.add(0, null);
        }

        for (Package pkg : pkgsServer) {
            if (pkg == null) {
                System.out.println("NullPackage");
            } else {
                System.out.println(pkg.toString());
            }
        }

        return pkgsServer;
    }

    private String getNameBasedOnLevel(String name) {

        if (level == -1) {
            return getName(name);
        } else if (level == 0) {
            int index = name.indexOf("/");
            if (index == -1) {
                return name;
            } else {
                return name.substring(0, index);
            }
        } else if (level == 1) {
            int index = name.indexOf(stack.get(level - 1).name);

            name = name.substring(index);
            index = name.indexOf("/");
            name = name.substring(index + 1);
            index = name.indexOf("/");
            if (index == -1) {
                return name;
            } else {
                name = name.substring(index + 1);
                index = name.indexOf("/");

                if (index == -1) {
                    return name;
                } else {
                    return name.substring(0, index);
                }
            }
        } else {
            int index = name.indexOf(stack.get(level - 1).name);

            name = name.substring(index);
            index = name.indexOf("/");
            name = name.substring(index + 1);
            index = name.indexOf("/");
            if (index == -1) {
                return name;
            } else {
                return name.substring(0, index);
            }
        }
    }

    private class CustomComparator implements Comparator<Package> {

        @Override
        public int compare(Package p1, Package p2) {
            return getNameBasedOnLevel(
                    p1.packageInfo.getNames(Locale.getDefault().getLanguage())
                            .get(0)).compareTo(
                    getNameBasedOnLevel(p2.packageInfo.getNames(
                            Locale.getDefault().getLanguage()).get(0)));
        }

    }

    @SuppressLint("NewApi")
    private void updatePackages() {
        if (packageAdapter == null) {
            return;
        }

        if (storageType == PackageManagerComponent.INTERNAL_STORAGE) {

            File f = getFilesDir();
            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.INTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        } else if (storageType == PackageManagerComponent.EXTERNAL_STORAGE && storageNumber == 1) {
            File f = getExternalFilesDir(null);
            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.EXTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        } else {
            File f = getExternalFilesDirs(null)[storageNumber - 1];

            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.EXTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        }

        handler.post(new Runnable() {

            @Override
            public void run() {
                packageArray.clear();
                packageArray.addAll(getServerPackages());
                packageAdapter.notifyDataSetChanged();
                setProgressBarIndeterminateVisibility(false);
                // set item position
                getListView().setSelection(position);
            }
        });
    }

    @SuppressLint("NewApi")
    private void updatePackage(final String packageId) {
        if (packageAdapter == null) {
            return;
        }

        if (storageType == PackageManagerComponent.INTERNAL_STORAGE) {
            File f = getFilesDir();
            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.INTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        } else if (storageType == PackageManagerComponent.EXTERNAL_STORAGE
                && storageNumber == 1) {
            File f = getExternalFilesDir(null);
            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.EXTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        } else {
            File f = getExternalFilesDirs(null)[storageNumber - 1];
            if (f != null) {
                freeSpace = f.getFreeSpace() - Const.EXTERNAL_STORAGE_MIN
                        * 1048576;
            } else {
                freeSpace = 0;// isn't available
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                // Try to find the view that refers to the given package. Update
                // only this view.
                int start = getListView().getFirstVisiblePosition();
                int end = getListView().getLastVisiblePosition();
                for (int position = start; position <= end; position++) {
                    Package pkg = null;
                    try {
                        pkg = (Package) getListView().getItemAtPosition(
                                position);
                    } catch (Exception e) {
                        Log.e(Const.LOG_TAG,
                                "Exception while refreshing package status: "
                                        + e.toString());
                    }
                    if (pkg != null) {
                        if (packageId.equals(pkg.packageId)) {
                            PackageStatus packageStatus = packageManager
                                    .getLocalPackageStatus(packageId, -1);
                            PackageStatus packageStatusRouting = packageManagerRouting
                                    .getLocalPackageStatus(packageId + "-routing", -1);
                            pkg.packageStatus = packageStatus;
                            pkg.packageRoutingStatus = packageStatusRouting;
                            packageAdapter.getView(position, getListView()
                                            .getChildAt(position - start),
                                    getListView());
                            break;
                        }
                    }
                }
            }
        });
    }

    private void displayToast(final String msg) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBack();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}