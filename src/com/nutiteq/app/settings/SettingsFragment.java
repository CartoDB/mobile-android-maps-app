package com.nutiteq.app.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v4.util.ArrayMap;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.nutiteq.app.nutimap2.Const;
import com.nutiteq.app.nutimap2.MainActivity;
import com.nutiteq.app.nutimap2.MapApplication;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.nuticomponents.locationtracking.GPSTrackingDB;
import com.nutiteq.nuticomponents.packagemanager.PackageDownloadService;
import com.nutiteq.nuticomponents.packagemanager.PackageManagerComponent;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment {

	private ListPreference storage;

	private String storageValue;
	private String lastStorageValue;

	boolean isExternalAvaiable = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		final ListPreference lang = (ListPreference) findPreference("pref_lang_key");

		lang.setTitle(getString(R.string.lang) + " ("
				+ getRealLangName(lang.getValue()) + ")");

		if (MainActivity.isSupportedBySDK(Locale.getDefault().getLanguage())) {
			lang.getEntries()[0] = getString(R.string.lang_automatic) + " ("
					+ Locale.getDefault().getDisplayLanguage() + ")";
		} else {
			lang.getEntries()[0] = getString(R.string.lang_automatic) + " ("
					+ getString(R.string.lang_local) + ")";
		}

		lang.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object object) {
				MainActivity.setMapLanguage(object.toString());

				// register as Flurry event
				Map<String, String> parameters = new ArrayMap<String, String>();
				parameters.put("lang", object.toString());
				FlurryAgent.logEvent("CHANGE_LANGUAGE", parameters);

				lang.setTitle(getString(R.string.lang) + " ("
						+ getRealLangName(object.toString()) + ")");
				return true;
			}
		});

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

				((MapApplication) getActivity().getApplication())
						.getGPSTrackingDB().setMeasurementUnit(m);

				return true;
			}
		});

		storage = (ListPreference) findPreference("pref_storage_key");

		// it must be disabled because packages is downloading
		if (PackageDownloadService.isLive) {
			storage.setEnabled(false);
		}

		// disable if external storage isn't available
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			File[] files = getActivity().getExternalFilesDirs(null);

			// check if external is avaiable
			isExternalAvaiable = false;
			for (int i = 0; i < files.length; i++) {
				if (files[i] != null) {
					isExternalAvaiable = true;
					break;
				}
			}

			if (!isExternalAvaiable) {
				storage.setEnabled(false);
				isExternalAvaiable = false;
			}
		} else {
			if (!Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())
					|| ((MapApplication) getActivity().getApplication())
							.getPackageManagerComponent()
							.isSetToDefaultInternal()) {
				storage.setEnabled(false);
				isExternalAvaiable = false;
			}
		}

		setFreeStorageInfo();

		storage.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				setFreeStorageInfo();

				return true;
			}
		});

		storageValue = storage.getValue();

		setStorageTitle(storageValue);

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

				// register setting change with Flurry
				Map<String, String> parameters = new ArrayMap<String, String>();
				parameters.put("storageValue", storageValue);
				parameters.put("lastStorageValue", lastStorageValue);
				FlurryAgent.logEvent("CHANGE_STORAGE", parameters);

				if (storageValue.equals(Const.INTERNAL)) {
					boolean hasEnoughMemory = false;
					long freeInternal = getActivity().getFilesDir()
							.getFreeSpace()
							- Const.INTERNAL_STORAGE_MIN
							* 1048576;
					long l = 0;

					// one external storage
					if (lastStorageValue.equals(Const.EXTERNAL)) {
						File file = getActivity().getExternalFilesDir(null);

						if (file == null) {
							Toast.makeText(getActivity(),
									getString(R.string.moving_files_error),
									Toast.LENGTH_LONG).show();
							storageValue = lastStorageValue;

							return false;
						}

						File[] f = new File(file, "mappackages").listFiles();

						if (f == null) {
							Toast.makeText(getActivity(),
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
					} else {
						File[] files = getActivity().getExternalFilesDirs(null);

						if (files == null) {
							Toast.makeText(getActivity(),
									getString(R.string.moving_files_error),
									Toast.LENGTH_LONG).show();
							storageValue = lastStorageValue;

							return false;
						}

						File[] f = new File(files[Integer
								.parseInt(lastStorageValue
										.substring(lastStorageValue
												.indexOf(" ") + 1)) - 1],
								"mappackages").listFiles();

						if (f == null) {
							Toast.makeText(getActivity(),
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
								Toast.makeText(getActivity(),
										getString(R.string.moving_files_error),
										Toast.LENGTH_LONG).show();

								storage.setValue(lastStorageValue);
								storageValue = lastStorageValue;

								setStorageTitle(storageValue);
							}
						};

						int multiStorage = 1;
						if (lastStorageValue.equals(Const.EXTERNAL)) {
							multiStorage = 1;
						} else {
							multiStorage = Integer.parseInt(lastStorageValue
									.substring(lastStorageValue.indexOf(" ") + 1));
						}

						((MapApplication) getActivity().getApplication())
								.getPackageManagerComponent()
								.movePackageManagerFromTo(
										PackageManagerComponent.EXTERNAL_STORAGE,
										multiStorage,
										PackageManagerComponent.INTERNAL_STORAGE,
										1, r1, r2, getActivity());
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.moving_files_no_space),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}
				} else if (storageValue.equals(Const.EXTERNAL)) {
					boolean hasEnoughMemory = false;
					File file = getActivity().getExternalFilesDir(null);

					if (file == null) {
						Toast.makeText(getActivity(),
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					long freeExternal = file.getFreeSpace()
							- Const.EXTERNAL_STORAGE_MIN * 1048576;
					long l = 0;

					File[] f = new File(getActivity().getFilesDir(),
							"mappackages").listFiles();

					if (f == null) {
						Toast.makeText(getActivity(),
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
								Toast.makeText(getActivity(),
										getString(R.string.moving_files_error),
										Toast.LENGTH_LONG).show();

								storage.setValue(lastStorageValue);
								storageValue = lastStorageValue;

								setStorageTitle(storageValue);
							}
						};

						((MapApplication) getActivity().getApplication())
								.getPackageManagerComponent()
								.movePackageManagerFromTo(
										PackageManagerComponent.INTERNAL_STORAGE,
										1,
										PackageManagerComponent.EXTERNAL_STORAGE,
										1, r1, r2, getActivity());
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.moving_files_no_space),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}
				} else {
					boolean hasEnoughMemory = false;
					long free;

					if (storageValue.equals(Const.INTERNAL)) {
						free = getActivity().getFilesDir().getFreeSpace()
								- Const.INTERNAL_STORAGE_MIN * 1048576;
					} else {
						File[] files = getActivity().getExternalFilesDirs(null);

						if (files == null) {
							Toast.makeText(getActivity(),
									getString(R.string.moving_files_error),
									Toast.LENGTH_LONG).show();
							storageValue = lastStorageValue;

							return false;
						}

						free = files[Integer.parseInt(storageValue
								.substring(storageValue.indexOf(" ") + 1)) - 1]
								.getFreeSpace()
								- Const.EXTERNAL_STORAGE_MIN * 1048576;
					}

					long l = 0;

					File[] f;

					if (lastStorageValue.equals(Const.INTERNAL)) {
						f = new File(getActivity().getFilesDir(), "mappackages")
								.listFiles();
					} else {
						File[] files = getActivity().getExternalFilesDirs(null);

						if (files == null) {
							Toast.makeText(getActivity(),
									getString(R.string.moving_files_error),
									Toast.LENGTH_LONG).show();
							storageValue = lastStorageValue;

							return false;
						}

						f = new File(
								files[Integer.parseInt(lastStorageValue
										.substring(lastStorageValue
												.indexOf(" ") + 1)) - 1],
								"mappackages").listFiles();
					}

					if (f == null) {
						Toast.makeText(getActivity(),
								getString(R.string.moving_files_error),
								Toast.LENGTH_LONG).show();
						storageValue = lastStorageValue;

						return false;
					}

					for (int i = 0; i < f.length; i++) {
						l += f[i].length();
					}

					if (free > l) {
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
								Toast.makeText(getActivity(),
										getString(R.string.moving_files_error),
										Toast.LENGTH_LONG).show();

								storage.setValue(lastStorageValue);
								storageValue = lastStorageValue;

								setStorageTitle(storageValue);
							}
						};

						int from = 1;
						int fromNumber = 1;
						if (lastStorageValue.equals(Const.INTERNAL)) {
							from = PackageManagerComponent.INTERNAL_STORAGE;
							fromNumber = 1;
						} else if (lastStorageValue.equals(Const.EXTERNAL)) {
							from = PackageManagerComponent.EXTERNAL_STORAGE;
							fromNumber = 1;
						} else {
							from = PackageManagerComponent.EXTERNAL_STORAGE;
							fromNumber = Integer.parseInt(lastStorageValue
									.substring(lastStorageValue.indexOf(" ") + 1));
						}

						int toExternalNumber = 1;
						if (storageValue.equals(Const.EXTERNAL)) {
							toExternalNumber = 1;
						} else {
							toExternalNumber = Integer.parseInt(storageValue
									.substring(storageValue.indexOf(" ") + 1));
						}

						((MapApplication) getActivity().getApplication())
								.getPackageManagerComponent()
								.movePackageManagerFromTo(
										from,
										fromNumber,
										PackageManagerComponent.EXTERNAL_STORAGE,
										toExternalNumber, r1, r2, getActivity());
					} else {
						Toast.makeText(getActivity(),
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

		Activity activity = getActivity();

		// I think on some device getActivity() can return null when called from
		// asyn task because first happen fragment life cycle and than activity
		// life cycle, it's not so important to refresh free storage when moving
		// files is finished because it's refreshed and when user click on map
		// storage pref
		if (activity != null) {

			// internal storage folder, maybe it can return null so for sure I
			// check if it's null
			File internalFolder = activity.getFilesDir();

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
							+ String.format(
									Locale.getDefault(),
									"%.2f",
									(freeInternal - Const.INTERNAL_STORAGE_MIN * 1024 * 1024)
											* 1.0f / 1024 / 1024 / 1024)
							+ " GB " + getString(R.string.free) + ")";
				}
			}

			// getExternalFilesDirs works only from API 19
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				String[] entriesValues;
				String[] entriesNames;

				String external = "";

				// if it returns null than that storage is not avaiable
				File[] externalFiles = activity.getExternalFilesDirs(null);
				ArrayList<File> files = new ArrayList<File>();

				for (int i = 0; i < externalFiles.length; i++) {
					if (externalFiles[i] != null) {
						files.add(externalFiles[i]);
					}
				}

				entriesValues = new String[1 + files.size()];
				entriesNames = new String[1 + files.size()];

				entriesValues[0] = Const.INTERNAL;
				entriesNames[0] = getString(R.string.internal) + internal;

				for (int i = 0; i < files.size(); i++) {
					entriesValues[i + 1] = Const.EXTERNAL;
					entriesNames[i + 1] = getString(R.string.external);

					if (files.get(i).getFreeSpace()
							- Const.EXTERNAL_STORAGE_MIN * 1024 * 1024 < 1024 * 1024) {
						external = " ("
								+ ((files.get(i).getFreeSpace() - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024)
								+ " KB " + getString(R.string.free) + ")";
					} else if ((files.get(i).getFreeSpace() - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024 < 1024) {
						external = " ("
								+ ((files.get(i).getFreeSpace() - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024)
								+ " MB " + getString(R.string.free) + ")";
					} else {
						external = " ("
								+ String.format(
										Locale.getDefault(),
										"%.2f",
										(files.get(i).getFreeSpace() - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024)
												* 1.0f / 1024 / 1024 / 1024)
								+ " GB " + getString(R.string.free) + ")";
					}

					if (files.size() > 1) {
						entriesValues[i + 1] += " " + (i + 1);
						entriesNames[i + 1] += " " + (i + 1) + external;
					} else {
						entriesValues[i + 1] += " " + (i + 1);
						entriesNames[i + 1] += external;
					}
				}

				storage.setEntries(entriesNames);
				storage.setEntryValues(entriesValues);
			} else {
				String external = "";

				if (isExternalAvaiable) {
					File externalFolder = activity.getExternalFilesDir(null);

					if (externalFolder != null) {
						long freeExternal = externalFolder.getFreeSpace();

						if (freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024
								* 1024 < 1024 * 1024) {
							external = " ("
									+ ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024)
									+ " KB " + getString(R.string.free) + ")";
						} else if ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024 < 1024) {
							external = " ("
									+ ((freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024) / 1024 / 1024)
									+ " MB " + getString(R.string.free) + ")";
						} else {
							external = " ("
									+ String.format(
											Locale.getDefault(),
											"%.2f",
											(freeExternal - Const.EXTERNAL_STORAGE_MIN * 1024 * 1024)
													* 1.0f / 1024 / 1024 / 1024)
									+ " GB " + getString(R.string.free) + ")";
						}
					}
				}

				String[] entries2 = new String[2];
				entries2[0] = getString(R.string.internal) + internal;
				entries2[1] = getString(R.string.external) + external;

				storage.setEntries(entries2);
			}
		}
	}

	private void setStorageTitle(String s) {
		if (s.toString().contains(Const.EXTERNAL)) {
			if (storage.getEntries().length > 2) {
				storage.setTitle(getString(R.string.storage) + " ("
						+ getString(R.string.external) + " "
						+ (Integer.parseInt(s.substring(s.indexOf(" ") + 1)))
						+ ")");
			} else {
				storage.setTitle(getString(R.string.storage) + " ("
						+ getString(R.string.external) + ")");
			}
		} else {
			storage.setTitle(getString(R.string.storage) + " ("
					+ getString(R.string.internal) + ")");
		}
	}
}
