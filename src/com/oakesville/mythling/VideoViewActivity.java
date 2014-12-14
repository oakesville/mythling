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

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;
import com.oakesville.mythling.util.SystemUiHider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class VideoViewActivity extends Activity {

    private static final String TAG = VideoViewActivity.class.getSimpleName();

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final boolean TOGGLE_ON_CLICK = true; // toggle vs show on interaction

    private VideoView videoView;
    private int position;
    private ProgressBar progressBar;
    private MediaController mediaController;
    private SystemUiHider systemUiHider;
    private AppSettings appSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        appSettings = new AppSettings(getApplicationContext());

        videoView = (VideoView) findViewById(R.id.video_view);

        createProgressBar();
        startProgress();

        try {
            if (mediaController == null)
                mediaController = new MediaController(this);

            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.parse(URLDecoder.decode(getIntent().getDataString(), "UTF-8")));

            systemUiHider = new SystemUiHider(this, videoView, SystemUiHider.FLAG_HIDE_NAVIGATION);
            systemUiHider.setup();

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
                    systemUiHider.show();
                }
            });

            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (TOGGLE_ON_CLICK) {
                        systemUiHider.toggle();
                    } else {
                        systemUiHider.show();
                    }
                }
            });

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

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls are available.
        delayedHide(100);
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

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener delayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler hideHandler = new Handler();
    Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            systemUiHider.hide();
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
}
