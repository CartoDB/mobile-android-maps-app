package com.nutiteq.app.map.hamburger;

/**
 * Created by aareundo on 07/10/16.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.carto.utils.BitmapUtils;
import com.nutiteq.app.map.MainActivity;
import com.nutiteq.app.map.async.ChangeMapStyle;
import com.nutiteq.app.nutimap3d.dev.R;
import com.nutiteq.app.search.SearchableActivity;
import com.nutiteq.app.utils.Utils;
import com.nutiteq.nuticomponents.customviews.AnimatedExpandableListView;
import com.nutiteq.nuticomponents.customviews.NutiteqCheckBox;
import com.nutiteq.nuticomponents.customviews.SearchHamburgerView;

import java.util.List;

import static com.nutiteq.app.map.MainActivity.*;

/**
 * Adapter for our list of {@link GroupItem}s.
 */
public class HamburgerAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter {

    private LayoutInflater inflater;

    private List<MainActivity.GroupItem> items;

    MainActivity context;

    public HamburgerAdapter(Context context) {

        inflater = LayoutInflater.from(context);
        this.context = (MainActivity)context;
    }

    public void setData(List<MainActivity.GroupItem> items) {
        this.items = items;
    }

    @Override
    public ChildItem getChild(int groupPosition, int childPosition) {
        return items.get(groupPosition).items.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
        ChildHolder holder;
        ChildItem item = getChild(groupPosition, childPosition);

        if (convertView == null) {
            holder = new ChildHolder();
            convertView = inflater.inflate(R.layout.hamburger_submenu_item, parent, false);

            holder.title = (TextView) convertView.findViewById(R.id.textTitle);

            convertView.setTag(holder);
        } else {
            holder = (ChildHolder) convertView.getTag();
        }

        holder.title.setText(item.title);

        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return items.get(groupPosition).items.size();
    }

    @Override
    public GroupItem getGroup(int groupPosition) {
        return items.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        final GroupHolder holder;
        final GroupItem item = getGroup(groupPosition);

        if (convertView == null) {
            holder = new GroupHolder();
            if (groupPosition == 0) {
                convertView = inflater.inflate(R.layout.hamburger_logo, parent, false);

                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.textTitle);

            } else if (groupPosition == 1) {

                convertView = inflater.inflate(R.layout.hamburger_menu_search, parent, false);

                SearchHamburgerView searchHamburgerView = (SearchHamburgerView) convertView.findViewById(R.id.search_text);
                searchHamburgerView.setObjects(context.mainActivity, SearchableActivity.class);
                searchHamburgerView.setHamburgerMenu(context.hamburgerMenuLayout, context.hamburgerMenuList);

                holder.image = (ImageView) convertView.findViewById(R.id.search);
                holder.editText = (SearchHamburgerView) convertView.findViewById(R.id.search_text);
                holder.image2 = (ImageView) convertView.findViewById(R.id.voice);

            } else if (groupPosition > 1 && groupPosition < 4) {

                convertView = inflater.inflate(R.layout.hamburger_menu_item, parent, false);

                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.textTitle);

            } else if (groupPosition == 4) {
                convertView = inflater.inflate(R.layout.hamburger_menu_map_style_item, parent, false);

                holder.title = (TextView) convertView.findViewById(R.id.textTitle);
                holder.checkBox = (NutiteqCheckBox) convertView.findViewById(R.id.bright_style);
            } else {
                convertView = inflater.inflate(R.layout.hamburger_menu_item2, parent, false);

                holder.title = (TextView) convertView.findViewById(R.id.textTitle);
            }

            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }

        if (groupPosition == 0) {
            holder.title.setText(item.title);
        } else if (groupPosition == 1) {

            holder.editText.setFocusable(true);
            holder.editText.requestFocus();

            holder.editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (count == 0) {
                        if (Utils.isSpeechRecognitionActivityPresented(context.mainActivity)) {
                            holder.image2.setImageResource(R.drawable.voice);

                            holder.image2.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    context.startVoiceForm();
                                }
                            });

                            holder.image2.setVisibility(View.VISIBLE);
                        } else {
                            holder.image2.setVisibility(View.GONE);
                        }
                    } else if (count > 0) {
                        holder.image2.setImageResource(R.drawable.clear);

                        holder.image2.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                holder.editText.setText("");
                            }
                        });

                        holder.image2.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            holder.image.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    holder.editText.requestFocus();

                    holder.editText.setFocusable(true);
                    holder.editText.requestFocus();
                    holder.editText.setCursorVisible(true);

                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(holder.editText, InputMethodManager.SHOW_IMPLICIT);
                }
            });

            if (Utils.isSpeechRecognitionActivityPresented(context.mainActivity)) {
                holder.image2.setVisibility(View.VISIBLE);

                holder.image2.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        context.startVoiceForm();
                    }
                });
            } else {
                holder.image2.setVisibility(View.GONE);
            }
        }

        if (groupPosition > 3) {
            holder.title.setText(item.title);
        } else if (groupPosition > 1 && groupPosition < 4) {
            holder.image.setImageDrawable(item.image);
            holder.title.setText(item.title);

            if (groupPosition > 1) {
                convertView.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                holder.image.setImageDrawable(item.imageHover);

                                break;
                            case MotionEvent.ACTION_UP:
                                holder.image.setImageDrawable(item.image);

                                break;
                        }

                        return false;
                    }
                });
            }
        }

        if (groupPosition == 4) {
            holder.checkBox.setChecked(context.isBrightStyle);

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(
                        CompoundButton buttonView, boolean isChecked) {

                    if (isChecked) {
                        setIsNotDarkStyle(false);
                    } else {
                        setIsNotDarkStyle(true);
                    }

                    if (!context.isNotDarkStyle) {
                        context.mapView.getOptions().setBackgroundBitmap(BitmapUtils
                                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                                        context.getResources(), R.drawable.white_tile)));
                    } else {
                        context.mapView.getOptions().setBackgroundBitmap(BitmapUtils
                                .createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(
                                        context.getResources(), R.drawable.dark_tile)));
                    }

                    ChangeMapStyle changeMapStyle = new ChangeMapStyle(context);
                    changeMapStyle.execute();

                    SharedPreferences.Editor editor = context.preferences.edit();
                    editor.putBoolean("isbrightstyle", isChecked);
                    editor.commit();
                }
            });
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }
}
