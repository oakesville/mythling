/**
 * Copyright 2014 Donald Oakes
 *
 * This file is part of Mythling.
 *
 * Mythling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mythling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mythling.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.oakesville.mythling.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * A utility class that helps with showing and hiding system UI such as the
 * status bar and navigation/system bar.
 * For more on system bars, see <a href=
 * "http://developer.android.com/design/get-started/ui-overview.html#system-bars"
 * > System Bars</a>.
 *
 * @see android.view.View#setSystemUiVisibility(int)
 * @see android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN
 */
public class SystemUiHider {

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the status bar. If there is a navigation bar, show and
     * hide will toggle low profile mode.
     */
    public static final int FLAG_FULLSCREEN = 0x2;

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the navigation bar, if it's present on the device and
     * the device allows hiding it. In cases where the navigation bar is present
     * but cannot be hidden, show and hide will toggle low profile mode.
     */
    public static final int FLAG_HIDE_NAVIGATION = FLAG_FULLSCREEN | 0x4;

    protected Activity activity;
    protected View anchorView;

    /**
     * The current UI hider flags.
     *
     * @see #FLAG_FULLSCREEN
     * @see #FLAG_HIDE_NAVIGATION
     */
    protected int flags;

    /**
    * Flags for {@link View#setSystemUiVisibility(int)} to use when showing the system UI.
    */
    private int showFlags;

    /**
     * Flags for {@link View#setSystemUiVisibility(int)} to use when hiding the system UI.
     */
    private int hideFlags;

    /**
     * Flags to test against the first parameter in
     * {@link android.view.View.OnSystemUiVisibilityChangeListener#onSystemUiVisibilityChange(int)}
     * to determine the system UI visibility state.
     */
    private int testFlags;


    /**
     * Whether or not the system UI is currently visible. This is a cached value
     * from calls to {@link #hide()} and {@link #show()}.
     */
    private boolean visible = true;


    /**
     * The current visibility callback.
     */
    protected OnVisibilityChangeListener onVisibilityChangeListener = dummyListener;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public SystemUiHider(Activity activity, View anchorView, int flags) {
        this.activity = activity;
        this.anchorView = anchorView;
        this.flags = flags;

        showFlags = View.SYSTEM_UI_FLAG_VISIBLE;
        hideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        testFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;

        if ((this.flags & FLAG_FULLSCREEN) != 0) {
            // If the client requested fullscreen, add flags relevant to hiding
            // the status bar. Note that some of these constants are new as of
            // API 16 (Jelly Bean). It is safe to use them, as they are inlined
            // at compile-time and do nothing on pre-Jelly Bean devices.
            showFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            hideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if ((this.flags & FLAG_HIDE_NAVIGATION) != 0) {
            // If the client requested hiding navigation, add relevant flags.
            showFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            hideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            testFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

    }

    /**
     * Sets up the system UI hider. Should be called from {@link Activity#onCreate}.
     */
    public void setup() {
        anchorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);
    }

    public boolean isVisible() {
        return visible;
    }

    public void hide() {
        anchorView.setSystemUiVisibility(hideFlags);
    }

    public void show() {
        anchorView.setSystemUiVisibility(showFlags);
    }

    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Registers a callback, to be triggered when the system UI visibility changes.
     */
    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        if (listener == null) {
            listener = dummyListener;
        }

        onVisibilityChangeListener = listener;
    }

    /**
     * Dummy no-op callback for use when there is no other listener set.
     */
    private static OnVisibilityChangeListener dummyListener = new OnVisibilityChangeListener() {
        public void onVisibilityChange(boolean visible) {
        }
    };

    /**
     * A callback interface used to listen for system UI visibility changes.
     */
    public interface OnVisibilityChangeListener {
        public void onVisibilityChange(boolean visible);
    }

    private View.OnSystemUiVisibilityChangeListener systemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int vis) {
        // Test against testFlags to see if the system UI is visible.
        if ((vis & testFlags) != 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // Pre-Jelly Bean, we must manually hide the action bar and use the old window flags API.
                activity.getActionBar().hide();
                activity.getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            // Trigger the registered listener and cache the visibility state.
            onVisibilityChangeListener.onVisibilityChange(false);
            visible = false;

        } else {
            anchorView.setSystemUiVisibility(showFlags);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // Pre-Jelly Bean, we must manually show the action bar and use the old window flags API.
                activity.getActionBar().show();
                activity.getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            // Trigger the registered listener and cache the visibility state.
            onVisibilityChangeListener.onVisibilityChange(true);
            visible = true;
        }
        }
    };
}
