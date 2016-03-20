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
package com.oakesville.mythling.vlc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.IVLCVout.Callback;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.media.PlaybackOptions;
import com.oakesville.mythling.util.HttpHelper.AuthType;
import com.oakesville.mythling.util.MediaStreamProxy;
import com.oakesville.mythling.util.MediaStreamProxy.ProxyInfo;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

public class VlcMediaPlayer extends MediaPlayer implements com.oakesville.mythling.media.MediaPlayer {

    private static final String TAG = VlcMediaPlayer.class.getSimpleName();

    private int itemLength; // seconds
    public int getItemLength() {
        return itemLength;
    }

    public boolean isItemSeekable() {
        return itemLength > 0 && proxy == null; // proxied seek awaits issue #65
    }

    private boolean durationMismatch;
    public boolean isDurationMismatch() { return durationMismatch; }

    public boolean supportsSeekCorrection() {
        return true;
    }

    private int seekCorrectionTolerance; // ms
    public int getSeekCorrectionTolerance() { return seekCorrectionTolerance; }
    public void setSeekCorrectionTolerance(int tolerance) { this.seekCorrectionTolerance = tolerance; }

    private List<String> libVlcMediaOptions;
    private boolean isHls;

    /**
     * Proxy is needed for Digest and Basic auth since libVLC doesn't support.
     * Also to avoid MythTV services always setting Content-Type=video/mp2p for mpg files.
     */
    private MediaStreamProxy proxy;
    public boolean isProxying() {
        return proxy != null;
    }

    public int inferItemLength() {
        float p = getPosition();
        if (p > 0)
            return (int)(getTime() / (p * 1000));
        else
            return 0;
    }

    public VlcMediaPlayer(SurfaceView videoView, SurfaceView subtitlesView, List<String> libVlcOptions) {
        super(createLibVlc(libVlcOptions));
        libvlc.setOnHardwareAccelerationError(hardwareAccelerationErrorHandler);

        setMaxPlayRate(64); // TODO pref
        setEventListener(vlcEventListener);

        // video output
        final IVLCVout vout = getVLCVout();
        vout.setVideoView(videoView);
        if (subtitlesView != null)
            vout.setSubtitlesView(subtitlesView);
        vout.addCallback(nativeCallback);
        vout.attachViews();
    }

    public void playMedia(Uri mediaUri, int metaLength, AuthType authType, List<String> mediaOptions) throws IOException {
        itemLength = metaLength;
        Log.d(TAG, "Video designated length: " + itemLength);

        isHls = PlaybackOptions.isHls(mediaUri);
        ProxyInfo proxyInfo = MediaStreamProxy.needsAuthProxy(mediaUri);

        libVlcMediaOptions = getLibVlcMediaOptions(mediaOptions, mediaUri);

        Media media;
        if (proxyInfo == null) {
            media = new Media(libvlc, mediaUri);
        }
        else {
            // libvlc needs proxying to support authentication
            proxy = new MediaStreamProxy(proxyInfo, authType);
            proxy.init();
            proxy.start();
            String playUrl = "http://" + proxy.getLocalhost().getHostAddress() + ":" + proxy.getPort() + mediaUri.getPath();
            if (mediaUri.getQuery() != null)
                playUrl += "?" + mediaUri.getQuery();
            Log.i(TAG, "Media proxy URL: " + playUrl);
            media = new Media(libvlc, Uri.parse(playUrl));
        }

        if (libVlcMediaOptions.isEmpty()) {
            media.setHWDecoderEnabled(true, false);
            media.addOption(":network-caching=2500");
        }
        else {
            for (String mediaOption : libVlcMediaOptions)
                media.addOption(mediaOption);
        }

        setMedia(media);
        play();
    }

    public void playMedia(FileDescriptor fd, int metaLength, List<String> mediaOptions) {
        Media media = new Media(libvlc, fd);
        itemLength = metaLength;
        Log.d(TAG, "Video designated length: " + itemLength);
        isHls = false;
        libVlcMediaOptions = getLibVlcMediaOptions(mediaOptions, null);
        if (libVlcMediaOptions.isEmpty()) {
            media.setHWDecoderEnabled(true, false);
        }
        else {
            for (String mediaOption : libVlcMediaOptions) {
                media.addOption(mediaOption);
            }
        }
        setMedia(media);
        play();
    }

    /**
     * Weeds out pseudo-parameters, and format-specific options that don't apply for the given URI
     * (relies on file extension appearing last in the uri).
     */
    private List<String> getLibVlcMediaOptions(List<String> mediaOptions, Uri uri) {
        List<String> libVlcMediaOptions = new ArrayList<String>();
        for (String mediaOption : mediaOptions) {
            int colon = mediaOption.indexOf(':');
            if (colon == 0) {
                libVlcMediaOptions.add(mediaOption);
            }
            else if (colon > 0) {
                if (uri != null) {
                    String fileExt = mediaOption.substring(0, colon);
                    if (uri.toString().endsWith(fileExt))
                        libVlcMediaOptions.add(mediaOption.substring(colon));
                }
            }
        }
        return libVlcMediaOptions;
    }

    public void doRelease() {
        if (proxy != null)
            proxy.stop();
        if (libvlc == null)
            return;
        stop();
        final IVLCVout vout = getVLCVout();
        vout.removeCallback(nativeCallback);
        vout.detachViews();
        libvlc.release();
        libvlc = null;
        super.release();
    }

    private static LibVLC libvlc;
    private static LibVLC createLibVlc(List<String> mediaOptions) {
        // libvlc
        ArrayList<String> options = new ArrayList<String>();
        if (mediaOptions != null)
            options.addAll(mediaOptions);
        if (BuildConfig.DEBUG && !options.contains("-vvv"))
            options.add("-vvv");
        libvlc = new LibVLC(options);

        return libvlc;
    }

    public String getVersion() {
        return libvlc.version();
    }

    /**
     * Set current position.
     * Note: skip() is more accurate for time-based positioning.
     */
    @Override
    public void setPosition(float pos) {
        if (isItemSeekable()) {
            if (pos < 0)
                pos = 0;
            else if (pos > 1)
                pos = 1;
            super.setPosition(pos);
        }
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        long time;
        if (getLength() <= 0 && playRate == 0) // otherwise incorrect after skip while paused
            time = (long)(getPosition()*getItemLength()) * 1000;
        else
            time = getTime();
        return time == -1 ? 0 : (int)(time/1000);
    }

    public void setSeconds(int secs) {
        if (getLength() > 0)
            setTime(secs * 1000);
        else
            setPosition((float)secs/itemLength);
    }

    public void skip(int delta) {
        if (getLength() <= 0 && seekCorrectionTolerance > 0) {
            // use seek correction
            if (!isTargeting()) {
                long newTarget = getTime() + delta * 1000;
                if (newTarget <= 0)
                    setPosition(0);
                else if (newTarget >= (itemLength*1000))
                    setPosition(1);
                else
                    target(newTarget);
            }
        }
        else {
            doSkip(delta);
        }
    }

    /**
     * Seeks with retry when tolerance is exceeded.
     */
    private void target(long t) {
        target = t;
        long d = target - getTime();
        float newPos = getPosition() + (float)d/(itemLength*1000);
        setPosition(newPos);
        audioTrack = getAudioTrack();
        targetTimeoutHandler.removeCallbacks(targetTimeoutAction);
        targetTimeoutHandler.postDelayed(targetTimeoutAction, targetTimeout);
        setAudioTrack(-1); // mute until target reached
    }

    private void clearTarget() {
        targetTimeoutHandler.removeCallbacks(targetTimeoutAction);
        target = -1;
        if (!isReleased())
            setAudioTrack(audioTrack);
    }

    private Handler targetTimeoutHandler = new Handler();
    private Runnable targetTimeoutAction = new Runnable() {
        public void run() {
            if (target > 0)
                Log.d(TAG, "Seek target timed out after " + targetTimeout + " ms");
            clearTarget();
        }
    };

    private long target; // ms
    public boolean isTargeting() { return target > 0; }
    private int targetTimeout = 5000; // ms
    private int audioTrack;

    /**
     * Seek forward or backward delta seconds.
     * @return fractional position or -1 if not seekable
     */
    public float doSkip(int delta) {
        if (isItemSeekable()) {
            long len = getLength();
            if (len > 0) {
                // seems that setPosition() causes hang for mpg if length known, so must replicate bounds logic
                long curTime = getTime();
                final long newTime = curTime + (delta*1000);
                if (newTime < 0) {
                    setTime(0);
                    return 0;
                }
                else if (newTime > len) {
                    setTime(len);
                    return 1;
                }
                else {
                    float f = setTime(newTime)/len;
                    if (curTime < 1000 && newTime > 10000)  {  // sometimes restore is inaccurate for items where vlc knows length
                        if (seekCorrectionTolerance > 0) {
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    try {
                                        long deltaMs = newTime - getTime() - 1;
                                        if (Math.abs(deltaMs) > seekCorrectionTolerance) {
                                            Log.e(TAG, "Correcting restore position by: " + deltaMs + " ms");
                                            doSkip((int)(deltaMs/1000));
                                        }
                                    }
                                    catch (Exception ex) {
                                        // TODO: IllegalStateException: can't get VLCObject instance
                                        Log.e(TAG, ex.getMessage(), ex);
                                    }
                               }
                            }, 1000);
                        }
                    }
                    return f;
                }
            }
            else {
                // for unknown length setTime() never works, so use setPosition()
                float curPos = getPosition();
                float newPos = curPos + (float)delta/(float)itemLength;
                if (newPos < 0) {
                    setPosition(0);
                    return 0;
                }
                else if (newPos > 1) {
                    setPosition(1);
                    return 1;
                }
                else {
                    setPosition(newPos);
                    return newPos;
                }
            }
        }
        return -1;
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

    private int playRate = 1;
    public int getPlayRate() { return playRate; }

    @Override
    public void play() {
        if (playRate != 1) {
            playRate = 1;
            setRate(1f);
        }
        if (!isPlaying())
            super.play();
    }


    @Override
    public void pause() {
        playRate = 0;
        if (isPlaying())
            super.pause();
    }

    /**
     * Step up the fast-forward rate by a factor of two
     * (resets playRate = +2 if maxPlayRate would be exceeded).
     * @return the new playRate
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
            if (!isReleased() && playRate > 1) {
                float f = doSkip(playRate);
                if (f >= 1) {
                   stop();
                   eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.end));
                   if (proxy != null)
                       proxy.stop();
                }
                else {
                    if (shiftListener != null)
                        shiftListener.onShift(playRate);
                    fastForwardHandler.postDelayed(this, 1000);
                }
            }
        }
    };

    /**
     * Step up the rewind rate by a factor of two
     * (resets playRate = -2 if maxPlayRate would be exceeded).
     * @return the new playRate
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
            if (!isReleased() && playRate < 0) {
                float begin = doSkip(playRate);
                if (begin == 0)
                    play();
                else
                    rewindHandler.postDelayed(this, 1000);

                if (shiftListener != null)
                    shiftListener.onShift(playRate);
            }
        }
    };

    private MediaPlayerEventListener eventListener;
    public void setMediaPlayerEventListener(MediaPlayerEventListener listener) {
        this.eventListener = listener;
    }

    private MediaPlayer.EventListener vlcEventListener = new MediaPlayer.EventListener() {
        private int samples = 0;
        private int minSamples = 10;
        private int maxSamples = 30;
        private long length;  // length reported by libvlc

        @Override
        public void onEvent(MediaPlayer.Event event) {
            if (eventListener != null) {
                switch(event.type) {
                    case MediaPlayer.Event.Opening:
                        length = 0;
                        break;
                    case MediaPlayer.Event.Playing:
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.playing));
                        break;
                    case MediaPlayer.Event.Paused:
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.paused));
                        break;
                    case MediaPlayer.Event.Stopped:
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.stopped));
                        break;
                    case MediaPlayer.Event.EndReached:
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.end));
                        if (proxy != null)
                            proxy.stop();
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.error));
                        break;
                    case MediaPlayer.Event.TimeChanged:
                        if (length <= 0 && samples < maxSamples) {
                            length = getLength(); // reported length
                            if (length > 0) {
                                Log.i(TAG, "Video length determined: " + length);
                                if (isHls) {
                                    if (itemLength > 0) {
                                        // duration inaccuracy based on meta length
                                        // (don't trust HLS reported length)
                                        int itemLengthMs = itemLength * 1000;
                                        long lengthOffset = itemLengthMs - length;
                                        Log.i(TAG, "Length offset: " + lengthOffset);
                                        if (Math.abs((float)lengthOffset/itemLengthMs) > 0.01)
                                            durationMismatch = true;
                                    }
                                }
                                else {
                                    itemLength = (int)(length/1000);
                                }
                            }
                        }

                        if (samples > minSamples && samples < maxSamples) {
                            if (length <= 0) // not known to vlc (usually true for streamed files)
                                durationMismatch = true;
                            // infer length if no meta
                            if (itemLength == 0) {
                                length = inferItemLength();
                                if (length != itemLength) {
                                    Log.i(TAG, "Estimated video length: " + length);
                                    itemLength = (int)length;
                                }
                            }
                        }

                        samples++;
                        eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.time));
                        break;
                    case MediaPlayer.Event.PositionChanged:
                        if (target > 0) {
                            long d = target - getTime();
                            Log.d(TAG, "Seek delta ms: " + d);
                            if (Math.abs(d) > seekCorrectionTolerance) {
                                float newPos = getPosition() + (float)d/(itemLength*1000);
                                Log.w(TAG, "Correcting position: " + newPos);
                                setPosition(newPos);
                            }
                            else {
                                clearTarget();
                                eventListener.onEvent(new MediaPlayerEvent(MediaPlayerEventType.seek));
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private MediaPlayerShiftListener shiftListener;
    public void setMediaPlayerShiftListener(MediaPlayerShiftListener listener) {
        this.shiftListener = listener;
    }

    private LibVLC.HardwareAccelerationError hardwareAccelerationErrorHandler = new LibVLC.HardwareAccelerationError() {
        public void eventHardwareAccelerationError() {
            if (eventListener != null){
                MediaPlayerEvent event = new MediaPlayerEvent(MediaPlayerEventType.error);
                event.message = LibVLC.HardwareAccelerationError.class.getName();
                eventListener.onEvent(event);
            }
        }
    };

    private MediaPlayerLayoutChangeListener layoutChangeListener;
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener) {
        this.layoutChangeListener = listener;
    }

    private Callback nativeCallback = new Callback() {
        public void onNewLayout(IVLCVout vout, int width, int height,
                int visibleWidth, int visibleHeight, int aspectNumerator, int aspectDenominator) {
            if (layoutChangeListener != null)
                layoutChangeListener.onLayoutChange(width, height, aspectNumerator, aspectDenominator);
        }

        public void onSurfacesCreated(IVLCVout vout) {
        }

        public void onSurfacesDestroyed(IVLCVout vout) {
        }
    };
}
