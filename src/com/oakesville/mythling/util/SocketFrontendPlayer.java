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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import io.oakesville.media.Item;
import io.oakesville.media.TvShow;

/**
 * Does not work with video storage groups.  Only used for features not supported by ServiceFrontendPlayer
 * (music playback and video playback when no storage groups).
 */
public class SocketFrontendPlayer implements FrontendPlayer {
    private static final String TAG = SocketFrontendPlayer.class.getSimpleName();

    private final AppSettings appSettings;
    private final String charSet;
    private final String basePath;
    private final Item item;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String status;

    public SocketFrontendPlayer(AppSettings settings, String basePath, Item item, String charSet) {
        this.appSettings = settings;
        this.basePath = basePath;
        this.item = item;
        this.charSet = charSet;
    }

    public boolean checkIsPlaying() throws IOException {
        int timeout = 5000;  // TODO: pref

        status = null;
        new StatusTask().execute();
        while (status == null && timeout > 0) {
            try {
                Thread.sleep(100);
                timeout -= 100;
            } catch (InterruptedException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
            }
        }
        if (status == null)
            throw new IOException(Localizer.getStringRes(R.string.error_frontend_status_) + appSettings.getFrontendHost() + ":" + appSettings.getFrontendSocketPort());

        return status.startsWith("Playback");
    }

    public void play() {
        new PlayItemTask().execute();
    }

    public void stop() {
        new StopTask().execute();
    }

    private class PlayItemTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                String filepath = basePath == null ? "" : basePath;
                if (item.getPath() != null)
                    filepath += "/" + item.getPath();
                filepath += "/" + item.getFileName();
                open(charSet);
                if (item.isMusic())
                    run("play music file " + filepath);
                else if (item.isRecording() || item.isLiveTv()) {
                    String loc = run("query location");
                    if (!"playbackbox".equals(loc))
                        run("jump playbackrecordings"); // avoid "ERROR: Timed out waiting for reply from player"
                    run("play program " + ((TvShow) item).getChannelId() + " " + ((TvShow) item).getStartTimeParam());
                } else
                    run("play file " + filepath);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), Localizer.getStringRes(R.string.frontend_playback_error_) + " '" + item.getFileName() + "': " + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class StopTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                open();
                if (item.isMusic())
                    run("play music stop");
                else if (item.isRecording())
                    run("play program stop");
                else
                    run("play stop\n");
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), Localizer.getStringRes(R.string.error_stopping_frontend_playback_) + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class StatusTask extends AsyncTask<URL, Integer, Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                open();
                status = run("query location");
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), Localizer.getStringRes(R.string.error_frontend_status_) + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String run(String command) throws IOException {
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Running frontend control socket command: '" + command + "'");
        out.println(command);
        out.flush();
        String line = in.readLine();
        if (line != null) {
            if (line.equals("MythFrontend Network Control"))  // why?
            {
                in.readLine();
                in.readLine();
                line = in.readLine();
            }
            if (line.startsWith("#"))
                line = line.substring(2);
            if (line.startsWith("ERROR:"))
                throw new IOException(line);
        }
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Frontend control socket response: " + line);
        return line;
    }

    private void open() throws IOException {
        open("UTF-8");
    }

    private void open(String charset) throws IOException {
        String frontendIp = appSettings.getFrontendHost();
        InetAddress serverAddr = InetAddress.getByName(frontendIp);
        int frontendPort = appSettings.getFrontendSocketPort();
        socket = new Socket(serverAddr, frontendPort);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset)), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void close() throws IOException {
        if (out != null)
            out.close();
        if (in != null)
            in.close();
        if (socket != null)
            socket.close();
    }
}
