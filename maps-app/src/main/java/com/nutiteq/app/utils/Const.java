package com.nutiteq.app.utils;

import com.nutiteq.app.nutimap3d.dev.BuildConfig;

public class Const {

	public static final String SEARCH_X = "search_x";
	public static final String SEARCH_Y = "search_y";

	public static final String SEARCH_TITLE = "search_t";
	public static final String SEARCH_DESC = "search_d";

	public static final int MAP_ZOOM_MIN = 0;
	public static final int MAP_ZOOM_MAX = 22;

	public static final String SHARED_PREFS = "com.nutiteq.offmaps";

	public static final String ROTATION = "rotation";
	public static final String ZOOM = "zoom";
	public static final String TILT = "tilt";

	public static final String FOCUS_X = "focus_x";
	public static final String FOCUS_Y = "focus_y";

	public static final String IS_HAMBURGER = "hamburger";
	public static final String SEARCH_FOCUS = "search";

	public static final String LOCATION_X = "location_x";
	public static final String LOCATION_Y = "location_y";
	public static final String LOCATIONS_SIZE = "location_s";
	public static final String LOCATION_X2 = "location_x2";
	public static final String LOCATION_Y2 = "location_y2";
	public static final String LOCATIONS_SIZE2 = "location_s2";

	public static final String DROPPED_VIEW_VISIBLE = "dropped_view_visible";
	public static final String DROPPED_VIEW_TITLE = "dropped_view_title";
	public static final String DROPPED_VIEW_DESCRIPTION = "dropped_view_description";
	public static final String DROPPED_VIEW_X = "dropped_view_x";
	public static final String DROPPED_VIEW_Y = "dropped_view_y";
	public static final String DROPPED_VIEW_FAVORITE = "dropped_view_favorite";

	public static final String LONG_PIN_VISIBLE = "long_pin_visible";
	public static final String LONG_PIN_X = "long_pin_x";
	public static final String LONG_PIN_Y = "long_pin_y";
	public static final String FAVORITE_ID = "favorite_id";

	public static final String ROUTE_VIEW_VISIBLE = "route_visible";
	public static final String ROUTE_VIEW_FULL_DISTANCE = "route_full_distance";
	public static final String ROUTE_VIEW_TIME = "route_time";
	public static final String ROUTE_VIEW_INSTRUCTION_NUMBER = "route_number";
	public static final String ROUTE_VIEW_DESCRIPTION = "route_description";
	public static final String ROUTE_VIEW_DISTANCE = "route_distance";
	public static final String ROUTE_VIEW_BITMAP_TYPE = "route_bitmap_type";
	public static final String ROUTE_VIEW_AZIMUTH = "route_azimuth";
	public static final String ROUTE_VIEW_LOCATION_X = "route_location_x";
	public static final String ROUTE_VIEW_LOCATION_Y = "route_location_y";
	public static final String ROUTE_VIEW_LINE_POINTS_NUMBER = "route_line_points_number";
	public static final String ROUTE_VIEW_LINE_POINT_X = "route_line_point_x";
	public static final String ROUTE_VIEW_LINE_POINT_Y = "route_line_point_y";
	public static final String ROUTE_VIEW_IS_UP = "route_view_is_up";
	public static final String ROUTE_VIEW_STOP_PIN_X = "route_view_stop_pin_x";
	public static final String ROUTE_VIEW_STOP_PIN_Y = "route_view_stop_pin_y";

	public static final String LOG_TAG = "OfflineMaps3D";

	// offline vector map data source from assets
	public static final String BASE_PACKAGE_ASSET_NAME = "world_0_5.mbtiles";
	public static final String BASE_PACKAGE_ID = "basepkg";

	// Tile cache size. Larger number will give better performance but higher
	// memory usage
	public static final int TILE_CACHE_SIZE = 32 * 1024 * 1024;

	// names of map styles
	public static final String MAP_STYLE_BRIGHT = "style";
	public static final String MAP_STYLE_GREY = "nutiteq_dark";
	public static final String MAP_STYLE_FILE = "nutibright-v3.zip";

	public static final String FIREBASE_URL = "https://vivid-heat-3178.firebaseio.com/";

	// names of map language
	public static final String MAP_LANGUAGE_AUTOMATIC = "automatic";
	public static final String MAP_LANGUAGE_ENGLISH = "en";
	public static final String MAP_LANGUAGE_GERMAN = "de";
	public static final String MAP_LANGUAGE_FRENCH = "fr";
	public static final String MAP_LANGUAGE_RUSSIAN = "ru";
	public static final String MAP_LANGUAGE_CHINESE = "zh";
	public static final String MAP_LANGUAGE_SPANISH = "es";
	public static final String MAP_LANGUAGE_ITALIAN = "it";
	public static final String MAP_LANGUAGE_ESTONIAN = "et";
	public static final String MAP_LANGUAGE_LOCAL = "";

	// ID for HOCKEY SDK
	public static final String HOCKEYAPP_ID = BuildConfig.DEBUG ? "2a90c57f65f6f8821e857247416f5475"
			: "94d69b5ee0f22c85af784ba041109921";

	// value in MB which indicate unreachable storage, for example if 1030 MB is
	// free only 1000 is reachable, I use this because on my device when there
	// is below 30MB free external storage, package manager crash if it's
	// downloading package, I think 30 is good value for all devices
	// http://developer.android.com/training/basics/data-storage/files.html#GetFreeSpace
	public static final int EXTERNAL_STORAGE_MIN = 30;
	public static final int INTERNAL_STORAGE_MIN = 30;

	public static final String LICENSE_KEY = "XTUMwQ0ZFQmU4M0RFTFRZb01xcVFFMEdhUmRmdHlIbjNBaFVBd05ldHBKa1pLYnJaOVBia0F6Z2REWnQzVWV3PQoKYXBwVG9rZW49ZmFmMTFhNjUtNGQ4OS00YWY1LThmMWEtZjc2N2IzNTFkYjkzCnBhY2thZ2VOYW1lPWNvbS5jYXJ0by5tYXBzLmFwcC5kcm9pZApvbmxpbmVMaWNlbnNlPTEKcHJvZHVjdHM9c2RrLWFuZHJvaWQtNC4qCndhdGVybWFyaz1jdXN0b20K";

	// from colors.xml it doesn't works for action bar background
	public static final String NUTITEQ_GREEN = "#00b483";

	// values for preferences, please don't change it values
	public static final String INTERNAL = "Internal";
	public static final String EXTERNAL = "External";
	public static final String METRIC = "Metric";
	public static final String IMPERIAL = "Imperial";
	public static final String LANG_AUTOMATIC = "Automatic";
	public static final String LANG_LOCAL = "Local";
	public static final String LANG_ENGLISH = "English";
	public static final String LANG_GERMAN = "German";
	public static final String LANG_FRENCH = "French";
	public static final String LANG_RUSSIAN = "Russian";
	public static final String LANG_ITALIAN = "Italian";
	public static final String LANG_SPANISH = "Spanish";
	public static final String LANG_CHINESE = "Chinese";
	public static final String LANG_ESTONIAN = "Estonian";

	public static final String PREF_UNIT_KEY = "pref_unit_key";
	public static final String PREF_STORAGE_KEY = "pref_storage_key";
	public static final String PREF_LANG_KEY = "pref_lang_key";
	public static final String PREF_BUILDINGS_3D_KEY = "pref_buildings_3d";
	public static final String PREF_BRIGHT_STYLE = "pref_bright_style";


	public static final String[] newPackageMap = {
			"AR", "AR", "AT", "AT", "AT", "AT", "AT",
			"AT", "AT", "AU", "AU", "AU", "BD", "BD", "BE", "BE", "BE",
			"BE", "BE", "BE", "BE", "BE", "BE", "BE", "BE", "BY", "BY",
			"BA", "BA", "BR", "BR", "BR", "BG", "BG", "-", "CA-QC",
			"CA-ON", "CL", "CL", "CN", "CN", "CN", "CN", "CO", "CO", "CD",
			"HR", "HR", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ", "CZ",
			"CZ", "CZ", "CZ", "DK", "DK", "DK", "DK", "DK", "DK", "DK",
			"DK", "DK", "DK", "EG", "EG", "ES-IB", "ES", "ES-CN", "ES-CN",
			"ES", "ES", "ES", "ES", "ES", "ES", "ES-CT", "ES", "ES-CT",
			"ES", "EE", "EE", "EE", "FI", "FI", "FR-B", "FR-U", "FR",
			"FR-Q", "FR-O", "FR-V", "FR-U", "FR-K", "FR-R", "FR-U", "FR",
			"FR-G", "FR-E", "FR-A", "FR-N", "DE-BB", "DE-NW", "DE-HB",
			"DE-HE", "-", "DE-HH", "DE-NI", "DE-BW", "DE-NW", "DE-SN",
			"DE-BY", "DE-BY", "DE-BW", "GR", "GR", "GR", "GR", "GR", "GR",
			"GR", "GR", "GR", "GB-ENG", "HT", "HT", "HU", "HU", "IL", "IL",
			"IL", "IL", "IN", "IN", "IN", "IN", "IN", "IN", "IN", "IN",
			"IQ", "IQ", "IR", "IR", "IE", "IE", "IT", "IT", "IT", "IT",
			"JP", "JP", "KE", "KE", "CD", "KR", "KR", "SA", "SA", "LV",
			"LV", "GB-ENG", "LT", "LT", "LT", "LU", "MC", "MX", "MX", "MM",
			"MM", "NG", "NG", "KP", "KP", "NL", "NL", "NO", "NO", "NO",
			"NO", "NO", "NO", "NO", "NO", "NO", "NO", "NO", "PE", "PE",
			"PH", "PH", "PK", "PK", "PK", "PL", "PL", "PL", "PL", "PL",
			"PL", "PL", "PL", "PL", "PL", "PL", "PL", "PT", "PT", "RO",
			"RO", "RO", "ZA", "ZA", "ZA", "ZA", "RU-CFD", "RU-CFD",
			"RU-NWFD", "CH", "CH", "CH", "SG", "SK", "SK", "SK", "SI",
			"SI", "RS", "RS", "SE", "SE", "SE", "SE", "SE", "SE", "SE",
			"SE", "SE", "SE", "SE", "SE", "SE", "SE", "TH", "TH", "TR",
			"TR", "TR", "TR", "TW", "TW", "UA", "UA", "GB", "GB", "GB",
			"GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB",
			"GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "GB", "UA",
			"UA", "US-AK", "US-AZ", "US-CA", "US-CA", "US-CA", "US-CA",
			"US-DC", "US-FL", "US-FL", "US-FL", "US-GA", "-", "US-HI",
			"US-IL", "US-IL", "US-NV", "US-MA", "US-MI", "US-NY", "US-NY",
			"US-OH", "US-OH", "US-PA", "US-PA", "US-TX", "US-TX", "US-TX",
			"US-TX", "US-WA", "US-WA", "-"
	};

	public static final String[] oldPackageMap = {
			"ar_buenosaires_0_15", "ar_general_0_10",
			"at_general_0_12", "at_vienna_0_17", "au_graz_0_16",
			"au_innsbruck_0_16", "au_klagenfurt_0_17", "au_linz_0_16",
			"au_salzburg_0_16", "aus_general_0_9", "aus_melbourne_0_15",
			"aus_sydney_0_16", "ban_dhaka_0_17", "ban_general_0_12",
			"be_aalst_0_17", "be_antwerp_0_16", "be_brugge_0_17",
			"be_brussels_0_17", "be_chaleroi_0_17", "be_general_0_13",
			"be_gent_0_17", "be_leuven_0_17", "be_liege_0_17",
			"be_mons_0_17", "be_namur_0_17", "bel_general_0_11",
			"bl_minsk_0_16", "bos_general_0_12", "bos_sarejevo_0_17",
			"br_general_0_9", "br_rio_0_17", "br_saopaulo_0_16",
			"bul_general_0_12", "bul_sofia_0_17", "can_general_0_9",
			"can_montreal_0_16", "can_toronto_0_16", "chi_general_0_11",
			"chi_santiago_0_16", "chn_general_0_10", "chn_hk_0_15",
			"chn_peking_0_16", "chn_shanghai_0_15", "col_bogota_0_17",
			"col_general_0_11", "con_general_0_10", "cro_general_0_12",
			"cro_zagreb_0_17", "cz_brno_0_17", "cz_ceskebudejovice_0_17",
			"cz_general_0_12", "cz_hk_0_17", "cz_karlovyvary_0_17",
			"cz_olomouc_0_17", "cz_ostrava_0_17", "cz_pardubice_0_17",
			"cz_plzen_0_17", "cz_prague_0_17", "cz_ustinadlabem_0_17",
			"dk_Aalborg_0_17", "dk_Esbjerg_0_17", "dk_Horsens_0_17",
			"dk_Kolding_0_17", "dk_Odense_0_17", "dk_Roskilde_0_17",
			"dk_Vejle_0_17", "dk_arhus_0_17", "dk_copenhagen_0_17",
			"dk_general_0_12", "eg_cairo_0_16", "eg_general_0_11",
			"es_balearic_0_14", "es_bilbao_0_16", "es_canary_0_14",
			"es_laspalmasgrancanaria_0_17", "es_malaga_0_16",
			"es_murcia_0_16", "es_palma_0_16", "es_sevilla_0_16",
			"es_valencia_0_16", "es_zaragoza_0_16", "esp_barcelona_0_17",
			"esp_general_0_11", "esp_girona_0_17", "esp_madrid_0_17",
			"est_general", "est_tallinn", "est_tartu", "fi_general_0_10",
			"fi_helsinki_0_16", "fr_bordeaux_0_17", "fr_cannes_0_17",
			"fr_general_0_11", "fr_havre_0_17", "fr_lille_0_17",
			"fr_lyon_0_17", "fr_marseille_0_17", "fr_montpellier_0_17",
			"fr_nantes_0_17", "fr_nice_0_17", "fr_paris_0_16",
			"fr_reims_0_17", "fr_rennes_0_17", "fr_strasbourg_0_17",
			"fr_toulouse_0_17", "ger_berlin_0_16", "ger_bonn_0_17",
			"ger_bremen_0_17", "ger_frankfurtmain_0_17",
			"ger_general_0_11", "ger_hamburg_0_16", "ger_hannover_0_17",
			"ger_karlsruhe_0_17", "ger_koeln_0_17", "ger_leipzig_0_17",
			"ger_muenchen_0_17", "ger_nuernberg_0_17",
			"ger_stuttgart_0_17", "gr_athena_0_16", "gr_corfu_0_16",
			"gr_crete_0_14", "gr_general_0_12", "gr_iraklio_0_17",
			"gr_kos_0_16", "gr_larissa_0_17", "gr_patra_0_17",
			"gr_thessaloniki_0_16", "greater_london_0_16",
			"haiti_general_0_13", "haiti_portauprince_0_17",
			"hu_budapest_0_16", "hu_general_0_12", "il_general_0_13",
			"il_haifa_0_17", "il_jerusalem_0_16", "il_telaviv_0_16",
			"in_bangalore_0_16", "in_chennai_0_17", "in_delhi_0_16",
			"in_general_0_10", "in_kolkata_0_16", "in_mumbai_0_16",
			"ind_general_0_10", "ind_jakarta_0_16", "iq_baghdad_0_16",
			"iq_general_0_11", "ir_general_0_10", "ir_tehran_0_16",
			"ire_dublin_0_16", "ire_general_0_12", "it_general_0_11",
			"it_milano_0_17", "it_rome_0_17", "it_venezia_0_18",
			"jp_general_0_11", "jp_tokyo_0_15", "kenya_general_0_12",
			"kenya_nairobi_0_16", "kon_kinshasa_0_17", "kor_general_0_12",
			"kor_seoul_0_16", "ksa_general_0_11", "ksa_riyadh_0_16",
			"lat_general_0_13", "lat_riga_0_16", "london_central_0_17",
			"lt_general_0_12", "lt_kaunas_0_17", "lt_vilnius_0_17",
			"lux_general_0_17", "monaco_0_18", "mx_general_0_10",
			"mx_mexicocity_0_16", "my_general_0_11", "my_yangon_0_17",
			"ng_general_0_11", "ng_lagos_0_16", "nkor_general_0_12",
			"nkor_phoenyan_0_17", "nl_amsterdam_0_17", "nl_general_0_13",
			"no_Drammen_0_17", "no_Fredrikstad_0_17",
			"no_Kristiansand_0_17", "no_Stavanger_0_17", "no_Tromso_0_17",
			"no_bergen_0_17", "no_general_0_10", "no_greater_Oslo_0_15",
			"no_lillehammer_0_17", "no_oslo_0_17", "no_trondheim_0_17",
			"peru_general_0_11", "peru_lima_0_16", "ph_general_0_11",
			"ph_manila_0_17", "pk_general_0_11", "pk_karachi_0_16",
			"pk_lahore_0_16", "pl_Bydgoszcz_0_17", "pl_Katowice_0_17",
			"pl_Lublin_0_17", "pl_bialystok_0_17", "pl_gdansk_0_17",
			"pl_general_0_11", "pl_krakow_0_17", "pl_lodz_0_17",
			"pl_poznan_0_17", "pl_szczecin_0_17", "pl_warszawa_0_17",
			"pl_wroclaw_0_17", "pt_general_0_12", "pt_lisboa_0_17",
			"rom_bucharest_0_17", "rom_cluj_0_17", "rom_general_0_11",
			"rsa_capetown_0_16", "rsa_general_0_11",
			"rsa_johannesburg_0_16", "rsa_pretoria_0_16", "ru_europe_0_10",
			"ru_moskva_0_17", "ru_stpeterburg_0_16", "sch_general_0_13",
			"sch_geneve_0_17", "sch_zuerich_0_17", "sin_general_0_16",
			"sk_bratislava_0_17", "sk_general_0_12", "sk_kosishe_0_17",
			"slo_general_0_13", "slo_ljubliana_0_17", "sr_belgrad_0_17",
			"sr_general_0_12", "swe_Gothenburg_0_17", "swe_Gotland_0_17",
			"swe_general_0_10", "swe_greater_stockholm_0_16",
			"swe_helsingborg_0_17", "swe_kalmar_0_17",
			"swe_karlskrona_0_17", "swe_linkoping_0_17", "swe_lund_0_17",
			"swe_malmo_0_17", "swe_orebro_0_17", "swe_stockholm_0_17",
			"swe_uppsala_0_17", "swe_vasteras_0_17", "th_bangkok_0_17",
			"th_general_0_11", "tr_ankara_0_16", "tr_izmir_0_16",
			"tur_general_0_11", "tur_istanbul_0_17", "tw_general_0_13",
			"tw_taipei_0_16", "ua_kharkiv_0_17", "ua_odessa_0_17",
			"uk_birmingham_0_17", "uk_bradford_0_17", "uk_brighton_0_17",
			"uk_bristol_0_17", "uk_cambridge_0_17", "uk_cardiff_0_17",
			"uk_edinburgh_0_17", "uk_general_0_11", "uk_glasgow_0_17",
			"uk_isleofman_0_16", "uk_leeds_0_17", "uk_liverpool_0_17",
			"uk_manchester_0_17", "uk_newcastleupontyne_0_17",
			"uk_oxford_0_17", "uk_plymouth_0_17", "uk_portsmouth_0_17",
			"uk_preston_0_17", "uk_reading_0_17", "uk_southampton_0_17",
			"uk_strokeontent_0_17", "uk_sunderland_0_17",
			"ukr_general_0_11", "ukr_kiev_0_16", "us_ak_Anchorage_0_16",
			"us_az_phoenix_0_16", "us_ca_0_12", "us_ca_bayarea_0_15",
			"us_ca_losangeles_0_15", "us_ca_sandiego_0_16", "us_dc_0_16",
			"us_fl_0_11", "us_fl_Jacksonville_0_16", "us_fl_miami_0_16",
			"us_ga_atlanta_0_16", "us_general_0_9", "us_hi_0_14",
			"us_il_0_13", "us_il_chicago_0_16", "us_lasvegas_0_16",
			"us_ma_boston_0_16", "us_mi_detroit_0_16", "us_ny_0_11",
			"us_nyc_0_16", "us_oh_0_13", "us_oh_columbus_0_16",
			"us_pa_0_13", "us_pa_Philadelphia_0_16", "us_tx_0_11",
			"us_tx_dallas_0_16", "us_tx_houston_0_16",
			"us_tx_sanantonio_0_16", "us_wa_0_12", "us_wa_seatlle_0_16",
			"world"
	};
}
