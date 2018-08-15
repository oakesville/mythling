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

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import com.oakesville.mythling.util.HttpHelper.AuthType;

import android.net.Uri;

public interface MediaPlayer {

    public class MediaPlayerEvent {
        public MediaPlayerEvent(MediaPlayerEventType type) {
            this.type = type;
        }

        public MediaPlayerEventType type;
        public int position;
        public String message;
    }

    public enum MediaPlayerEventType {
        playing,
        time,
        seek,
        buffered,
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
        public void onLayoutChange(int width, int height, int aspectNumerator, int aspectDenominator);
    }

    public String getVersion();

    /**
     * @param metaLength length from mythtv database
     */
    public void playMedia(Uri mediaUri, int metaLength, AuthType authType, List<String> options) throws IOException;

    public void playMedia(FileDescriptor fileDescriptor, int metaLength, List<String> options) throws IOException;

    public boolean isPlaying();
    public boolean isProxying();
    public boolean isTargeting(); // wait for seek to settle

    public void play();
    public void pause();

    /**
     * @return length in seconds (or zero if unknown)
     */
    public int getItemLength();

    public boolean isItemSeekable();

    public boolean supportsSeekCorrection();
    public int getSeekCorrectionTolerance();
    public void setSeekCorrectionTolerance(int seekCorrectionTolerance);

    /**
     * Significant mismatch leading to inaccurate seeks.
     */
    public boolean isDurationMismatch();

    /**
     * @return video position in seconds (or zero if unknown)
     */
    public int getSeconds();

    /**
     * Set the video position (only works if seekable).
     */
    public void setSeconds(int secs);

    /**
     * Skip forward or backward.
     */
    public void skip(int delta);

    /**
     * Step up the rewind rate by a factor of two
     */
    public void stepUpRewind();

    /**
     * Step up the fast-forward rate by a factor of two
     */
    public void stepUpFastForward();

    public int getPlayRate();

    public int getVolume();
    public void volumeUp();
    public void volumeDown();

    public String nextAudioTrack();

    /**
     * Release player and associated native stuff.
     */
    public void doRelease();
    public boolean isReleased();

    public void setMediaPlayerEventListener(MediaPlayerEventListener listener);
    public void setMediaPlayerShiftListener(MediaPlayerShiftListener listener);
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener);

}
