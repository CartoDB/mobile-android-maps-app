package com.nutiteq.app.info;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nutiteq.app.utils.Const;
import com.nutiteq.app.nutimap3d.dev.R;
import com.carto.ui.BaseMapView;

public class InfoActivity extends Activity {

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_info);

        String versionName = "";

        try {
            versionName = getString(R.string.version)
                    + " "
                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView title = (TextView) findViewById(R.id.title1);
        title.setText(title.getText() + " " + versionName);

        // makes links to receive click, only with xml attributes it doesn't
        // work
        TextView tv = (TextView) findViewById(R.id.desc2);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        TextView device = (TextView) findViewById(R.id.device);
        device.setText(BaseMapView.getSDKVersion());

//        device.setText(Build.MANUFACTURER.toUpperCase(Locale.getDefault())
//                + " " + Build.MODEL + " " + Build.ID);

        Button b = (Button) findViewById(R.id.feedback);

        // didn't find solution from xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));

            getActionBar().setHomeButtonEnabled(true);

            b.setBackgroundResource(R.color.nutiteq_green);
            b.setTextColor(Color.WHITE);

            getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.menu_info) + "</b>"));
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
