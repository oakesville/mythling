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
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.json.JSONException;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.AllTunersInUseException;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TvShow;

import android.util.Log;

public class Recorder {
    private static final String TAG = Recorder.class.getSimpleName();

    private AppSettings appSettings;
    private Map<String,StorageGroup> storageGroups;

    public Recorder(AppSettings appSettings, Map<String,StorageGroup> storageGroups) {
        this.appSettings = appSettings;
        this.storageGroups = storageGroups;
    }

    public Recorder(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    private TvShow tvShow;
    private int recRuleId;
    private Recording recording;

    public Recording getRecording() {
        return recording;
    }

    /**
     * Returns true if a matching recording was already scheduled.  Must be called from a background thread.
     */
    public boolean scheduleRecording(TvShow show) throws IOException, JSONException, ParseException {
        boolean preExist = false;

        // check whether there's a recording for chanid and starttime
        tvShow = show;
        recording = getRecording(show);
        if (recording != null) {
            preExist = true;
        } else {
            // schedule the recording
            URL addRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/AddRecordSchedule?" + show.getChanIdStartTimeParams()
                    + "&EndTime=" + show.getEndTimeParam() + "&Title=" + show.getEncodedTitle() + "&Station=" + show.getCallsign() + "&FindDay=0&FindTime=00:00:00&Type=single");

            String addRecJson = new String(getServiceHelper(addRecUrl).post());
            recRuleId = new MythTvParser(appSettings, addRecJson).parseUint();
            if (recRuleId <= 0)
                throw new IOException(Localizer.getStringRes(R.string.problem_scheduling_recording_) + show.getTitle());
        }

        return preExist;
    }

    /**
     * Wait for recording to be available.
     */
    public void waitAvailable() throws IOException, JSONException, ParseException, InterruptedException {
        // wait for content to be available
        int timeout = appSettings.getTunerTimeout() * 1000;
        while (recording == null && timeout > 0) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Awaiting recording ...");
            long before = System.currentTimeMillis();
            recording = getRecording(tvShow);
            long elapsed = System.currentTimeMillis() - before;
            if (elapsed < 1000) {
                Thread.sleep(1000 - elapsed);
                timeout -= 1000;
            } else {
                timeout -= elapsed;
            }
        }

        if (recording == null) {
            if (recRuleId > 0) {
                // remove the recording rule
                URL remRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/RemoveRecordSchedule?RecordId=" + recRuleId);
                getServiceHelper(remRecUrl).post();
            }
            throw new FileNotFoundException(Localizer.getStringRes(R.string.no_recording_available));
        }

        // wait a few seconds
        int lagSeconds = 10;  // TODO: prefs
        Thread.sleep(lagSeconds * 1000);
    }

    public void deleteRecording(Recording recording) throws IOException, JSONException, InterruptedException {
        // delete the recording
        URL delRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/RemoveRecorded?ChanId=" + recording.getChannelId() + "&StartTime=" + recording.getStartTimeParam());
        String delRecRes = new String(getServiceHelper(delRecUrl).post());
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Delete recording result: " + delRecRes);

        boolean deleteResult = new MythTvParser(appSettings, delRecRes).parseBool();
        if (!deleteResult)
            throw new IOException(Localizer.getStringRes(R.string.problem_deleting_recording_) + recording.getTitle());
    }

    private Recording getRecording(TvShow tvShow) throws IOException, JSONException, ParseException {
        HttpHelper recordingsHelper = appSettings.getMediaListDownloader(new URL[]{appSettings.getMediaListUrl(MediaType.recordings)});
        String recordingsListJson = new String(recordingsHelper.get());
        MediaListParser jsonParser = appSettings.getMediaListParser(recordingsListJson);
        MediaList recordingsList = jsonParser.parseMediaList(MediaType.recordings, storageGroups);
        Date now = new Date();
        int tunersInUse = 0;
        for (Category cat : recordingsList.getCategories()) {
            for (Item item : cat.getItems()) {
                Recording rec = ((Recording) item);
                if (rec.getChannelId() == tvShow.getChannelId() && rec.getProgramStart().equals(tvShow.getStartTimeRaw())) {
                    recording = rec;
                    return recording;
                } else if (!"Deleted".equals(rec.getRecordingGroup()) && rec.getStartTime().compareTo(now) <= 0 && rec.getEndTime().compareTo(now) >= 0) {
                    if (rec.getRecordId() != 0) {
                        tunersInUse++;
                    }
                }
            }
        }
        int tunerLimit = appSettings.getTunerLimit();
        if (tunerLimit != 0 && tunersInUse >= tunerLimit)
            throw new AllTunersInUseException(tunerLimit);
        return null;
    }

    private HttpHelper getServiceHelper(URL url) throws MalformedURLException {
        HttpHelper helper = new HttpHelper(appSettings.getUrls(url), appSettings.getMythTvServicesAuthType(), appSettings.getPrefs());
        helper.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
        return helper;
    }
}
