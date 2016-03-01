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

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.HttpHelper.AuthType;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AndroidMediaPlayer extends android.media.MediaPlayer implements MediaPlayer {

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private Context context;
    private SurfaceView videoView;
    private int lengthOffset; // meta item length - getDuration() in ms

    private int itemLength; // seconds
    public int getItemLength() {
        // prefer designated itemLength if known since getDuration() is inaccurate for HLS
        if (itemLength > 0)
            return itemLength;
        int d = getDuration();
        return d == -1 ? 0 : d / 1000;
    }

    private int playRate = 1;
    public int getPlayRate() { return playRate; }

    public AndroidMediaPlayer(Context context, SurfaceView videoView) {
        this.context = context;
        this.videoView = videoView;
        videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                setDisplay(holder);
            }
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        setOnErrorListener(mpListener);
        setOnCompletionListener(mpListener);
        setOnPreparedListener(mpListener);
        setOnSeekCompleteListener(mpListener);
        setOnInfoListener(mpListener);
        setOnBufferingUpdateListener(mpListener);
        setOnVideoSizeChangedListener(mpListener);
        setMaxPlayRate(64); // TODO pref
    }

    public void playMedia(Uri mediaUri, int metaLength, AuthType authType, List<String> options) throws IOException {
        lengthOffset = 0;
        itemLength = metaLength;
        Log.d(TAG, "Video designated length: " + itemLength);
        setDataSource(context, mediaUri);
        prepare();
        start();
    }

    public void playMedia(FileDescriptor fileDescriptor, int metaLength, List<String> options) throws IOException {
        lengthOffset = 0;
        itemLength = metaLength;
        Log.d(TAG, "Video designated length: " + itemLength);
        setDataSource(fileDescriptor);
        prepare();
        start();
    }

    public String getVersion() {
        return String.valueOf(AppSettings.getAndroidVersion());
    }

    public boolean isProxying() {
        return false;
    }

    public void play() {
        playRate = 1;
        start();
    }

    @Override
    public void pause() throws IllegalStateException {
        playRate = 0;
        super.pause();
    }

    @Override
    public boolean isItemSeekable() {
        return getDuration() != -1;
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        return getCurrentPosition() / 1000;
    }

    public void setSeconds(int secs) {
        // use skip() to handle lengthOffset
        int delta = secs - getSeconds();
        skip(delta);
    }

    public void skip(int delta) {
        doSkip(delta);
    }

    /**
     * @return new position in ms
     */
    public int doSkip(int delta) {
        int curPos = getCurrentPosition();
        int newPos = curPos + (delta * 1000);

        if (lengthOffset != 0) {
            // correct for inaccurate duration (more off toward end of stream)
            int correctionMs = (int) (((float)newPos/(float)(itemLength*1000))*(lengthOffset));
            newPos = curPos + (delta * 1000) - correctionMs;
        }

        if (newPos < 0) {
            newPos = 0;
        }
        else {
            int lenMs = getItemLength() * 1000;
            if (newPos > lenMs)
                newPos = lenMs - 1;
        }

        seekTo(newPos);
        return newPos;
    }

    private int maxPlayRate = 1;
    /**
     * Max multiplier for fast-forward and rewind.
     */
    public int getMaxPlayRate() {
        if (isItemSeekable())
            return maxPlayRate;
        else
            return 1;
    }
    public void setMaxPlayRate(int maxRate) {
        if (maxRate <= 0)
            this.maxPlayRate = 1;
        else
            this.maxPlayRate = maxRate;
    }

    // TODO: fast forward and rewind handling is duplicated in VlcMediaPlayer
    /**
     * Step up the fast-forward rate by a factor of two
     * (resets playRate = +2 if maxPlayRate would be exceeded).
     * @return the new playRate
     */
    public int stepUpFastForward() {
        if (isItemSeekable()) {
            if (isPlaying())
                super.pause(); // avoid setting playRate = 0 in this.pause()

            int newPlayRate = 2;
            if (playRate > 1) {
                newPlayRate = playRate * 2;
                if (newPlayRate > maxPlayRate)
                    newPlayRate = 2;
            }

            if (newPlayRate > 1 && playRate <= 1) {
                if (shiftListener != null)
                    shiftListener.onShift(0);
                fastForwardHandler.postDelayed(fastForwardAction, 100);
            }

            playRate = newPlayRate;

        }

        return playRate;
    }

    private Handler fastForwardHandler = new Handler();
    private Runnable fastForwardAction = new Runnable() {
        public void run() {
            if (!isReleased() && playRate > 1) {
                skip(playRate);
                if (shiftListener != null)
                    shiftListener.onShift(playRate);
                fastForwardHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Step up the rewind rate by a factor of two
     * (resets playRate = -2 if maxPlayRate would be exceeded).
     * @return the new playRate
     */
    public int stepUpRewind() {
        if (isItemSeekable()) {
            if (isPlaying())
                super.pause(); // avoid setting playRate = 0 in this.pause()

            int newPlayRate = -2;

            if (playRate < 0) {
                newPlayRate = playRate * 2;
                if (-newPlayRate > maxPlayRate)
                    newPlayRate = -2;
            }

            if (newPlayRate < 0 && playRate >= 0) {
                if (shiftListener != null)
                    shiftListener.onShift(0);
                rewindHandler.postDelayed(rewindAction, 100);
            }

            playRate = newPlayRate;
        }

        return playRate;
    }

    private Handler rewindHandler = new Handler();
    private Runnable rewindAction = new Runnable() {
        public void run() {
            if (!isReleased() && playRate < 0) {
                int begin = doSkip(playRate);
                if (begin == 0) {
                    play();
                }
                else {
                    if (shiftListener != null)
                        shiftListener.onShift(playRate);
                    rewindHandler.postDelayed(this, 1000);
                }
            }
        }
    };

    private boolean released;

    @Override
    public void doRelease() {
        super.release();
        timingAction = null;
        released = true;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    private MediaPlayerShiftListener shiftListener;
    public void setMediaPlayerShiftListener(MediaPlayerShiftListener listener) {
        this.shiftListener = listener;
    }

    private MediaPlayerLayoutChangeListener layoutChangeListener;
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener) {
        this.layoutChangeListener = listener;
    }

    private MediaPlayerEventListener eventListener;
    public void setMediaPlayerEventListener(MediaPlayerEventListener listener) {
        this.eventListener = listener;
    }

    private AndroidMediaPlayerListener mpListener = new AndroidMediaPlayerListener();

    private class AndroidMediaPlayerListener implements android.media.MediaPlayer.OnErrorListener,
      android.media.MediaPlayer.OnPreparedListener, android.media.MediaPlayer.OnCompletionListener,
      android.media.MediaPlayer.OnBufferingUpdateListener, android.media.MediaPlayer.OnSeekCompleteListener,
      android.media.MediaPlayer.OnVideoSizeChangedListener, android.media.MediaPlayer.OnInfoListener {

        private int duration;
        private int samples = 0;
        private int maxSamples = 10;

        public void onCompletion(android.media.MediaPlayer mp) {
            if (!isReleased() && eventListener != null)
                eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.end));
        }

        public void onPrepared(android.media.MediaPlayer mp) {
            if (!isReleased() && eventListener != null) {
                if (timingAction == null) {
                    timingAction = new Runnable() {
                        public void run() {
                            if (!isReleased()) {
                                if (duration <= 0 && samples++ <= maxSamples) {
                                    duration = getDuration();
                                    if (duration > 0 && itemLength > 0) {
                                        // correct duration inaccuracy based on meta length
                                        lengthOffset = (itemLength * 1000) - duration;
                                        Log.i(TAG, "Adjusting length by offset: " + lengthOffset);
                                    }
                                }
                                if (eventListener != null)
                                    eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.time));
                                timingHandler.postDelayed(this, 1000);
                            }
                        }
                    };
                    timingHandler.postDelayed(timingAction, 1000);
                }
                eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.playing));
            }
        }

        public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
            MediaPlayerEvent event = new MediaPlayerEvent(MediaPlayerEventType.error);
            event.message = mp.getClass().getName() + "Error: " + what + " (" + extra + ")";
            if (!isReleased() && eventListener != null)
                eventListener.onEvent(event);
            else
                Log.e(TAG, event.message);
            return true;
        }

        public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
            MediaPlayerEvent event = new MediaPlayerEvent(MediaPlayerEventType.error);
            event.position = (percent/100) * getItemLength();
        }

        public boolean onInfo(android.media.MediaPlayer mp, int what, int extra) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "INFO: what: " + what + ", extra: " + extra);
            return false;
        }

        public void onVideoSizeChanged(android.media.MediaPlayer mp, int width, int height) {
            if (layoutChangeListener != null)
                layoutChangeListener.onLayoutChange(width, height, 1, 1);
        }

        public void onSeekComplete(android.media.MediaPlayer mp) {
            // TODO Auto-generated method stub
        }
    }

    private Handler timingHandler = new Handler();
    private Runnable timingAction;

}
