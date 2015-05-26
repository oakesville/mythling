/**
 * Copyright 2015 Donald Oakes
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
package com.oakesville.mythling.firetv;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class FireTvViewPager extends ViewPager {

    private DpadMediaKeyHandler dpadMediaKeyHandler;
    public void setDpadMediaKeyHandler(DpadMediaKeyHandler dpadMediaKeyHandler) {
        this.dpadMediaKeyHandler = dpadMediaKeyHandler;
    }

    public FireTvViewPager(Context context) {
        super(context);
    }

    public FireTvViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean executeKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                boolean handled = dpadMediaKeyHandler.handleFastForward();
                if (handled)
                    return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_REWIND) {
                boolean handled = dpadMediaKeyHandler.handleRewind();
                if (handled)
                    return true;
            }
        }
        return super.executeKeyEvent(event);
    }

    public interface DpadMediaKeyHandler {
        boolean handleFastForward();
        boolean handleRewind();
    }
}
