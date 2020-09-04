package org.tokend.template.features.settings.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Source: https://github.com/consp1racy/android-support-preference
 * <br/>
 * License: Apache License v2.0
 */
public class PreferenceDividerDecoration extends RecyclerView.ItemDecoration {

    private final Drawable mDivider;
    private final int mDividerHeight;
    private boolean mDrawTop = false;
    private boolean mDrawBottom = false;
    private boolean mDrawBetweenItems = true;
    private boolean mDrawBetweenCategories = true;
    private int mPaddingLeft;
    private int mPaddingRight;

    public PreferenceDividerDecoration(Drawable divider, int dividerHeight) {
        mDivider = divider;
        mDividerHeight = dividerHeight;
    }

    public PreferenceDividerDecoration(Context context, @DrawableRes int divider, @DimenRes int dividerHeight) {
        mDivider = ContextCompat.getDrawable(context, divider);
        mDividerHeight = context.getResources().getDimensionPixelSize(dividerHeight);
    }

    public boolean getDrawTop() {
        return mDrawTop;
    }

    /**
     * Controls whether to draw divider above the first item.
     *
     * @param drawTop
     * @return
     */
    public PreferenceDividerDecoration drawTop(boolean drawTop) {
        mDrawTop = drawTop;
        return this;
    }

    public boolean getDrawBottom() {
        return mDrawBottom;
    }

    /**
     * Controls whether to draw divider at the bottom of the last item.
     *
     * @param drawBottom
     * @return
     */
    public PreferenceDividerDecoration drawBottom(boolean drawBottom) {
        mDrawBottom = drawBottom;
        return this;
    }

    public boolean getDrawBetweenItems() {
        return mDrawBetweenItems;
    }

    public PreferenceDividerDecoration drawBetweenItems(boolean drawBetweenItems) {
        mDrawBetweenItems = drawBetweenItems;
        return this;
    }

    public boolean getDrawBetweenCategories() {
        return mDrawBetweenCategories;
    }

    public PreferenceDividerDecoration drawBetweenCategories(boolean drawBetweenCategories) {
        mDrawBetweenCategories = drawBetweenCategories;
        return this;
    }

    public PreferenceDividerDecoration setPaddingLeft(int paddingLeft) {
        this.mPaddingLeft = paddingLeft;
        return this;
    }

    public PreferenceDividerDecoration setPaddingRignt(int paddingRignt) {
        this.mPaddingRight = paddingRignt;
        return this;
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft() + mPaddingLeft;
        int right = parent.getWidth() - parent.getPaddingRight() - mPaddingRight;

        final PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) parent.getAdapter();
        final int adapterCount = adapter.getItemCount();

        boolean wasLastPreferenceGroup = false;
        for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++) {
            final View child = parent.getChildAt(i);

            final int adapterPosition = parent.getChildAdapterPosition(child);
            Preference preference = adapter.getItem(adapterPosition);

            boolean skipNextAboveDivider = false;
            if (adapterPosition == 0) {
                if (mDrawTop) {
                    drawAbove(c, left, right, child);
                }
                skipNextAboveDivider = true;
            }

            if (preference instanceof PreferenceGroup
                    && !(preference instanceof PreferenceScreen)) {
                if (mDrawBetweenCategories) {
                    if (!skipNextAboveDivider) {
                        drawAbove(c, left, right, child);
                        skipNextAboveDivider = true;
                    }
                }
                wasLastPreferenceGroup = true;
            } else {
                if (mDrawBetweenItems && !wasLastPreferenceGroup) {
                    if (!skipNextAboveDivider) {
                        drawAbove(c, left, right, child);
                        skipNextAboveDivider = true;
                    }
                }
                wasLastPreferenceGroup = false;
            }

            if (adapterPosition == adapterCount - 1) {
                if (mDrawBottom) {
                    drawBottom(c, left, right, child);
                }
            }
        }
    }

    private void drawAbove(Canvas c, int left, int right, View child) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int top = child.getTop() - params.topMargin - mDividerHeight;
        final int bottom = top + mDividerHeight;
        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);
    }

    private void drawBottom(Canvas c, int left, int right, View child) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int top = child.getBottom() + params.bottomMargin - mDividerHeight;
        final int bottom = top + mDividerHeight;
        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(c);
    }
}
