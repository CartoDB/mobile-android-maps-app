package com.nutiteq.nuticomponents.customviews;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class SearchHamburgerView extends EditText {

    private Activity mainActivity;
    private Class<?> searchableClass;

    private DrawerLayout hamburgerMenuLayout = null;
    private AnimatedExpandableListView hamburgerList;

    public SearchHamburgerView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String result = SearchHamburgerView.this.getText().toString();
                    SearchHamburgerView.this.setText("");

                    hideKeyboard();

                    hamburgerMenuLayout.closeDrawer(hamburgerList);

                    if (result.equals("")) {
                        return false;
                    } else {

                        if (mainActivity == null) {
                            mainActivity = (Activity)context;
                        }

                        Intent intent = new Intent(Intent.ACTION_SEARCH);
                        intent.setClass(mainActivity, searchableClass);
                        intent.putExtra(SearchManager.QUERY, result);

                        mainActivity.startActivityForResult(intent, 2);

                        return true;
                    }
                }

                return false;
            }
        });
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            hideKeyboard();
        }

        return super.dispatchKeyEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            setCursorVisible(true);
        }
        return super.onTouchEvent(event);
    }

    /**
     * Set main activity on which this component is attached and your .class for
     * searchable activity
     */
    public void setObjects(Activity mainActivity, Class<?> searchableClass) {
        this.mainActivity = mainActivity;
        this.searchableClass = searchableClass;
    }

    public void setHamburgerMenu(DrawerLayout hamburgerMenuLayout,
                                 AnimatedExpandableListView hamburgerList) {
        this.hamburgerMenuLayout = hamburgerMenuLayout;
        this.hamburgerList = hamburgerList;
    }

    public void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) mainActivity
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mainActivity.getCurrentFocus()
                    .getWindowToken(), 0);
        } catch (Exception e) {

        }
        setCursorVisible(false);
    }
}
