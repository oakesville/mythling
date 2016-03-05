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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.LiveStreamInfo;
import com.oakesville.mythling.media.Recording;

import android.util.Log;

public class Transcoder {
    private static final String TAG = Transcoder.class.getSimpleName();


    private AppSettings appSettings;

    /**
     * transcoder needs to know about the storage group to match-up
     * currently-executing jobs versus the requested playback item
     */
    public Transcoder(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    private LiveStreamInfo streamInfo;

    public LiveStreamInfo getStreamInfo() {
        return streamInfo;
    }

    public boolean beginTranscode(Item item) throws IOException {
        return beginTranscode(item, null);
    }

    /**
     * Returns true if a matching live stream already existed.  Must be called from a background thread.
     */
    public boolean beginTranscode(Item item, String videoQuality) throws IOException {
        URL baseUrl = appSettings.getMythTvServicesBaseUrl();

        boolean preExist = false;
        int maxTranscodes = appSettings.getTranscodeJobLimit();
        boolean filtered = false;  // filtering doesn't work for shit

        // check if stream is already available
        URL streamListUrl;
        if (filtered)
            streamListUrl = new URL(baseUrl + "/Content/GetFilteredLiveStreamList?FileName=" + item.getFileName().replaceAll(" ", "%20"));
        else
            streamListUrl = new URL(baseUrl + "/Content/GetLiveStreamList");

        String liveStreamJson = new String(getServiceDownloader(streamListUrl).get(), "UTF-8");
        List<LiveStreamInfo> liveStreams = new MythTvParser(appSettings, liveStreamJson).parseStreamInfoList();


        int[] resValues = appSettings.getVideoResValues();
        int[] vidBrValues = appSettings.getVideoBitrateValues();
        int[] audBrValues = appSettings.getAudioBitrateValues();

        int desiredRes;
        int desiredVidBr;
        int desiredAudBr;
        if (AppSettings.EXTERNAL_VIDEO_QUALITY.equals(videoQuality)) {
            desiredRes = appSettings.getExternalVideoRes();
            desiredVidBr = appSettings.getExternalVideoBitrate();
            desiredAudBr = appSettings.getExternalAudioBitrate();
        }
        else if (AppSettings.INTERNAL_VIDEO_QUALITY.equals(videoQuality)) {
            desiredRes = appSettings.getInternalVideoRes();
            desiredVidBr = appSettings.getInternalVideoBitrate();
            desiredAudBr = appSettings.getInternalAudioBitrate();
        }
        else {
            desiredRes = appSettings.getVideoRes();
            desiredVidBr = appSettings.getVideoBitrate();
            desiredAudBr = appSettings.getAudioBitrate();
        }

        int inProgress = 0;

        for (LiveStreamInfo liveStream : liveStreams) {
            if (liveStream.getStatusCode() != LiveStreamInfo.STATUS_CODE_STOPPED
                    && liveStream.matchesItem(item) && liveStream.matchesQuality(desiredRes, desiredVidBr, desiredAudBr, resValues, vidBrValues, audBrValues)) {
                streamInfo = liveStream;
                preExist = true;
                break;
            } else {
                if ("Transcoding".equals(liveStream.getMessage()))
                    inProgress++;
            }
        }

        if (streamInfo == null) {
            if (inProgress >= maxTranscodes && maxTranscodes != 0) {
                throw new RuntimeException(Localizer.getStringRes(R.string.transcode_jobs_running, String.valueOf(inProgress)));
            }
            // add the stream
            URL addStreamUrl;
            if (item.isRecording())
                addStreamUrl = new URL(baseUrl + "/Content/AddRecordingLiveStream?" + ((Recording) item).getChanIdStartTimeParams() + "&" + appSettings.getVideoQualityParams(videoQuality));
            else
                addStreamUrl = new URL(baseUrl + "/Content/AddVideoLiveStream?Id=" + item.getId() + "&" + appSettings.getVideoQualityParams(videoQuality));
            String addStreamJson = new String(getServiceDownloader(addStreamUrl).get(), "UTF-8");
            streamInfo = new MythTvParser(appSettings, addStreamJson).parseStreamInfo();

            // get the actual streamInfo versus requested
            try {
                // occasionally the follow-up request comes too soon, especially with multiple transcodes running on slower systems
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            URL getStreamUrl = new URL(baseUrl + "/Content/GetLiveStream?Id=" + streamInfo.getId());
            String getStreamJson = new String(getServiceDownloader(getStreamUrl).get(), "UTF-8");
            streamInfo = new MythTvParser(appSettings, getStreamJson).parseStreamInfo();
            if (streamInfo.getRelativeUrl().isEmpty())
                throw new IOException(Localizer.getStringRes(R.string.no_live_stream_found));
        }

        return preExist;
    }

    /**
     * Wait for content to be available.
     */
    public void waitAvailable() throws IOException, InterruptedException {
        // wait for content to be available
        String streamUrl = appSettings.getMythTvServicesBaseUrl() + streamInfo.getRelativeUrl();
        // avoid retrieving unnecessary audio-only streams
        int lastDot = streamUrl.lastIndexOf('.');
        streamUrl = streamUrl.substring(0, lastDot) + ".av" + streamUrl.substring(lastDot);

        byte[] streamBytes = null;
        int timeout = appSettings.getTranscodeTimeout() * 1000;
        boolean hasTs = false;
        while ((streamBytes == null || !hasTs) && timeout > 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Awaiting transcode...");
                Log.d(TAG, "streamInfo.getRelativeUrl(): " + streamInfo.getRelativeUrl());
                Log.d(TAG, "streamUrl: " + streamUrl);
            }
            long before = System.currentTimeMillis();
            try {
                streamBytes = getServiceDownloader(new URL(streamUrl)).get();
                if (streamBytes != null) {
                    hasTs = new String(streamBytes).indexOf(".ts") > 0;
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "streamBytes:\n" + new String(streamBytes));
                }
            } catch (IOException ex) {
                // keep trying
            }
            long elapsed = System.currentTimeMillis() - before;
            if (elapsed < 1000) {
                Thread.sleep(1000 - elapsed);
                timeout -= 1000;
            } else {
                timeout -= elapsed;
            }
        }

        if (!hasTs)
            throw new FileNotFoundException(Localizer.getStringRes(R.string.no_stream_available_) + "\n" + streamUrl);

        // wait one more second for good measure
        int lagSeconds = 1;  // TODO: prefs?
        Thread.sleep(lagSeconds * 1000);
    }

    private HttpHelper getServiceDownloader(URL url) throws MalformedURLException {
        HttpHelper downloader = new HttpHelper(appSettings.getUrls(url), appSettings.getMythTvServicesAuthType(), appSettings.getPrefs());
        downloader.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
        return downloader;
    }

}
