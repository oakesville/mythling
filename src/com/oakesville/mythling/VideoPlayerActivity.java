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
package com.oakesville.mythling;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaPlayer;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEvent;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEventListener;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerLayoutChangeListener;
import com.oakesville.mythling.util.Reporter;
import com.oakesville.mythling.util.TextBuilder;
import com.oakesville.mythling.vlc.VlcMediaPlayer;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class VideoPlayerActivity extends Activity {

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();

    private AppSettings appSettings;
    private Uri videoUri;
    private int itemLength;

    private ProgressBar progressBar;
    private SurfaceView surface;
    private FrameLayout surfaceFrame;

    private MediaPlayer mediaPlayer;
    private int videoWidth;
    private int videoHeight;

    private int screenAspectNumerator;
    private int screenAspectDenominator;

    // seek
    private TextView totalLengthText;
    private TextView currentPositionText;
    private SeekBar seekBar;
    private ImageButton playBtn;
    private ImageButton pauseBtn;

    private int savedPosition;
    private boolean done;

    private int hideUiDelay = 1500;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            getActionBar().hide();

        appSettings = new AppSettings(getApplicationContext());
        if (!Localizer.isInitialized())
            Localizer.initialize(appSettings);

        surface = (SurfaceView) findViewById(R.id.player_surface);
        surface.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showUi();
            }
        });

        surfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);

        createProgressBar();

        try {
            videoUri = Uri.parse(getIntent().getDataString());

            currentPositionText = (TextView) findViewById(R.id.current_pos);
            seekBar = (SeekBar) findViewById(R.id.player_seek);
            totalLengthText = (TextView) findViewById(R.id.total_len);

            currentPositionText.setText(new TextBuilder().appendDuration(0).toString());

            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.setSeconds(progress);
                        currentPositionText.setText(new TextBuilder().appendDuration(progress).toString());
                    }
                }
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            playBtn = (ImageButton) findViewById(R.id.ctrl_play);
            playBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mediaPlayer.play();
                    showPause();
                }
            });

            pauseBtn = (ImageButton) findViewById(R.id.ctrl_pause);
            pauseBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mediaPlayer.pause();
                    showPlay();
                }
            });

            ImageButton fastBack = (ImageButton) findViewById(R.id.ctrl_jump_back);
            fastBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(-600);
                }
            });

            ImageButton skipBack = (ImageButton) findViewById(R.id.ctrl_skip_back);
            skipBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(-10);
                }
            });

            ImageButton rewind = (ImageButton) findViewById(R.id.ctrl_rewind);
            rewind.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    int playRate = mediaPlayer.stepUpRewind();
                    showPlay();
                    Toast.makeText(getApplicationContext(), "<< " + (-playRate) + "x", Toast.LENGTH_SHORT).show();
                }
            });

            ImageButton ffwd = (ImageButton) findViewById(R.id.ctrl_ffwd);
            ffwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    int playRate = mediaPlayer.stepUpFastForward();
                    if (playRate == 1)
                        showPause();
                    else
                        showPlay();
                    Toast.makeText(getApplicationContext(), ">> " + playRate + "x", Toast.LENGTH_SHORT).show();
                }
            });

            ImageButton skipFwd = (ImageButton) findViewById(R.id.ctrl_skip_fwd);
            skipFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(+30);
                }
            });

            ImageButton fastFwd = (ImageButton) findViewById(R.id.ctrl_fast_fwd);
            fastFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(+600);
                }
            });
        }
        catch (Exception ex) {
            progressBar.setVisibility(View.GONE);
            Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHideUi(250);
    }

    private void showPlay() {
        pauseBtn.setVisibility(View.GONE);
        playBtn.setVisibility(View.VISIBLE);
    }

    private void showPause() {
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
    }

    private void skip(int delta) {
        int newPos = mediaPlayer.skip(delta);
        currentPositionText.setText(new TextBuilder().appendDuration(newPos).toString());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeSurfaceLayout(videoWidth, videoHeight);
    }

    @Override
    protected void onResume() {
        done = false;
        super.onResume();
        savedPosition = appSettings.getVideoPlaybackPosition(videoUri);
        createPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (done)
            appSettings.clearVideoPlaybackPosition(videoUri);
        else
            appSettings.setVideoPlaybackPosition(videoUri, mediaPlayer.getSeconds());
        releasePlayer();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedPosition = mediaPlayer.getSeconds();
        savedInstanceState.putInt(AppSettings.VIDEO_PLAYBACK_POSITION, savedPosition); // TODO per item
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        savedPosition = savedInstanceState.getInt(AppSettings.VIDEO_PLAYBACK_POSITION);
        // TODO seek to saved position
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void changeSurfaceLayout(int width, int height) {
        videoWidth = width;
        videoHeight = height;

        // get screen size
        int sw = getWindow().getDecorView().getWidth();
        int sh = getWindow().getDecorView().getHeight();
        double dw = sw, dh = sh;
        // getWindow().getDecorView() doesn't always take orientation into account, so correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // check sanity
        if (dw * dh == 0 || videoWidth * videoHeight == 0) {
            String msg = "Invalid surface size";
            Log.e(TAG, msg);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            if (appSettings.isErrorReportingEnabled())
                new Reporter(msg).send();
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        if (screenAspectDenominator == screenAspectNumerator) {
            // no indication about density, assume 1:1
            vw = width;
            ar =  (double)width / (double)height;
        } else {
            // use the specified aspect ratio
            vw = width * (double)screenAspectNumerator / screenAspectDenominator;
            ar = vw / height;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        // case SURFACE_BEST_FIT:
        if (dar < ar)
            dh = dw / ar;
        else
            dw = dh * ar;

        // TODO: subtitles
        SurfaceView subtitlesSurface = null;

        // set display size
        LayoutParams lp = surface.getLayoutParams();
        lp.width  = (int) dw;
        lp.height = (int) dh;
        surface.setLayoutParams(lp);
        if (subtitlesSurface != null)
            subtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = surfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        surfaceFrame.setLayoutParams(lp);

        surface.invalidate();
        if (subtitlesSurface != null)
            subtitlesSurface.invalidate();
    }

    private void createPlayer() {
        releasePlayer();
        try {
            Log.i(TAG, "Playing video: " + videoUri);

            mediaPlayer = new VlcMediaPlayer(surface, null); // TODO subtitles
            mediaPlayer.setLayoutChangeListener(new MediaPlayerLayoutChangeListener() {
                public void onLayoutChange(int width, int height, int sarNum, int sarDen) {
                    if (width * height == 0)
                        return;
                    screenAspectNumerator = sarNum;
                    screenAspectDenominator = sarDen;
                    changeSurfaceLayout(width, height);
                }
            });
            mediaPlayer.setMediaPlayerEventListener(new MediaPlayerEventListener() {
                public void onEvent(MediaPlayerEvent event) {
                    if (event == MediaPlayerEvent.playing) {
                        progressBar.setVisibility(View.GONE);
                        seekBarHandler.postDelayed(updateSeekBarAction, 100);
                    }
                    else if (event == MediaPlayerEvent.end) {
                        savedPosition = 0;
                        done = true;
                        finish();
                    }
                    else if (event == MediaPlayerEvent.error) {
                        String msg = "Media player error";
                        Log.e(TAG, msg);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                        if (appSettings.isErrorReportingEnabled())
                            new Reporter(msg).send();
                        finish();
                    }
                    else if (event == MediaPlayerEvent.seekable) {
                        if (mediaPlayer.getItemLength() != itemLength) {
                            itemLength = mediaPlayer.getItemLength();
                            totalLengthText.setText(new TextBuilder().appendDuration(itemLength).toString());
                            seekBar.setMax(itemLength); // max is length in seconds
                        }
                        if (savedPosition > 0) {
                            mediaPlayer.setSeconds(savedPosition);
                            savedPosition = 0;
                        }
                    }
                }
            });

            progressBar.setVisibility(View.VISIBLE);
            mediaPlayer.playMedia(videoUri);
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            progressBar.setVisibility(View.GONE);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error creating player: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null)
            mediaPlayer.doRelease();

        videoWidth = 0;
        videoHeight = 0;
    }

    private Handler seekBarHandler = new Handler();
    private Runnable updateSeekBarAction = new Runnable() {
        public void run() {
            if (!mediaPlayer.isReleased()) {
                if (mediaPlayer.isItemSeekable()) {
                    int curPos = mediaPlayer.getSeconds();
                    currentPositionText.setText(new TextBuilder().appendDuration(curPos).toString());
                    seekBar.setProgress(curPos);
                }
                seekBarHandler.postDelayed(this, 250);
            }
        }
    };

    private void delayedHideUi(int delayMs) {
        hideHandler.removeCallbacks(hideAction);
        hideHandler.postDelayed(hideAction, delayMs);
    }
    private Handler hideHandler = new Handler();
    private final Runnable hideAction = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // safe to use these constants as they're inlined at compile-time and do nothing on earlier devices
            surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void showUi() {
        hideHandler.removeCallbacks(hideAction);
        surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);


        delayedHideUi(hideUiDelay);
    }

    protected ProgressBar createProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return progressBar;
    }
}
