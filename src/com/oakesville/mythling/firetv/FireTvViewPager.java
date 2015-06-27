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
