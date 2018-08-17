/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Duplicated from Android ImageView, except upscales images if smaller than desired dims.
 */
public class ImageView extends android.support.v7.widget.AppCompatImageView {
    private boolean adjustViewBounds;
    private int maxWidth;
    private int maxHeight;

    public ImageView(Context context) {
        super(context);
    }

    public ImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setMaxWidth(int maxWidth) {
        super.setMaxWidth(maxWidth);
        this.maxWidth = maxWidth;
    }

    @Override
    public void setMaxHeight(int maxHeight) {
        super.setMaxHeight(maxHeight);
        this.maxHeight = maxHeight;
    }

    /**
     * Compatibility with pre api 16, since getAdjustViewBounds() was added in 16.
     */
    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        super.setAdjustViewBounds(adjustViewBounds);
        this.adjustViewBounds = adjustViewBounds;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable mDrawable = getDrawable();
        if (mDrawable == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int mDrawableWidth = mDrawable.getIntrinsicWidth();
        int mDrawableHeight = mDrawable.getIntrinsicHeight();

        float desiredAspect = 0.0f;

        boolean resizeWidth = false;
        boolean resizeHeight = false;

        final int actualWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int actualHeight = MeasureSpec.getSize(heightMeasureSpec);

        int w = mDrawableWidth;
        int h = mDrawableHeight;
        if (w <= 0)
            w = 1;
        if (h <= 0)
            h = 1;

        desiredAspect = (float) w / (float) h;

        if (adjustViewBounds) {
            // modified to resize regardless of MeasureSpec
            float actualAspect = (float) actualWidth / (float) actualHeight;
            if (actualAspect > desiredAspect)
                resizeWidth = true;
            else if (actualAspect < desiredAspect)
                resizeHeight = true;
        }

        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            widthSize = resolveAdjustedSize(w + pleft + pright, maxWidth, widthMeasureSpec);
            heightSize = resolveAdjustedSize(h + ptop + pbottom, maxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                float actualAspect = (float) (widthSize - pleft - pright) / (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {
                    boolean done = false;
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize - ptop - pbottom)) + pleft + pright;
                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect) + ptop + pbottom;
                        if (newHeight <= heightSize) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int maxSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = Math.min(desiredSize, maxSize);
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(Math.min(desiredSize, specSize), maxSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }
}
