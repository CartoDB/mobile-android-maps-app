package com.nutiteq.app.nutimap2;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.nutiteq.app.locationbookmarks.LocationBookmark;
import com.nutiteq.app.locationbookmarks.LocationBookmarksDB;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.core.MapPos;
import com.nutiteq.datasources.LocalVectorDataSource;
import com.nutiteq.graphics.Color;
import com.nutiteq.nuticomponents.NominatimService;
import com.nutiteq.nuticomponents.customviews.CompassView;
import com.nutiteq.nuticomponents.customviews.ScaleBarView;
import com.nutiteq.projections.Projection;
import com.nutiteq.styles.BalloonPopupMargins;
import com.nutiteq.styles.BalloonPopupStyleBuilder;
import com.nutiteq.styles.MarkerStyleBuilder;
import com.nutiteq.ui.ClickType;
import com.nutiteq.ui.MapClickInfo;
import com.nutiteq.ui.MapEventListener;
import com.nutiteq.ui.MapView;
import com.nutiteq.ui.VectorElementClickInfo;
import com.nutiteq.ui.VectorElementsClickInfo;
import com.nutiteq.utils.BitmapUtils;
import com.nutiteq.vectorelements.BalloonPopup;
import com.nutiteq.vectorelements.Billboard;
import com.nutiteq.vectorelements.Marker;
import com.nutiteq.vectorelements.Popup;
import com.nutiteq.vectorelements.VectorElement;

public class MyMapEventListener extends MapEventListener {

	private MapView mapView;
	private CompassView compassView;
	private ScaleBarView scaleBarView;
	private PackageSuggestion packageSuggestion;
	private LocalVectorDataSource vectorDataSource;
	private ImageButton bookmarkButton;
	private LocalVectorDataSource bookmarkDataSource;

	private ArrayList<LocationBookmark> locationBookmarks;
	private ArrayList<Marker> pins;

	private BalloonPopup oldClickLabel;

	private int strokeWidth;

	private Projection projection;
	private Context context;
	private Handler handler;

	private LocationBookmarksDB locationBookmarkDB;

	private com.nutiteq.graphics.Bitmap bookmarkBitmap;

	private VectorElement vectorElement;
	private Billboard billboard;

	private float angle = Float.MAX_VALUE;

	private Random rand = new Random();

	// contains information about selected location bookmark id and vector
	// element, it's init when user click on location bookmarks pin
	private int id;
	private VectorElement selectedVectorElement;

	private MarkerStyleBuilder markerStyleBuilder;

	private BalloonPopupStyleBuilder balloonPopupStyleBuilder;

	public MyMapEventListener(MapView mapView, CompassView compassView,
			ScaleBarView scalBarView, PackageSuggestion packageSuggestion,
			LocalVectorDataSource vectorDataSource, ImageButton bookmarkButton,
			LocalVectorDataSource bookmarkDataSource) {

		this.mapView = mapView;
		this.compassView = compassView;
		this.scaleBarView = scalBarView;
		this.packageSuggestion = packageSuggestion;
		this.vectorDataSource = vectorDataSource;
		this.bookmarkButton = bookmarkButton;
		this.bookmarkDataSource = bookmarkDataSource;

		handler = new Handler(mapView.getContext().getMainLooper());

		bookmarkButton.setBackgroundColor(mapView.getContext().getResources()
				.getColor(R.color.nutiteq_green));

		angle = mapView.getMapRotation();

		DisplayMetrics metrics = mapView.getContext().getResources()
				.getDisplayMetrics();
		strokeWidth = (int) (metrics.density * 1.0f);

		projection = mapView.getOptions().getBaseProjection();
		context = mapView.getContext();

		locationBookmarkDB = new LocationBookmarksDB(context);
		if (!locationBookmarkDB.isOpen()) {
			locationBookmarkDB.open();
		}

		Bitmap searchMarkerBitmap = BitmapFactory.decodeResource(
				context.getResources(), R.drawable.location_bookmark);
		bookmarkBitmap = BitmapUtils
				.createBitmapFromAndroidBitmap(searchMarkerBitmap);

		markerStyleBuilder = new MarkerStyleBuilder();
		markerStyleBuilder.setBitmap(bookmarkBitmap);

		markerStyleBuilder.setHideIfOverlapped(false);
		markerStyleBuilder.setSize(32);

		balloonPopupStyleBuilder = new BalloonPopupStyleBuilder();
		balloonPopupStyleBuilder.setCornerRadius(2);
		balloonPopupStyleBuilder.setTriangleHeight(0);
		balloonPopupStyleBuilder.setRightMargins(new BalloonPopupMargins(10,
				10, 10, 10));
		balloonPopupStyleBuilder.setLeftMargins(new BalloonPopupMargins(10, 10,
				10, 10));
		balloonPopupStyleBuilder.setTitleMargins(new BalloonPopupMargins(0, 10,
				0, 0));
		balloonPopupStyleBuilder.setDescriptionMargins(new BalloonPopupMargins(
				0, 0, 0, 10));
		balloonPopupStyleBuilder.setLeftColor(new Color((short) 255,
				(short) 255, (short) 255, (short) 255));
		balloonPopupStyleBuilder.setTitleColor(new com.nutiteq.graphics.Color(
				(short) 48, (short) 48, (short) 48, (short) 255));
		balloonPopupStyleBuilder.setDescriptionColor(new Color((short) 133,
				(short) 133, (short) 133, (short) 255));
		balloonPopupStyleBuilder.setStrokeColor(new Color((short) 208,
				(short) 208, (short) 208, (short) 108));
		balloonPopupStyleBuilder.setStrokeWidth(strokeWidth);
		balloonPopupStyleBuilder.setPlacementPriority(1);

		refreshFavoriteLocationsOnMap();
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
	public void onMapClicked(final MapClickInfo mapClickInfo) {
		// Remove old click label
		if (oldClickLabel != null) {
			vectorDataSource.remove(oldClickLabel);
			oldClickLabel = null;
		}

		if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DOUBLE) {
			mapView.zoom(1.5f, mapClickInfo.getClickPos(), 0.6f);
		} else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DUAL) {
			mapView.zoom(-1.5f, 0.6f);
		}

		if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
			longClick(mapClickInfo.getClickPos());
		} else {
			if (bookmarkButton.getVisibility() == View.VISIBLE) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						bookmarkButton.setVisibility(View.GONE);
					}
				});
			}
		}
	}

	@Override
	public void onVectorElementClicked(
			final VectorElementsClickInfo vectorElementsClickInfo) {
		// Multiple vector elements can be clicked at the same time, we only
		// care about the one closest to the camera
		VectorElementClickInfo clickInfo = vectorElementsClickInfo
				.getVectorElementClickInfos().get(0);

		vectorElement = clickInfo.getVectorElement();

		if (vectorElement instanceof Popup) {
			// set zoom to 17 if it is smaller than 17
			if (mapView.getZoom() < 17) {
				mapView.zoom(17 - mapView.getZoom(), ((Popup) vectorElement)
						.getRootGeometry().getCenterPos(), 0.6f);
			}
		} else {
			// Remove old click label
			if (oldClickLabel != null) {
				vectorDataSource.remove(oldClickLabel);
				oldClickLabel = null;
			}

			if (bookmarkButton.getVisibility() == View.VISIBLE) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						bookmarkButton.setVisibility(View.GONE);
					}
				});
			}
		}

		BalloonPopup clickPopup = null;

		// If the element is billboard, attach the click label to the
		// billboard element
		if (vectorElement instanceof Billboard) {
			billboard = (Billboard) vectorElement;
			String title = billboard.getMetaDataElement("title");
			String description = billboard.getMetaDataElement("description");

			if (!title.equals("") || !description.equals("")) {
				clickPopup = new BalloonPopup(billboard,
						balloonPopupStyleBuilder.buildStyle(), title,
						description);
			}

			if (vectorElement instanceof BalloonPopup) {
				// nothing to do
			} else if (description.contains(context.getString(R.string.lat))) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						bookmarkButton
								.setImageResource(android.R.drawable.ic_menu_delete);
						bookmarkButton.setVisibility(View.VISIBLE);
						bookmarkButton.setEnabled(true);
						bookmarkButton.bringToFront();

						try {
							id = Integer.parseInt(billboard
									.getMetaDataElement("id"));
						} catch (Exception e) {
							id = -1;
						}

						selectedVectorElement = vectorElement;

						bookmarkButton
								.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(View v) {
										bookmarkButton.setEnabled(false);
										bookmarkButton.setOnClickListener(null);

										boolean isDone = locationBookmarkDB
												.deleteLocationBookmark(id);

										if (isDone) {
											if (oldClickLabel != null) {
												vectorDataSource
														.remove(oldClickLabel);
											}
											if (vectorElement != null) {
												bookmarkDataSource
														.remove(selectedVectorElement);
											}

											handler.post(new Runnable() {

												@Override
												public void run() {
													bookmarkButton
															.setVisibility(View.GONE);
													Toast.makeText(
															context,
															context.getString(R.string.location_bookmark_delete),
															Toast.LENGTH_SHORT)
															.show();
												}
											});
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
										}
									}
								});
					}
				});
			} else {
				handler.post(new Runnable() {

					@Override
					public void run() {
						if (oldClickLabel != null) {
							bookmarkButton
									.setImageResource(android.R.drawable.ic_menu_add);
							bookmarkButton.setVisibility(View.VISIBLE);
							bookmarkButton.setEnabled(true);
							bookmarkButton.bringToFront();

							bookmarkButton
									.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View v) {
											bookmarkButton.setEnabled(false);
											bookmarkButton
													.setOnClickListener(null);

											String location = oldClickLabel
													.getTitle()
													+ ", "
													+ oldClickLabel
															.getDescription();

											insertLocationBookmarkInDB(location);
										}
									});
						} else {
							if (vectorElementsClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
								longClick(vectorElement.getGeometry()
										.getCenterPos());
							} else if (vectorElementsClickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE) {
								if (mapView.getZoom() < 17) {
									mapView.zoom(17 - mapView.getZoom(),
											((Billboard) vectorElement)
													.getRootGeometry()
													.getCenterPos(), 0.6f);
								}
							}
						}
					}
				});
			}
		}

		if (clickPopup != null) {
			vectorDataSource.add(clickPopup);
			oldClickLabel = clickPopup;
		}
	}

	public void setOldClickLabelWhenSearch(final BalloonPopup oldClickLabel,
			final VectorElement vectorElement) {
		this.oldClickLabel = oldClickLabel;
		this.vectorElement = vectorElement;

		handler.post(new Runnable() {

			@Override
			public void run() {
				bookmarkButton.setImageResource(android.R.drawable.ic_menu_add);
				bookmarkButton.setVisibility(View.VISIBLE);
				bookmarkButton.setEnabled(true);
				bookmarkButton.bringToFront();

				bookmarkButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						bookmarkButton.setEnabled(false);
						bookmarkButton.setOnClickListener(null);

						String location = oldClickLabel.getTitle() + ", "
								+ oldClickLabel.getDescription();

						insertLocationBookmarkInDB(location);
					}
				});
			}
		});
	}

	public void refreshFavoriteLocationsOnMap() {
		bookmarkDataSource.removeAll();

		locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();

		pins = new ArrayList<Marker>();

		for (int i = 0; i < locationBookmarks.size(); i++) {
			int red = locationBookmarks.get(i).red;
			int green = locationBookmarks.get(i).green;
			int blue = locationBookmarks.get(i).blue;

			markerStyleBuilder.setColor(new Color(android.graphics.Color.rgb(
					red, green, blue)));

			Marker marker = new Marker(new MapPos(locationBookmarks.get(i).lon,
					locationBookmarks.get(i).lat),
					markerStyleBuilder.buildStyle());

			String location = locationBookmarks.get(i).location;

			final MapPos wgs = projection.toWgs84(new MapPos(locationBookmarks
					.get(i).lon, locationBookmarks.get(i).lat));

			marker.setMetaDataElement("title", location);
			marker.setMetaDataElement("description", formatDesc(wgs));
			marker.setMetaDataElement("id", locationBookmarks.get(i).id + "");

			bookmarkDataSource.add(marker);

			pins.add(marker);
		}
	}

	public void selectLocationBookmark(long id) {
		for (int i = 0; i < locationBookmarks.size(); i++) {
			if (locationBookmarks.get(i).id == id) {
				mapView.setFocusPos(new MapPos(locationBookmarks.get(i).lon,
						locationBookmarks.get(i).lat), 1);

				BalloonPopup clickPopup;

				MapPos wgs = projection.toWgs84(new MapPos(locationBookmarks
						.get(i).lon, locationBookmarks.get(i).lat));

				String title = locationBookmarks.get(i).location;
				String description = formatDesc(wgs);

				clickPopup = new BalloonPopup(pins.get(i),
						balloonPopupStyleBuilder.buildStyle(), title,
						description);

				vectorDataSource.add(clickPopup);
				oldClickLabel = clickPopup;

				break;
			}
		}
	}

	public BalloonPopupStyleBuilder getBalloonPopupStyleBuilder() {
		return balloonPopupStyleBuilder;
	}

	private void insertLocationBookmarkInDB(String location) {
		int red = randInt(0, 255);
		int green = randInt(0, 255);
		int blue = randInt(0, 255);

		long id = locationBookmarkDB.insertLocationBookmark(oldClickLabel
				.getRootGeometry().getCenterPos().getX(), oldClickLabel
				.getRootGeometry().getCenterPos().getY(), location, red, green,
				blue);

		if (id != -1) {
			markerStyleBuilder.setColor(new Color(android.graphics.Color.rgb(
					red, green, blue)));

			final MapPos wgs = projection.toWgs84(oldClickLabel
					.getRootGeometry().getCenterPos());

			Marker marker = new Marker(oldClickLabel.getRootGeometry()
					.getCenterPos(), markerStyleBuilder.buildStyle());

			marker.setMetaDataElement("title", location);
			marker.setMetaDataElement("description", formatDesc(wgs));
			marker.setMetaDataElement("id", id + "");

			vectorDataSource.remove(oldClickLabel);

			if (vectorElement != null) {
				vectorDataSource.remove(vectorElement);
			}

			oldClickLabel = null;

			bookmarkDataSource.add(marker);

			handler.post(new Runnable() {

				@Override
				public void run() {
					bookmarkButton.setVisibility(View.GONE);
					Toast.makeText(
							context,
							context.getString(R.string.location_bookmark_added),
							Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			handler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(context,
							context.getString(R.string.bookmark_error),
							Toast.LENGTH_SHORT).show();
				}
			});
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
					String location = nominatium.reverseGeocode(wgs.getX(),
							wgs.getY());

					if (location == null) {
						// check if it's same balloon
						if (oldClickLabel != null
								&& oldClickLabel
										.getTitle()
										.equals(context
												.getString(R.string.retrieving_location))) {
							oldClickLabel.setTitle(context
									.getString(R.string.location));
						}
					} else {
						// check if it's same baloon
						if (oldClickLabel != null
								&& oldClickLabel
										.getTitle()
										.equals(context
												.getString(R.string.retrieving_location))) {
							oldClickLabel.setTitle(location);
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

	private void longClick(MapPos clickPoint) {
		BalloonPopup clickPopup;

		BalloonPopupStyleBuilder balloonPopupStyleBuilder = new BalloonPopupStyleBuilder();
		balloonPopupStyleBuilder.setCornerRadius(2);
		balloonPopupStyleBuilder.setTriangleHeight(0);
		balloonPopupStyleBuilder.setRightMargins(new BalloonPopupMargins(10,
				10, 10, 10));
		balloonPopupStyleBuilder.setLeftMargins(new BalloonPopupMargins(10, 10,
				10, 10));
		balloonPopupStyleBuilder.setTitleMargins(new BalloonPopupMargins(0, 10,
				0, 0));
		balloonPopupStyleBuilder.setDescriptionMargins(new BalloonPopupMargins(
				0, 0, 0, 10));
		balloonPopupStyleBuilder.setLeftColor(new Color((short) 255,
				(short) 255, (short) 255, (short) 255));
		balloonPopupStyleBuilder.setTitleColor(new com.nutiteq.graphics.Color(
				(short) 48, (short) 48, (short) 48, (short) 255));
		balloonPopupStyleBuilder.setDescriptionColor(new Color((short) 133,
				(short) 133, (short) 133, (short) 255));
		balloonPopupStyleBuilder.setStrokeColor(new Color((short) 208,
				(short) 208, (short) 208, (short) 108));
		balloonPopupStyleBuilder.setStrokeWidth(strokeWidth);
		balloonPopupStyleBuilder.setPlacementPriority(1);
		balloonPopupStyleBuilder.setTriangleHeight(12);

		final MapPos wgs = projection.toWgs84(clickPoint);

		clickPopup = new BalloonPopup(clickPoint,
				balloonPopupStyleBuilder.buildStyle(),
				context.getString(R.string.retrieving_location),
				formatDesc(wgs));

		vectorDataSource.add(clickPopup);

		oldClickLabel = clickPopup;

		handler.post(new Runnable() {

			@Override
			public void run() {
				bookmarkButton.setImageResource(android.R.drawable.ic_menu_add);
				bookmarkButton.setVisibility(View.VISIBLE);
				bookmarkButton.setEnabled(true);
				bookmarkButton.bringToFront();

				bookmarkButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						bookmarkButton.setEnabled(false);
						bookmarkButton.setOnClickListener(null);

						String location = "";

						if (oldClickLabel
								.getTitle()
								.equals(context
										.getString(R.string.retrieving_location))) {
							location = context.getString(R.string.location);
						} else {
							location = oldClickLabel.getTitle();
						}

						insertLocationBookmarkInDB(location);
					}
				});
			}
		});

		doReverseGeocoding(wgs);
	}

	private String formatDesc(MapPos wgs) {
		return String.format(Locale.getDefault(), "%s %.6f %s %s %.6f",
				context.getString(R.string.lat), wgs.getY(), ",",
				context.getString(R.string.lon), wgs.getX());
	}
}
