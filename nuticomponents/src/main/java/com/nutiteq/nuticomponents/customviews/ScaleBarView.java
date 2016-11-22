package com.nutiteq.nuticomponents.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.carto.core.MapPos;
import com.carto.core.ScreenPos;
import com.nutiteq.nuticomponents.BuildConfig;
import com.carto.ui.MapView;
import com.nutiteq.nuticomponents.Utility;

/**
 * @author Milan Ivankovic, Nole
 *         <p/>
 *         Scale bar view shows map distance as one horizontal line in KM or MI
 *         based on zoom and y coordinate. It can be drawn from left, center or
 *         right. Default is from center. You must set nutiteq MapView to this
 *         class because it needs it for calculation. Also in MapEventListener
 *         from method onMapMoved you must call method from this class
 *         notifyScaleBar, otherwise ScaleBarView will not be drawn because it
 *         needs refreshments for every move.
 */
public class ScaleBarView extends View {

    private MapView mapView;

    public static final int METRIC_UNIT = 0;
    public static final int IMPERIAL_UNIT = 1;

    private int measurementUnit = METRIC_UNIT;

    private int width;
    private int height;

    private Paint paint;

    private int padTop;
    private int padBottom;
    private int padLeft;
    private int padRight;

    private float scaleWidth;
    private String scaleText;

    // defaults values for normal 160 dpi screen
    private int barHeight = 16;
    private int barMinWidth = 32;
    private int barMaxWidth = 128;

    public final static int SCALE_DRAW_LEFT = 1;
    public final static int SCALE_DRAW_CENTER = 2;
    public final static int SCALE_DRAW_RIGHT = 3;

    private int scaleBarDrawMode = SCALE_DRAW_CENTER;

    int[] allowedScalesInMeters = {10000000, 5000000, 2000000, 1000000,
            500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500,
            200, 100, 50, 20, 10, 5, 2, 1};

    int[] allowedScalesInFeets = {5280000, 2640000, 1056000, 528000, 264000,
            105600, 52800, 26400, 10560, 5280, 2640, 1056, 528, 200, 100, 50,
            20, 10, 5, 2, 1};

    private Handler handler = new Handler();

    public ScaleBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        padTop = getPaddingTop();
        padBottom = getPaddingBottom();
        padLeft = getPaddingLeft();
        padRight = getPaddingRight();

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        barMinWidth = (int) (barMinWidth * metrics.density);
        barMaxWidth = (int) (barMaxWidth * metrics.density);

        width = barMaxWidth + padLeft + padRight;
        height = (int) (barHeight * metrics.density) + padBottom + padTop;

        paint = new Paint();
        paint.setColor(Color.rgb(0, 129, 94));
        paint.setStrokeWidth(2 * metrics.density);
        paint.setTextSize(13 * metrics.density);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        if (isScaleBar) {

            if (scaleBarDrawMode == SCALE_DRAW_LEFT) {
                c.drawText(scaleText, padLeft,
                        height - padTop - paint.getStrokeWidth() * 2, paint);
                c.drawLine(padLeft, height - padTop, scaleWidth + padLeft,
                        height - padTop, paint);
            } else if (scaleBarDrawMode == SCALE_DRAW_CENTER) {
                c.drawText(scaleText, width / 2 - paint.measureText(scaleText)
                                / 2, height - padTop - paint.getStrokeWidth() * 2,
                        paint);
                c.drawLine(width / 2 - scaleWidth / 2, height - padTop, width
                        / 2 + scaleWidth / 2, height - padTop, paint);
            } else if (scaleBarDrawMode == SCALE_DRAW_RIGHT) {
                c.drawText(scaleText,
                        width - padRight - paint.measureText(scaleText), height
                                - padTop - paint.getStrokeWidth() * 2, paint);
                c.drawLine(width - padRight - scaleWidth, height - padTop,
                        width - padRight, height - padTop, paint);
            }

        } else {
            c.drawColor(Color.TRANSPARENT);
        }
    }

    private boolean isScaleBar;

    public void notifyScaleBar() {
        handler.post(new Runnable() {

            @Override
            public void run() {

                if (mapView.getZoom() >= 5) {
                    if (measurementUnit == METRIC_UNIT) {
                        MapPos mp1 = mapView.screenToMap(new ScreenPos(mapView
                                .getWidth() / 2, mapView.getHeight() / 2));
                        MapPos mp2 = mapView.screenToMap(new ScreenPos(mapView
                                .getWidth() / 2 + 1, mapView.getHeight() / 2));

                        float onePxInMercatorMeters = (float) Utility.distanceBetweenPoints(mp1, mp2);

						/*
                         * // Alternative version, for reference float dpiScale
						 * = mapView.getOptions().getDPI() / 160.0f; double
						 * scale = baseProj.getBounds().getDelta().getX() /
						 * 256.0 / Math.pow(2.0, mapView.getZoom()); float
						 * onePxInMercatorMetersAlt = (float) (scale /
						 * dpiScale);
						 */

                        double lat = mapView.getOptions().getBaseProjection()
                                .toWgs84(mapView.getFocusPos()).getY();
                        double cos = Math.cos(lat * Math.PI / 180.0);
                        float onePxInMeters = (float) (onePxInMercatorMeters * cos);

                        isScaleBar = false;

                        for (int i = allowedScalesInMeters.length - 1; i >= 0; i--) {
                            scaleWidth = allowedScalesInMeters[i]
                                    / onePxInMeters;

                            if (scaleWidth >= barMinWidth
                                    && scaleWidth <= barMaxWidth) {
                                if (allowedScalesInMeters[i] >= 1000)
                                    scaleText = allowedScalesInMeters[i] / 1000
                                            + " km";
                                else
                                    scaleText = allowedScalesInMeters[i] + " m";
                                isScaleBar = true;
                                break;
                            }
                        }
                    } else if (measurementUnit == IMPERIAL_UNIT) {
                        MapPos mp1 = mapView.screenToMap(new ScreenPos(mapView
                                .getWidth() / 2, mapView.getHeight() / 2));
                        MapPos mp2 = mapView.screenToMap(new ScreenPos(mapView
                                .getWidth() / 2 + 1, mapView.getHeight() / 2));

                        float onePxInMercatorMeters = (float) Utility.distanceBetweenPoints(mp1, mp2);

                        double lat = mapView.getOptions().getBaseProjection()
                                .toWgs84(mapView.getFocusPos()).getY();
                        double cos = Math.cos(lat * Math.PI / 180.0);
                        float onePxInFeets = (float) (onePxInMercatorMeters * cos) * 3.28084f;

                        isScaleBar = false;

                        for (int i = allowedScalesInFeets.length - 1; i >= 0; i--) {
                            scaleWidth = allowedScalesInFeets[i] / onePxInFeets;

                            if (scaleWidth >= barMinWidth
                                    && scaleWidth <= barMaxWidth) {

                                if (allowedScalesInFeets[i] >= 528) {
                                    float f = allowedScalesInFeets[i] * 1.0f / 5280f;
                                    if (f >= 1)
                                        scaleText = (int) f + " mi";
                                    else
                                        scaleText = f + " mi";
                                } else
                                    scaleText = allowedScalesInFeets[i] + " ft";

                                isScaleBar = true;
                                break;
                            }
                        }
                    }
                } else {
                    isScaleBar = false;
                }

                if (BuildConfig.DEBUG) {
                    scaleText += " zoom: " + mapView.getZoom();
                }

                invalidate();
            }
        });
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
        notifyScaleBar();
    }

    /**
     * Draw scale bar from left, center or right. Default is left.
     *
     * @param scaleBarDrawMode SCALE_DRAW_LEFT, SCALE_DRAW_CENTER or SCALE_DRAW_RIGHT
     */
    public void setDrawMode(int scaleBarDrawMode) {
        this.scaleBarDrawMode = scaleBarDrawMode;
        invalidate();
    }

    /**
     * set measurement unit, metric or imperial
     *
     * @param measurementUnit METRIC_UNIT, IMPERIAL_UNIT
     */
    public void setMeasurementUnit(int measurementUnit) {
        this.measurementUnit = measurementUnit;
        notifyScaleBar();
    }

    /**
     * Set color for scaleBarView
     */
    public void setColor(int red, int green, int blue, int alpha) {
        paint.setColor(Color.rgb(red, green, blue));
        paint.setAlpha(alpha);
    }
}