package com.nutiteq.app.settings;

import java.io.File;
import java.util.Locale;

import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.nutiteq.app.nutimap2.Const;
import com.nutiteq.app.nutimap2.MainActivity;
import com.nutiteq.app.nutimap2.MapApplication;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingDB;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadService;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerComponent;

public class SettingsActivity9 extends PreferenceActivity {

	private ListPreference storage;

	private String storageValue;
	private String lastStorageValue;

	boolean isExternalAvaiable = true;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		final ListPreference lang = (ListPreference) findPreference("pref_lang_key");

		lang.setTitle(getString(R.string.lang) + " ("
				+ getRealLangName(lang.getValue()) + ")");

		lang.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object object) {
				MainActivity.setMapLanguage(object.toString());
				lang.setTitle(getString(R.string.lang) + " ("
						+ getRealLangName(object.toString()) + ")");
				return true;
			}
		});

		if (MainActivity.isSupportedBySDK(Locale.getDefault().getLanguage())) {
			lang.getEntries()[0] = getString(R.string.lang_automatic) + " ("
					+ Locale.getDefault().getDisplayLanguage() + ")";
		} else {
			lang.getEntries()[0] = getString(R.string.lang_automatic) + " ("
					+ getString(R.string.lang_local) + ")";
		}

		final ListPreference unit = (ListPreference) findPreference("pref_unit_key");
		unit.setTitle(getString(R.string.measurement) + " ("
				+ getRealUnitName(unit.getValue()) + ")");
		unit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object object) {
				MainActivity.setMeasurementUnit(object.toString());
				unit.setTitle(getString(R.string.measurement) + " ("
						+ getRealUnitName(object.toString()) + ")");

				int m = 0;
				if (object.toString().equals(Const.METRIC)) {
					m = GPSTrackingDB.METRIC;
				} else if (object.toString().equals(Const.IMPERIAL)) {
					m = GPSTrackingDB.IMPERIAL;
				}

				((MapApplication) getApplication()).getGPSTrackingDB()
						.setMeasurementUnit(m);

				return true;
			}
		});

		storage = (ListPreference) findPreference("pref_storage_key");

		if (!((MapApplication) getApplication()).getPackageManagerComponent()
				.isSetToDefaultInternal()) {
			setStorageTitle(storage.getValue());
		} else {
			storage.setTitle(getString(R.string.storage) + " ("
					+ getString(R.string.internal) + ")");
		}

		// it must be disabled because packages is downloading
		if (PackageDownloadService.isLive) {
			storage.setEnabled(false);
		}

		// disable if external storage isn't available
		if (!Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| ((MapApplication) getApplication())
						.getPackageManagerComponent().isSetToDefaultInternal()) {
			storage.setEnabled(false);
			isExternalAvaiable = false;
		}

		setFreeStorageInfo();

		storageValue = storage.getValue();

		storage.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object object) {
				// nothing changed
				if (object.toString().equals(storageValue)) {
					return false;
				}

				lastStorageValue = storageValue;
				storageValue = object.toString();

				if (storageValue.equals(Const.INTERNAL)) {
					boolean hasEnoughMemory = false;
					long freeInternal = getFilesDir().getFreeSpace()
							- Const.INTERNAL_STORAGE_MIN * 1048576;
					long l = 0;

					File file = getExternalFilesDir(null);

					if (file == null) {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					File[] f = new File(file, "mappackages").listFiles();

					if (f == null) {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					for (int i = 0; i < f.length; i++) {
						l += f[i].length();
					}

					if (freeInternal > l) {
						hasEnoughMemory = true;
					} else {
						hasEnoughMemory = false;
					}

					if (hasEnoughMemory) {
						Runnable r1 = new Runnable() {

							@Override
							public void run() {
								setFreeStorageInfo();
								MainActivity.setShouldUpdateBaseLayer(true);
							}
						};
						Runnable r2 = new Runnable() {

							@Override
							public void run() {
								Toast.makeText(SettingsActivity9.this,
										getString(R.string.moving_files_error),
										Toast.LENGTH_LONG).show();

								storage.setValue(lastStorageValue);
								storageValue = lastStorageValue;

								setStorageTitle(storageValue);
							}
						};

						((MapApplication) getApplication())
								.getPackageManagerComponent()
								.movePackageManagerFromTo(
										PackageManagerComponent.EXTERNAL_STORAGE,
										1,
										PackageManagerComponent.INTERNAL_STORAGE,
										1, r1, r2, SettingsActivity9.this);
					} else {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_no_space),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;
						return false;
					}
				} else if (storageValue.equals(Const.EXTERNAL)) {

					boolean hasEnoughMemory = false;
					long freeExternal = 0;

					// in some rare situation can return null
					File file = getExternalFilesDir(null);

					if (file == null) {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					freeExternal = file.getFreeSpace()
							- Const.EXTERNAL_STORAGE_MIN * 1048576;

					long l = 0;

					File[] f = new File(getFilesDir(), "mappackages")
							.listFiles();

					if (f == null) {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					for (int i = 0; i < f.length; i++) {
						l += f[i].length();
					}

					if (freeExternal > l) {
						hasEnoughMemory = true;
					} else {
						hasEnoughMemory = false;
					}

					if (hasEnoughMemory) {
						Runnable r1 = new Runnable() {

							@Override
							public void run() {
								setFreeStorageInfo();
								MainActivity.setShouldUpdateBaseLayer(true);
							}
						};
						Runnable r2 = new Runnable() {

							@Override
							public void run() {
								Toast.makeText(SettingsActivity9.this,
										getString(R.string.moving_files_error),
										Toast.LENGTH_LONG).show();

								storage.setValue(lastStorageValue);
								storageValue = lastStorageValue;

								setStorageTitle(storageValue);
							}
						};

						((MapApplication) getApplication())
								.getPackageManagerComponent()
								.movePackageManagerFromTo(
										PackageManagerComponent.INTERNAL_STORAGE,
										1,
										PackageManagerComponent.EXTERNAL_STORAGE,
										1, r1, r2, SettingsActivity9.this);
					} else {
						Toast.makeText(SettingsActivity9.this,
								getString(R.string.moving_files_no_space),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;
						return false;
					}
				}

				setStorageTitle(object.toString());

				return true;
			}
		});
	}

	private void setStorageTitle(String s) {
		if (s.toString().contains(Const.EXTERNAL)) {
			storage.setTitle(getString(R.string.storage) + " ("
					+ getString(R.string.external) + ")");
		} else {
			storage.setTitle(getString(R.string.storage) + " ("
					+ getString(R.string.internal) + ")");
		}
	}

	private String getRealLangName(String lang) {
		if (lang.equals(Const.LANG_AUTOMATIC)) {
			return getString(R.string.lang_automatic);
		}

		if (lang.equals(Const.LANG_LOCAL)) {
			return getString(R.string.lang_local);
		}

		if (lang.equals(Const.LANG_ENGLISH)) {
			return getString(R.string.lang_en);
		}

		if (lang.equals(Const.LANG_GERMAN)) {
			return getString(R.string.lang_de);
		}

		if (lang.equals(Const.LANG_FRENCH)) {
			return getString(R.string.lang_fr);
		}

		if (lang.equals(Const.LANG_RUSSIAN)) {
			return getString(R.string.lang_ru);
		}

		if (lang.equals(Const.LANG_CHINESE)) {
			return getString(R.string.lang_cn);
		}

		if (lang.equals(Const.LANG_SPANISH)) {
			return getString(R.string.lang_es);
		}

		if (lang.equals(Const.LANG_ITALIAN)) {
			return getString(R.string.lang_it);
		}

		if (lang.equals(Const.LANG_ESTONIAN)) {
			return getString(R.string.lang_et);
		}

		return "";
	}

	private String getRealUnitName(String unit) {
		if (unit.equals(Const.METRIC)) {
			return getString(R.string.metric);
		}

		if (unit.equals(Const.IMPERIAL)) {
			return getString(R.string.imperial);
		}

		return "";
	}

	private void setFreeStorageInfo() {
		String internal = "";
		String external = "";

		// internal storage folder, maybe it can return null so for sure I
		// check if it's null
		File internalFolder = getFilesDir();

		if (internalFolder != null) {
			long freeInternal = internalFolder.getFreeSpace();

			if (freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024 < 1024 * 1024) {
				internal = " ("
						+ ((freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024) / 1024)
						+ " KB " + getString(R.string.free) + ")";
			} else if ((freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024 < 1024) {
				internal = " ("
						+ ((freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024)
						+ " MB " + getString(R.string.free) + ")";
			} else {
				internal = " ("
						+ String.format(Locale.getDefault(),
								"%.2f",
								(freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024)
										* 1.0f / 1024 / 1024 / 1024) + " GB "
						+ getString(R.string.free) + ")";
			}
		}

		if (isExternalAvaiable) {
			// external storage folder, maybe it can return null so for sure I
			// check if it's null
			File externalFolder = getExternalFilesDir(null);

			if (externalFolder != null) {
				long freeExternal = externalFolder.getFreeSpace();

				if (freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024 < 1024 * 1024) {
					external = " ("
							+ ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024)
							+ " KB " + getString(R.string.free) + ")";
				} else if ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024 < 1024) {
					external = " ("
							+ ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024)
							+ " MB " + getString(R.string.free) + ")";
				} else {
					external = " ("
							+ String.format(Locale.getDefault(),
									"%.2f",
									(freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024)
											* 1.0f / 1024 / 1024 / 1024)
							+ " GB " + getString(R.string.free) + ")";
				}
			}
		}

		String[] entries = new String[2];
		entries[0] = getString(R.string.internal) + internal;
		entries[1] = getString(R.string.external) + external;

		storage.setEntries(entries);
	}
}
