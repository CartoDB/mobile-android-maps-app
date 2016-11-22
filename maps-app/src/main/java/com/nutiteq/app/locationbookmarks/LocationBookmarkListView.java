package com.nutiteq.app.locationbookmarks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ListView;


public class LocationBookmarkListView extends ListView {

    private int width;

    private Paint paintShadow;

    private int shadowHeight;

    private Bitmap bitmap;

    public LocationBookmarkListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        width = metrics.widthPixels;

        paintShadow = new Paint();
        paintShadow.setColor(Color.rgb(224, 224, 224));
        paintShadow.setAntiAlias(true);
        paintShadow.setStrokeWidth(metrics.density * 1f);

        shadowHeight = (int) (metrics.density * 2f);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            shadowHeight = (int) (metrics.density * 2f);

            paintShadow.setShadowLayer(shadowHeight, 0.0f, 0.0f, Color.rgb(204, 204, 204));
        }

        createBitmap();
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        super.dispatchDraw(c);

        c.drawBitmap(bitmap, 0, 0, paintShadow);
    }

    private void createBitmap() {
        int w = width;
        int h = shadowHeight;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types

        bitmap = Bitmap.createBitmap(w, h, conf);

        Canvas c = new Canvas(bitmap);

        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        c.drawLine(0, 0, width, 0, paintShadow);
    }
}
