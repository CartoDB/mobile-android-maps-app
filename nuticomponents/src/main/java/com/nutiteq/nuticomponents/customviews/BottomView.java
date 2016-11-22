package com.nutiteq.nuticomponents.customviews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.carto.core.MapPos;
import com.nutiteq.nuticomponents.R;
import com.nutiteq.nuticomponents.Utility;

import java.util.Locale;

public class BottomView extends View implements OnTouchListener {

    public static final int METRIC_UNIT = 0;
    public static final int IMPERIAL_UNIT = 1;

    private int measurementUnit = METRIC_UNIT;

    private int width;
    private int height;

    private String description = " ";

    private MapPos mapPos;

    private boolean isAddingFavorite = true;

    private OnDroppedPinViewClickListener droppedPinListener;

    private Bitmap bmpCanvas;

    private Canvas c;

    private Paint defaultPaint = new Paint();
    private Paint paintShadow;

    private int shadowHeight;

    private Bitmap routeStart;

    private Bitmap addFavorite;

    private Bitmap removeFavorite;

    private int paddingForBitmaps;

    private int leftMarginForText;

    private Paint paintLine1;
    private Paint paintLine2;

    private Paint paintFavorite;

    private int heightAnimation = 0;

    boolean isAnimationIn = false;
    boolean isAnimationOut = false;

    boolean isAnimationInFinished = false;
    boolean isAnimationOutFinished = false;

    private float percentOfAnimation = 0f;

    private int animationDuration = 160; // in miliseconds
    private long animationStartTime;

    private String title;

    public BottomView(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        width = metrics.widthPixels;
        height = (int) (metrics.density * 80f);

        addFavorite = BitmapFactory.decodeResource(getResources(),
                R.drawable.add_favorite);

        removeFavorite = BitmapFactory.decodeResource(getResources(),
                R.drawable.remove_favorite);

        routeStart = BitmapFactory.decodeResource(getResources(),
                R.drawable.route_start);

        title = getResources().getString(R.string.dropped_pin);

        paddingForBitmaps = (int) (metrics.density * 12f);

        leftMarginForText = 2 * paddingForBitmaps + addFavorite.getWidth() * 2;

        paintLine1 = new Paint();
        paintLine1.setColor(Color.rgb(67, 67, 67));
        paintLine1.setTextSize(18 * metrics.density);
        paintLine1.setAntiAlias(true);

        paintLine2 = new Paint();
        paintLine2.setColor(Color.rgb(164, 164, 164));
        paintLine2.setTextSize(16 * metrics.density);
        paintLine2.setAntiAlias(true);

        paintFavorite = new Paint();
        paintFavorite.setColor(Color.rgb(252, 1, 80));
        paintFavorite.setStyle(Paint.Style.FILL);
        paintFavorite.setAntiAlias(true);

        shadowHeight = 0;

        paintShadow = new Paint();
        paintShadow.setColor(Color.WHITE);
        paintShadow.setAntiAlias(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            shadowHeight = (int) (metrics.density * 3f);

            paintShadow.setShadowLayer(shadowHeight, 0.0f, 0.0f,
                    Color.rgb(164, 164, 164));
        }

        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec + shadowHeight);

        if (bmpCanvas != null) {
            createBitmap();
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bmpCanvas != null) {
            c.drawBitmap(bmpCanvas, 0, heightAnimation, defaultPaint);
        } else {
            createBitmap();
        }

        if (isAnimationIn) {
            if (heightAnimation == shadowHeight) {
                isAnimationIn = false;
                isAnimationInFinished = true;
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    heightAnimation = shadowHeight;
                } else {
                    heightAnimation = ((height + shadowHeight) - (int) (percentOfAnimation * (height)));
                }
            }

            invalidate();
        } else if (isAnimationOut) {
            if (heightAnimation == (height + shadowHeight)) {
                isAnimationOut = false;
                isAnimationOutFinished = true;

                setVisibility(View.INVISIBLE);
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    heightAnimation = (height + shadowHeight);
                } else {
                    heightAnimation = (int) (percentOfAnimation * (height)) + shadowHeight;
                }

                invalidate();
            }
        }
    }

    private boolean isLeftButtonDown = false;
    private boolean isRightButtonDown = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() > paddingForBitmaps * 2 + addFavorite.getWidth() / 2 - addFavorite.getWidth() &&
                        event.getX() < paddingForBitmaps * 2 + addFavorite.getWidth() / 2 + addFavorite.getWidth() &&
                        event.getY() > shadowHeight / 2 + height / 2 - addFavorite.getWidth() &&
                        event.getY() < shadowHeight / 2 + height / 2 + addFavorite.getWidth()) {
                    isLeftButtonDown = true;
                } else if (event.getX() > width - routeStart.getWidth() - paddingForBitmaps &&
                        event.getX() < width - paddingForBitmaps &&
                        event.getY() > shadowHeight / 2 + height / 2 - routeStart.getHeight() / 2 &&
                        event.getY() < shadowHeight / 2 + height / 2 + routeStart.getHeight() / 2) {
                    isRightButtonDown = true;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (isLeftButtonDown && !(event.getX() > paddingForBitmaps * 2 + addFavorite.getWidth() / 2 - addFavorite.getWidth() &&
                        event.getX() < paddingForBitmaps * 2 + addFavorite.getWidth() / 2 + addFavorite.getWidth() &&
                        event.getY() > shadowHeight / 2 + height / 2 - addFavorite.getWidth() &&
                        event.getY() < shadowHeight / 2 + height / 2 + addFavorite.getWidth())) {
                    isLeftButtonDown = false;
                } else if (isRightButtonDown && !(event.getX() > width - routeStart.getWidth() - paddingForBitmaps &&
                        event.getX() < width - paddingForBitmaps &&
                        event.getY() > shadowHeight / 2 + height / 2 - routeStart.getHeight() / 2 &&
                        event.getY() < shadowHeight / 2 + height / 2 + routeStart.getHeight() / 2)) {
                    isRightButtonDown = false;
                }

                break;
            case MotionEvent.ACTION_UP:
                if (isLeftButtonDown) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);

                    if (droppedPinListener != null) {
                        if (droppedPinListener.onLeftButtonClick()) {
                            isAddingFavorite = !isAddingFavorite;

                            createBitmap();
                        }

                        isLeftButtonDown = false;
                    }
                } else if (isRightButtonDown) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK);

                    if (droppedPinListener != null) {
                        if (droppedPinListener.onRightButtonClick()) {

                        }

                        isRightButtonDown = false;
                    }
                }

                break;
        }

        return true;
    }

    public boolean getIsAddingFavorite() {
        return isAddingFavorite;
    }

    public void show(String title, String description, MapPos mapPos, boolean isAddingFavorite) {
        this.title = title;

        show(description, mapPos, isAddingFavorite);
    }

    public void show(String description, MapPos mapPos, boolean isAddingFavorite) {
        this.mapPos = mapPos;
        this.isAddingFavorite = isAddingFavorite;
        if (description.equals("")) {
            this.description = getResources().getString(R.string.retrieving_location);
        } else {
            this.description = description;
        }

        createBitmap();

        if (getVisibility() != VISIBLE && !isAnimationIn && !isAnimationInFinished) {
            setVisibility(VISIBLE);

            animationStartTime = System.currentTimeMillis();
            isAnimationIn = true;

            heightAnimation = (height + shadowHeight);

            isAnimationOut = false;
        }

        invalidate();
    }

    public void hide() {
        if (!isAnimationOut) {
            this.description = " ";

            isAnimationInFinished = false;
            isAnimationIn = false;

            animationStartTime = System.currentTimeMillis();
            isAnimationOut = true;
            heightAnimation = shadowHeight;

            invalidate();
        }
    }

    private double distance = 0;
    private String time = "";

    public void setTitle(double distance, String time) {
        this.distance = distance;
        this.time = time;

        this.title = formatDistance(distance) + time;

        createBitmap();
    }

    public void setTitle(String title) {
        this.title = title;
        createBitmap();
    }

    public String getTitle() {
        return title;
    }

    public void setDescription(String description) {
        this.description = description.trim();
        createBitmap();
    }

    public String getDescription() {
        String s = description;

        if (s.equals(getResources().getString(R.string.retrieving_location))) {
            s = getResources().getString(R.string.location);
        }

        return s;
    }

    public MapPos getLocation() {
        return mapPos;
    }

    public boolean getIsAddingFavoriteState() {
        return isAddingFavorite;
    }

    private void createBitmap() {
        int w = width;
        int h = height;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;

        bmpCanvas = Bitmap.createBitmap(w, h + shadowHeight, conf);

        c = new Canvas(bmpCanvas);

        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        c.drawRect(0, shadowHeight, width, height + shadowHeight, paintShadow);

        c.drawCircle(paddingForBitmaps * 2 + addFavorite.getWidth() / 2, shadowHeight / 2 + height / 2, addFavorite.getWidth(), paintFavorite);

        if (isAddingFavorite) {
            c.drawBitmap(addFavorite, paddingForBitmaps * 2, shadowHeight / 2 + height / 2 - addFavorite.getHeight() / 2, defaultPaint);
        } else {
            c.drawBitmap(removeFavorite, paddingForBitmaps * 2, shadowHeight / 2 + height / 2 - addFavorite.getHeight() / 2, defaultPaint);
        }

        c.drawBitmap(
                routeStart, width - routeStart.getWidth() - paddingForBitmaps,
                shadowHeight / 2 + height / 2 - routeStart.getHeight() / 2, defaultPaint
        );

        String[] s1 = Utility.wrapText(title, paintLine1, width - 2 * leftMarginForText - paddingForBitmaps, 0);

        if (s1.length == 1) {
            c.drawText(title, leftMarginForText, height / 2, paintLine1);
        } else {
            c.drawText(s1[0] + " ...", leftMarginForText, height / 2, paintLine1);
        }

        String[] s2 = Utility.wrapText(description, paintLine2, width - 2 * leftMarginForText - paddingForBitmaps, 0);

        if (s2.length == 1) {
            c.drawText(description, leftMarginForText, height / 2 + paintLine1.getTextSize(), paintLine2);
        } else {
            c.drawText(s2[0] + " ...", leftMarginForText, height / 2 + paintLine1.getTextSize(), paintLine2);
        }

        postInvalidate();
    }

    public void setMeasurementUnit(int measurementUnit) {
        this.measurementUnit = measurementUnit;

        if (!(distance == 0 && time.equals(""))) {
            this.title = formatDistance(distance) + time;
        }

        createBitmap();
    }

    private String formatDistance(double distance) {
        int d = (int) distance;

        if (measurementUnit == METRIC_UNIT) {
            if (d < 1000) {
                return d + " m";
            } else {
                return String.format(Locale.getDefault(), "%.1f %s", d / 1000f, "km");
            }
        } else if (measurementUnit == IMPERIAL_UNIT) {
            // meters to ft
            d *= 3.28084f;

            if (d < 528) {
                return d + " ft";
            } else {
                float mi = d / 5280.0f;

                return String.format(Locale.getDefault(), "%.2f %s", mi, " mi");
            }
        } else {
            return "";
        }
    }

    public void setOnDroppedPinViewClickListener(
            OnDroppedPinViewClickListener eventListener) {
        droppedPinListener = eventListener;
    }

    public interface OnDroppedPinViewClickListener {
        public Boolean onLeftButtonClick();

        public Boolean onRightButtonClick();
    }
}
