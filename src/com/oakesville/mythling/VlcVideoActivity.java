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

import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.util.Reporter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class VlcVideoActivity extends Activity implements IVLCVout.Callback, LibVLC.HardwareAccelerationError {

    public static final String EXTRA_LENGTH_SECS = "com.oakesville.mythling.EXTRA_LENGTH_SECS";

    private static final String TAG = VlcVideoActivity.class.getSimpleName();

    private AppSettings appSettings;
    private Uri videoUri;
    private ProgressBar progressBar;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mediaPlayer;
    private int videoWidth;
    private int videoHeight;

    // seek
    private int length; // sec
    private TextView curPosText;
    private SeekBar seekBar;
    private ImageButton playCtrl;
    private ImageButton pauseCtrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        appSettings = new AppSettings(getApplicationContext());
        if (!Localizer.isInitialized())
            Localizer.initialize(appSettings);

        surface = (SurfaceView) findViewById(R.id.surface);
        holder = surface.getHolder();
        //holder.addCallback(this);

        createProgressBar();

        try {
            videoUri = Uri.parse(URLDecoder.decode(getIntent().getDataString(), "UTF-8"));
            System.out.println("videoUri: " + videoUri);

            Intent intent = getIntent();
            length = intent.getExtras().getInt(EXTRA_LENGTH_SECS);

            if (length > 0) {
                curPosText = (TextView) findViewById(R.id.current_pos);
                curPosText.setText("00:00");
                TextView totalLenText = (TextView) findViewById(R.id.total_len);
                totalLenText.setText(convertTime(length * 1000));

                seekBar = (SeekBar) findViewById(R.id.player_seek);
                seekBar.setMax(length); // max is length in seconds
                seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            float pos = progress / (float)(length); // fraction
                            long curPos = (long)(pos * length * 1000);
                            curPosText.setText(convertTime(curPos));
                            mediaPlayer.setPosition(pos);
                        }
                    }
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            }

            playCtrl = (ImageButton) findViewById(R.id.ctrl_play);
            playCtrl.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    play();
                }
            });

            pauseCtrl = (ImageButton) findViewById(R.id.ctrl_pause);
            pauseCtrl.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    pause();
                }
            });

            ImageButton fastBack = (ImageButton) findViewById(R.id.ctrl_jump_back);
            fastBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    seek(-600);
                }
            });

            ImageButton skipBack = (ImageButton) findViewById(R.id.ctrl_skip_back);
            skipBack.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    seek(-10);
                }
            });

            ImageButton skipFwd = (ImageButton) findViewById(R.id.ctrl_skip_fwd);
            skipFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    seek(+30);
                }
            });

            ImageButton fastFwd = (ImageButton) findViewById(R.id.ctrl_fast_fwd);
            fastFwd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    seek(+600);
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

    private void play() {
        playCtrl.setVisibility(View.GONE);
        pauseCtrl.setVisibility(View.VISIBLE);
        handler.postDelayed(updateSeekBar, 250);
        mediaPlayer.play();
    }

    private void pause() {
        pauseCtrl.setVisibility(View.GONE);
        playCtrl.setVisibility(View.VISIBLE);
        mediaPlayer.pause();
    }

    private void seek(int delta) {
        int pos = (int) (mediaPlayer.getPosition() * length);
        pos += delta;
        if (pos > length)
            pos = length;
        else if (pos < 0)
            pos = 0;
        seekBar.setProgress(pos);
        mediaPlayer.setPosition((float)pos / (float)length);
    }

    private String convertTime(long ms) {
        long mins = 0;
        long hrs = 0;

        long secs = ms / 1000;
        if (secs >= 60) {
          mins = secs / 60;
          secs = secs % 60;
        }
        if (mins >= 60) {
            hrs = mins / 60;
            mins = mins % 60;
        }

        String s = padTwo(mins) + ":" + padTwo(secs);
        if (hrs > 0)
            s = hrs + ":" + s;

        System.out.println("converted: " + ms + " ms" );
        System.out.println("  to: " + s);

        return s;
    }

    private static String padTwo(long l) {
        String s = String.valueOf(l);
        if (s.length() == 1)
            s = "0" + s;
        return s;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(videoWidth, videoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(videoUri);
        System.out.println("help me");
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void setSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        if (videoWidth * videoHeight <= 1)
            return;
        if (holder == null || surface == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into account, so correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) videoWidth / (float) videoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(videoWidth, videoHeight);

        // set display size
        LayoutParams lp = surface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        surface.setLayoutParams(lp);
        surface.invalidate();
    }

    private void createPlayer(Uri videoUri) {
        releasePlayer();
        try {
            Log.d(TAG, "Playing video: " + videoUri);

            // libvlc
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            if (BuildConfig.DEBUG)
                options.add("-vvv");
            libvlc = new LibVLC(options);
            libvlc.setOnHardwareAccelerationError(this);
            holder.setKeepScreenOn(true);

            // media player
            mediaPlayer = new MediaPlayer(libvlc);
            mediaPlayer.setEventListener(playerListener);

            // video output
            final IVLCVout vout = mediaPlayer.getVLCVout();
            vout.setVideoView(surface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, videoUri);
            mediaPlayer.setMedia(m);
            handler.postDelayed(updateSeekBar, 250);
            mediaPlayer.play();
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error creating player: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this more cleanly
    private void releasePlayer() {
        if (libvlc == null)
            return;
        mediaPlayer.stop();
        final IVLCVout vout = mediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        videoWidth = 0;
        videoHeight = 0;
    }

    private MediaPlayer.EventListener playerListener = new VlcPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;
        // store video size
        videoWidth = width;
        videoHeight = height;
        setSize(videoWidth, videoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
    }

    private static class VlcPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<VlcVideoActivity> owner;

        public VlcPlayerListener(VlcVideoActivity owner) {
            this.owner = new WeakReference<VlcVideoActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VlcVideoActivity player = owner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    @Override
    public void eventHardwareAccelerationError() {
        String msg = "Hardware acceleration error";
        Log.e(TAG, msg);
        releasePlayer();
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        if (appSettings.isErrorReportingEnabled())
            new Reporter(msg).send();
    }

    protected ProgressBar createProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return progressBar;
    }

    private Handler handler = new Handler();
    private Runnable updateSeekBar = new Runnable() {
        public void run() {
            if (mediaPlayer.isPlaying()) {
                float pos = mediaPlayer.getPosition();
                long curPos = (long)(pos * length * 1000);
                curPosText.setText(convertTime(curPos));
                seekBar.setProgress((int)(pos * length));
                handler.postDelayed(this, 250);
            }
        }
    };
}
