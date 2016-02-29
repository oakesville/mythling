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
import java.util.ArrayList;
import java.util.List;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.AndroidMediaPlayer;
import com.oakesville.mythling.media.Cut;
import com.oakesville.mythling.media.MediaPlayer;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEvent;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEventListener;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerEventType;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerLayoutChangeListener;
import com.oakesville.mythling.media.MediaPlayer.MediaPlayerShiftListener;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.PlaybackOptions;
import com.oakesville.mythling.media.PlaybackOptions.PlaybackOption;
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

    private static final String TAG = VideoPlayerActivity.class.getSimpleName();

    public static final String PLAYER = "com.oakesville.mythling.PLAYER";
    public static final String AUTH_TYPE = "com.oakesville.mythling.AUTH_TYPE";
    public static final String ITEM_LENGTH_SECS = "com.oakesville.mythling.ITEM_LENGTH_SECS";
    public static final String ITEM_CUT_LIST = "com.oakesville.mythling.ITEM_CUT_LIST";

    private static final String SKIP_TV_PLAYER_HINT_PREF = "skip_tv_player_hint";

    private static int showUiShort = 3500;  // for use when showing for user interaction
    private static int showUiLong = 4000;   // for skip since this can take some time

    private AppSettings appSettings;
    private Uri videoUri;
    private String playerOption;
    private AuthType authType;

    private int itemLength; // this will be zero if not known definitively
    private String autoSkip;
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

    private int savedPosition;
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
                showUi(false);
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
            playerOption = getIntent().getStringExtra(PLAYER);
            if (playerOption == null) {
                // if launched through external intent, try settings
                String fileType = PlaybackOptions.PROPERTY_DEFAULT;
                String streamMode = PlaybackOptions.STREAM_FILE;
                String lastPathSeg = videoUri.getLastPathSegment();
                if (lastPathSeg != null) {
                    int lastDot = lastPathSeg.lastIndexOf('.');
                    if (lastDot > 0) {
                        fileType = lastPathSeg.substring(lastDot + 1);
                        if ("m3u8".equals(fileType))
                            streamMode = PlaybackOptions.STREAM_HLS;
                    }
                }
                String network = appSettings.isExternalNetwork() ? PlaybackOptions.NETWORK_EXTERNAL : PlaybackOptions.NETWORK_INTERNAL;
                if ("file".equals(videoUri.getScheme()))
                    network = PlaybackOptions.NETWORK_DOWNLOAD;
                PlaybackOption playbackOption = appSettings.getPlaybackOptions().getOption(MediaType.videos, fileType, network, streamMode);
                if (playbackOption != null)
                    playerOption = playbackOption.getPlayer();
            }
            if (playerOption == null)
                playerOption = appSettings.getPlaybackOptions().getDefaultPlayer();
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
                        showUi(false);
                        if (mediaPlayer.isItemSeekable())
                            mediaPlayer.setSeconds(progress);
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
                    play();
                    showPause();
                }
            });

            pauseBtn = (ImageButton) findViewById(R.id.ctrl_pause);
            pauseBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer.isProxying()) {
                        // pending issue #65
                        finish();
                    }
                    else {
                        pause();
                        showPlay();
                    }
                }
            });

            ImageButton jumpBack = (ImageButton) findViewById(R.id.ctrl_jump_back);
            jumpBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(-appSettings.getJumpInterval());
                }
            });

            ImageButton skipBack = (ImageButton) findViewById(R.id.ctrl_skip_back);
            skipBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(-appSettings.getSkipBackInterval());
                }
            });

            ImageButton rewind = (ImageButton) findViewById(R.id.ctrl_rewind);
            rewind.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    rewind();
                    if (mediaPlayer.getPlayRate() != 1)
                      showPlay();
                }
            });

            ImageButton ffwd = (ImageButton) findViewById(R.id.ctrl_fast_fwd);
            ffwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    fastForward();
                    if (mediaPlayer.getPlayRate() != 1)
                      showPlay();
                }
            });

            ImageButton skipFwd = (ImageButton) findViewById(R.id.ctrl_skip_fwd);
            skipFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    skip(appSettings.getSkipForwardInterval());
                }
            });

            ImageButton jumpFwd = (ImageButton) findViewById(R.id.ctrl_jump_fwd);
            jumpFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
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
        // delayedHideUi(showUiShort);
    }

    private void showPlay() {
        pauseBtn.setVisibility(View.GONE);
        playBtn.setVisibility(View.VISIBLE);
    }

    private void showPause() {
        playBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
    }

    private void play() {
        showUi(false);
        mediaPlayer.play();
    }

    private void pause() {
        showUi(false);
        mediaPlayer.pause();
    }

    private void skip(int delta) {
        showUi(true);
        mediaPlayer.skip(delta);
    }

    private void fastForward() {
        showUi(false);
        int playRate = mediaPlayer.stepUpFastForward();
        Toast.makeText(getApplicationContext(), ">> " + playRate + "x", Toast.LENGTH_SHORT).show();
    }

    private void rewind() {
        showUi(false);
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
        autoSkip = appSettings.getAutoSkip();

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
            appSettings.setVideoPlaybackPosition(videoUri, mediaPlayer.getSeconds());
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
            if (playerOption.equals(PlaybackOptions.PLAYER_ANDROID))
                mediaPlayer = new AndroidMediaPlayer(getApplicationContext(), surface);
            else if (playerOption.equals(PlaybackOptions.PLAYER_LIBVLC))
                mediaPlayer = new VlcMediaPlayer(surface, null, appSettings.getVlcOptions()); // TODO subtitles
            else
                throw new IllegalArgumentException("Unsupported player option: " + playerOption);
            Log.i(TAG, "MediaPlayer: " + playerOption + " " + mediaPlayer.getVersion());

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
                    if (event.type == MediaPlayerEventType.playing) {
                        progressFrame.setVisibility(View.GONE);
                        if (itemLength != 0) {
                            // length is already known -- don't wait for seekability determination
                            mediaPlayer.setItemLength(itemLength);
                            totalLengthText.setText(new TextBuilder().appendDuration(itemLength).toString());
                            seekBar.setMax(itemLength); // max is length in seconds
                        }
                    }
                    else if (event.type == MediaPlayerEventType.end) {
                        savedPosition = 0;
                        done = true;
                        finish();
                    }
                    else if (event.type == MediaPlayerEventType.error) {
                        final String msg = getString(R.string.media_player_error_) + event.message;
                        Log.e(TAG, msg);
                        // seems this doesn't always happen on the ui thread
                        runOnUiThread(new Runnable() {
                            public void run() {
                              Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                            }
                          });
                        if (appSettings.isErrorReportingEnabled())
                            new Reporter(msg).send();
                        finish();
                    }
                    else if (event.type == MediaPlayerEventType.seekable) {
                        if (mediaPlayer.getItemLength() != itemLength) {
                            // now the length is known definitively
                            itemLength = mediaPlayer.getItemLength();
                            totalLengthText.setText(new TextBuilder().appendDuration(itemLength).toString());
                            seekBar.setMax(itemLength); // max is length in seconds
                        }
                    }
                    else if (event.type == MediaPlayerEventType.time) {
                        int pos = mediaPlayer.getSeconds();

                        // infer length if needed // TODO: for android player?
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
                                showUi(true);
                                Toast.makeText(getApplicationContext(), getString(R.string.restoring_saved_position), Toast.LENGTH_SHORT).show();
                                mediaPlayer.setSeconds(savedPosition);
                                savedPosition = 0;
                            }
                        }

                        // auto skip
                        if (cutList != null && mediaPlayer.isItemSeekable() && mediaPlayer.getPlayRate() == 1) {
                            boolean inCut = false;
                            for (Cut cut : cutList) {
                                if (cut.start <= pos && cut.end > pos) {
                                    if (!cut.equals(currentCut)) {
                                        if (!AppSettings.AUTO_SKIP_OFF.equals(autoSkip)) {
                                            TextBuilder tb = new TextBuilder(getString(R.string.notify_auto_skip_));
                                            tb.appendDuration(cut.end - cut.start);
                                            Toast.makeText(getApplicationContext(), tb.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        if (AppSettings.AUTO_SKIP_ON.equals(autoSkip)) {
                                            int skip = (cut.end - pos);
                                            if (skip > 0) {
                                                skip(skip);
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
                    else if (event.type == MediaPlayerEventType.buffered) {
                        seekBar.setSecondaryProgress(event.position);
                    }
                }
            });

            mediaPlayer.setMediaPlayerShiftListener( new MediaPlayerShiftListener() {
                int pos; // seconds
                public void onShift(int delta) {
                    showUi(false);
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

            Log.i(TAG, "Playing video: " + videoUri);
            Log.i(TAG, "Using: " + mediaPlayer.getClass() + " v" + mediaPlayer.getVersion());

            if (videoUri.getScheme().equals("content")) {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(videoUri, "r");
                if (pfd == null)
                    throw new IOException("Unable to open file descriptor for: " + videoUri);
                mediaPlayer.playMedia(pfd.getFileDescriptor(), appSettings.getVlcMediaOptions());
            }
            else {
                mediaPlayer.playMedia(videoUri, authType, appSettings.getVlcMediaOptions());
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            progressFrame.setVisibility(View.GONE);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.playback_error_) + ex.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null)
            mediaPlayer.doRelease();

        videoWidth = 0;
        videoHeight = 0;
    }

    private void setAutoSkip(String autoSkip) {
        String entry = Localizer.getStringArrayEntry(R.array.auto_skip_values, R.array.auto_skip_entries, autoSkip);
        Toast.makeText(getApplicationContext(), entry, Toast.LENGTH_LONG).show();
        if (!this.autoSkip.equals(autoSkip)) {
            appSettings.setAutoSkip(autoSkip);
            this.autoSkip = autoSkip;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (appSettings.isTv() && event.getAction() == KeyEvent.ACTION_DOWN && !mediaPlayer.isReleased()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mediaPlayer.isPlaying())
                    pause();
                else
                    play();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                play();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                pause();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_REWIND) {
                rewind();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                fastForward();
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                skip(-appSettings.getSkipBackInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                skip(appSettings.getSkipForwardInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                if (specialKey(KeyEvent.KEYCODE_DPAD_UP))
                    return true;
                skip(-appSettings.getJumpInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (specialKey(KeyEvent.KEYCODE_DPAD_DOWN))
                    return true;
                skip(appSettings.getJumpInterval());
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                specialKey(KeyEvent.KEYCODE_DPAD_CENTER);
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
    private void showUi(boolean longDuration) {
        hideHandler.removeCallbacks(hideAction);
        surface.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        navControls.setVisibility(View.VISIBLE);

        if (longDuration)
            delayedHideUi(showUiLong);
        else
            delayedHideUi(showUiShort);
    }

    List<Integer> specialKeys;
    private boolean specialKey(int code) {
        specialKeyHandler.removeCallbacks(specialKeyAction);
        if (code == KeyEvent.KEYCODE_DPAD_CENTER) {
            specialKeys = new ArrayList<Integer>();
            specialKeys.add(code);
            specialKeyHandler.postDelayed(specialKeyAction, 1000);
            return true;
        }
        else if (specialKeys != null) {
            specialKeys.add(code);
            if (specialKeys.size() == 3) {
                if (specialKeys.get(1) == KeyEvent.KEYCODE_DPAD_UP && specialKeys.get(2) == KeyEvent.KEYCODE_DPAD_UP)
                    setAutoSkip(AppSettings.AUTO_SKIP_ON);
                else if (specialKeys.get(1) == KeyEvent.KEYCODE_DPAD_DOWN && specialKeys.get(2) == KeyEvent.KEYCODE_DPAD_DOWN)
                    setAutoSkip(AppSettings.AUTO_SKIP_OFF);
            }
            specialKeyHandler.postDelayed(specialKeyAction, 1000);
            return true;
        }
        return false;
    }
    private Handler specialKeyHandler = new Handler();
    private final Runnable specialKeyAction = new Runnable() {
        public void run() {
            specialKeys = null;
        }
    };

    private FrameLayout createProgressBar() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return (FrameLayout) findViewById(R.id.progress_frame);
    }

}
