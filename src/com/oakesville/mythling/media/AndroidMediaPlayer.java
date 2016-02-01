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

    private int itemLength; // seconds
    public int getItemLength() {
        int d = getDuration(); // prefer this if known
        return d == -1 ? itemLength : d / 1000;
    }
    public void setItemLength(int secs) {
        this.itemLength = secs;
    }

    public int inferItemLength() {
        return 0; // not applicable
    }

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
    }

    public void playMedia(Uri mediaUri, AuthType authType, List<String> options) throws IOException {
        setDataSource(context, mediaUri);
        prepare();
        start();
    }

    public void playMedia(FileDescriptor fileDescriptor, List<String> options) throws IOException {
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
        start();
    }

    @Override
    public boolean isItemSeekable() {
        // TODO
        return true;
    }

    /**
     * Get current position.  Zero if unknown.
     */
    public int getSeconds() {
        return getCurrentPosition() / 1000;
    }

    public void setSeconds(int secs) {
        seekTo(secs * 1000);
    }

    public void skip(int delta) {
        int newPos = getCurrentPosition() + (delta * 1000);

        if (newPos < 0) {
            newPos = 0;
        }
        else {
            int lenMs = getItemLength() * 1000;
            if (newPos > lenMs)
                newPos = lenMs - 1;
        }
        pause();
        seekTo(newPos);
        start();
    }

    @Override
    public int stepUpRewind() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    public int stepUpFastForward() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    public int getPlayRate() {
        // TODO Auto-generated method stub
        return 1;
    }

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

    @Override
    public void setMediaPlayerShiftListener(MediaPlayerShiftListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLayoutChangeListener(MediaPlayerLayoutChangeListener listener) {
        // TODO Auto-generated method stub

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
            // TODO Auto-generated method stub
            return false;
        }

        public void onVideoSizeChanged(android.media.MediaPlayer mp, int width, int height) {
            // TODO Auto-generated method stub
        }

        public void onSeekComplete(android.media.MediaPlayer mp) {
            // TODO Auto-generated method stub
        }
    }

    private Handler timingHandler = new Handler();
    private Runnable timingAction;

}
