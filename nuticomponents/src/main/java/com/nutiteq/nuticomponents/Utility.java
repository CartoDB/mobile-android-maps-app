package com.nutiteq.nuticomponents;

import android.content.res.AssetManager;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import com.carto.core.MapPos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Utility {

    public static boolean copyAssetToSDCard(AssetManager assetManager,
                                            String fileName, String toDir) throws IOException {

        // check if external storage is available
        if (!Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {
            Log.d(Const.LOG_TAG, "externel starage isn't available");
            return false;
        }

        InputStream in = assetManager.open(fileName);
        File outFile = new File(toDir, fileName);

        if (outFile.exists()) {
            Log.d(Const.LOG_TAG,
                    "file already exits: " + outFile.getAbsolutePath());
            return true;
        }

        // check if there is enough space on external storage
        long freeSpace = Environment.getExternalStorageDirectory()
                .getUsableSpace() / 1048576;// in MB
        // world_0_5.mbtile is 3.3 MB
        if (freeSpace < Const.EXTERNAL_STORAGE_MIN + 4) {
            Log.d(Const.LOG_TAG,
                    "there is no enough space on externel starage");
            return false;
        }

        OutputStream out = new FileOutputStream(outFile);
        copyFile(in, out);
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;

        Log.i(Const.LOG_TAG, "copy done to " + outFile.getAbsolutePath());

        return true;
    }

    public static boolean copyAssetToInternalMemory(AssetManager assetManager,
                                                    String fileName, String toDir) throws IOException {

        InputStream in = assetManager.open(fileName);
        File outFile = new File(toDir, fileName);

        if (outFile.exists()) {
            Log.d(Const.LOG_TAG,
                    "file already exits: " + outFile.getAbsolutePath());
            return true;
        }

        // check if there is enough space on external storage
        long freeSpace = new File(toDir).getFreeSpace() / 1048576;// in MB
        // world_0_5.mbtile is 3.3 MB
        if (freeSpace < Const.INTERNAL_STORAGE_MIN + 4) {
            Log.d(Const.LOG_TAG,
                    "there is no enough space on externel starage");
            return false;
        }

        OutputStream out = new FileOutputStream(outFile);
        copyFile(in, out);
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;

        Log.i(Const.LOG_TAG, "copy done to " + outFile.getAbsolutePath());

        return true;
    }

    private static void copyFile(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static String formatTimestampToDateFormat(long timestamp) {
        Date date = new Date(timestamp);

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd - HH:mm:ss", Locale.getDefault());

        return dateFormat.format(date);
    }

    // calculate distance in meters for two WGS84 point
    public static long calculateDistance(double userLat, double userLng,
                                         double venueLat, double venueLng) {
        final double AVERAGE_RADIUS_OF_EARTH = 6378137;

        double latDistance = Math.toRadians(userLat - venueLat);
        double lngDistance = Math.toRadians(userLng - venueLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat))
                * Math.cos(Math.toRadians(venueLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(AVERAGE_RADIUS_OF_EARTH * c);
    }

    public static String[] wrapText(String text, Paint paint, int wrapWidth,
                                    int currentX) {
        // wrapped text lines
        final List<String> wt = new LinkedList<String>();
        final int len = text.length();
        if (len == 0) {
            return new String[0];
        }

        int first = 0;
        int last = 0;
        boolean emptylineflag = false;
        for (int i = 0; i < len; i++) {
            // split at '\n'
            if (text.charAt(i) == '\n') {
                wt.add(text.substring(first, i));
                first = i + 1;
                last = i;
                emptylineflag = false;
                continue;
            }

            // update last if necessary
            if (text.charAt(i) == ' ') {
                last = i;
                // don't check to split - we would have splitted at the previous
                // char
                continue;
            }

            // check that wrapping is switched on
            if (wrapWidth < 0) {
                continue;
            }

            // check length, see if we should split
            float w = paint.measureText(text, first, i + 1);
            // we should not split if we printed nothing on the previous line
            if ((first == 0 && w + currentX > wrapWidth)
                    || (first != 0 && w > wrapWidth)) {
                // yep, we should split, first to last, then the rest
                // if last is equal to first, the first word doesn't fit, so add
                // an
                // empty string
                // but set a flag to avoid adding it again
                if (first < last || (first == last && !emptylineflag)) {
                    String s = text.substring(first, last);
                    wt.add(s);
                }
                if (first < last) {
                    // start with last+1
                    emptylineflag = false;
                    first = last + 1;
                } else {
                    emptylineflag = true;
                }
            }
        }

        // add the last string
        wt.add(text.substring(first));

        return wt.toArray(new String[wt.size()]);
    }

    // MercatorMeters result between two points, base projection arguments, only can be used for two points which are really close to each other
    public static double distanceBetweenPoints(MapPos point1, MapPos point2) {
        double dx = point1.getX() - point2.getX();
        double dy = point1.getY() - point2.getY();

        return Math.sqrt(dx * dx + dy * dy);
    }
}
