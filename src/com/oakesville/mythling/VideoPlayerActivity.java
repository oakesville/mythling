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

import java.io.IOException;
import java.util.List;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.Cut;
import com.oakesville.mythling.media.MediaPlayer;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEvent;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEventListener;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerLayoutChangeListener;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerShiftListener;
import com.oakesville.mythling.prefs.PrefDismissDialog;
import com.oakesville.mythling.util.HttpHelper.AuthType;
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
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class VideoPlayerActivity extends Activity {

    public static final String AUTH_TYPE = "com.oakesville.mythling.AUTH_TYPE";
    public static final String ITEM_LENGTH_SECS = "com.oakesville.mythling.ITEM_LENGTH_SECS";
    public static final String ITEM_CUT_LIST = "com.oakesville.mythling.ITEM_CUT_LIST";

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    private static final String SKIP_TV_PLAYER_HINT_PREF = "skip_tv_player_hint";

    private static int HIDE_UI_INITIAL_DELAY = 2000;
    private static int HIDE_UI_POST_TOUCH_DELAY = 3000;

    private AppSettings appSettings;
    private Uri videoUri;
    private AuthType authType;

    private int itemLength; // this will be zero if not known definitively
    private String commercialSkip;
    private List<Cut> cutList;
    private Cut currentCut;

    private FrameLayout progressFrame;
    private SurfaceView surface;
    private FrameLayout surfaceFrame;
    private LinearLayout navControls;

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

    private float savedPosition;
    private boolean done;

    private boolean showTvControlsHint;

    @SuppressWarnings("unchecked")
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

        navControls = (LinearLayout) findViewById(R.id.nav_controls);

        if (appSettings.isTv())
            findViewById(R.id.nav_touch_controls).setVisibility(View.GONE);

        progressFrame = createProgressBar();
        progressFrame.setVisibility(View.VISIBLE);

        try {
            videoUri = getIntent().getData();
            String at = getIntent().getStringExtra(AUTH_TYPE);
            authType = at == null ? null : AuthType.valueOf(at);
            itemLength = getIntent().getIntExtra(ITEM_LENGTH_SECS, 0);
            cutList = (List<Cut>) getIntent().getSerializableExtra(ITEM_CUT_LIST);

            currentPositionText = (TextView) findViewById(R.id.current_pos);
            seekBar = (SeekBar) findViewById(R.id.player_seek);
            totalLengthText = (TextView) findViewById(R.id.total_len);

            currentPositionText.setText(new TextBuilder().appendDuration(0).toString());

            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        showUi();
                        if (mediaPlayer.isItemSeekable())
                            mediaPlayer.setPosition((float)progress/(float)(itemLength));
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
                    showUi();
                    mediaPlayer.play();
                    showPause();
                }
            });

            pauseBtn = (ImageButton) findViewById(R.id.ctrl_pause);
            pauseBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    mediaPlayer.pause();
                    showPlay();
                }
            });

            ImageButton fastBack = (ImageButton) findViewById(R.id.ctrl_jump_back);
            fastBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    skip(-appSettings.getJumpInterval());
                }
            });

            ImageButton skipBack = (ImageButton) findViewById(R.id.ctrl_skip_back);
            skipBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    skip(-appSettings.getSkipBackInterval());
                }
            });

            ImageButton rewind = (ImageButton) findViewById(R.id.ctrl_rewind);
            rewind.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    rewind();
                    showPlay();
                }
            });

            ImageButton ffwd = (ImageButton) findViewById(R.id.ctrl_fast_fwd);
            ffwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    fastForward();
                    showPlay();
                }
            });

            ImageButton skipFwd = (ImageButton) findViewById(R.id.ctrl_skip_fwd);
            skipFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    skip(appSettings.getSkipForwardInterval());
                }
            });

            ImageButton fastFwd = (ImageButton) findViewById(R.id.ctrl_jump_fwd);
            fastFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showUi();
                    skip(appSettings.getJumpInterval());
                }
            });
        }
        catch (Exception ex) {
            progressFrame.setVisibility(View.GONE);
            Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHideUi(HIDE_UI_INITIAL_DELAY);
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
        mediaPlayer.skip(delta);
    }

    private void fastForward() {
        int playRate = mediaPlayer.stepUpFastForward();
        Toast.makeText(getApplicationContext(), ">> " + playRate + "x", Toast.LENGTH_SHORT).show();
    }

    private void rewind() {
        int playRate = mediaPlayer.stepUpRewind();
        Toast.makeText(getApplicationContext(), "<< " + (-playRate) + "x", Toast.LENGTH_SHORT).show();
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
        if (appSettings.isSavePositionOnExit())
            savedPosition = appSettings.getVideoPlaybackPosition(videoUri);
        else
            savedPosition = 0;
        commercialSkip = appSettings.getCommercialSkip();

        if (appSettings.isTv()) {
            showTvControlsHint = !appSettings.getBooleanPref(SKIP_TV_PLAYER_HINT_PREF, false);
            if (showTvControlsHint) {
                String title = getString(R.string.title_tv_player_hint);
                String msg = getString(R.string.tv_player_hint);
                PrefDismissDialog hintDlg = new PrefDismissDialog(appSettings, title, msg, SKIP_TV_PLAYER_HINT_PREF);
                hintDlg.show(getFragmentManager());
            }
        }

        createPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (done || !appSettings.isSavePositionOnExit())
            appSettings.clearVideoPlaybackPosition(videoUri);
        else
            appSettings.setVideoPlaybackPosition(videoUri, mediaPlayer.getPosition());
        releasePlayer();
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

                private int minSampleLength = 3;
                private int correctableDelta = 5;  // don't update if off by less than this

                public void onEvent(MediaPlayerEvent event) {
                    if (event == MediaPlayerEvent.playing) {
                        progressFrame.setVisibility(View.GONE);
                        if (itemLength != 0) {
                            // length is already known -- don't wait for seekability determination
                            mediaPlayer.setItemLength(itemLength);
                            totalLengthText.setText(new TextBuilder().appendDuration(itemLength).toString());
                            seekBar.setMax(itemLength); // max is length in seconds
                        }
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
                            // now the length is known definitively
                            itemLength = mediaPlayer.getItemLength();
                            totalLengthText.setText(new TextBuilder().appendDuration(itemLength).toString());
                            seekBar.setMax(itemLength); // max is length in seconds
                        }
                    }
                    else if (event == MediaPlayerEvent.time) {

                        int pos = mediaPlayer.getSeconds();

                        // infer length if needed
                        if (itemLength == 0) {
                            Log.i(TAG, "Estimating video length");
                            if (pos > minSampleLength) {
                                int prevLen = mediaPlayer.getItemLength();
                                int len = mediaPlayer.inferItemLength();
                                if (Math.abs(len - prevLen) > correctableDelta) {
                                    totalLengthText.setText(new TextBuilder().appendDuration(len).toString());
                                    seekBar.setMax(len);
                                }
                            }
                        }

                        // update seek bar
                        currentPositionText.setText(new TextBuilder().appendDuration(pos).toString());
                        seekBar.setProgress(pos);
                        // restore saved position
                        if (mediaPlayer.isItemSeekable()) {
                            if (savedPosition > 0) {
                                Toast.makeText(getApplicationContext(), getString(R.string.restoring_saved_position), Toast.LENGTH_SHORT).show();
                                mediaPlayer.setPosition(savedPosition);
                                savedPosition = 0;
                                showUi();
                            }
                        }

                        // commercial skip
                        if (cutList != null && mediaPlayer.getPlayRate() == 1) {
                            boolean inCut = false;
                            for (Cut cut : cutList) {
                                if (cut.start <= pos && cut.end > pos) {
                                    if (!cut.equals(currentCut)) {
                                        if (!AppSettings.COMMERCIAL_SKIP_OFF.equals(commercialSkip)) {
                                            TextBuilder tb = new TextBuilder(getString(R.string.notify_commercial_skip_));
                                            tb.appendDuration(cut.end - cut.start);
                                            Toast.makeText(getApplicationContext(), tb.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        if (AppSettings.COMMERCIAL_SKIP_ON.equals(commercialSkip)) {
                                            int skip = (cut.end - pos);
                                            if (skip > 0) {
                                                showUi();
                                                mediaPlayer.skip(skip);
                                            }
                                        }
                                    }
                                    inCut = true;
                                    currentCut = cut;
                                    break;
                                }
                            }
                            if (!inCut)
                                currentCut = null;
                        }
                    }
                }
            });

            mediaPlayer.setMediaPlayerShiftListener( new MediaPlayerShiftListener() {
                int pos; // seconds
                public void onShift(int delta) {
                    showUi();
                    if (delta == 0) {
                        pos = mediaPlayer.getSeconds();
                    }
                    else {
                        pos += delta;
                        currentPositionText.setText(new TextBuilder().appendDuration(pos).toString());
                        seekBar.setProgress(pos);
                    }
                }
            });

            if (videoUri.getScheme().equals("content")) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(videoUri, "r");
                if (pfd == null)
                    throw new IOException("Unable to open file descriptor for: " + videoUri);
                mediaPlayer.playMedia(pfd.getFileDescriptor());
            }
            else {
                mediaPlayer.playMedia(videoUri, authType);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            progressFrame.setVisibility(View.GONE);
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (appSettings.isTv() && event.getAction() == KeyEvent.ACTION_DOWN && !mediaPlayer.isReleased()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                showUi();
                if (mediaPlayer.isPlaying())
                    mediaPlayer.pause();
                else
                    mediaPlayer.play();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                showUi();
                mediaPlayer.play();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                showUi();
                mediaPlayer.pause();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_REWIND) {
                showUi();
                rewind();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                showUi();
                fastForward();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                showUi();
                skip(-appSettings.getSkipBackInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                showUi();
                skip(appSettings.getSkipForwardInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                showUi();
                skip(-appSettings.getJumpInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                showUi();
                skip(appSettings.getJumpInterval());
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

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

            // hide playback controls
            navControls.setVisibility(View.INVISIBLE);
        }
    };

    @SuppressLint("InlinedApi")
    private void showUi() {
        hideHandler.removeCallbacks(hideAction);
        surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        navControls.setVisibility(View.VISIBLE);

        delayedHideUi(HIDE_UI_POST_TOUCH_DELAY);
    }

    private FrameLayout createProgressBar() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return (FrameLayout) findViewById(R.id.progress_frame);
    }

}
