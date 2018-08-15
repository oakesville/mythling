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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.HttpHelper.AuthType;
import com.oakesville.mythling.util.MediaStreamProxy;
import com.oakesville.mythling.util.MediaStreamProxy.ProxyInfo;
import com.oakesville.mythling.util.Reporter;

import org.json.JSONArray;

import java.io.IOException;

public class MusicPlaybackService extends Service {

    private static final String TAG = MusicPlaybackService.class.getSimpleName();

    public static final String EXTRA_MESSENGER = "com.oakesville.mythling.EXTRA_MUSIC_PLAYBACK_MESSENGER";
    public static final String EXTRA_MUSIC_QUEUE = "com.oakesville.mythling.EXTRA_MUSIC_QUEUE";

    public static final int MESSAGE_PLAYER_PREPARED = 0;
    public static final int MESSAGE_PLAYBACK_STOPPED = 1; // different from action (callback to initiator)

    public static final String ACTION_PLAY = "com.oakesville.mythling.PLAY";
    public static final String ACTION_STOP = "com.oakesville.mythling.STOP";
    public static final String ACTION_PLAY_PAUSE = "com.oakesville.mythling.PLAY_PAUSE";
    public static final String ACTION_PLAYBACK_STOPPED = "com.oakesville.mythling.PLAYBACK_STOPPED";

    private AppSettings appSettings;
    private MediaPlayer mediaPlayer;
    private Messenger playbackMessenger;
    private JSONArray musicQueue;
    private int queueIndex;
    private boolean isPaused;
    private OnAudioFocusChangeListener audioFocusListener;

    private MediaStreamProxy proxy;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            appSettings = new AppSettings(this);
            queueIndex = 0;

            Bundle extras = intent.getExtras();

            if (extras != null) {
                playbackMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
                String queue = extras.getString(EXTRA_MUSIC_QUEUE);
                musicQueue = queue == null ? null : new JSONArray();
            }

            if (intent.getAction().equals(ACTION_PLAY)) {
                initializeMediaPlayer();
                Uri uri = intent.getData();
                playFromUri(uri);
            }
            else if (intent.getAction().equals(ACTION_PLAY_PAUSE)) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isPaused = true;
                    }
                    else {
                        mediaPlayer.start();
                        isPaused = false;
                    }
                }
            }
            else if (intent.getAction().equals(ACTION_STOP)) {
                stopPlayback();
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
        }

        return START_NOT_STICKY;
    }

    private void stopPlayback() {
        queueIndex = 0;
        musicQueue = null;
        stopSelf();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                sendBroadcast(new Intent(ACTION_PLAYBACK_STOPPED));
                releasePlayer();
            }
            if (proxy != null)
                proxy.stop();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(audioFocusListener);
            if (playbackMessenger != null) {
                try {
                    Message msg = Message.obtain();
                    msg.what = MESSAGE_PLAYBACK_STOPPED;
                    playbackMessenger.send(msg);
                }
                catch (RemoteException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }
    }

    private void initializeMediaPlayer() {
        if (audioFocusListener == null) {
            audioFocusListener = new OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                        stopPlayback();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.unregisterMediaButtonEventReceiver(new ComponentName(MusicPlaybackService.this, MusicPlaybackButtonReceiver.class));
                    }
                }
            };
        }
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int res = am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            am.registerMediaButtonEventReceiver(new ComponentName(this, MusicPlaybackButtonReceiver.class));
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                if (proxy != null)
                    proxy.stop();
            } else if (isPaused) {
                mediaPlayer.reset();
            }

            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    if (playbackMessenger != null) {
                        try {
                            Message msg = Message.obtain();
                            msg.what = MESSAGE_PLAYER_PREPARED;
                            playbackMessenger.send(msg);
                        }
                        catch (RemoteException ex) {
                            Log.e(TAG, ex.getMessage(), ex);
                            if (appSettings.isErrorReportingEnabled())
                                new Reporter(ex).send();
                        }
                    }
                }
            });
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (appSettings.isMusicPlaybackContinue()) {
                        queueIndex++;
                        if (musicQueue != null && queueIndex < musicQueue.length() - 1) {
                            try {
                                Uri nextUri = Uri.parse(musicQueue.getString(queueIndex));
                                playFromUri(nextUri);
                            }
                            catch (Exception ex) {
                                Log.e(TAG, ex.getMessage(), ex);
                                if (appSettings.isErrorReportingEnabled())
                                    new Reporter(ex).send();
                            }
                        }
                    }
                    else {
                        stopPlayback();
                    }
                }
            });
        }
    }

    private void playFromUri(Uri uri) throws IOException {
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.reset();
        Log.d(TAG, "Play music: " + uri);
        ProxyInfo proxyInfo = MediaStreamProxy.needsAuthProxy(uri);
        if (proxyInfo == null) {
            mediaPlayer.setDataSource(this, uri);
        }
        else {
            // needs proxying to support authentication (see issue #55)
            proxy = new MediaStreamProxy(proxyInfo, AuthType.valueOf(appSettings.getMythTvServicesAuthType()));
            proxy.init();
            proxy.start();
            String playUrl = "http://" + proxy.getLocalhost().getHostAddress() + ":" + proxy.getPort() + uri.getPath();
            if (uri.getQuery() != null)
                playUrl += "?" + uri.getQuery();
            mediaPlayer.setDataSource(this, Uri.parse(playUrl));
        }

        mediaPlayer.prepareAsync();
    }

    private void releasePlayer() {
        isPaused = false;
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        releasePlayer();
    }
}