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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;

public class MusicPlaybackService extends Service {
    
    private static final String TAG = MusicPlaybackService.class.getSimpleName();
    
    public static final String EXTRA_MESSENGER = "com.oakesville.mythling.EXTRA_MUSIC_PLAYBACK_MESSENGER";
    public static final int MESSAGE_PLAYER_PREPARED = 0;
    public static final int MESSAGE_PLAYBACK_STOPPED = 1; // different from action (callback to initiator)
    
    public static final String ACTION_PLAYBACK_STOPPED = "com.oakesville.mythling.PLAYBACK_STOPPED";
    
    public static final String ACTION_PLAY = "com.oakesville.mythling.PLAY";
    public static final String ACTION_STOP = "com.oakesville.mythling.STOP";
    public static final String ACTION_PLAY_PAUSE = "com.oakesville.mythling.PLAY_PAUSE";
    
    private AppSettings appSettings;
    private MediaPlayer mediaPlayer;
    private Messenger playbackMessenger;
    private OnAudioFocusChangeListener audioFocusListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        try { 
            appSettings = new AppSettings(this);
            
            Bundle extras = intent.getExtras();
            
            if (extras != null)
                playbackMessenger = (Messenger)extras.get(EXTRA_MESSENGER);
    
            if (intent.getAction().equals(ACTION_PLAY)) {
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
                                    if (BuildConfig.DEBUG)
                                        Log.e(TAG, ex.getMessage(), ex);
                                    if (appSettings.isErrorReportingEnabled())
                                        new Reporter(ex).send();
                                }
                            }
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                        public void onCompletion(MediaPlayer mp) {
                            stopPlayback();
                        }
                    });
                    
                    mediaPlayer.setDataSource(this, intent.getData());
                    mediaPlayer.prepareAsync();
                }
            }
            else if (intent.getAction().equals(ACTION_PLAY_PAUSE)) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.pause();
                    else
                        mediaPlayer.start();
                }
            }
            else if (intent.getAction().equals(ACTION_STOP)) {
                stopPlayback();
            }
        }
        catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
        }

        return START_NOT_STICKY;
    }
    
    private void stopPlayback() {
        stopSelf();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                sendBroadcast(new Intent(ACTION_PLAYBACK_STOPPED));
                releasePlayer();
            }
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(audioFocusListener);
            if (playbackMessenger != null) {
                try {
                    Message msg = Message.obtain();
                    msg.what = MESSAGE_PLAYBACK_STOPPED;
                    playbackMessenger.send(msg);
                }
                catch (RemoteException ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }       
    }
    
    private void releasePlayer() {
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