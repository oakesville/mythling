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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;

/**
 * Only used for music.
 * TODO: Either implement a playback service as described here:
 * http://developer.android.com/guide/topics/media/mediaplayer.html,
 * or use android.content.Intent.CATEGORY_APP_MUSIC to launch the
 * standard music player app.
 */
public class MusicPlaybackButtonReceiver extends BroadcastReceiver {

    private static final String TAG = MusicPlaybackButtonReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    try {
                        switch(event.getKeyCode()) {
                            case KeyEvent.KEYCODE_MEDIA_STOP :
                                Intent stopMusic = new Intent(context, MusicPlaybackService.class);
                                stopMusic.setAction(MusicPlaybackService.ACTION_STOP);
                                context.startService(stopMusic);
                                break;
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE :
                                Intent playPause = new Intent(context, MusicPlaybackService.class);
                                playPause.setAction(MusicPlaybackService.ACTION_PLAY_PAUSE);
                                context.startService(playPause);
                                break;
                        }
                    }
                    catch (Exception ex) {
                        if (BuildConfig.DEBUG)
                            Log.e(TAG, ex.getMessage(), ex);
                        if (new AppSettings(context).isErrorReportingEnabled())
                            new Reporter(ex).send();
                    }
                }
            }
        }
    }
}
