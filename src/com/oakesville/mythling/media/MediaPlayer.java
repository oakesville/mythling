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

    class MediaPlayerEvent {
        public MediaPlayerEvent(MediaPlayerEventType type) {
            this.type = type;
        }

        public final MediaPlayerEventType type;
        public int position;
        public String message;
    }

    enum MediaPlayerEventType {
        playing,
        time,
        seek,
        buffered,
        paused,
        stopped,
        end,
        error
    }

    interface MediaPlayerEventListener {
        void onEvent(MediaPlayerEvent event);
    }

    interface MediaPlayerShiftListener {
        void onShift(int delta);
    }

    interface MediaPlayerLayoutChangeListener {
        void onLayoutChange(int width, int height, int aspectNumerator, int aspectDenominator);
    }

    String getVersion();

    /**
     * @param metaLength length from mythtv database
     */
    void playMedia(Uri mediaUri, int metaLength, AuthType authType, List<String> options) throws IOException;

    void playMedia(FileDescriptor fileDescriptor, int metaLength, List<String> options) throws IOException;

    boolean isPlaying();
    boolean isProxying();
    boolean isTargeting(); // wait for seek to settle

    void play();
    void pause();

    /**
     * @return length in seconds (or zero if unknown)
     */
    int getItemLength();

    boolean isItemSeekable();

    boolean supportsSeekCorrection();
    int getSeekCorrectionTolerance();
    void setSeekCorrectionTolerance(int seekCorrectionTolerance);

    /**
     * Significant mismatch leading to inaccurate seeks.
     */
    boolean isDurationMismatch();

    /**
     * @return video position in seconds (or zero if unknown)
     */
    int getSeconds();

    /**
     * Set the video position (only works if seekable).
     */
    void setSeconds(int secs);

    /**
     * Skip forward or backward.
     */
    void skip(int delta);

    /**
     * Step up the rewind rate by a factor of two
     */
    void stepUpRewind();

    /**
     * Step up the fast-forward rate by a factor of two
     */
    void stepUpFastForward();

    int getPlayRate();

    int getVolume();
    void volumeUp();
    void volumeDown();

    String nextAudioTrack();

    /**
     * Release player and associated native stuff.
     */
    void doRelease();
    boolean isReleased();

    void setMediaPlayerEventListener(MediaPlayerEventListener listener);
    void setMediaPlayerShiftListener(MediaPlayerShiftListener listener);
    void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener);

}
