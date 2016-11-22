package com.nutiteq.app.map.hamburger;

import android.view.View;
import android.widget.ExpandableListView;

import com.nutiteq.app.map.MainActivity;

/*
 * The group click listener for AnimatedExpandableListView in the hamburger
 * menu
 */
public class HamburgerMenuGroupClickListener implements ExpandableListView.OnGroupClickListener {

    MainActivity context;

    public HamburgerMenuGroupClickListener(MainActivity context) {
        this.context = context;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

        if (groupPosition > 6) {
            // We call collapseGroupWithAnimation(int) and
            // expandGroupWithAnimation(int) to animate
            // group expansion/collapse.
            if (context.hamburgerMenuList.isGroupExpanded(groupPosition)) {
                context.hamburgerMenuList.collapseGroupWithAnimation(groupPosition);
            } else {
                context.hamburgerMenuList.expandGroupWithAnimation(groupPosition);
            }
        } else {
            // click actions
            v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
            context.groupItemClick(groupPosition);
        }

        return true;
    }
}

