package com.nutiteq.nuticomponents.customviews.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.nutiteq.nuticomponents.customviews.AnimatedExpandableListView;

/**
 * 
 * @author Milan Ivankovic, Nole
 * 
 *         Hamburger view is part of search bar which is positioned on left and
 *         has action button which opens slide menu. There is no sense to use it
 *         without SearchView and VoiceView.
 * 
 */
public class HamburgerView extends View implements OnTouchListener {

	private SearchView searchView;
	private DrawerLayout hamburgerMenuLayout = null;
	private AnimatedExpandableListView hamburgerList;

	private int width;
	private int height;

	private Paint paint = new Paint();
	private Paint paint2 = new Paint();
	private Paint paint3 = new Paint();

	private int hamburger1;
	private int hamburger2;

	private int h;

	@SuppressLint("ClickableViewAccessibility")
	public HamburgerView(Context context, AttributeSet attrs) {
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

		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth((int) (metrics.density * 1.0f));
		paint.setAntiAlias(true);

		paint2.setColor(Color.rgb(232, 232, 232));
		paint2.setStyle(Paint.Style.STROKE);
		paint2.setStrokeWidth((int) (metrics.density * 1.0f));
		paint2.setAntiAlias(true);

		paint3.setColor(Color.rgb(0, 180, 131));
		paint3.setStrokeWidth((int) (metrics.density * 2));
		paint3.setAntiAlias(true);

		h = (int) (metrics.density * 5);

		width = (int) ((width - 2.0f * 10.0f * metrics.density) * 0.18f);

		hamburger1 = (int) (width * 0.36f);
		hamburger2 = (int) (width * 0.67f);

		setOnTouchListener(this);
	}

	@Override
	protected void onMeasure(int w, int h) {
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas c) {
		super.onDraw(c);

		c.drawColor(Color.WHITE);

		c.drawLine(0, paint2.getStrokeWidth() / 2, width,
				paint2.getStrokeWidth() / 2, paint2);
		c.drawLine(paint2.getStrokeWidth() / 2, 0, paint2.getStrokeWidth() / 2,
				height, paint2);
		c.drawLine(0, height - paint2.getStrokeWidth() / 2, width, height
				- paint2.getStrokeWidth() / 2, paint2);

		c.drawLine(hamburger1, height / 2 - h, hamburger2, height / 2 - h,
				paint3);
		c.drawLine(hamburger1, height / 2, hamburger2, height / 2, paint3);
		c.drawLine(hamburger1, height / 2 + h, hamburger2, height / 2 + h,
				paint3);
	}

	private boolean isHamburgerDown = false;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (event.getX() >= 0 && event.getX() <= width) {
				isHamburgerDown = true;
			}

			break;
		case MotionEvent.ACTION_MOVE:
			if (!(event.getX() >= 0 && event.getX() <= width)) {
				isHamburgerDown = false;
			}

			break;
		case MotionEvent.ACTION_UP:
			if (isHamburgerDown) {
				playSoundEffect(android.view.SoundEffectConstants.CLICK);

				isHamburgerDown = false;
				searchView.hideKeyboard();
				hamburgerMenuLayout.openDrawer(hamburgerList);
			}

			break;
		}

		return true;
	}

	public void setHamburgerMenu(DrawerLayout hamburgerMenuLayout,
			AnimatedExpandableListView hamburgerList) {
		this.hamburgerMenuLayout = hamburgerMenuLayout;
		this.hamburgerList = hamburgerList;
	}

	public void setSearchView(SearchView sw) {
		this.searchView = sw;
	}
}
