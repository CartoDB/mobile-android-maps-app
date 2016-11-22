package com.nutiteq.nuticomponents.locationtracking;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nutiteq.nuticomponents.R;

import java.util.ArrayList;
import java.util.HashMap;

public class LocationTrackingActivityList extends ListActivity {

	private LocationTrackingDB locationTrackingDB;
	private TrackData[] tracksData;
	private int[] tracksID;

	private long curentTrackID;

	private int itemPosition = 0;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationTrackingDB = ((LocationTrackingApplicationInterface) getApplication())
				.getLocationTrackingDB();

		curentTrackID = locationTrackingDB.getCurentTrackID();

		init();

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos,
					long id) {

				if (tracksData[pos] != null) {
					boolean isCurentTrack;

					if (curentTrackID == tracksID[pos]) {
						isCurentTrack = true;
					} else {
						isCurentTrack = false;
					}

					((LocationTrackingApplicationInterface) getApplication())
							.addTrackOnMap(tracksData[pos], isCurentTrack);

					finish();
				} else {
					finish();

					Toast.makeText(LocationTrackingActivityList.this,
							getString(R.string.no_locations), Toast.LENGTH_LONG)
							.show();
				}
			}
		});

		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
					long l) {
				if (curentTrackID == tracksID[pos]) {
					return false;
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							LocationTrackingActivityList.this);

					builder.setTitle(getString(R.string.track) + " "
							+ tracksID[pos]);

					final String[] items = new String[2];
					items[0] = getString(R.string.gpx);
					items[1] = getString(R.string.delete);

					final int p = pos;

					builder.setItems(items,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int i) {
									if (i == 0) {
										Toast.makeText(getApplicationContext(),
												"TODO", Toast.LENGTH_LONG)
												.show();
									} else if (i == 1) {
										if (locationTrackingDB
												.deleteTrack(tracksID[p])) {
											itemPosition = getListView()
													.getFirstVisiblePosition();
											init();
										} else {
											Toast.makeText(
													LocationTrackingActivityList.this,
													getString(R.string.track_delete_error),
													Toast.LENGTH_LONG).show();
										}
									}
								}
							});

					builder.show();

					return true;
				}
			}
		});
	}

	private void init() {
		tracksID = locationTrackingDB.getTracksID();
		int l = tracksID.length;

		tracksData = new TrackData[l];

		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>(
				l);

		for (int i = 0; i < l; i++) {
			HashMap<String, String> item = new HashMap<String, String>();

			tracksData[i] = locationTrackingDB.getTrack(tracksID[i]);

			item.put("line1", getString(R.string.track) + " " + tracksID[i]);

			if (tracksData[i] != null) {
				String startLocation = "";
				String endLocation = "";
				String location = "";

				if (tracksData[i].startLocation != null
						&& !tracksData[i].startLocation.equals("")) {
					startLocation = getString(R.string.start_location) + " "
							+ tracksData[i].startLocation;
				}
				if (tracksData[i].endLocation != null
						&& !tracksData[i].endLocation.equals("")) {
					endLocation = getString(R.string.end_location) + " "
							+ tracksData[i].endLocation;
				}

				if (!startLocation.equals("")) {
					location += "\n" + startLocation;
				}
				if (!endLocation.equals("")) {
					location += "\n" + endLocation;
				}

				item.put("line2", getString(R.string.start_time) + " "
						+ tracksData[i].startTime + "\n"
						+ getString(R.string.duration) + " "
						+ tracksData[i].duration + "\n"
						+ getString(R.string.length) + " "
						+ tracksData[i].distance + location);
			} else {
				item.put("line2", getString(R.string.no_track));
			}

			list.add(item);
		}

		SimpleAdapter listAdapter = new SimpleAdapter(this, list,
				R.layout.gps_track_list, new String[] { "line1", "line2" },
				new int[] { R.id.text1, R.id.text2 });
		setListAdapter(listAdapter);

		getListView().setSelection(itemPosition);
	}
}
