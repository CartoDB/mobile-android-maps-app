package com.nutiteq.app.locationbookmarks;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nutiteq.app.nutimap2.Const;
import com.nutiteq.app.nutimap2.MainActivity;
import com.nutiteq.app.nutimap3d.dev.R;

public class LocationBookmarksListActivity extends ListActivity {

	private LocationBookmarksDB locationBookmarkDB;

	private ArrayList<LocationBookmark> locationBookmarks;

	private ArrayList<Drawable> searchPinDrawables;

	private String selectedLocation = "";

	private int numberOfBookmarks = 0;

	private int itemPosition = 0;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationBookmarkDB = new LocationBookmarksDB(this);
		if (!locationBookmarkDB.isOpen()) {
			locationBookmarkDB.open();
		}

		init();

		// there is no location bookmars, so inform user with toast msg
		if (numberOfBookmarks == 0) {
			Toast.makeText(this, getString(R.string.bookmarks_zero),
					Toast.LENGTH_LONG).show();
		}

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos,
					long id) {
				MainActivity.locationBookmarkId = locationBookmarks.get(pos).id;
				finish();
			}
		});

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v,
					final int pos, long l) {
				selectedLocation = locationBookmarks.get(pos).location;

				AlertDialog.Builder builder = new AlertDialog.Builder(
						LocationBookmarksListActivity.this);

				builder.setTitle(getString(R.string.options));

				final String[] items = new String[2];
				items[0] = getString(R.string.edit);
				items[1] = getString(R.string.delete);

				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int i) {
						if (i == 0) {
							AlertDialog alertDialog = new AlertDialog.Builder(
									LocationBookmarksListActivity.this)
									.create();

							final EditText input = new EditText(
									LocationBookmarksListActivity.this);
							input.setText(selectedLocation);
							input.setSelection(selectedLocation.length());

							alertDialog.setTitle(getString(R.string.edit));
							alertDialog.setView(input);
							alertDialog.setButton(
									DialogInterface.BUTTON_POSITIVE,
									LocationBookmarksListActivity.this
											.getString(R.string.ok),
									new OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (locationBookmarkDB.updateLocationBookmark(
													locationBookmarks.get(pos).id,
													input.getText().toString()
															.trim())) {
												Toast.makeText(
														getApplicationContext(),
														getString(R.string.bookmark_updated),
														Toast.LENGTH_LONG)
														.show();
												itemPosition = getListView()
														.getFirstVisiblePosition();
												init();

												// refresh favorite locations on
												// map
												MainActivity.shouldRefreshFavoriteLocations = true;
											} else {
												Toast.makeText(
														getApplicationContext(),
														getString(R.string.bookmark_updated_error),
														Toast.LENGTH_LONG)
														.show();
											}
										}
									});
							alertDialog.setButton(
									DialogInterface.BUTTON_NEGATIVE,
									LocationBookmarksListActivity.this
											.getString(R.string.cancel),
									new OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											// nothing to do here
										}
									});

							alertDialog.show();
						} else if (i == 1) {
							if (locationBookmarkDB
									.deleteLocationBookmark(locationBookmarks
											.get(pos).id)) {
								Toast.makeText(getApplicationContext(),
										getString(R.string.bookmark_deleted),
										Toast.LENGTH_LONG).show();
								itemPosition = getListView()
										.getFirstVisiblePosition();
								init();

								// refresh favorite locations on map
								MainActivity.shouldRefreshFavoriteLocations = true;
							} else {
								Toast.makeText(
										getApplicationContext(),
										getString(R.string.bookmark_delete_error),
										Toast.LENGTH_LONG).show();
							}
						}
					}
				});
				builder.show();

				return true;
			}
		});

		// didn't find solution from xml
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(
					new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));
			getListView().setSelector(R.xml.selector);
			getListView().setBackgroundColor(Color.WHITE);
		}
	}

	private void init() {
		locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();
		searchPinDrawables = new ArrayList<Drawable>();

		numberOfBookmarks = locationBookmarks.size();

		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < numberOfBookmarks; i++) {
			list.add(locationBookmarks.get(i).location);

			Drawable d = ResizeImage(R.drawable.location_bookmark);
			d.mutate().setColorFilter(
					Color.rgb(locationBookmarks.get(i).red,
							locationBookmarks.get(i).green,
							locationBookmarks.get(i).blue), Mode.MULTIPLY);

			searchPinDrawables.add(d);
		}

		ListAdapter adapter = new ListAdapter(this,
				R.layout.location_bookmark_track_list, R.id.text, list);

		setListAdapter(adapter);

		getListView().setSelection(itemPosition);
	}

	private static class ListItemHolder {
		ImageView image;
		TextView text;
	}

	public class ListAdapter extends ArrayAdapter<String> {

		Context context;
		int layoutResourceId;
		ArrayList<String> list;
		ListItemHolder holder;

		public ListAdapter(Context context, int layoutResourceId, int textRes,
				ArrayList<String> list) {
			super(context, layoutResourceId, textRes, list);

			this.context = context;
			this.layoutResourceId = layoutResourceId;
			this.list = list;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View row = convertView;
			holder = null;

			if (row == null) {
				LayoutInflater inflater = ((Activity) context)
						.getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);

				holder = new ListItemHolder();
				holder.image = (ImageView) row.findViewById(R.id.image);
				holder.text = (TextView) row.findViewById(R.id.text);

				row.setTag(holder);
			} else {
				holder = (ListItemHolder) row.getTag();
			}

			holder.text.setText(list.get(position));
			// with setBackground case error because it's from API 16
			holder.image
					.setBackgroundDrawable(searchPinDrawables.get(position));

			return row;
		}
	}

	public Drawable ResizeImage(int imageID) {
		BitmapDrawable bd = (BitmapDrawable) this.getResources().getDrawable(
				imageID);
		double imageHeight = bd.getBitmap().getHeight();
		double imageWidth = bd.getBitmap().getWidth();

		double ratio = 0.24 * getResources().getDisplayMetrics().density;
		int newImageHeight = (int) (imageHeight * ratio);
		int newImageWidth = (int) (imageWidth * ratio);

		Bitmap bMap = BitmapFactory.decodeResource(getResources(), imageID);
		Drawable drawable = new BitmapDrawable(this.getResources(),
				getResizedBitmap(bMap, newImageWidth, newImageHeight));

		return drawable;
	}

	/************************ Resize Bitmap *********************************/
	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
		int width = bm.getWidth();
		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);

		return resizedBitmap;
	}
}
