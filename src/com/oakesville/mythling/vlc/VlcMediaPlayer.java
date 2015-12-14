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

    private Uri mediaUri;
    public Uri getMediaUri() {
        return mediaUri;
    }

    private int itemLength; // seconds
    public int getItemLength() {
        return itemLength; // TODO inferred for HLS?
    }

    public VlcMediaPlayer(SurfaceView videoView, SurfaceView subtitlesView) {
        super(createLibVlc());
        libvlc.setOnHardwareAccelerationError(hardwareAccelerationErrorHandler);

        setMaxPlayRate(16); // TODO pref
        setEventListener(vlcEventListener);

        // video output
        final IVLCVout vout = getVLCVout();
        vout.setVideoView(videoView);
        if (subtitlesView != null)
            vout.setSubtitlesView(subtitlesView);
        vout.addCallback(nativeCallback);
        vout.attachViews();
    }

    public void playMedia(Uri mediaUri, int itemLength) {
        this.mediaUri = mediaUri;
        this.itemLength = itemLength;
        Media m = new Media(libvlc, mediaUri);
        setMedia(m);
        play();
    }

    public void doRelease() {
        if (libvlc == null)
            return;
        stop();
        final IVLCVout vout = getVLCVout();
        // TODO: vout.removeCallback(nativeCallback);
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
     */
    public void setSeconds(int pos) {
        if (itemLength > 0) {
            float fraction = pos / (float)(itemLength);
            setPosition(fraction);
        }
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        if (itemLength > 0) {
            return (int)(getPosition() * itemLength);
        }
        else {
            return 0;
        }
    }

    /**
     * Seek forward or backward.
     * @return if successful the new position in seconds, otherwise zero
     */
    public int seek(int delta) {
        if (itemLength > 0) {
            int newPos = getSeconds() + delta;
            setSeconds(newPos);
            return newPos;
        }
        else {
            return 0;
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
     * (resets playRate = +1 if maxPlayRate would be exceeded).
     * @return the new playRate
     */
    public int stepUpFastForward() {
        // TODO: mute

        int newPlayRate = 1;
        if (playRate < 1)
            newPlayRate = 1;
        else {
            newPlayRate = playRate * 2;
            if (newPlayRate > maxPlayRate)
                newPlayRate = 1;
        }

        playRate = newPlayRate;

        if (playRate == 1)
            play();
        else
            setRate(playRate);

        return playRate;
    }

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
        private int count = 0;
        public void run() {
            if (!isReleased() && playRate < 0) {
                if (count >= maxPlayRate) {
                    seek(-1);
                    count = 0;
                }
                else {
                    count += -playRate;
                }
                rewindHandler.postDelayed(this, 1000/maxPlayRate);
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
                layoutChangeListener.onLayoutChange(width, height);
        }

        public void onSurfacesCreated(IVLCVout vout) {
        }

        public void onSurfacesDestroyed(IVLCVout vout) {
        }
    };
}
