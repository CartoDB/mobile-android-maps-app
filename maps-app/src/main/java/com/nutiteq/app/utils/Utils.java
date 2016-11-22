package com.nutiteq.app.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;

import java.util.List;

/**
 * Created by aareundo on 07/10/16.
 */

public class Utils {

    /**
     * Checks availability of speech recognizing Activity
     *
     * @param callerActivity � Activity that called the checking
     * @return true � if Activity there available, false � if Activity is
     * absent
     */
    public static boolean isSpeechRecognitionActivityPresented(Activity callerActivity) {

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
}
