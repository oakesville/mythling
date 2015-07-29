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
package com.oakesville.mythling.util;

import java.io.IOException;
import java.net.URL;

import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.util.HttpHelper.AuthType;

public class ServiceFrontendPlayer implements FrontendPlayer {
    private static final String TAG = ServiceFrontendPlayer.class.getSimpleName();

    private AppSettings appSettings;
    private Item item;

    private String state;

    public ServiceFrontendPlayer(AppSettings appSettings, Item item) {
        this.appSettings = appSettings;
        this.item = item;
    }

    public boolean checkIsPlaying() throws IOException, JSONException {
        int timeout = 5000;  // TODO: pref

        state = null;
        new StatusTask().execute();
        while (state == null && timeout > 0) {
            try {
                Thread.sleep(100);
                timeout -= 100;
            } catch (InterruptedException ex) {
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
            }
        }
        if (state == null)
            throw new IOException(Localizer.getStringRes(R.string.error_frontend_status_) + appSettings.getFrontendServiceBaseUrl());

        return !state.equals("idle");
    }

    public void play() {
        new PlayItemTask().execute();
    }

    public void stop() {
        new StopTask().execute();
    }

    private class StatusTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                URL url = new URL(appSettings.getFrontendServiceBaseUrl() + "/Frontend/GetStatus");
                HttpHelper downloader = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
                String frontendStatusJson = new String(downloader.get(), "UTF-8");
                state = new MythTvParser(appSettings, frontendStatusJson).parseFrontendStatus();
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class PlayItemTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                URL url = appSettings.getFrontendServiceBaseUrl();
                if (item.isRecording() || item.isLiveTv())
                    url = new URL(url + "/Frontend/PlayRecording?" + ((Recording) item).getChanIdStartTimeParams());
                else if (item.isMusic())
                    throw new UnsupportedOperationException(Localizer.getStringRes(R.string.music_not_supported_by_svc_fe_player));
                else
                    url = new URL(url + "/Frontend/PlayVideo?Id=" + item.getId());

                HttpHelper poster = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
                String resJson = new String(poster.post(), "UTF-8");
                boolean res = new MythTvParser(appSettings, resJson).parseBool();
                if (!res)
                    throw new ServiceException(Localizer.getStringRes(R.string.frontend_playback_failed_) + url);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), Localizer.getStringRes(R.string.frontend_playback_error_) + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class StopTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                URL url = new URL(appSettings.getFrontendServiceBaseUrl() + "/Frontend/SendAction?Action=STOPPLAYBACK");
                HttpHelper poster = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
                String resJson = new String(poster.post(), "UTF-8");
                boolean res = new MythTvParser(appSettings, resJson).parseBool();
                if (!res)
                    throw new ServiceException(Localizer.getStringRes(R.string.error_stopping_frontend_playback_) + url);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), Localizer.getStringRes(R.string.error_frontend_status_) + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
