package com.nutiteq.app.map.async;

import android.os.AsyncTask;

import com.nutiteq.app.map.MainActivity;

/**
 * Created by aareundo on 07/10/16.
 */

public class ChangeMapStyle extends AsyncTask<Void, Void, Boolean> {

    MainActivity context;

    public  ChangeMapStyle(MainActivity context) {
        this.context = context;
    }
    /**
     * The system calls this to perform work in a worker thread and delivers
     * it the parameters given to AsyncTask.execute()
     *
     * @return
     */
    protected Boolean doInBackground(Void... q) {
        context.updateBaseLayer();
        return true;
    }

    /**
     * The system calls this to perform work in the UI thread and delivers
     * the result from doInBackground()
     */
    protected void onPostExecute(Boolean b) {
        // nothing to do
    }
}
