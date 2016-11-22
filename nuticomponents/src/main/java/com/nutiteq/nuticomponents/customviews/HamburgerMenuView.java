package com.nutiteq.nuticomponents.customviews;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.nutiteq.nuticomponents.R;

public class HamburgerMenuView extends View implements View.OnTouchListener {

    private int width;
    private int height;

    private Bitmap bitmap;

    private Paint paint = new Paint();

    private DrawerLayout hamburgerMenuLayout = null;
    private AnimatedExpandableListView hamburgerList;

    private boolean isDown = false;

    public HamburgerMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);

        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.hamburger_menu);

        width = bitmap.getWidth();
        height = bitmap.getHeight();

        setOnTouchListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(width, height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawBitmap(bitmap, 0, 0, paint);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() > 0 && event.getX() < width && event.getY() > 0 && event.getY() < height) {
                    isDown = true;

                    return true;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getY() > width || event.getY() > height) {
                    isDown = false;

                    return false;
                }

                break;
            case MotionEvent.ACTION_UP:
                if (isDown) {
                    isDown = false;

                    playSoundEffect(android.view.SoundEffectConstants.CLICK);

                    hamburgerMenuLayout.openDrawer(hamburgerList);

                    return true;
                }

                break;
        }

        return false;
    }

    public void setHamburgerMenu(DrawerLayout hamburgerMenuLayout,
                                 AnimatedExpandableListView hamburgerList) {
        this.hamburgerMenuLayout = hamburgerMenuLayout;
        this.hamburgerList = hamburgerList;
    }
}
