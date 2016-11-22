package com.nutiteq.app.nutimap2;

import java.util.Locale;

import net.hockeyapp.android.FeedbackManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.nutiteq.app.nutimap3d.dev.R;

public class InfoActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_info);

		// makes links to receive click, only with xml attributes it doesn't
		// work
		TextView tv = (TextView) findViewById(R.id.desc2);
		tv.setMovementMethod(LinkMovementMethod.getInstance());

		TextView device = (TextView) findViewById(R.id.device);
		device.setText(Build.MANUFACTURER.toUpperCase(Locale.getDefault())
				+ " " + Build.MODEL + " " + Build.ID);

		Button b = (Button) findViewById(R.id.feedback);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showFeedbackActivity();
			}
		});

		// didn't find solution from xml
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(
					new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));
			b.setBackgroundResource(R.color.nutiteq_green);
			b.setTextColor(Color.WHITE);
		}
	}

	// show feedback activity with HOCKEY SDK
	public void showFeedbackActivity() {
		FeedbackManager.register(this, Const.HOCKEYAPP_ID, null);
		FeedbackManager.showFeedbackActivity(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
