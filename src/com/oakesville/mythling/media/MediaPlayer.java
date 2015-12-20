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
package com.oakesville.mythling.media;

import android.net.Uri;

public interface MediaPlayer {

    public enum MediaPlayerEvent {
        playing,
        seekable,
        time,
        position,
        paused,
        stopped,
        end,
        error
    }

    public interface MediaPlayerEventListener {
        public void onEvent(MediaPlayerEvent event);
    }

    public interface MediaPlayerShiftListener {
        public void onShift(int delta);
    }

    public interface MediaPlayerLayoutChangeListener {
        public void onLayoutChange(int width, int height, int sarNumerator, int sarDenominator);
    }

    /**
     * @param mediaUri
     */
    public void playMedia(Uri mediaUri);

    public boolean isPlaying();

    public void play();
    public void pause();

    /**
     * @return length in seconds (or zero if unknown)
     */
    public int getItemLength();
    public void setItemLength(int secs);

    /**
     * Calculate item length based on position/time.
     */
    public int inferItemLength();

    public boolean isItemSeekable();

    /**
     * @return video position in seconds (or zero if unknown)
     */
    public int getSeconds();

    public float getPosition();
    /**
     * Set the video position (only works if seekable).
     */
    public void setPosition(float pos);

    /**
     * Skip forward or backward.
     * @return past end
     */
    public boolean skip(int delta);

    /**
     * Step up the rewind rate by a factor of two
     * @return the new playRate
     */
    public int stepUpRewind();

    /**
     * Step up the fast-forward rate by a factor of two
     * @return the new playRate
     */
    public int stepUpFastForward();

    /**
     * Release player and associated native stuff.
     */
    public void doRelease();
    public boolean isReleased();

    public void setMediaPlayerEventListener(MediaPlayerEventListener listener);
    public void setMediaPlayerShiftListener(MediaPlayerShiftListener listener);
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener);

}
