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

/**
 * @author Milan Ivankovic, Nole
 *         <p/>
 *         Custom location button with 4 states and 4 different clicks actions.
 *         Currently it's required that all bitmaps has same width and height,
 *         otherwise clicks on screen will have strange behavior. Click bitmap
 *         is same as without click and easy can be adjusted to have different
 *         click bitmap.
 */

public class LocationView extends View implements OnTouchListener {

    public static final int LOCATION_STATE_OFF = 1;
    public static final int LOCATION_STATE_ON = 2;
    public static final int LOCATION_STATE_FIX = 3;
    public static final int LOCATION_STATE_TURN = 4;

    private static int LONG_PRESS_TIME = 600;

    final Handler handler = new Handler();

    private int state = LOCATION_STATE_OFF;

    private Boolean isLongClickON = false;

    private LocationButtonClickListener clickListener;
    private LocationButtonLongClickListener longClickListener;
    private LocationButtonGPSTrackingListener gpsTrackingListener;

    private Bitmap bitmapStateOffNormal;
    private Bitmap bitmapStateOffHover;
    private Bitmap bitmapStateOnNormal;
    private Bitmap bitmapStateOnHover;
    private Bitmap bitmapStateFixNormal;
    private Bitmap bitmapStateFixHover;
    private Bitmap bitmapStateTurnNormal;
    private Bitmap bitmapStateTurnHover;

    private int width;
    private int height;

    private Paint paintDefault = new Paint();
    private Paint paintBackgroundColor = new Paint();
    private Paint paintBackgroundColorAnimation = new Paint();
    private Paint paintBackgroundColorAnimation2 = new Paint();
    private Paint paintBackgroundLongClickOn = new Paint();

    private boolean isClick = false;
    private int radius;
    private DisplayMetrics metrics;
    private Boolean isNextState = true;
    private boolean isLongHandled;

    Runnable longPressed = new Runnable() {
        public void run() {
            if (longClickListener != null) {
                longClickListener.onLongClick();
                isLongHandled = true;
            }
        }
    };

    private int offset;

    private int deltaAnimation = 0;
    private boolean isTrackingOn = false;

    private Bitmap backgroundBitmap;

    private int width2;
    private int height2;

    private int heightAnimation = 0;

    boolean isAnimationUp = false;
    boolean isAnimationDown = false;

    boolean isAnimationUpFinished = false;
    boolean isAnimationDownFinished = false;

    private float percentOfAnimation = 0f;

    private int animationDuration = 160; // in miliseconds
    private int animationDuration2 = 1080; // in miliseconds don't change
    private long animationStartTime;

    private int bottomViewHeight;

    private boolean isRotationAnimation = false;
    private boolean isRotationAnimationFinish = false;
    private boolean isFirstLocationFound = false;

    private Matrix matrix = new Matrix();

    @SuppressLint("ClickableViewAccessibility")
    public LocationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        state = LOCATION_STATE_OFF;

        bitmapStateOffNormal = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_off);
        bitmapStateOffHover = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_off);

        bitmapStateOnNormal = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_on);
        bitmapStateOnHover = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_on);

        bitmapStateFixNormal = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_fix);
        bitmapStateFixHover = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_fix);

        bitmapStateTurnNormal = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_turn);
        bitmapStateTurnHover = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_turn);

        backgroundBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.location_background);

        width = backgroundBitmap.getWidth() + getPaddingLeft()
                + getPaddingRight();
        height = backgroundBitmap.getHeight() + getPaddingBottom()
                + getPaddingTop();

        width2 = bitmapStateOffNormal.getWidth();
        height2 = bitmapStateOffNormal.getHeight();

        metrics = getResources().getDisplayMetrics();

        bottomViewHeight = (int) (metrics.density * 80);

        paintBackgroundColor.setColor(Color.rgb(252, 61, 19));
        paintBackgroundColor.setAlpha(192);
        paintBackgroundColor.setStrokeWidth(2 * metrics.density);

        paintBackgroundColorAnimation.setColor(Color.rgb(252, 61, 19));
        paintBackgroundColorAnimation.setAlpha(192);
        paintBackgroundColorAnimation.setStrokeWidth(2 * metrics.density);

        paintBackgroundColorAnimation2.setColor(Color.rgb(252, 61, 19));
        paintBackgroundColorAnimation2.setAlpha(192);
        paintBackgroundColorAnimation2.setStrokeWidth(2 * metrics.density);

        paintBackgroundLongClickOn.setColor(Color.rgb(0, 180, 131));
        paintBackgroundLongClickOn.setAlpha(192);

        offset = (int) (6f * metrics.density);

        radius = (int) (bitmapStateOffNormal.getWidth() / 2 + offset);

        matrix.reset();

        matrix.setTranslate(width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation);

        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int w, int h) {
        invalidate();

        setMeasuredDimension(width, height
                + deltaAnimation + bottomViewHeight);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        if (isLongClickON) {
            c.drawCircle(width / 2, height / 2
                    + deltaAnimation + bottomViewHeight + heightAnimation, radius, paintBackgroundLongClickOn);
        } else {
            c.drawBitmap(backgroundBitmap, getPaddingLeft(), getPaddingTop() + bottomViewHeight + heightAnimation, paintDefault);
        }

        switch (state) {
            case LOCATION_STATE_OFF:
                if (isClick) {
                    c.drawBitmap(bitmapStateOffHover, width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                } else {
                    c.drawBitmap(bitmapStateOffNormal,
                            width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                }

                break;
            case LOCATION_STATE_ON:
                if (isFirstLocationFound && isRotationAnimationFinish) {
                    if (isClick) {
                        c.drawBitmap(bitmapStateOnHover, width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                    } else {
                        c.drawBitmap(bitmapStateOnNormal, width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                    }
                } else {
                    if (isClick) {
                        c.drawBitmap(bitmapStateOnHover, matrix, paintDefault);
                    } else {
                        c.drawBitmap(bitmapStateOnNormal, matrix, paintDefault);
                    }
                }

                break;
            case LOCATION_STATE_FIX:
                if (isFirstLocationFound && isRotationAnimationFinish) {
                    if (isClick) {
                        c.drawBitmap(bitmapStateFixHover, width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                    } else {
                        c.drawBitmap(bitmapStateFixNormal,
                                width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                    }
                } else {
                    if (isClick) {
                        c.drawBitmap(bitmapStateFixHover, matrix, paintDefault);
                    } else {
                        c.drawBitmap(bitmapStateFixNormal,
                                matrix, paintDefault);
                    }
                }

                break;
            case LOCATION_STATE_TURN:
                if (isClick) {
                    c.drawBitmap(bitmapStateTurnHover,
                            width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                } else {
                    c.drawBitmap(bitmapStateTurnNormal, width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation, paintDefault);
                }

                break;
        }

        if (isRotationAnimation) {
            percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                    / (animationDuration2 * 1.0f);

            if (percentOfAnimation >= 1) {
                isRotationAnimationFinish = true;
                animationStartTime = System.currentTimeMillis();
            }

            matrix.postRotate(6, width / 2 - width2 / 2 + bitmapStateOnHover.getWidth() / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation + bitmapStateOnHover.getHeight() / 2);

            invalidate();
        }

        if (isAnimationUp) {
            if (heightAnimation == -bottomViewHeight) {
                isAnimationUp = false;
                isAnimationUpFinished = true;

                matrix.setTranslate(width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation);
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    heightAnimation = -bottomViewHeight;
                } else {
                    heightAnimation = -(int) (percentOfAnimation * (bottomViewHeight));
                }

                matrix.setTranslate(width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation);
            }

            invalidate();
        } else if (isAnimationDown) {
            if (heightAnimation == 0) {
                isAnimationDown = false;
                isAnimationDownFinished = true;

                matrix.setTranslate(width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation);
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    heightAnimation = 0;
                } else {
                    heightAnimation = -(bottomViewHeight - (int) (percentOfAnimation * (bottomViewHeight)));
                }

                matrix.setTranslate(width / 2 - width2 / 2, height / 2 - height2 / 2 + bottomViewHeight + heightAnimation);
            }

            invalidate();
        }
    }

    private boolean isClickPointInLocationCircle(int x, int y) {
        if (x > (width / 2 - radius)
                && x < (width / 2 + radius)
                && y > (height / 2 - radius + heightAnimation + bottomViewHeight)
                && y < (height / 2 + radius + heightAnimation + bottomViewHeight)) {
            return true;
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isClickPointInLocationCircle((int) event.getX(),
                        (int) event.getY())) {
                    isLongHandled = false;
                    handler.postDelayed(longPressed, LONG_PRESS_TIME);

                    isClick = true;
                    invalidate();

                    return true;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (isClick
                        && !isClickPointInLocationCircle((int) event.getX(),
                        (int) event.getY())) {
                    handler.removeCallbacks(longPressed);

                    isClick = false;
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (isClick && !isLongHandled) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);

                    handler.removeCallbacks(longPressed);

                    isClick = false;
                    invalidate();

                    if (clickListener != null) {
                        switch (state) {
                            case LOCATION_STATE_OFF:
                                isNextState = clickListener.onLocationStateOff();

                                animationStartTime = System.currentTimeMillis();
                                isRotationAnimation = true;
                                isRotationAnimationFinish = false;
                                isFirstLocationFound = false;

                                break;
                            case LOCATION_STATE_ON:
                                isNextState = clickListener.onLocationStateOn();

                                break;
                            case LOCATION_STATE_FIX:
                                isNextState = clickListener.onLocationStateFix();

                                break;
                            case LOCATION_STATE_TURN:
                                isNextState = clickListener.onLocationStateTurn();

                                break;
                        }
                    }

                    if (isNextState) {
                        state++;
                        if (state > LOCATION_STATE_TURN) {
                            state = LOCATION_STATE_OFF;
                        }

                        invalidate();
                    }

                    return true;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressed);

                break;
        }

        return false;
    }

    public void goUp() {
        if (!isAnimationUp && !isAnimationUpFinished) {
            animationStartTime = System.currentTimeMillis();
            isAnimationUp = true;

            heightAnimation = 0;

            isAnimationDown = false;
        }

        invalidate();
    }

    public void goDown() {
        if (!isAnimationDown) {
            isAnimationUpFinished = false;
            isAnimationUp = false;

            animationStartTime = System.currentTimeMillis();
            isAnimationDown = true;
            heightAnimation = -bottomViewHeight;

            invalidate();
        }
    }

    /**
     * Get state of LocationView
     */

    public int getState() {
        return state;
    }

    /**
     * Set state of LocationView
     */
    public void setState(int state) {
        this.state = state;
        invalidate();
    }

    /**
     * Call this to indicate that first location is found
     */
    public void setIsFirstLocationFound() {
        isFirstLocationFound = true;
    }

    /**
     * Refresh states for animation when location isn't found yett
     */
    public void refresh() {
        animationStartTime = System.currentTimeMillis();
        isRotationAnimation = true;
        isRotationAnimationFinish = false;
        isFirstLocationFound = false;

        invalidate();
    }

    /**
     * Return if GPS tracking is on or off
     */
    public boolean isTrackingOn() {
        return isTrackingOn;
    }

    /**
     * Set if long click action is ON or OFF
     */
    public void setIsLongClickOn(boolean isLongClickON) {
        this.isLongClickON = isLongClickON;
        invalidate();
    }

    public boolean getIsLongClickON() {
        return isLongClickON;
    }

    /**
     * Set background color for custom button
     */
    public void setBackgroundColor(int red, int green, int blue, int alpha) {
        paintBackgroundColor.setColor(Color.rgb(red, green, blue));
        paintBackgroundColor.setAlpha(alpha);
    }

    /**
     * Set background color for custom button when long click action is ON
     */
    public void setBackgroundColorLongClickOn(int red, int green, int blue,
                                              int alpha) {
        paintBackgroundLongClickOn.setColor(Color.rgb(red, green, blue));
        paintBackgroundLongClickOn.setAlpha(alpha);
    }

    public void setLocationButtonClickListener(
            LocationButtonClickListener eventListener) {
        clickListener = eventListener;
    }

    public void setLocationButtonLongClickListener(
            LocationButtonLongClickListener eventListener) {
        longClickListener = eventListener;
    }

    public void setLocationButtonGPSTrackingListener(
            LocationButtonGPSTrackingListener eventListener) {
        gpsTrackingListener = eventListener;
    }

    public interface LocationButtonClickListener {
        public Boolean onLocationStateOff();

        public Boolean onLocationStateOn();

        public Boolean onLocationStateFix();

        public Boolean onLocationStateTurn();
    }

    public interface LocationButtonLongClickListener {
        public Boolean onLongClick();
    }

    public interface LocationButtonGPSTrackingListener {
        public Boolean onTrackingOn();

        public Boolean onTrackingOff();
    }
}