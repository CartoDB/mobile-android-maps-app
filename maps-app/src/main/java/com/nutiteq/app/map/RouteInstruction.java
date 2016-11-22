package com.nutiteq.app.map;

import android.graphics.Bitmap;

import com.carto.core.MapPos;

public class RouteInstruction {

    public final static int START_BITMAP_TYPE = 1;
    public final static int END_BITMAP_TYPE = 2;
    public final static int LEFT_BITMAP_TYPE = 3;
    public final static int RIGHT_BITMAP_TYPE = 4;
    public final static int FORWARD_BITMAP_TYPE = 5;

    private String description;
    private double distance;
    private Bitmap bitmap;
    private MapPos location;
    private float azimuth;
    public boolean isBlank = false;

    // For orientation change
    // 1 - start
    // 2 - end
    // 3 - left
    // 4 - right
    // 5 - forward
    private int bitmapType;

    public RouteInstruction(String description, double distance, Bitmap bitmap, int bitmapType, MapPos location, float azimuth) {
        this.description = description;
        this.distance = distance;
        this.bitmap = bitmap;
        this.bitmapType = bitmapType;
        this.location = location;
        this.azimuth = azimuth;
    }

    public double getDistance() {
        return distance;
    }

    public String getDescription() {
        return description;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getBitmapType() {
        return bitmapType;
    }

    public MapPos getLocation() {
        return location;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setLocation(MapPos location) {
        this.location = location;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    public void setBitmapType(int bitmapType) {
        this.bitmapType = bitmapType;
    }
}
