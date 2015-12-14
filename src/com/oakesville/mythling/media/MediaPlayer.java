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
        paused,
        stopped,
        end,
        error
    }

    public interface MediaPlayerEventListener {
        public void onEvent(MediaPlayerEvent event);
    }

    /**
     * @param mediaUri
     * @param itemLength seconds (zero if unknown)
     */
    public void playMedia(Uri mediaUri, int itemLength);

    public void play();
    public void pause();

    /**
     * @return media position in seconds (or zero if unknown)
     */
    public int getSeconds();

    /**
     * Set the media position (only works if seekable).
     */
    public void setSeconds(int pos);

    /**
     * Seek forward or backward.
     * @return if successful the new position in seconds, otherwise zero
     */
    public int seek(int delta);

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

}
