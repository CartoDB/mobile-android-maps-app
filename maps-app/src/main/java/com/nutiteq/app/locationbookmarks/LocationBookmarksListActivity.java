package com.nutiteq.app.locationbookmarks;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.nutiteq.app.utils.Const;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.nutimap3d.dev.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocationBookmarksListActivity extends Activity {

    private ListView listView;

    private LocationBookmarksDB locationBookmarkDB;

    private ArrayList<LocationBookmark> locationBookmarks;

    private ArrayList<Drawable> searchPinDrawables;

    private String selectedLocation = "";

    private int numberOfBookmarks = 0;

    private int itemPosition = 0;

    private Bitmap addFavorite;

    private Canvas c;
    private Paint paintCircle = new Paint();

    private Firebase myFirebase;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationBookmarkDB = new LocationBookmarksDB(this);
        if (!locationBookmarkDB.isOpen()) {
            locationBookmarkDB.open();
        }

        paintCircle.setAntiAlias(true);

        addFavorite = BitmapFactory.decodeResource(getResources(),
                R.drawable.add_favorite);

        myFirebase = new Firebase(Const.FIREBASE_URL);

        setContentView(R.layout.location_bookmark_activity);

        listView = (ListView) findViewById(R.id.listView);

        init();

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int pos,
                                    long id) {
                MainActivity.locationBookmarkId = locationBookmarks.get(pos).getId();

                finish();
            }
        });

        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v,
                                           final int pos, long l) {
                selectedLocation = locationBookmarks.get(pos).getDescription();

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        LocationBookmarksListActivity.this);

                builder.setTitle(getString(R.string.options));

                final String[] items = new String[2];
                items[0] = getString(R.string.edit);
                items[1] = getString(R.string.delete);

                builder.setItems(items, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        if (i == 0) {
                            final String firebaseNodeKey = locationBookmarkDB.getFirebaseNodeKey(locationBookmarks
                                    .get(pos).getId());

                            AlertDialog alertDialog = new AlertDialog.Builder(
                                    LocationBookmarksListActivity.this)
                                    .create();

                            final EditText input = new EditText(
                                    LocationBookmarksListActivity.this);
                            input.setText(selectedLocation);
                            input.setSelection(selectedLocation.length());

                            alertDialog.setTitle(getString(R.string.edit));
                            alertDialog.setView(input);
                            alertDialog.setButton(
                                    DialogInterface.BUTTON_POSITIVE,
                                    LocationBookmarksListActivity.this
                                            .getString(R.string.ok),
                                    new OnClickListener() {

                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            if (locationBookmarkDB.updateLocationBookmarkDescription(
                                                    locationBookmarks.get(pos).getId(),
                                                    input.getText().toString()
                                                            .trim())) {
                                                Map<String, Object> favorite = new HashMap<String, Object>();
                                                favorite.put("location", input.getText().toString()
                                                        .trim());

                                                myFirebase.child("favorite").child(MainActivity.primaryEmail).child(firebaseNodeKey).updateChildren(favorite);

                                                Toast.makeText(
                                                        getApplicationContext(),
                                                        getString(R.string.bookmark_updated),
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                                itemPosition = listView
                                                        .getFirstVisiblePosition();
                                                init();

                                                // refresh favorite locations on
                                                // map
                                                MainActivity.shouldRefreshFavoriteLocations = true;
                                            } else {
                                                Toast.makeText(
                                                        getApplicationContext(),
                                                        getString(R.string.bookmark_updated_error),
                                                        Toast.LENGTH_LONG)
                                                        .show();
                                            }
                                        }
                                    });
                            alertDialog.setButton(
                                    DialogInterface.BUTTON_NEGATIVE,
                                    LocationBookmarksListActivity.this
                                            .getString(R.string.cancel),
                                    new OnClickListener() {

                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            // nothing to do here
                                        }
                                    });

                            alertDialog.show();
                        } else if (i == 1) {
                            String firebaseNodeKey = locationBookmarkDB.getFirebaseNodeKey(locationBookmarks
                                    .get(pos).getId());

                            if (locationBookmarkDB
                                    .deleteLocationBookmark(locationBookmarks
                                            .get(pos).getId())) {
                                myFirebase.child("favorite").child(MainActivity.primaryEmail).child(firebaseNodeKey).removeValue();

                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.bookmark_deleted),
                                        Toast.LENGTH_LONG).show();
                                itemPosition = listView
                                        .getFirstVisiblePosition();
                                init();

                                // refresh favorite locations on map
                                MainActivity.shouldRefreshFavoriteLocations = true;
                            } else {
                                Toast.makeText(
                                        getApplicationContext(),
                                        getString(R.string.bookmark_delete_error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                builder.show();

                return true;
            }
        });

        // didn't find solution from xml
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setBackgroundDrawable(
                    new ColorDrawable(Color.parseColor(Const.NUTITEQ_GREEN)));
            listView.setSelector(R.xml.selector);
            listView.setBackgroundColor(Color.WHITE);

            getActionBar().setHomeButtonEnabled(true);

            getActionBar().setIcon(
                    new ColorDrawable(getResources().getColor(android.R.color.transparent)));

            getActionBar().setTitle(Html.fromHtml("<b>&nbsp&nbsp&nbsp&nbsp " + getString(R.string.bookmarks_menu) + "</b>"));
        }
    }

    private void init() {
        locationBookmarks = locationBookmarkDB.getAllLocationBookmarks();
        searchPinDrawables = new ArrayList<Drawable>();

        numberOfBookmarks = locationBookmarks.size();

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < numberOfBookmarks; i++) {
            list.add(locationBookmarks.get(i).getDescription());

            searchPinDrawables.add(createDrawable(locationBookmarks.get(i).getRed(), locationBookmarks.get(i).getGreen(), locationBookmarks.get(i).getBlue()));
        }

        ListAdapter adapter = new ListAdapter(this,
                R.layout.location_bookmark_list, R.id.text2, list);

        listView.setAdapter(adapter);

        listView.setSelection(itemPosition);

        listView.setDivider(null);
        listView.setDividerHeight(0);
    }

    private Drawable createDrawable(int r, int g, int b) {
        paintCircle.setColor(Color.rgb(r, g, b));

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int w = (int) (metrics.density * 44f);

        Bitmap bmp = Bitmap.createBitmap(w, w, conf);

        c = new Canvas(bmp);

        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        c.drawCircle(w / 2, w / 2, w / 2, paintCircle);

        c.drawBitmap(addFavorite, w / 2 - addFavorite.getWidth() / 2, w / 2 - addFavorite.getHeight() / 2, paintCircle);

        return new BitmapDrawable(getResources(), bmp);
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

    private static class ListItemHolder {
        ImageView image;
        TextView text;
        TextView text2;
    }

    public class ListAdapter extends ArrayAdapter<String> {

        Context context;
        int layoutResourceId;
        ArrayList<String> list;
        ListItemHolder holder;

        public ListAdapter(Context context, int layoutResourceId, int textRes,
                           ArrayList<String> list) {
            super(context, layoutResourceId, textRes, list);

            this.context = context;
            this.layoutResourceId = layoutResourceId;
            this.list = list;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            View row = convertView;
            holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context)
                        .getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ListItemHolder();
                holder.image = (ImageView) row.findViewById(R.id.image);
                holder.text = (TextView) row.findViewById(R.id.text);
                holder.text2 = (TextView) row.findViewById(R.id.text2);

                row.setTag(holder);
            } else {
                holder = (ListItemHolder) row.getTag();
            }

            holder.text.setText(getString(R.string.location) + " " + (position + 1));
            holder.text2.setText(list.get(position));
            // with setBackground case error because it's from API 16
            holder.image
                    .setBackgroundDrawable(searchPinDrawables.get(position));

            return row;
        }
    }
}
