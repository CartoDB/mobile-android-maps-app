package com.nutiteq.nuticomponents.customviews.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.speech.RecognizerIntent;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.nutiteq.nuticomponents.R;

import java.util.List;
import java.util.Locale;

/**
 * 
 * @author Milan Ivankovic, Nole
 * 
 *         Voice view is part of search bar which is positioned on right of
 *         SearchView and has action button which opens voice activity. You must
 *         set instance of your main activity and in you main activity you must
 *         implement onActivityResult. For requestCode 1 you will have in
 *         data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0)
 *         string which is voice recognized. You can use that string to start
 *         search by your web service
 * 
 */
public class VoiceView extends View implements OnTouchListener {

	private Activity mainActivity;

	private int width;
	private int height;

	private Paint paint = new Paint();
	private Paint paint2 = new Paint();

	private Bitmap voiceBitmap;

	private int voice1;
	private int voice2;

	private int m;

	private boolean isVoiceAppOnDevice = false;

	@SuppressLint("ClickableViewAccessibility")
	public VoiceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		voiceBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.voice);

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

		width = (int) ((width - 2.0f * 10.0f * metrics.density) * 0.18f);

		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth((int) (metrics.density * 1.0f));
		paint.setAntiAlias(true);

		paint2.setColor(Color.rgb(232, 232, 232));
		paint2.setStyle(Paint.Style.STROKE);
		paint2.setStrokeWidth((int) (metrics.density * 1.0f));
		paint2.setAntiAlias(true);

		m = width - (int) (20.0f * metrics.density + 22.0f * metrics.density);

		voice1 = m - (int) (10.0f * metrics.density);
		voice2 = m + voiceBitmap.getWidth() + (int) (10.0f * metrics.density);

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
		c.drawLine(width - paint2.getStrokeWidth() / 2, 0,
				width - paint2.getStrokeWidth() / 2, height, paint2);
		c.drawLine(0, height - paint2.getStrokeWidth() / 2, width, height
				- paint2.getStrokeWidth() / 2, paint2);

		if (isVoiceAppOnDevice) {
			c.drawBitmap(voiceBitmap, m, height / 2 - voiceBitmap.getHeight()
					/ 2, paint);
		}
	}

	boolean isVoiceDown = false;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (isVoiceAppOnDevice) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (event.getX() >= voice1 && event.getX() <= voice2) {
					isVoiceDown = true;
				}

				break;
			case MotionEvent.ACTION_MOVE:
				if (!(event.getX() >= voice1 && event.getX() <= voice2)) {
					isVoiceDown = false;
				}

				break;
			case MotionEvent.ACTION_UP:
				if (isVoiceDown) {
					playSoundEffect(android.view.SoundEffectConstants.CLICK);

					Intent intent = new Intent(
							RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							Locale.getDefault());
					mainActivity.startActivityForResult(intent, 1);
				}

				break;
			}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks availability of speech recognizing Activity
	 * 
	 * @param callerActivity
	 *            � Activity that called the checking
	 * @return true � if Activity there available, false � if Activity is
	 *         absent
	 */
	private static boolean isSpeechRecognitionActivityPresented(
			Activity callerActivity) {
		try {
			// getting an instance of package manager
			PackageManager pm = callerActivity.getPackageManager();
			// a list of activities, which can process speech recognition Intent
			List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
					RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

			if (activities.size() != 0) { // if list not empty
				return true; // then we can recognize the speech
			}
		} catch (Exception e) {

		}

		return false; // we have no activities to recognize the speech
	}

	/**
	 * Set main activity on which this component is attached
	 */
	public void setMainActivity(Activity mainActivity) {
		this.mainActivity = mainActivity;

		if (isSpeechRecognitionActivityPresented(mainActivity)) {
			isVoiceAppOnDevice = true;
		} else {
			isVoiceAppOnDevice = false;
		}
	}
}
