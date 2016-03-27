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
import com.oakesville.mythling.util.MediaStreamProxy;
import com.oakesville.mythling.util.MediaStreamProxy.ProxyInfo;

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
    private boolean durationMismatch;
    public boolean isDurationMismatch() { return durationMismatch; }
    private MediaStreamProxy proxy;

    private int itemLength; // seconds
    public int getItemLength() {
        // prefer designated itemLength if known since getDuration() is inaccurate for HLS
        if (itemLength > 0)
            return itemLength;
        int d = getDuration();
        return d == -1 ? 0 : d / 1000;
    }

    public boolean supportsSeekCorrection() {
        return false;
    }

    /**
     * Currently just delays progress update since seek correction not supported.
     */
    private int seekCorrectionTolerance; // ms
    public int getSeekCorrectionTolerance() { return seekCorrectionTolerance; }
    public void setSeekCorrectionTolerance(int tolerance) { this.seekCorrectionTolerance = tolerance; }

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
        setScreenOnWhilePlaying(true);
        setMaxPlayRate(64); // TODO pref
    }

    /**
     * options not used
     */
    public void playMedia(Uri mediaUri, int metaLength, AuthType authType, List<String> mediaOptions) throws IOException {
        lengthOffset = 0;
        durationMismatch = false;
        itemLength = metaLength;
        Log.d(TAG, "Video designated length: " + itemLength);

        if (!PlaybackOptions.isHls(mediaUri) && mediaOptions.contains(AppSettings.PROXY_ANDROID_AUTHENTICATED_PLAYBACK)) {
            ProxyInfo proxyInfo = MediaStreamProxy.needsAuthProxy(mediaUri);
            if (proxyInfo != null) {
                // needs proxying to support authentication
                proxy = new MediaStreamProxy(proxyInfo, authType);
                proxy.init();
                proxy.start();
                String playUrl = "http://" + proxy.getLocalhost().getHostAddress() + ":" + proxy.getPort() + mediaUri.getEncodedPath();
                if (mediaUri.getQuery() != null)
                    playUrl += "?" + mediaUri.getQuery();
                Log.i(TAG, "Media proxy URL: " + playUrl);
                mediaUri = Uri.parse(playUrl);
            }
        }

        setDataSource(context, mediaUri);
        prepare();
        start();
    }

    /**
     * options not used
     */
    public void playMedia(FileDescriptor fileDescriptor, int metaLength, List<String> mediaOptions) throws IOException {
        lengthOffset = 0;
        durationMismatch = false;
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
        return proxy != null;
    }

    public void play() {
        playRate = 1;
        start();
    }

    public void pause() throws IllegalStateException {
        playRate = 0;
        super.pause();
    }

    public boolean isItemSeekable() {
        return getDuration() != -1 && proxy == null;
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        return getCurrentPosition() / 1000;
    }

    public void setSeconds(int secs) {
        if (!seekNeedsCorrection())
            seekTo(secs * 1000);
        else {
            if (!isTargeting())
                target(secs * 1000);
        }
    }

    public void skip(int delta) {
        int newPos = getCurrentPosition() + delta * 1000;
        if (newPos < 0) {
            seekTo(0);
        }
        else if (itemLength > 0 && newPos > itemLength * 1000) {
            seekTo(itemLength * 1000);
        }
        else {
            if (seekNeedsCorrection()) {
                if (!isTargeting())
                    target(newPos);
            }
            else {
                seekTo(newPos);
            }
        }
    }

    private boolean seekNeedsCorrection() {
        return lengthOffset != 0 && seekCorrectionTolerance > 0;
    }

    /**
     * Does not actually retry seek, but returns isTargeting() == true
     * until target is reached or timeout (overloading pref seekCorrectionTolerance) occurs.
     * This prevents confusing seek bar position jumps.
     */
    private void target(int t) {
        target = t;
        // try to correct for inaccurate duration
        float frac = (float)target/(itemLength*1000);
        int newPos = (int)(frac*getDuration());
        seekTo(newPos);
        targetTimeoutHandler.removeCallbacks(targetTimeoutAction);
        targetTimeoutHandler.postDelayed(targetTimeoutAction, seekCorrectionTolerance);
    }

    private void clearTarget() {
        target = -1;
    }

    private Handler targetTimeoutHandler = new Handler();
    private Runnable targetTimeoutAction = new Runnable() {
        public void run() {
            if (target >= 0)
                eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.seek));
            clearTarget();
            Log.d(TAG, "Seek target timed out after " + seekCorrectionTolerance + " ms");
        }
    };

    private int target; // ms
    public boolean isTargeting() {
        return target > 0 || (playRate != 1 && playRate != 0);
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

    private int shiftPos = 0; // ms

    /**
     * Step up the fast-forward rate by a factor of two
     * (resets playRate = +2 if maxPlayRate would be exceeded).
     */
    public void stepUpFastForward() {
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
                shiftPos = getCurrentPosition();
                if (shiftListener != null)
                    shiftListener.onShift(0);
                fastForwardHandler.post(fastForwardAction);
            }

            playRate = newPlayRate;

        }
    }

    private Handler fastForwardHandler = new Handler();
    private Runnable fastForwardAction = new Runnable() {
        public void run() {
            if (!isReleased()) {
                if (playRate <= 1) {
                    // fast forward done
                    skip((shiftPos - getCurrentPosition()) / 1000);
                }
                else {
                    // fast-forwarding
                    shiftPos = shiftPos + playRate * 1000;
                    if (shiftPos >= getItemLength() * 1000) {
                        stop();
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.end));
                    }
                    else {
                        if (!seekNeedsCorrection()) {
                            // go ahead and seek to show frame
                            seekTo(shiftPos);
                        }
                        fastForwardHandler.postDelayed(this, 1000);
                        if (shiftListener != null)
                            shiftListener.onShift(playRate);
                    }
                }
            }
        }
    };

    /**
     * Step up the rewind rate by a factor of two
     * (resets playRate = -2 if maxPlayRate would be exceeded).
     */
    public void stepUpRewind() {
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
                shiftPos = getCurrentPosition();
                if (shiftListener != null)
                    shiftListener.onShift(0);
                rewindHandler.post(rewindAction);
            }

            playRate = newPlayRate;
        }
    }

    private Handler rewindHandler = new Handler();
    private Runnable rewindAction = new Runnable() {
        public void run() {
            if (!isReleased()) {
                if (playRate >= 0) {
                    // rewind done
                    skip((shiftPos - getCurrentPosition()) / 1000);
                }
                else {
                    // rewinding
                    shiftPos = shiftPos + playRate * 1000;
                    if (shiftPos <= 0) {
                        seekTo(0);
                        play();
                    }
                    else {
                        if (!seekNeedsCorrection()) {
                            // go ahead and seek to show frame
                            seekTo(shiftPos);
                        }
                        rewindHandler.postDelayed(this, 1000);
                    }
                    if (shiftListener != null)
                        shiftListener.onShift(playRate);
                }
            }
        }
    };

    private boolean released;

    @Override
    public void doRelease() {
        super.release();
        if (proxy != null)
            proxy.stop();
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
            if (proxy != null)
                proxy.stop();
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
                                        int itemLengthMs = itemLength * 1000;
                                        lengthOffset = itemLengthMs - duration;
                                        if (Math.abs((float)lengthOffset/itemLengthMs) > 0.01)
                                            durationMismatch = true;
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
            MediaPlayerEvent event = new MediaPlayerEvent(MediaPlayerEventType.buffered);
            event.position = (percent/100) * getItemLength();
            // TODO fire this event
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
            if (target > 0 && !isReleased()) {
                long d = target - getCurrentPosition();
                Log.d(TAG, "Seek delta ms: " + d);
                if (Math.abs(d) < seekCorrectionTolerance) {
                    clearTarget();
                    eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.seek));
                }
            }
        }
    }

    private Handler timingHandler = new Handler();
    private Runnable timingAction;

}
