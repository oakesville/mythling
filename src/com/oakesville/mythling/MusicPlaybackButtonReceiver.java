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
import com.oakesville.mythling.util.Reporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

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
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
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
                        Log.e(TAG, ex.getMessage(), ex);
                        if (new AppSettings(context).isErrorReportingEnabled())
                            new Reporter(ex).send();
                    }
                }
            }
        }
    }
}
