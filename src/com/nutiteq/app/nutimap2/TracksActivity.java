package com.nutiteq.app.nutimap2;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingActivityList;

public class TracksActivity extends GPSTrackingActivityList {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// didn't find solution from xml
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(
					new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));
			getListView().setSelector(R.xml.selector);
			getListView().setBackgroundColor(Color.WHITE);
		}
	}
}
