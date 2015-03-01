/**
 * Copyright 2014 Donald Oakes
 *
 * This file is part of Mythling.
 *
 * Mythling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mythling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mythling.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.oakesville.mythling;

import java.net.URLDecoder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;

public class VideoViewActivity extends Activity {

    private static final String TAG = VideoViewActivity.class.getSimpleName();

    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private int position;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private boolean fullScreen;
    private AppSettings appSettings;
    private Uri videoUri;
    private boolean done;

    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        getActionBar().hide();

        appSettings = new AppSettings(getApplicationContext());

        videoView = (VideoView) findViewById(R.id.video_view);

        createProgressBar();

        try {
            if (mediaController == null) {
                mediaController = new MediaController(this) {
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                            ((Activity) getContext()).finish();
                        return super.dispatchKeyEvent(event);
                    }
                };
            }
            videoView.setMediaController(mediaController);

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mediaPlayer) {
                    if (!isSeekable(videoUri))
                        position = 0;
                    if (position > 0) {
                        VideoViewActivity.this.mediaPlayer = mediaPlayer;
                        mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                            public void onSeekComplete(MediaPlayer mp) {
                                progressBar.setVisibility(View.GONE);
                                videoView.start();
                            }
                        });
                        videoView.seekTo(position);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        videoView.start();
                    }
                }
            });

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    position = 0;
                    done = true;
                    finish();
                }
            });

            try {
                videoView.setOnInfoListener(new OnInfoListener() {
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            progressBar.setVisibility(View.GONE);
                        }
                        return false;
                    }
                });
            }
            catch (NoSuchMethodError err) {
                // not supported in some versions
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                videoView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onSystemUiVisibilityChange(int vis) {
                        // video view may have redisplayed notification bar
                        if (fullScreen && (vis != View.SYSTEM_UI_FLAG_FULLSCREEN))
                            videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                });
            }

            videoUri = Uri.parse(URLDecoder.decode(getIntent().getDataString(), "UTF-8"));
            videoView.setVideoURI(videoUri);
            videoView.requestFocus();
            progressBar.setVisibility(View.VISIBLE);
        } catch (Exception ex) {
            progressBar.setVisibility(View.GONE);
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // delayed hide - hint to the user that UI controls are available.
        delayedHide(500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (done)
            appSettings.clearVideoPlaybackPosition(videoUri);
        else
            appSettings.setVideoPlaybackPosition(videoUri, videoView.getCurrentPosition());
        if (mediaPlayer != null)
            mediaPlayer.setOnSeekCompleteListener(null);
        videoView.stopPlayback();  // release mediaPlayer
    }

    @Override
    protected void onResume() {
        done = false;
        super.onResume();
        position = appSettings.getVideoPlaybackPosition(videoUri);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(AppSettings.VIDEO_PLAYBACK_POSITION, videoView.getCurrentPosition());
        videoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt(AppSettings.VIDEO_PLAYBACK_POSITION);
        videoView.seekTo(position);  // hopefully short term seek works for HLS
    }

    private boolean isSeekable(Uri videoUri) {
        // TODO: better logic to exclude HLS
        return videoUri != null && videoUri.toString().indexOf(".m3u8") == -1;
    }

    protected ProgressBar createProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return progressBar;
    }

    Handler hideHandler = new Handler();
    Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            setFullScreen(true);
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setFullScreen(boolean fullScreen) {
        if (fullScreen) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LOW_PROFILE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            else {
                videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
        else {
            videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        this.fullScreen = fullScreen;
    }
}
