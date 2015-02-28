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

import java.lang.reflect.Method;
import java.net.URLDecoder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;

public class VideoViewActivity extends Activity {

    private static final String TAG = VideoViewActivity.class.getSimpleName();

    private VideoView videoView;
    private int position;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private boolean fullScreen;
    private AppSettings appSettings;


    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        appSettings = new AppSettings(getApplicationContext());

        videoView = (VideoView) findViewById(R.id.video_view);

        createProgressBar();
        startProgress();

        try {
            try {
                // try and get actual screen dimens, including system windows
                Display display = getWindowManager().getDefaultDisplay();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    DisplayMetrics metrics = new DisplayMetrics();
                    display.getRealMetrics(metrics);
                    int width = metrics.widthPixels;
                    int height = metrics.heightPixels;
                    videoView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
                } else {
                    Method getRawWidth = Display.class.getMethod("getRawWidth");
                    Method getRawHeight = Display.class.getMethod("getRawHeight");
                    int width = (Integer) getRawWidth.invoke(display);
                    int height = (Integer) getRawHeight.invoke(display);
                    videoView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
                }
            }
            catch (Exception ex) {
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
            }

            if (mediaController == null) {
                mediaController = new MediaController(this) {
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                            ((Activity) getContext()).finish();
                        return super.dispatchKeyEvent(event);
                    }
                };
            }

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mediaPlayer) {
                    stopProgress();
                    videoView.seekTo(position);
                    if (position == 0)
                        videoView.start();
                    else
                        videoView.pause(); //if we come from a resumed activity, video playback will be paused
                }
            });


            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    position = 0;
                    setFullScreen(false);
                    onBackPressed();
                }
            });

//            videoView.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View view, MotionEvent event) {
//                    setFullScreen(false);
//                    mediaController.show(3000);
//                    delayedHide(3500);
//                    return false;
//                }
//            });

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

            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.parse(URLDecoder.decode(getIntent().getDataString(), "UTF-8")));
            videoView.requestFocus();
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            stopProgress();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // delayed hide - hint to the user that UI controls are available.
        delayedHide(200);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("Position", videoView.getCurrentPosition());
        videoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt("Position");
        videoView.seekTo(position);
    }

    protected ProgressBar createProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return progressBar;
    }

    protected void startProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void stopProgress() {
        progressBar.setVisibility(View.GONE);
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
            getActionBar().hide();
        }
        else {
            videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getActionBar().show();
        }
        this.fullScreen = fullScreen;
    }
}
