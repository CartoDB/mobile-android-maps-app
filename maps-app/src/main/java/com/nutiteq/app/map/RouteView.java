package com.nutiteq.app.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.nutiteq.app.map.listener.MyMapEventListener;
import com.nutiteq.app.nutimap3d.dev.R;
import com.carto.core.MapPos;
import com.carto.core.ScreenPos;
import com.nutiteq.nuticomponents.Utility;
import com.carto.ui.MapView;

import java.util.ArrayList;
import java.util.Locale;

public class RouteView extends View implements View.OnTouchListener {

    public static final int METRIC_UNIT = 0;
    public static final int IMPERIAL_UNIT = 1;

    private int measurementUnit = METRIC_UNIT;

    private int width;
    private int height;

    private int height2;

    private int drawHeight;

    private MapView mapView;
    private MyMapEventListener mapListener;

    private double distance;
    private String time = "";
    private ArrayList<RouteInstruction> instructions;

    private Paint paintDefault = new Paint();

    private Paint paintBlack = new Paint();

    private Paint paintWhite = new Paint();

    private Paint paintGray = new Paint();
    private Paint paintGray2 = new Paint();

    private Paint paintRed = new Paint();

    private Paint paintGrayClick = new Paint();

    private int leftMargin;
    private int topMargin;

    private Bitmap bitmapTop;
    private Canvas c;

    private Paint paintShadow;
    private Paint paintShadow2;

    private int shadowHeight;

    private int y = 0;
    private int deltaY = 0;

    private boolean isUp = false;

    private boolean isDownUp = false;

    private DisplayMetrics metrics;

    private int heightAnimation = 0;

    boolean isAnimationUp = false;
    boolean isAnimationUp2 = false;
    boolean isAnimationUp3 = false;
    boolean isAnimationDown = false;
    boolean isAnimationDown2 = false;

    boolean isAnimationUpFinished = false;
    boolean isAnimationUpFinished2 = false;
    boolean isAnimationDownFinished = false;

    private float percentOfAnimation = 0f;
    private float percentOfAnimation2 = 0f;

    private int animationDuration = 200;

    private long animationStartTime;
    private long animationStartTime2;

    private int rowHeight;

    private int item = -1;

    private int yMove = 0;
    private int move = 0;

    private int numberOfBitmaps = 0;
    private int bitmapHeight;
    private int rowNumberPerBitmap = 0;

    private int k1 = 0;
    private int k2 = 1;

    private int swipeHeight = 0;

    private int animationClickDuration = 300;
    private boolean isAnimationClickDown = false;
    private boolean isAnimationClickUp = false;

    private boolean isAnimationMove = false;
    private int animationMoveDuration = 800;

    private int numberOfBlankInstructions = 0;

    private int tmpSwipe = 0;

    public RouteView(Context context, AttributeSet attrs) {
        super(context, attrs);

        metrics = getResources().getDisplayMetrics();

        width = metrics.widthPixels;
        height = metrics.heightPixels;

        height2 = (int) (metrics.density * 80f);

        heightAnimation = height2;

        drawHeight = (int) (height / 3f);

        leftMargin = (int) (metrics.density * 16f);

        topMargin = (int) (metrics.density * 28f);

        leftBorder = (int) (metrics.density * 48f);

        rowHeight = (int) (51f * metrics.density);

        rowNumberPerBitmap = 1;

        bitmapHeight = rowNumberPerBitmap * rowHeight;

        paintDefault.setAntiAlias(true);

        paintBlack.setColor(Color.rgb(67, 67, 67));
        paintBlack.setAntiAlias(true);
        paintBlack.setTextSize(metrics.density * 17);

        paintWhite.setColor(Color.WHITE);
        paintWhite.setAntiAlias(true);
        paintWhite.setStyle(Paint.Style.FILL);

        paintGray.setColor(Color.rgb(140, 140, 140));
        paintGray.setAntiAlias(true);
        paintGray.setTextSize(metrics.density * 17);

        paintGray2.setColor(Color.rgb(140, 140, 140));
        paintGray2.setAntiAlias(true);
        paintGray2.setTextSize(metrics.density * 14);

        paintRed.setColor(Color.rgb(251, 61, 19));
        paintRed.setAntiAlias(true);
        paintRed.setTextSize(metrics.density * 17);

        paintGrayClick.setColor(Color.rgb(240, 240, 240));
        paintGrayClick.setAntiAlias(true);

        shadowHeight = 0;

        paintShadow = new Paint();
        paintShadow.setColor(Color.WHITE);
        paintShadow.setAntiAlias(true);

        paintShadow2 = new Paint();
        paintShadow2.setColor(Color.rgb(224, 224, 224));
        paintShadow2.setAntiAlias(true);
        paintShadow2.setStrokeWidth(metrics.density * 1f);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            shadowHeight = (int) (metrics.density * 3f);

            paintShadow.setShadowLayer(shadowHeight, 0.0f, 0.0f,
                    Color.rgb(164, 164, 164));

            paintShadow2.setShadowLayer(shadowHeight / 2, 0.0f, 0.0f,
                    Color.rgb(164, 164, 164));

            setLayerType(LAYER_TYPE_SOFTWARE, paintShadow2);
        }

        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        this.setMeasuredDimension(width, height - drawHeight);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec - drawHeight);

        if (bitmapTop != null) {
            createBitmap();
        }
    }

    private void drawList(Canvas c) {
        for (int i = k1; i <= k2; i++) {
            c.drawRect(0, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight - shadowHeight / 2, width, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + bitmapHeight + i * bitmapHeight, paintWhite);

            int k = (i + 1) * rowNumberPerBitmap;
            if (k > instructions.size()) {
                k = instructions.size();
            }

            int z = 0;

            if (!instructions.get(i).isBlank) {
                for (int j = i * rowNumberPerBitmap; j < k; j++) {
                    if (i == 0 && j == 0) {
                        if (item == j) {
                            c.drawRect(0, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + z * rowHeight - topMargin / 3, width, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + (z + 1) * rowHeight - topMargin / 3, paintGrayClick);
                        }

                        c.drawBitmap(instructions.get(0).getBitmap(), leftMargin, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + topMargin / 3, paintDefault);

                        c.drawText(instructions.get(0).getDescription(), leftMargin * 2 + instructions.get(0).getBitmap().getWidth(), height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + topMargin / 3 + paintBlack.getTextSize(), paintBlack);

                        if (item == j + 1 && item != instructions.size()) {
                            c.drawRect(0, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + (z + 1) * rowHeight - topMargin / 3, width, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + (z + 2) * rowHeight - topMargin / 3, paintGrayClick);
                        }
                    }

                    if (j > 0) {
                        if (item == j) {
                            c.drawRect(0, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + z * rowHeight - topMargin / 3, width, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + (z + 1) * rowHeight - topMargin / 3, paintGrayClick);
                        }

                        c.drawBitmap(instructions.get(j).getBitmap(), leftMargin, height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + topMargin / 6 + z * rowHeight, paintDefault);

                        c.drawText(String.format(Locale.getDefault(), "%s %s", getResources().getString(R.string.in), formatDistance(instructions.get(j - 1).getDistance())), leftMargin * 2 + instructions.get(0).getBitmap().getWidth(), height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + paintBlack.getTextSize() + z * rowHeight - paintGray2.getTextSize() / 2, paintGray2);

                        String[] s = Utility.wrapText(instructions.get(j).getDescription(), paintBlack, width - (leftMargin * 3 + instructions.get(0).getBitmap().getWidth()), 0);

                        if (s.length == 1) {
                            c.drawText(instructions.get(j).getDescription(), leftMargin * 2 + instructions.get(0).getBitmap().getWidth(), height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + topMargin / 3 + paintBlack.getTextSize() + z * rowHeight, paintBlack);
                        } else {
                            c.drawText(s[0] + " ...", leftMargin * 2 + instructions.get(0).getBitmap().getWidth(), height - height2 / 4 - drawHeight + swipeHeight + heightAnimation + deltaY + i * bitmapHeight + topMargin / 3 + paintBlack.getTextSize() + z * rowHeight, paintBlack);
                        }
                    }

                    z++;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (isUp) {
            drawList(c);

            if (bitmapTop != null) {
                c.drawBitmap(bitmapTop, 0, height - height2 - drawHeight + swipeHeight + heightAnimation + shadowHeight, paintDefault);
            }
        } else {
            drawList(c);

            if (bitmapTop != null) {
                c.drawBitmap(bitmapTop, 0, height - height2 - drawHeight + swipeHeight + heightAnimation, paintDefault);
            }
        }

        if (isAnimationUp) {
            if (heightAnimation == -(height - height2 + shadowHeight - drawHeight)) {
                isAnimationUp = false;
                isAnimationUpFinished = true;

                invalidate();
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation > 0.95f) {
                    isUp = true;
                }

                if (percentOfAnimation >= 1) {
                    heightAnimation = -(height - height2 + shadowHeight - drawHeight);
                } else {
                    heightAnimation = -height2 + -(int) (percentOfAnimation * (height - 2 * height2 + shadowHeight - drawHeight));
                }
            }

            invalidate();
        } else if (isAnimationUp2) {
            if (swipeHeight == 0) {
                isAnimationUp2 = false;
                isAnimationUpFinished2 = true;

                invalidate();
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation > 0.95f) {
                    isUp = true;
                }

                if (percentOfAnimation >= 1) {
                    swipeHeight = 0;
                } else {
                    swipeHeight = swipeHeight - (int) (percentOfAnimation * (swipeHeight));
                }
            }

            invalidate();
        } else if (isAnimationUp3) {
            if (swipeHeight == -(height - drawHeight)) {
                isAnimationUp3 = false;

                heightAnimation = -(height - height2 + shadowHeight - drawHeight);
                swipeHeight = 0;
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation > 0.95f) {
                    isUp = true;
                }

                if (percentOfAnimation >= 1) {
                    swipeHeight = -(height - drawHeight);
                } else {
                    swipeHeight = tmpSwipe + (int) -((height - drawHeight + tmpSwipe) * percentOfAnimation);
                }
            }

            invalidate();
        } else if (isAnimationDown) {
            if (heightAnimation == height2) {

                isAnimationDown = false;
                isAnimationDownFinished = true;

                swipeHeight = 0;

                isUp = false;

                invalidate();
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    heightAnimation = height2;
                } else {
                    heightAnimation = -(height - height2 + shadowHeight - drawHeight) + (int) (percentOfAnimation * (height - height2 + shadowHeight - drawHeight));
                }

                invalidate();
            }
        } else if (isAnimationDown2) {
            if (swipeHeight == 0) {
                isAnimationDown2 = false;
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    swipeHeight = 0;
                } else {
                    swipeHeight = tmpSwipe - (int) (tmpSwipe * percentOfAnimation);
                }
            }

            invalidate();
        } else if (isAnimationClickDown) {
            if (paintGrayClick.getColor() == Color.rgb(240, 240, 240)) {
                isAnimationClickDown = false;
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationClickDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    paintGrayClick.setColor(Color.rgb(240, 240, 240));
                } else {
                    paintGrayClick.setColor(Color.rgb((int) (255 - (255 - 240) * percentOfAnimation), (int) (255 - (255 - 240) * percentOfAnimation), (int) (255 - (255 - 240) * percentOfAnimation)));
                }

                invalidate();
            }
        } else if (isAnimationClickUp) {
            if (paintGrayClick.getColor() == Color.rgb(255, 255, 255)) {
                isAnimationClickUp = false;
                item = -1;
            } else {
                percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                        / (animationClickDuration * 1.0f);

                if (percentOfAnimation >= 1) {
                    paintGrayClick.setColor(Color.rgb(255, 255, 255));
                } else {
                    paintGrayClick.setColor(Color.rgb((int) (240 + (255 - 240) * percentOfAnimation), (int) (240 + (255 - 240) * percentOfAnimation), (int) (240 + (255 - 240) * percentOfAnimation)));
                }

                invalidate();
            }
        }

        if (isAnimationMove && speed > 0) {
            if (percentOfAnimation2 >= 1) {
                isAnimationMove = false;
            } else {
                percentOfAnimation2 = (System.currentTimeMillis() - animationStartTime2)
                        / (animationMoveDuration * 1.0f);

                if (d > 0) {
                    deltaY += (speed - (percentOfAnimation2 * speed)) * 9f * metrics.density;
                } else {
                    deltaY -= (speed - (percentOfAnimation2 * speed)) * 9f * metrics.density;
                }

                if (deltaY > 0) {
                    deltaY = 0;
                }

                if (deltaY < -((instructions.size() - numberOfBlankInstructions) * rowHeight - (height - height2 / 4 * 3) + drawHeight)) {
                    deltaY = -((instructions.size() - numberOfBlankInstructions) * rowHeight - (height - height2 / 4 * 3) + drawHeight);
                }

                k1 = (int) Math.abs(deltaY / (1f * bitmapHeight));

                k2 = k1 + (height - drawHeight) / rowHeight + 1;
                if (k2 >= numberOfBitmaps) {
                    k2 = numberOfBitmaps - 1;
                }

                invalidate();
            }
        }
    }

    private int d = 0;

    private int oldX;
    private int oldY;
    private int newX;
    private int newY;
    private long startTime;
    private float speed;
    private int leftBorder;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!isUp && swipeHeight == 0 && ((int) event.getX() < leftBorder || (int) event.getY() < height - drawHeight - height2)) {
            return false;
        }

        if (isAnimationDown || isAnimationUp || isAnimationUp) {
            return false;
        }

        if (!isUp) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    y = (int) event.getY();

                    return true;
                case MotionEvent.ACTION_MOVE:
                    swipeHeight += (int) event.getY() - y;
                    y = (int) event.getY();

                    if (swipeHeight > 0) {
                        swipeHeight = 0;
                    }

                    if (swipeHeight < -(height - drawHeight)) {
                        swipeHeight = -(height - drawHeight);
                    }

                    invalidate();

                    return true;
                case MotionEvent.ACTION_UP:
                    if (swipeHeight == 0) {
                        playSoundEffect(android.view.SoundEffectConstants.CLICK);
                        goUp3();
                    } else if (Math.abs(swipeHeight) < (height - drawHeight) / 5) {
                        goDown2();
                    } else {
                        goUp3();
                    }

                    return true;
            }
        } else {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    y = (int) event.getY();

                    if (isUp) {
                        if (y < height2 / 4 * 3) {
                            isDownUp = true;

                            return true;
                        } else {
                            item = (y - height2 / 4 * 3 - deltaY) / rowHeight;

                            yMove = (int) event.getY();

                            move = 0;

                            if (item >= instructions.size() - numberOfBlankInstructions) {
                                item = -1;
                            }

                            if (item > -1) {
                                animationStartTime = System.currentTimeMillis();
                                isAnimationClickDown = true;
                                isAnimationClickUp = false;
                            }

                            isAnimationMove = false;
                            speed = 0;

                            invalidate();
                        }
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (y < 0) {
                        isDownUp = false;

                        break;
                    }

                    if (!isDownUp && isUp && (instructions.size() - numberOfBlankInstructions) * rowHeight > height - height2 / 4 * 3 - drawHeight) {
                        long timerTime = System.currentTimeMillis() - startTime;

                        newX = (int) event.getX();
                        newY = (int) event.getY();

                        float distance = (float) Math.sqrt((newX - oldX) * (newX - oldX) + (newY - oldY) * (newY - oldY));
                        speed = distance / timerTime;

                        oldX = (int) event.getX();
                        oldY = (int) event.getY();

                        startTime = System.currentTimeMillis();

                        d = (int) event.getY() - y;

                        deltaY += d;
                        y = (int) event.getY();

                        if (deltaY > 0) {
                            deltaY = 0;
                        }

                        if (deltaY < -((instructions.size() - numberOfBlankInstructions) * rowHeight - (height - height2 / 4 * 3) + drawHeight)) {
                            deltaY = -((instructions.size() - numberOfBlankInstructions) * rowHeight - (height - height2 / 4 * 3) + drawHeight);
                        }

                        k1 = (int) Math.abs(deltaY / (1f * bitmapHeight));

                        k2 = k1 + (height - drawHeight) / rowHeight + 1;
                        if (k2 >= numberOfBitmaps) {
                            k2 = numberOfBitmaps - 1;
                        }
                    }

                    if (isDownUp) {
                        swipeHeight += (int) event.getY() - y;
                        y = (int) event.getY();

                        if (swipeHeight < 0) {
                            swipeHeight = 0;
                        }
                    }

                    if (isUp && item > -1) {
                        move = (int) Math.abs(event.getY() - yMove);

                        if (!isAnimationClickUp && move > 3 * metrics.density) {
                            animationStartTime = System.currentTimeMillis();
                            isAnimationClickUp = true;
                            isAnimationClickDown = false;
                        }
                    }

                    invalidate();

                    break;
                case MotionEvent.ACTION_UP:
                    if (isDownUp) {
                        isDownUp = false;

                        if (swipeHeight == 0) {
                            playSoundEffect(android.view.SoundEffectConstants.CLICK);
                            goDown();
                        } else if (Math.abs(swipeHeight) > (height - drawHeight) / 5) {
                            goDown();
                        } else {
                            goUp2();
                        }
                    } else {
                        percentOfAnimation2 = 0;
                        animationStartTime2 = System.currentTimeMillis();
                        isAnimationMove = true;
                    }

                    if (isUp) {
                        if (!isAnimationClickUp && mapView != null && item > -1) {
                            playSoundEffect(android.view.SoundEffectConstants.CLICK);

                            mapListener.addDirection(instructions.get(item));

                            final MapPos mapPos = instructions.get(item).getLocation();

                            mapView.setFocusPos(mapPos, 0.6f);
                            mapView.setZoom(15, 0.6f);

                            final int tmpItem = item;

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    mapView.setMapRotation(360 - instructions.get(tmpItem).getAzimuth(), mapPos, 0.6f);
                                }
                            }, 600);

                            animationStartTime = System.currentTimeMillis();
                            isAnimationClickUp = true;
                            isAnimationClickDown = false;

                            invalidate();
                        }
                    }

                    break;
            }
        }

        if (isUp) {
            return true;
        } else {
            return false;
        }
    }

    public void setMapView(MapView mapView, MyMapEventListener mapListener) {
        this.mapView = mapView;
        this.mapListener = mapListener;
    }

    public double getDistance() {
        return distance;
    }

    public String getTime() {
        return time;
    }

    public ArrayList<RouteInstruction> getInstructions() {
        return instructions;
    }

    public void show(double distance, String time, ArrayList<RouteInstruction> instructions) {
        deltaY = 0;
        swipeHeight = 0;
        d = 0;

        setVisibility(VISIBLE);

        this.distance = distance;
        this.time = time;
        this.instructions = instructions;

        numberOfBlankInstructions = 0;

        while (true) {
            if (instructions.size() * bitmapHeight < height - drawHeight - height2 / 4 * 3) {
                instructions.add(new RouteInstruction("", 0, null, 0, null, 0));
                instructions.get(instructions.size() - 1).isBlank = true;
                numberOfBlankInstructions++;
            } else {
                break;
            }
        }

        createBitmap();

        k1 = 0;
        k2 = (height - drawHeight) / rowHeight + 1;

        if (k2 >= numberOfBitmaps) {
            k2 = numberOfBitmaps - 1;
        }
    }

    public void goUp() {
        if (instructions != null) {
            if (!isUp && !isAnimationUp && !isAnimationDown) {
                isUp = true;

                isAnimationDown = false;
                isAnimationDownFinished = false;

                heightAnimation = -height2 - swipeHeight;

                animationStartTime = System.currentTimeMillis();
                isAnimationUp = true;

                mapView.getOptions().setFocusPointOffset(new ScreenPos(0, height / 2 - drawHeight / 2));

                invalidate();
            }
        }
    }

    public void goUp2() {
        if (instructions != null) {
            if (!isAnimationUp && !isAnimationDown) {
                isUp = true;

                isAnimationDown = false;
                isAnimationDownFinished = false;

                animationStartTime = System.currentTimeMillis();
                isAnimationUp2 = true;

                mapView.getOptions().setFocusPointOffset(new ScreenPos(0, height / 2 - drawHeight / 2));

                invalidate();
            }
        }
    }

    public void goUp3() {
        if (instructions != null) {
            if (!isAnimationUp && !isAnimationDown) {
                isUp = true;

                isAnimationDown = false;
                isAnimationDownFinished = false;

                tmpSwipe = swipeHeight;

                animationStartTime = System.currentTimeMillis();
                isAnimationUp3 = true;

                mapView.getOptions().setFocusPointOffset(new ScreenPos(0, height / 2 - drawHeight / 2));

                invalidate();
            }
        }
    }

    public boolean isUp() {
        return isUp;
    }

    public void goDown() {
        if (!isAnimationDown && !isAnimationUp) {
            if (isUp) {
                isAnimationUpFinished = false;
                isAnimationUp = false;

                animationStartTime = System.currentTimeMillis();
                isAnimationDown = true;
                heightAnimation = -(height - height2 + shadowHeight - drawHeight);

                invalidate();
            }
        }
    }

    public void goDown2() {
        if (!isAnimationDown && !isAnimationUp) {
            isAnimationUpFinished = false;
            isAnimationUp = false;

            tmpSwipe = swipeHeight;

            animationStartTime = System.currentTimeMillis();
            isAnimationDown2 = true;

            invalidate();
        }
    }

    private void createBitmap() {
        int w = width;
        int h = height2 / 4 * 3 + shadowHeight / 2;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;

        bitmapTop = Bitmap.createBitmap(w, h, conf);

        c = new Canvas(bitmapTop);

        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        c.drawRect(0, shadowHeight, width, h - shadowHeight, paintShadow);

        c.drawText(time, leftMargin, h / 2 + paintRed.getTextSize() / 3, paintRed);

        Rect rect = new Rect();
        paintRed.getTextBounds(time, 0, time.length(), rect);

        c.drawText("  (" + formatDistance(distance) + ")", leftMargin + rect.width(), h / 2 + paintRed.getTextSize() / 3 + shadowHeight / 4, paintGray);

        numberOfBitmaps = instructions.size();

        invalidate();
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

    public void setMeasurementUnit(int measurementUnit) {
        this.measurementUnit = measurementUnit;

        if (instructions != null) {
            createBitmap();
        } else {
            invalidate();
        }
    }
}
