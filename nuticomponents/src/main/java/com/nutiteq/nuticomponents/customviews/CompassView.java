package com.nutiteq.nuticomponents.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.nutiteq.nuticomponents.R;
import com.carto.ui.MapView;

/**
 * 
 * @author Milan Ivankovic, Nole
 * 
 *         Compass is custom view for showing world direction. It use same
 *         bitmap for normal and click state but it can be adjust very easy to
 *         has different bitmap for click state. Click on this view rotate
 *         bitmap to 0 degrees. You must set MapView and GPSView with methods
 *         setObjects otherwise it will crash. Also in MapEventListener from
 *         method onMapMoved you must call method from this class
 *         notifyCompassView, otherwise CompassView will not be drawn correctly
 *         because it needs refreshments for every map rotation.
 * 
 */

public class CompassView extends View implements OnTouchListener {

	private MapView mapView;
	private LocationView locationButton;

	private Bitmap bitmapNormal;
	private Bitmap bitmapHover;

	private int width;
	private int height;
	private int centerX;
	private int centerY;

	private Matrix matrix = new Matrix();
	private Paint paint = new Paint();

	private boolean isHover = false;

	private Handler handler = new Handler();

	// for animation when compass is 0
	private boolean isCompassNorthVisible = false;
	private long timeAnimStart;
	private float percenetAnimElapsedReverse;

	private int radius;
	private Paint paintCircleBorder = new Paint();
	private Paint paintCircle = new Paint();

	private static final int ALFA_FOR_CIRCLE = 164;

	private int strokeWidth;

	private Runnable r;

	@SuppressLint("ClickableViewAccessibility")
	public CompassView(Context context, AttributeSet attrs) {
		super(context, attrs);

		bitmapNormal = BitmapFactory.decodeResource(getResources(),
				R.drawable.compass);
		bitmapHover = BitmapFactory.decodeResource(getResources(),
				R.drawable.compass);

		DisplayMetrics metrics = getResources().getDisplayMetrics();

		strokeWidth = (int) (metrics.density * 1.0f);

		width = bitmapNormal.getWidth() + getPaddingLeft() + getPaddingRight()
				+ strokeWidth;
		height = bitmapNormal.getHeight() + getPaddingBottom()
				+ getPaddingTop() + strokeWidth;

		centerX = width / 2;
		centerY = height / 2;

		radius = bitmapNormal.getWidth() / 2 + strokeWidth;

		paintCircleBorder.setColor(Color.rgb(220, 220, 220));
		paintCircleBorder.setStyle(Paint.Style.STROKE);
		paintCircleBorder.setStrokeWidth(strokeWidth);
		paintCircleBorder.setAntiAlias(true);

		paintCircle.setColor(Color.WHITE);
		paintCircle.setAlpha(ALFA_FOR_CIRCLE);
		paintCircle.setAntiAlias(true);

		paint.setAntiAlias(true);

		matrix.reset();

		r = new Runnable() {

			@Override
			public void run() {
				isCompassNorthVisible = false;
				timeAnimStart = System.currentTimeMillis();
				invalidate();
			}
		};

		setOnTouchListener(this);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas c) {
		super.onDraw(c);

		if (mapView.getMapRotation() != 0) {
			isCompassNorthVisible = true;

			paint.setAlpha(255);
			paintCircle.setAlpha(ALFA_FOR_CIRCLE);
			paintCircleBorder.setAlpha(ALFA_FOR_CIRCLE);

			if (isHover) {
				c.drawCircle(centerX, centerY, radius, paintCircle);
				c.drawCircle(centerX, centerY, radius, paintCircleBorder);
				c.drawBitmap(bitmapHover, matrix, paint);
			} else {
				c.drawCircle(centerX, centerY, radius, paintCircle);
				c.drawCircle(centerX, centerY, radius, paintCircleBorder);
				c.drawBitmap(bitmapNormal, matrix, paint);
			}
		} else {
			if (isCompassNorthVisible) {
				if (isHover) {
					c.drawCircle(centerX, centerY, radius, paintCircle);
					c.drawCircle(centerX, centerY, radius, paintCircleBorder);
					c.drawBitmap(bitmapHover, matrix, paint);
				} else {
					c.drawCircle(centerX, centerY, radius, paintCircle);
					c.drawCircle(centerX, centerY, radius, paintCircleBorder);
					c.drawBitmap(bitmapNormal, matrix, paint);
				}

				handler.postDelayed(r, 1600);
			} else {
				if (paint.getAlpha() > 0) {
					percenetAnimElapsedReverse = 1 - (System
							.currentTimeMillis() - timeAnimStart) / 800.0f;

					if (percenetAnimElapsedReverse < 0) {
						percenetAnimElapsedReverse = 0;
					}

					paint.setAlpha((int) (255 * percenetAnimElapsedReverse));
					paintCircle.setAlpha((int) (ALFA_FOR_CIRCLE * percenetAnimElapsedReverse));
					paintCircleBorder.setAlpha((int) (ALFA_FOR_CIRCLE * percenetAnimElapsedReverse));

					if (isHover) {
						c.drawCircle(centerX, centerY, radius, paintCircle);
						c.drawCircle(centerX, centerY, radius, paintCircleBorder);
						c.drawBitmap(bitmapHover, matrix, paint);
					} else {
						c.drawCircle(centerX, centerY, radius, paintCircle);
						c.drawCircle(centerX, centerY, radius, paintCircleBorder);
						c.drawBitmap(bitmapNormal, matrix, paint);
					}

					invalidate();
				} else {
					c.drawColor(Color.TRANSPARENT);
				}

			}

		}
	}

	private boolean isUp = false;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (event.getX() > getPaddingLeft()
					&& event.getX() < (width - getPaddingRight())
					&& event.getY() > (getPaddingTop())
					&& event.getY() < (height - getPaddingBottom())) {

				// when compass is on 0, user can pan a map from compass
				// location
				if (mapView.getMapRotation() != 0) {
					isHover = true;
					invalidate();

					return true;
				}
			}

			break;
		case MotionEvent.ACTION_MOVE:
			if (isHover) {
				if (!(event.getX() > getPaddingLeft()
						&& event.getX() < (width - getPaddingRight())
						&& event.getY() > (getPaddingTop()) && event.getY() < (height - getPaddingBottom()))) {

					isHover = false;
					invalidate();

				}
			}

			break;
		case MotionEvent.ACTION_UP:

			isUp = false;

			if (isHover) {
				playSoundEffect(android.view.SoundEffectConstants.CLICK);

				isHover = false;

				invalidate();

				// disable turn mode, so map can be rotated to north
				if (locationButton != null
						&& locationButton.getState() == LocationView.LOCATION_STATE_TURN) {
					locationButton.setState(LocationView.LOCATION_STATE_ON);
				}

				mapView.setMapRotation(0,
						Math.abs(mapView.getMapRotation()) * 0.005f);
				isUp = true;
			}

			return isUp;
		}

		return false;
	}

	public void notifyCompass() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				matrix.setTranslate(getPaddingLeft(), getPaddingTop());
				matrix.postRotate(mapView.getMapRotation(), centerX, centerY);

				invalidate();
			}
		});
	}

	/**
	 * Set MapView and GPSView, so CompassView can get map rotation and set map
	 * rotation. GPSView is need because of GPS_TURN_MODE which need to be
	 * disabled when user click on compass
	 * 
	 */
	public void setObjects(MapView mapView, LocationView gpsButton) {
		this.mapView = mapView;
		this.locationButton = gpsButton;
		notifyCompass();
	}
}
