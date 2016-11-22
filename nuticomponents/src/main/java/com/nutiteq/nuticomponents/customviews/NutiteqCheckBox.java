package com.nutiteq.nuticomponents.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.CheckBox;

public class NutiteqCheckBox extends CheckBox {

    private int width;
    private int height;

    private boolean isChecked = false;

    private int radius;

    private DisplayMetrics metrics;

    private Paint paintCircle = new Paint();
    private Paint paintRoundRect = new Paint();

    private RectF rectF;

    private long animationStartTime;

    private boolean isAnimation = false;
    private boolean isAnimationFinished = false;

    private float percentOfAnimation;

    private float animationDuration = 240;

    private int deltaX = 0;

    private int r;
    private int g;
    private int b;

    private int r2;
    private int g2;
    private int b2;

    public NutiteqCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);

        metrics = getResources().getDisplayMetrics();

        radius = (int) (metrics.density * 9f);

        r = 217 - 22;
        g = 217 - 168;
        b = 217 - 112;

        r2 = 140 - 14;
        g2 = 140 - 112;
        b2 = 140 - 76;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        rectF = new RectF(0, height / 2 - radius / 3 * 2, width - 7 * radius / 3, height / 2 + radius / 3 * 2);

        isChecked = isChecked();

        if (isChecked) {
            paintCircle.setColor(Color.rgb(14, 112, 76));
            paintCircle.setAntiAlias(true);

            paintRoundRect.setColor(Color.rgb(22, 168, 112));
            paintRoundRect.setAntiAlias(true);
        } else {
            paintCircle.setColor(Color.rgb(140, 140, 140));
            paintCircle.setAntiAlias(true);

            paintRoundRect.setColor(Color.rgb(217, 217, 217));
            paintRoundRect.setAntiAlias(true);
        }

        if (isChecked) {
            deltaX = width - 7 * radius / 3 - radius;
        } else {
            deltaX = radius;
        }

        this.setMeasuredDimension(width, height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (!isAnimation && isChecked != isChecked()) {
            animationStartTime = System.currentTimeMillis();

            isAnimation = true;
            isAnimationFinished = false;

            invalidate();
        } else {
            c.drawRoundRect(rectF, 8 * metrics.density, 8 * metrics.density, paintRoundRect);
            c.drawCircle(deltaX, height / 2, radius, paintCircle);
        }

        if (isAnimation) {
            if (isChecked) {
                if (isAnimationFinished) {
                    isAnimation = false;
                } else {
                    percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                            / (animationDuration * 1.0f);

                    if (percentOfAnimation >= 1) {
                        paintRoundRect.setColor(Color.rgb(217, 217, 217));
                        paintCircle.setColor(Color.rgb(140, 140, 140));
                        isAnimationFinished = true;
                        isChecked = false;
                        deltaX = radius;
                    } else {
                        paintRoundRect.setColor(
                                Color.rgb(22 + (int) (percentOfAnimation * (r)),
                                168 + (int) (percentOfAnimation * (g)),
                                112 + (int) (percentOfAnimation * (b))))
                        ;
                        paintCircle.setColor(
                                Color.rgb(14 + (int) (percentOfAnimation * (r2)),
                                112 + (int) (percentOfAnimation * (g2)),
                                76 + (int) (percentOfAnimation * (b2)))
                        );

                        deltaX = width - 7 * radius / 3 - radius - (int) (percentOfAnimation * (width - 7 * radius / 3 - 2 * radius));
                    }

                    invalidate();
                }
            } else {
                if (isAnimationFinished) {
                    isAnimation = false;
                } else {
                    percentOfAnimation = (System.currentTimeMillis() - animationStartTime)
                            / (animationDuration * 1.0f);

                    if (percentOfAnimation >= 1) {
                        paintRoundRect.setColor(Color.rgb(22, 168, 112));
                        paintCircle.setColor(Color.rgb(14, 112, 76));
                        isAnimationFinished = true;
                        isChecked = true;
                        deltaX = width - 7 * radius / 3 - radius;
                    } else {
                        paintRoundRect.setColor(
                                Color.rgb(217 - (int) (percentOfAnimation * (r)),
                                217 - (int) (percentOfAnimation * (g)),
                                217 - (int) (percentOfAnimation * (b)))
                        );

                        paintCircle.setColor(
                                Color.rgb(140 - (int) (percentOfAnimation * (r2)),
                                140 - (int) (percentOfAnimation * (g2)),
                                140 - (int) (percentOfAnimation * (b2)))
                        );

                        deltaX = radius + (int) (percentOfAnimation * (width - 7 * radius / 3 - 2 * radius));
                    }

                    invalidate();
                }
            }
        }
    }
}
