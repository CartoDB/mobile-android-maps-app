package com.nutiteq.nuticomponents.customviews.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.nutiteq.nuticomponents.R;

/**
 * 
 * @author Milan Ivankovic, Nole
 * 
 *         SearchView is part of search bar which is positioned in middle, to
 *         right of HamburgerView. There is no sense to use it without
 *         HamburgerView and VoiceView. You must set main activity on which this
 *         component is attached and also .class for your searchable activity
 *         which you use for searching map with method setObjects
 * 
 */
public class SearchView extends AutoCompleteTextView {

	private Activity mainActivity;
	private Class<?> searchableClass;

	private SearchHistory searchDB;

	private int width;
	private int height;

	private Paint paint = new Paint();
	private Paint paint2 = new Paint();

	private Handler handler;

	public SearchView(Context context, AttributeSet attrs) {
		super(context, attrs);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		if (metrics.heightPixels > metrics.widthPixels) {
			width = metrics.widthPixels;
		} else {
			width = metrics.heightPixels;
		}

		String s = attrs.getAttributeValue(
				"http://schemas.android.com/apk/res/android", "layout_height");
		int i = s.indexOf(".");
		if (i == -1) {
			i = s.indexOf(",");
		}
		if (i == -1) {
			height = (int) (50 * metrics.density);
		} else {
			height = (int) (Integer.parseInt(s.subSequence(0, i).toString()) * metrics.density);
		}

		width = (int) ((width - 2.0f * 10.0f * metrics.density) * 0.64f);

		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth((int) (metrics.density * 1.0f));
		paint.setAntiAlias(true);

		paint2.setColor(Color.rgb(232, 232, 232));
		paint2.setStyle(Paint.Style.STROKE);
		paint2.setStrokeWidth((int) (metrics.density * 1.0f));
		paint2.setAntiAlias(true);

		searchDB = new SearchHistory(getContext());
		searchDB.open();

		setSingleLine(true);
		setHint(getResources().getString(R.string.search_view_text));
		setTextSize(16);
		setThreshold(1);
		setDropDownBackgroundDrawable(new ColorDrawable(
				Color.parseColor("#FFFFFF")));

		setCursorVisible(false);

		setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {

				if ((event.getAction() == KeyEvent.ACTION_UP)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {

					hideKeyboard();

					String result = SearchView.this.getText().toString();
					SearchView.this.setText("");

					Intent intent = new Intent(Intent.ACTION_SEARCH);
					intent.setClass(mainActivity, searchableClass);
					intent.putExtra(SearchManager.QUERY, result);
					mainActivity.startActivityForResult(intent, 2);

					searchDB.insertSearch(result);

					return true;
				}

				return false;
			}
		});

		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int pos,
					long id) {
				hideKeyboard();

				String result = parent.getItemAtPosition(pos).toString();
				SearchView.this.setText("");

				Intent intent = new Intent(Intent.ACTION_SEARCH);
				intent.setClass(mainActivity, searchableClass);
				intent.putExtra(SearchManager.QUERY, result);
				mainActivity.startActivityForResult(intent, 2);
			}
		});

		addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				setSearchHistory();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
	}

	private void setSearchHistory() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						mainActivity,
						R.layout.search_history,
						R.id.search,
						searchDB.getSearch(SearchView.this.getText().toString()));
				setAdapter(adapter);
			}
		});
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_UP) {
			hideKeyboard();
		}

		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onMeasure(int w, int h) {
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas c) {
		c.drawColor(Color.WHITE);

		c.drawLine(0, paint2.getStrokeWidth() / 2, width * 4,
				paint2.getStrokeWidth() / 2, paint2);
		c.drawLine(0, height - paint2.getStrokeWidth() / 2, width * 4, height
				- paint2.getStrokeWidth() / 2, paint2);

		super.onDraw(c);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			setCursorVisible(true);
		}
		return super.onTouchEvent(event);
	}

	/**
	 * Set main activity on which this component is attached and your .class for
	 * searchable activity
	 */
	public void setObjects(Activity mainActivity, Class<?> searchableClass) {
		this.mainActivity = mainActivity;
		this.searchableClass = searchableClass;

		handler = new Handler(mainActivity.getMainLooper());
	}

	public void hideKeyboard() {
		try {
			InputMethodManager imm = (InputMethodManager) mainActivity
					.getSystemService(Activity.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mainActivity.getCurrentFocus()
					.getWindowToken(), 0);
		} catch (Exception e) {

		}
		setCursorVisible(false);
	}
}
