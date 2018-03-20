package com.nutiteq.nuticomponents;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.carto.core.MapPos;
import com.carto.core.MapTile;
import com.carto.layers.TileLayer;
import com.carto.packagemanager.PackageInfo;
import com.carto.packagemanager.PackageInfoVector;
import com.carto.packagemanager.PackageManager;
import com.carto.packagemanager.PackageTileStatus;
import com.carto.ui.MapView;
import com.flurry.android.FlurryAgent;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadService;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import android.support.v4.util.ArrayMap;

public class PackageSuggestion {

    private static int TASK_DELAY = 300;

    private MapView mapView;
    private TileLayer baseLayer;
    private PackageManager packageManager;
    private PackageManager packageManagerRouting;
    private Button suggestionButton;

    private boolean taskPending;
    private int taskCounter;
    private PackageInfo currentPkg;

    private boolean isPckChanged = false;

    private boolean hasUpdate = false;
    private boolean hasRoutingUpdate = false;

    private Activity context;

    public PackageSuggestion(Activity context, MapView mapView, TileLayer baseLayer, PackageManager packageManager,
                             PackageManager packageManagerRouting, Button suggestionButton) {
        this.context = context;

        this.mapView = mapView;
        this.baseLayer = baseLayer;
        this.packageManager = packageManager;
        this.packageManagerRouting = packageManagerRouting;
        this.suggestionButton = suggestionButton;

        notifyMapMoved();
    }

    public void notifyMapMoved() {
        if (mapView == null || baseLayer == null) {
            return;
        }

        // If task is pending, nothing to do
        synchronized (this) {
            if (taskPending) {
                return;
            }
            taskPending = true;
            taskCounter++;
        }

        // Create new task for finding the package
        PackageFinderTask task = new PackageFinderTask();
        // Execute the task in order, relative to other tasks
        task.execute();
    }

    private String getName(String name) {
        int i = name.lastIndexOf("/");
        if (i == -1) {
            return name;
        } else {
            return name.substring(i + 1);
        }
    }

    public void resetSuggestion() {
        currentPkg = null;
    }

    private class PackageFinderTask extends AsyncTask<Void, Void, PackageInfo>
    {
        int currentTaskCounter;

        MapPos position;
        float zoom;

        @Override
        protected PackageInfo doInBackground(Void... params) {
            try {
                Thread.sleep(TASK_DELAY);
            } catch (InterruptedException e) {
            }

            final MapTile mapTile;

            synchronized (PackageSuggestion.this) {
                taskPending = false;
                currentTaskCounter = taskCounter;

//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                        position = mapView.getFocusPos();
                        zoom = mapView.getZoom();
//                    }
//                });

                // If zoom level is below threshold, this means that the
                // tiles are in base package

                System.out.println("Zoom: " + zoom);
                System.out.println("Required: " + (Const.BASE_PACKAGE_ZOOM_LEVELS + 0.5f));
                System.out.println("IsZoomBelow: " + (zoom < Const.BASE_PACKAGE_ZOOM_LEVELS + 0.5f));
                System.out.println("IsZoomBelow: " + (zoom < 6.5f));

                if (zoom < Const.BASE_PACKAGE_ZOOM_LEVELS + 0.5f) {
                    return null;
                }

                // Resolve tiles and do fast check wrt to current package
                mapTile = baseLayer.calculateMapTile(position, Const.TILEMASK_ZOOM_LEVELS);
                if (currentPkg != null) {
                    PackageTileStatus tileStatus = currentPkg.getTileMask().getTileStatus(mapTile);
                    if (tileStatus != PackageTileStatus.PACKAGE_TILE_STATUS_MISSING) {
                        return currentPkg;
                    }
                }
            }

            // Find best package
            PackageInfoVector allPkgs = packageManager.getServerPackages();
            System.out.println("Package count: " + allPkgs.size());
            PriorityQueue<PackageInfo> sortedPkgs = new PriorityQueue<PackageInfo>(1, new Comparator<PackageInfo>() {

                public int compare(PackageInfo pkg1, PackageInfo pkg2) {
                    PackageTileStatus tileStatus1 = pkg1.getTileMask().getTileStatus(mapTile);
                    PackageTileStatus tileStatus2 = pkg2.getTileMask().getTileStatus(mapTile);

                    if (tileStatus1 != tileStatus2) {
                        // prefer full package
                        return tileStatus1 == PackageTileStatus.PACKAGE_TILE_STATUS_FULL ? -1 : 1;
                    }

                    boolean downloaded1 = packageManager.getLocalPackageStatus(pkg1.getPackageId(), -1) != null;
                    boolean downloaded2 = packageManager.getLocalPackageStatus(pkg2.getPackageId(), -1) != null;

                    if (downloaded1 != downloaded2) {
                        return downloaded1 ? -1 : 1; // prefer
                        // already
                        // downloaded
                        // package
                    }

                    return Long.signum(pkg1.getSize().longValue() - pkg2.getSize().longValue()); // prefer
                    // smaller
                    // package
                }
            });

            for (int i = 0; i < allPkgs.size(); i++) {
                PackageInfo pkg = allPkgs.get(i);
                PackageTileStatus tileStatus = pkg.getTileMask().getTileStatus(mapTile);

                if (tileStatus != PackageTileStatus.PACKAGE_TILE_STATUS_MISSING) {
                    sortedPkgs.add(pkg);
                }
            }

            PackageInfo bestPkg;

            if (sortedPkgs.size() > 1) {
                // check if there are special cases

                boolean isUSMD = false;
                boolean isUSDC = false;

                boolean isUSNY = false;
                boolean isUSNJ = false;

                PackageInfo US_DC = null;
                PackageInfo US_NY = null;

                PackageInfo p = sortedPkgs.poll();
                bestPkg = p;
                String s = "";

                while (p != null) {
                    s = p.getPackageId();

                    if (s.equals("US-MD")) {
                        isUSMD = true;
                    } else if (s.equals("US-DC")) {
                        isUSDC = true;
                        US_DC = p;
                    } else if (s.equals("US-NY")) {
                        isUSNY = true;
                        US_NY = p;
                    } else if (s.equals("US-NJ")) {
                        isUSNJ = true;
                    }

                    p = sortedPkgs.poll();
                }

                if (isUSMD && isUSDC) {
                    bestPkg = US_DC;
                } else if (isUSNJ && isUSNY) {
                    bestPkg = US_NY;
                }
            } else {
                bestPkg = sortedPkgs.poll();
            }

            return bestPkg;
        }

        @Override
        protected void onPostExecute(final PackageInfo pkg) {

            synchronized (PackageSuggestion.this) {
                // If not up-to-date, ignore
                if (taskCounter != currentTaskCounter) {
                    return;
                }

                // If package did not change, nothing to do
                if (currentPkg == pkg) {
                    isPckChanged = false;
                    return;
                }

                isPckChanged = true;
                currentPkg = pkg;
            }

            if (pkg == null) {
                suggestionButton.setVisibility(LinearLayout.GONE);
            } else {
                PackageInfoVector packageInfoVectorLocal = packageManager
                        .getLocalPackages();
                PackageInfoVector packageRoutingInfoVectorLocal = packageManagerRouting
                        .getLocalPackages();

                hasUpdate = false;
                hasRoutingUpdate = false;

                for (int i = 0; i < packageInfoVectorLocal.size(); i++) {
                    PackageInfo packageInfoLocal = packageInfoVectorLocal
                            .get(i);

                    if (packageManager.getLocalPackageStatus(
                            packageInfoLocal.getPackageId(),
                            packageInfoLocal.getVersion()) != null
                            && packageInfoLocal.getPackageId().equals(
                            pkg.getPackageId())
                            && pkg.getVersion() > packageInfoLocal
                            .getVersion()) {
                        hasUpdate = true;
                        break;
                    }
                }

                PackageInfo packageRoutingInfoServer = packageManagerRouting.getServerPackage(pkg.getPackageId() + "");

                // there is no routing pkg for map pkg always!
                if (packageRoutingInfoServer != null) {
                    for (int j = 0; j < packageRoutingInfoVectorLocal.size(); j++) {
                        PackageInfo packageRoutingInfoLocal = packageRoutingInfoVectorLocal.get(j);

                        if (packageRoutingInfoServer.getPackageId().equals(packageRoutingInfoLocal.getPackageId())
                                && packageRoutingInfoServer.getVersion() > packageRoutingInfoLocal.getVersion()) {
                            hasRoutingUpdate = true;

                            break;
                        }
                    }
                }

                if (hasUpdate || hasRoutingUpdate) {
                    suggestionButton.setVisibility(LinearLayout.VISIBLE);
                    suggestionButton.setEnabled(true);

                    long routingPkgSize = 0;
                    if (packageManagerRouting.getServerPackage(pkg.getPackageId() + "") != null) {
                        routingPkgSize = packageManagerRouting.getServerPackage(pkg.getPackageId() + "").getSize().longValue();
                    }

                    String size = " (";

                    if ((pkg.getSize().longValue() + routingPkgSize) < 1024 * 1024) {
                        size += (pkg.getSize().longValue() + routingPkgSize) / 1024 + " KB)";
                    } else if ((pkg.getSize().longValue() + routingPkgSize) / 1024 / 1024 < 1024) {
                        size += (pkg.getSize().longValue() + routingPkgSize) / 1024 / 1024
                                + " MB)";
                    } else {
                        size += String.format("%.2f", (pkg.getSize()
                                .longValue() + routingPkgSize) * 1.0f / 1024 / 1024 / 1024)
                                + " GB)";
                    }

                    suggestionButton.setTextColor(Color.WHITE);
                    suggestionButton.setText(mapView.getContext().getString(
                            R.string.update)
                            + " "
                            + getName(pkg.getNames(
                            Locale.getDefault().getLanguage()).get(
                            0)) + "\n" + size);

                    suggestionButton.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // flurry event
                            Map<String, String> parameters = new ArrayMap<String, String>();
                            parameters.put("package_id", pkg.getPackageId());

                            FlurryAgent.logEvent("PACKAGEUPDATE_START", parameters);

                            if (hasUpdate) {
                                packageManager.startPackageRemove(pkg.getPackageId());
                                packageManager.startPackageDownload(pkg.getPackageId());
                            }

                            if (hasRoutingUpdate) {
                                packageManagerRouting.startPackageRemove(pkg.getPackageId() + "");

                                packageManagerRouting.startPackageDownload(pkg.getPackageId() + "");
                            }

                            Intent intent = new Intent(mapView.getContext(), PackageDownloadService.class);

                            intent.putExtra("job", "download");
                            intent.putExtra("package_id", pkg.getPackageId());
                            intent.putExtra("position", 0);
                            intent.putExtra("level", -1);

                            mapView.getContext().startService(intent);

                            String message = mapView.getContext().getString(
                                    R.string.pck_up) + " " +
                                    getName(pkg.getNames(Locale.getDefault().getLanguage()).get(0)) + " " +
                                    mapView.getContext().getString(R.string.pck_dw2);

                            Toast.makeText(mapView.getContext(), message, Toast.LENGTH_LONG).show();

                            isPckChanged = false;

                            suggestionButton.setEnabled(false);
                            suggestionButton.setOnClickListener(null);

                            // make suggest UI to disappear
                            new Handler().postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    if (!isPckChanged) {
                                        suggestionButton.setVisibility(LinearLayout.GONE);
                                    }
                                }
                            }, 300);
                        }
                    });

                    Log.d(Const.LOG_TAG, "UpdateSuggestion: Update " + pkg.getPackageId());
                } else {
                    if (packageManager.getLocalPackageStatus(pkg.getPackageId(), -1) == null) {

                        suggestionButton.setVisibility(LinearLayout.VISIBLE);
                        suggestionButton.setEnabled(true);

                        long routingPkgSize = 0;
                        if (packageManagerRouting.getServerPackage(pkg.getPackageId() + "") != null) {
                            routingPkgSize = packageManagerRouting.getServerPackage(pkg.getPackageId() + "").getSize().longValue();
                        }

                        String size = " (";

                        if ((pkg.getSize().longValue() + routingPkgSize) < 1024 * 1024) {
                            size += (pkg.getSize().longValue() + routingPkgSize) / 1024 + " KB)";
                        } else if ((pkg.getSize().longValue() + routingPkgSize) / 1024 / 1024 < 1024) {
                            size += (pkg.getSize().longValue() + routingPkgSize) / 1024 / 1024 + " MB)";
                        } else {
                            float value = (pkg.getSize().longValue() + routingPkgSize) * 1.0f / 1024 / 1024 / 1024;
                            size += String.format(Locale.ENGLISH, "%.2f", value) + " GB)";
                        }


                        suggestionButton.setTextColor(Color.WHITE);
                        suggestionButton.setText(mapView.getContext().getString(
                                R.string.download_suggestion)
                                + " "
                                + getName(pkg.getNames(
                                Locale.getDefault().getLanguage())
                                .get(0)) + " " + size);
                        suggestionButton.setOnClickListener(new OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                // flurry event
                                Map<String, String> parameters = new ArrayMap<String, String>();
                                parameters.put("package_id", pkg.getPackageId());

                                FlurryAgent.logEvent("PACKAGEDOWNLOAD_START", parameters);

                                packageManager.startPackageDownload(pkg.getPackageId());

                                boolean routingPkg = false;
                                if (packageManagerRouting.getServerPackage(pkg.getPackageId() + "") != null) {
                                    packageManagerRouting.startPackageDownload(pkg.getPackageId() + "");
                                    routingPkg = true;
                                }

                                Intent intent = new Intent(mapView.getContext(), PackageDownloadService.class);

                                intent.putExtra("job", "download");
                                intent.putExtra("package_id", pkg.getPackageId());
                                intent.putExtra("routing_pkg", routingPkg);
                                intent.putExtra("position", 0);
                                intent.putExtra("level", -1);

                                mapView.getContext().startService(intent);

                                Toast.makeText(
                                        mapView.getContext(),
                                        mapView.getContext().getString(
                                                R.string.pck_dw)
                                                + " "
                                                + getName(pkg
                                                .getNames(
                                                        Locale.getDefault()
                                                                .getLanguage())
                                                .get(0))
                                                + " "
                                                + mapView
                                                .getContext()
                                                .getString(
                                                        R.string.pck_dw2),
                                        Toast.LENGTH_LONG).show();

                                isPckChanged = false;

                                suggestionButton.setEnabled(false);
                                suggestionButton.setOnClickListener(null);

                                // make suggest UI to disappear
                                new Handler().postDelayed(new Runnable() {

                                    @Override
                                    public void run() {
                                        if (!isPckChanged) {
                                            suggestionButton.setVisibility(LinearLayout.GONE);
                                        }
                                    }
                                }, 300);
                            }
                        });

                        Log.d(Const.LOG_TAG, "DownloadSuggestion: Download " + pkg.getPackageId());
                    } else {
                        suggestionButton.setVisibility(LinearLayout.GONE);
                    }
                }
            }
        }
    }

}
