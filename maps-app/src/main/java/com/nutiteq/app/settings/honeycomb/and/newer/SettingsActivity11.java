package com.nutiteq.app.settings.honeycomb.and.newer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;

import com.nutiteq.app.utils.Const;
import com.nutiteq.app.nutimap3d.dev.R;

public class SettingsActivity11 extends Activity {

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        // Didn't find solution from xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));

            getActionBar().setHomeButtonEnabled(true);

            getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.settings) + "</b>"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
