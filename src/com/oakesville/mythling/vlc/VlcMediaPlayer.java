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

import java.util.ArrayList;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.IVLCVout.Callback;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import com.oakesville.mythling.BuildConfig;

import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceView;

public class VlcMediaPlayer extends MediaPlayer implements com.oakesville.mythling.media.MediaPlayer {

    private static final String TAG = VlcMediaPlayer.class.getSimpleName();

    private Uri mediaUri;
    public Uri getMediaUri() {
        return mediaUri;
    }

    private int itemLength; // seconds
    public int getItemLength() {
        return itemLength;
    }
    public void setItemLength(int secs) {
        this.itemLength = secs;
    }

    public boolean isItemSeekable() {
        return itemLength > 0;
    }

    private boolean lengthDetermined; // libvlc knows the media length (not inferred)

    public int inferItemLength() {
        float p = getPosition();
        if (p > 0) {
            itemLength = (int)(getTime() / (p * 1000));
        }
        return itemLength;
    }

    public VlcMediaPlayer(SurfaceView videoView, SurfaceView subtitlesView) {
        super(createLibVlc());
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

    public void playMedia(Uri mediaUri) {
        this.mediaUri = mediaUri;
        Media m = new Media(libvlc, mediaUri);
        setMedia(m);
        play();
    }

    public void doRelease() {
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
    private static LibVLC createLibVlc() {
        // libvlc
        ArrayList<String> options = new ArrayList<String>();
        //options.add("--subsdec-encoding <encoding>");
        options.add("--aout=opensles");
        options.add("--audio-time-stretch"); // time stretching
        if (BuildConfig.DEBUG)
            options.add("-vvv");
        libvlc = new LibVLC(options);
        return libvlc;
    }

    /**
     * Set current position.
     * Note: skip() is more accurate.
     */
    public void setSeconds(int pos) {
        if (itemLength > 0) {
            // libvlc setTime() does not always work, so use setPosition()
            if (pos < 0)
                pos = 0;
            else if (pos > itemLength)
                pos = itemLength;
            float fraction = (float)pos/(float)(itemLength);
            setPosition(fraction);
        }
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        long time = getTime();
        return time == -1 ? 0 : (int)(getTime()/1000);
    }

    /**
     * Seek forward or backward.
     * @return if successful the new position in seconds, otherwise zero
     */
    public void skip(int delta) {
        if (itemLength > 0) {
            // setSeconds() is inaccurate for short intervals
            float curPos = getPosition();
            float newPos = curPos + (float)delta/(float)itemLength;
            setPosition(newPos);
        }
    }

    private int maxPlayRate = 1;
    /**
     * Max multiplier for fast-forward and rewind.
     */
    public int getMaxPlayRate() {
        if (itemLength > 0)
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
    public int stepUpFastForward() {
        if (itemLength > 0) {
            int newPlayRate = 2;
            if (playRate > 1) {
                newPlayRate = playRate * 2;
                if (newPlayRate > maxPlayRate)
                    newPlayRate = 2;
            }

            if (newPlayRate > 1 && playRate <= 1)
                fastForwardHandler.postDelayed(fastForwardAction, 100);

            playRate = newPlayRate;

            if (isPlaying())
                super.pause(); // avoid setting playRate = 0 in this.pause()
        }

        return playRate;
    }

    private Handler fastForwardHandler = new Handler();
    private Runnable fastForwardAction = new Runnable() {
        public void run() {
            if (!isReleased() && playRate > 1) {
                skip(playRate);
                // TODO
//                int newPos = skip(playRate);
//                if (newPos < itemLength)
//                    fastForwardHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Step up the rewind rate by a factor of two
     * (resets playRate = -2 if maxPlayRate would be exceeded).
     * @return the new playRate
     */
    public int stepUpRewind() {
        if (itemLength > 0) {
            int newPlayRate = -2;

            if (playRate < 0) {
                newPlayRate = playRate * 2;
                if (-newPlayRate > maxPlayRate)
                    newPlayRate = -2;
            }

            if (newPlayRate < 0 && playRate >= 0)
                rewindHandler.postDelayed(rewindAction, 100);

            playRate = newPlayRate;

            if (isPlaying())
                super.pause(); // avoid setting playRate = 0 in this.pause()
        }

        return playRate;
    }

    private Handler rewindHandler = new Handler();
    private Runnable rewindAction = new Runnable() {
        public void run() {
            if (!isReleased() && playRate < 0) {
                skip(playRate);
                // TODO
//                int newPos = skip(playRate);
//                if (newPos == 0)
//                    play();
//                else
//                    rewindHandler.postDelayed(this, 1000);
            }
        }
    };


    private MediaPlayerEventListener eventListener;
    public void setMediaPlayerEventListener(MediaPlayerEventListener listener) {
        this.eventListener = listener;
    }

    private MediaPlayer.EventListener vlcEventListener = new MediaPlayer.EventListener() {
        @Override
        public void onEvent(MediaPlayer.Event event) {
            if (eventListener != null) {
                switch(event.type) {
                    case MediaPlayer.Event.Playing:
                        eventListener.onEvent(MediaPlayerEvent.playing);
                        break;
                    case MediaPlayer.Event.Paused:
                        eventListener.onEvent(MediaPlayerEvent.paused);
                        break;
                    case MediaPlayer.Event.Stopped:
                        eventListener.onEvent(MediaPlayerEvent.stopped);
                        break;
                    case MediaPlayer.Event.EndReached:
                        eventListener.onEvent(MediaPlayerEvent.end);
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        eventListener.onEvent(MediaPlayerEvent.error);
                        break;
                    case MediaPlayer.Event.TimeChanged:
                        if (!lengthDetermined) {
                            long len = getLength();
                            if (len > 0) {
                                itemLength = (int)(len / 1000);
                                lengthDetermined = true;
                                eventListener.onEvent(MediaPlayerEvent.seekable);
                            }
                        }
                        eventListener.onEvent(MediaPlayerEvent.time);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private LibVLC.HardwareAccelerationError hardwareAccelerationErrorHandler = new LibVLC.HardwareAccelerationError() {
        public void eventHardwareAccelerationError() {
            if (eventListener != null)
                eventListener.onEvent(MediaPlayerEvent.error);
        }
    };

    private MediaPlayerLayoutChangeListener layoutChangeListener;
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener) {
        this.layoutChangeListener = listener;
    }

    private Callback nativeCallback = new Callback() {
        public void onNewLayout(IVLCVout vout, int width, int height,
                int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            if (layoutChangeListener != null)
                layoutChangeListener.onLayoutChange(width, height, sarNum, sarDen);
        }

        public void onSurfacesCreated(IVLCVout vout) {
        }

        public void onSurfacesDestroyed(IVLCVout vout) {
        }
    };
}
