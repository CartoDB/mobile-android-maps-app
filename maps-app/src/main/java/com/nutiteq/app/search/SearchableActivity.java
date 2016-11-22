package com.nutiteq.app.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.text.Html;
import android.view.Display;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.carto.core.Variant;
import com.nutiteq.app.utils.Const;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.nutimap3d.dev.R;
import com.carto.core.MapPos;
import com.nutiteq.nuticomponents.GeocodeResult;
import com.nutiteq.nuticomponents.NominatimService;
import com.carto.projections.EPSG3857;
import com.carto.projections.Projection;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.carto.vectorelements.Marker;

public class SearchableActivity extends ListActivity implements AdapterView.OnItemClickListener {

	private ProgressDialog progressDialog;

	private ArrayList<HashMap<String, String>> list;

	// for search results
	private Marker[] searchResultPlaces;

	private boolean isCancled;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		Intent intent = getIntent();

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

			String query = intent.getStringExtra(SearchManager.QUERY);

			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
					this,
					SearchSuggestionProvider.AUTHORITY,
					SearchSuggestionProvider.MODE
			);

			suggestions.saveRecentQuery(query, null);
		}

		getListView().setOnItemClickListener(this);

		// didn't find solution from xml
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));

			getListView().setSelector(R.xml.selector);
			getListView().setBackgroundColor(Color.WHITE);
			getActionBar().setHomeButtonEnabled(true);
			getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.menu_search) + "</b>"));
		}

		handleIntent(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> aparent, View v, int position, long id) {

		// set data for picked search
		Intent data = new Intent();

		data.putExtra(
				Const.SEARCH_X,
				searchResultPlaces[position].getGeometry().getCenterPos().getX());
		data.putExtra(
				Const.SEARCH_Y,
				searchResultPlaces[position].getGeometry().getCenterPos().getY());

		if (searchResultPlaces[position].getMetaDataElement("title") != null) {
			data.putExtra(Const.SEARCH_TITLE, searchResultPlaces[position].getMetaDataElement("title").getString());
		}

		if (searchResultPlaces[position].getMetaDataElement("description") != null) {
			data.putExtra(Const.SEARCH_DESC, searchResultPlaces[position].getMetaDataElement("description").getString());
		}

		if (searchResultPlaces[position].getMetaDataElement("x1") != null) {
			data.putExtra("x1", searchResultPlaces[position].getMetaDataElement("x1").getString());
		}
		if (searchResultPlaces[position].getMetaDataElement("x2") != null) {
			data.putExtra("x2", searchResultPlaces[position].getMetaDataElement("x2").getString());
		}
		if (searchResultPlaces[position].getMetaDataElement("y1") != null) {
			data.putExtra("y1", searchResultPlaces[position].getMetaDataElement("y1").getString());
		}
		if (searchResultPlaces[position].getMetaDataElement("y2") != null) {
			data.putExtra("y2", searchResultPlaces[position].getMetaDataElement("y2").getString());
		}

		setResult(Activity.RESULT_OK, data);

		finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMySearch(query);
		}
	}

	private void doMySearch(final String query) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				final NominatimService nominatium = new NominatimService();

				try {
					lockOrientation(SearchableActivity.this);

					handler.post(new Runnable() {

						@Override
						public void run() {
							progressDialog = new ProgressDialog(
									SearchableActivity.this);
							progressDialog
									.setMessage(getString(R.string.search_progress)
											+ " " + query);
							progressDialog.setCancelable(true);
							progressDialog
									.setOnCancelListener(new OnCancelListener() {

										@Override
										public void onCancel(
												DialogInterface dialog) {
											isCancled = true;
											finish();
										}
									});
							progressDialog.show();
						}
					});

					isCancled = false;

					ArrayList<GeocodeResult> geocodeResults = nominatium
							.geocode(query);

					handler.post(new Runnable() {

						@Override
						public void run() {
							progressDialog.dismiss();
						}
					});

					if (geocodeResults != null && !isCancled) {
						Projection proj = new EPSG3857();

						Bitmap searchMarkerBitmap = BitmapFactory
								.decodeResource(getResources(),
										R.drawable.search_marker);
						com.carto.graphics.Bitmap markerBitmap = BitmapUtils
								.createBitmapFromAndroidBitmap(searchMarkerBitmap);

						// Create marker style
						MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
						markerStyleBuilder.setBitmap(markerBitmap);
						markerStyleBuilder.setSize(36);
						MarkerStyle searchMarkerStyle = markerStyleBuilder.buildStyle();

						int l = geocodeResults.size();
						list = new ArrayList<HashMap<String, String>>(l);
						searchResultPlaces = new Marker[l];

						for (int i = 0; i < l; i++) {
							HashMap<String, String> item = new HashMap<String, String>();
							item.put("line1", geocodeResults.get(i).line1);
							item.put("line2", geocodeResults.get(i).line2);

							list.add(item);

							searchResultPlaces[i] = new Marker(proj.fromWgs84(new MapPos(
											geocodeResults.get(i).lon,
											geocodeResults.get(i).lat)),
									searchMarkerStyle);
							searchResultPlaces[i].setMetaDataElement("title", new Variant(geocodeResults.get(i).line1));
							searchResultPlaces[i].setMetaDataElement("description", new Variant(geocodeResults.get(i).line2));

							searchResultPlaces[i].setMetaDataElement(
									"x1",
									new Variant(geocodeResults.get(i).boundingBox.getMin().getX() + "")
							);
							searchResultPlaces[i].setMetaDataElement(
									"y1",
									new Variant(geocodeResults.get(i).boundingBox.getMin().getY() + "")
							);
							searchResultPlaces[i].setMetaDataElement(
									"x2",
									new Variant(geocodeResults.get(i).boundingBox.getMax().getX() + "")
							);
							searchResultPlaces[i].setMetaDataElement(
									"y2",
									new Variant(geocodeResults.get(i).boundingBox.getMax().getY() + "")
							);
						}

						setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

						if (list.size() > 0) {
							Message msg = handler.obtainMessage();
							handler.sendMessage(msg);
						} else {
							finish();
							handler.postDelayed(new Runnable() {

								@Override
								public void run() {
									Toast.makeText(
											SearchableActivity.this,
											getString(R.string.nothing_found)
													+ " " + query,
											Toast.LENGTH_LONG).show();
								}
							}, 500);
						}
					} else {
						handler.post(new Runnable() {

							@Override
							public void run() {
								if (!isCancled) {
									Toast.makeText(getApplicationContext(),
											getString(R.string.geocode_error),
											Toast.LENGTH_LONG).show();
								}
							}
						});
						finish();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	// handler to send search results to UI thread
	@SuppressLint("HandlerLeak")
	final Handler handler = new Handler() {

		public void handleMessage(Message msg) {

			SimpleAdapter listAdapter = new SimpleAdapter(
					SearchableActivity.this, list,
					R.layout.searchrow,
					new String[] { "line1", "line2" },
					new int[] { R.id.text1, R.id.text2 }
			);

			setListAdapter(listAdapter);
		}
	};

	private void lockOrientation(Activity activity) {
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
