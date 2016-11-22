package com.nutiteq.nuticomponents.customviews.search;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class SearchHistory {

	private SearchHistoryDBHelper dbHelper;
	private SQLiteDatabase database;

	private boolean isOpen = false;

	public SearchHistory(Context context) {
		dbHelper = new SearchHistoryDBHelper(context);
	}

	public void open() {
		try {
			database = dbHelper.getWritableDatabase();
			isOpen = true;
		} catch (SQLException e) {
			isOpen = false;
		}
	}

	public void close() {
		dbHelper.close();
		isOpen = false;
	}

	public void insertSearch(String s) {
		if (isOpen) {
			ContentValues values = new ContentValues();
			values.put(SearchHistoryDBHelper.COLUMN_NAME_SEARCH, s);

			if (getSearch2(s).length == 0) {
				database.insert(SearchHistoryDBHelper.TABLE_NAME, null, values);
			}
		}
	}

	public String[] getSearch(String s) {
		if (isOpen) {
			s = s.replaceAll("'", "''");

			ArrayList<String> result = new ArrayList<String>();

			String[] projection = { SearchHistoryDBHelper.COLUMN_NAME_SEARCH };

			String sortOrder = SearchHistoryDBHelper.COLUMN_NAME_SEARCH
					+ " DESC";

			String selection = SearchHistoryDBHelper.COLUMN_NAME_SEARCH
					+ " like '" + s + "%'";

			Cursor c = database.query(SearchHistoryDBHelper.TABLE_NAME,
					projection, selection, null, null, null, sortOrder);

			c.moveToFirst();
			while (!c.isAfterLast()) {
				result.add(c.getString(0));
				c.moveToNext();
			}
			c.close();

			String[] a = new String[result.size()];
			for (int i = 0; i < result.size(); i++) {
				a[i] = result.get(i);
			}

			return a;
		} else {
			return new String[0];
		}
	}

	public String[] getSearch2(String s) {
		if (isOpen) {
			s = s.replaceAll("'", "''");

			ArrayList<String> result = new ArrayList<String>();

			String[] projection = { SearchHistoryDBHelper.COLUMN_NAME_SEARCH };

			String sortOrder = SearchHistoryDBHelper.COLUMN_NAME_SEARCH
					+ " DESC";

			String selection = SearchHistoryDBHelper.COLUMN_NAME_SEARCH
					+ " = '" + s + "'";

			Cursor c = database.query(SearchHistoryDBHelper.TABLE_NAME,
					projection, selection, null, null, null, sortOrder);

			c.moveToFirst();
			while (!c.isAfterLast()) {
				result.add(c.getString(0));
				c.moveToNext();
			}
			c.close();

			String[] a = new String[result.size()];
			for (int i = 0; i < result.size(); i++) {
				a[i] = result.get(i);
			}

			return a;
		} else {
			return new String[0];
		}
	}

	public class SearchHistoryDBHelper extends SQLiteOpenHelper {

		public static final int DATABASE_VERSION = 1;
		public static final String DATABASE_NAME = "SearchHistory.db";

		private static final String TABLE_NAME = "search_history";

		private static final String COLUMN_NAME_ID = "_ID";
		private static final String COLUMN_NAME_SEARCH = "search";

		private static final String TEXT_TYPE = " TEXT";
		private static final String COMMA_SEP = ",";
		private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
				+ TABLE_NAME + " (" + COLUMN_NAME_ID + " INTEGER PRIMARY KEY"
				+ COMMA_SEP + COLUMN_NAME_SEARCH + TEXT_TYPE + " )";

		private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
				+ TABLE_NAME;

		public SearchHistoryDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_ENTRIES);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(SQL_DELETE_ENTRIES);
			onCreate(db);
		}

		public void onDowngrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			onUpgrade(db, oldVersion, newVersion);
		}
	}
}