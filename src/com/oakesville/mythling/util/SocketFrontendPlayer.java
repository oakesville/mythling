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

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.TvShow;

/**
 * Does not work with video storage groups.  Only used for features not supported by ServiceFrontendPlayer
 * (music playback and video playback when no storage groups).
 */
public class SocketFrontendPlayer implements FrontendPlayer {
    private static final String TAG = SocketFrontendPlayer.class.getSimpleName();

    private AppSettings appSettings;
    private String charSet;
    private String basePath;
    private Item item;
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
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
            }
        }
        if (status == null)
            throw new IOException("Unable to connect to mythfrontend: " + appSettings.getFrontendHost() + ":" + appSettings.getFrontendSocketPort());

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
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), "Error playing file '" + item.getFileName() + "': " + ex.toString(), Toast.LENGTH_LONG).show();
            } else {
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
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), "Error stopping playback: " + ex.toString(), Toast.LENGTH_LONG).show();
            } else {
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
                if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                if (appSettings.isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            } finally {
                try {
                    close();
                } catch (IOException ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(appSettings.getAppContext(), "Error checking status: " + ex.toString(), Toast.LENGTH_LONG).show();
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
