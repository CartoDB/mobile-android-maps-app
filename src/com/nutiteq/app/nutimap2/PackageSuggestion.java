package com.nutiteq.app.nutimap2;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.core.MapTile;
import com.nutiteq.layers.TileLayer;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadService;
import com.nutiteq.packagemanager.PackageInfo;
import com.nutiteq.packagemanager.PackageManager;
import com.nutiteq.packagemanager.PackageTileStatus;
import com.nutiteq.ui.MapView;
import com.nutiteq.wrappedcommons.PackageInfoVector;

public class PackageSuggestion {

	private static int TASK_DELAY = 300;

	private MapView mapView;
	private TileLayer tileLayer;
	private PackageManager packageManager;
	private Button button;

	private boolean taskPending;
	private int taskCounter;
	private PackageInfo currentPkg;

	private boolean isPckChanged = false;

	public void notifyMapMoved() {
		if (mapView == null) {
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
		AsyncTask<Void, Void, PackageInfo> task = new AsyncTask<Void, Void, PackageInfo>() {
			int currentTaskCounter;

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

					// If zoom level is below threshold, this means that the
					// tiles are in base package
					if (mapView.getZoom() < Const.BASE_PACKAGE_ZOOM_LEVELS + 0.5f) {
						return null;
					}

					// Resolve tiles and do fast check wrt to current package
					mapTile = tileLayer.calculateMapTile(mapView.getFocusPos(),
							Const.TILEMASK_ZOOM_LEVELS);
					if (currentPkg != null) {
						PackageTileStatus tileStatus = currentPkg.getTileMask()
								.getTileStatus(mapTile.getZoom(),
										mapTile.getX(), mapTile.getY());
						if (tileStatus != PackageTileStatus.PACKAGE_TILE_STATUS_MISSING) {
							return currentPkg;
						}
					}
				}

				// Find best package
				PackageInfoVector allPkgs = packageManager.getServerPackages();
				PriorityQueue<PackageInfo> sortedPkgs = new PriorityQueue<PackageInfo>(
						1, new Comparator<PackageInfo>() {
							public int compare(PackageInfo pkg1,
									PackageInfo pkg2) {
								PackageTileStatus tileStatus1 = pkg1
										.getTileMask().getTileStatus(
												mapTile.getZoom(),
												mapTile.getX(), mapTile.getY());
								PackageTileStatus tileStatus2 = pkg2
										.getTileMask().getTileStatus(
												mapTile.getZoom(),
												mapTile.getX(), mapTile.getY());
								if (tileStatus1 != tileStatus2) {
									return tileStatus1 == PackageTileStatus.PACKAGE_TILE_STATUS_FULL ? -1
											: 1; // prefer full package
								}
								boolean downloaded1 = packageManager
										.getLocalPackageStatus(
												pkg1.getPackageId(), -1) != null;
								boolean downloaded2 = packageManager
										.getLocalPackageStatus(
												pkg2.getPackageId(), -1) != null;
								if (downloaded1 != downloaded2) {
									return downloaded1 ? -1 : 1; // prefer
																	// already
																	// downloaded
																	// package
								}
								return Long.signum(pkg1.getSize().longValue()
										- pkg2.getSize().longValue()); // prefer
																		// smaller
																		// package
							}
						});
				for (int i = 0; i < allPkgs.size(); i++) {
					PackageInfo pkg = allPkgs.get(i);
					PackageTileStatus tileStatus = pkg.getTileMask()
							.getTileStatus(mapTile.getZoom(), mapTile.getX(),
									mapTile.getY());
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
					button.setVisibility(LinearLayout.GONE);
				} else {
					PackageInfoVector packageInfoVectorLocal = packageManager
							.getLocalPackages();

					boolean hasUpdate = false;

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

					if (hasUpdate) {
						button.setVisibility(LinearLayout.VISIBLE);
						button.setEnabled(true);

						String size = " (";

						if (pkg.getSize().longValue() < 1024 * 1024) {
							size += pkg.getSize().longValue() / 1024 + " KB)";
						} else if (pkg.getSize().longValue() / 1024 / 1024 < 1024) {
							size += pkg.getSize().longValue() / 1024 / 1024
									+ " MB)";
						} else {
							size += String.format("%.2f", pkg.getSize()
									.longValue() * 1.0f / 1024 / 1024 / 1024)
									+ " GB)";
						}

						button.setBackgroundResource(R.color.nutiteq_green);
						button.setTextColor(Color.WHITE);
						button.setText(mapView.getContext().getString(
								R.string.update)
								+ " "
								+ getName(pkg.getNames(
										Locale.getDefault().getLanguage()).get(
										0)) + "\n" + size);

						button.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								// flurry event
								Map<String, String> parameters = new ArrayMap<String, String>();
								parameters.put("package_id", pkg.getPackageId());

								FlurryAgent.logEvent("PACKAGEUPDATE_START",
										parameters);

								packageManager.startPackageRemove(pkg
										.getPackageId());

								packageManager.startPackageDownload(pkg
										.getPackageId());

								Intent intent = new Intent(
										mapView.getContext(),
										PackageDownloadService.class);

								intent.putExtra("job", "download");
								intent.putExtra("package_id",
										pkg.getPackageId());
								intent.putExtra("position", 0);
								intent.putExtra("level", -1);

								mapView.getContext().startService(intent);

								Toast.makeText(
										mapView.getContext(),
										mapView.getContext().getString(
												R.string.pck_up)
												+ " "
												+ getName(pkg.getNames(
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

								button.setEnabled(false);
								button.setOnClickListener(null);

								// make suggest UI to disappear
								new Handler().postDelayed(new Runnable() {

									@Override
									public void run() {
										if (!isPckChanged) {
											button.setVisibility(LinearLayout.GONE);
										}
									}
								}, 300);
							}
						});

						Log.d(Const.LOG_TAG,
								"UpdateSuggestion: Update "
										+ pkg.getPackageId());
					} else {
						if (packageManager.getLocalPackageStatus(
								pkg.getPackageId(), -1) == null) {

							button.setVisibility(LinearLayout.VISIBLE);
							button.setEnabled(true);

							String size = " (";

							if (pkg.getSize().longValue() < 1024 * 1024) {
								size += pkg.getSize().longValue() / 1024
										+ " KB)";
							} else if (pkg.getSize().longValue() / 1024 / 1024 < 1024) {
								size += pkg.getSize().longValue() / 1024 / 1024
										+ " MB)";
							} else {
								size += String.format("%.2f", pkg.getSize()
										.longValue()
										* 1.0f
										/ 1024
										/ 1024
										/ 1024)
										+ " GB)";
							}

							button.setBackgroundResource(R.color.nutiteq_green);
							button.setTextColor(Color.WHITE);
							button.setText(mapView.getContext().getString(
									R.string.download)
									+ " "
									+ getName(pkg.getNames(
											Locale.getDefault().getLanguage())
											.get(0)) + "\n" + size);
							button.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									// flurry event
									Map<String, String> parameters = new ArrayMap<String, String>();
									parameters.put("package_id",
											pkg.getPackageId());

									FlurryAgent
											.logEvent("PACKAGEDOWNLOAD_START",
													parameters);

									packageManager.startPackageDownload(pkg
											.getPackageId());

									Intent intent = new Intent(mapView
											.getContext(),
											PackageDownloadService.class);

									intent.putExtra("job", "download");
									intent.putExtra("package_id",
											pkg.getPackageId());
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

									button.setEnabled(false);
									button.setOnClickListener(null);

									// make suggest UI to disappear
									new Handler().postDelayed(new Runnable() {

										@Override
										public void run() {
											if (!isPckChanged) {
												button.setVisibility(LinearLayout.GONE);
											}
										}
									}, 300);
								}
							});

							Log.d(Const.LOG_TAG,
									"DownloadSuggestion: Download "
											+ pkg.getPackageId());
						} else {
							button.setVisibility(LinearLayout.GONE);
						}
					}
				}
			}
		};

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

	public void setObjects(MapView mapView, TileLayer tileLayer,
			PackageManager packageManager, Button button) {
		this.mapView = mapView;
		this.tileLayer = tileLayer;
		this.packageManager = packageManager;
		this.button = button;

		notifyMapMoved();
	}
}
