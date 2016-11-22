package com.nutiteq.nuticomponents.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;

import com.nutiteq.nuticomponents.R;

public class LocationSettingsSupportDialog extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				new ContextThemeWrapper(getActivity(), R.style.AppTheme));
		builder.setTitle(R.string.app_name)
				.setMessage(R.string.error_gps_disabled)
				.setIcon(R.drawable.icon)
				.setPositiveButton(R.string.gps_settings,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								// start location settings on device
								startActivity(new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

							}
						})
				.setNegativeButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								// nothing to do

							}
						});

		return builder.create();
	}
}
