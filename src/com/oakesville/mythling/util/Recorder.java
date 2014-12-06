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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.json.JSONException;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TunerInUseException;
import com.oakesville.mythling.media.TvShow;

public class Recorder
{
  private static final String TAG = Recorder.class.getSimpleName();

  private AppSettings appSettings;
  private Map<String,StorageGroup> storageGroups;
  
  public Recorder(AppSettings appSettings, Map<String,StorageGroup> storageGroups)
  {
    this.appSettings = appSettings;
    this.storageGroups = storageGroups;
  }
  
  private TvShow tvShow;
  private int recRuleId;
  private Recording recording;
  public Recording getRecording() { return recording; }
  
  /**
   * Returns true if a matching recording was already scheduled.  Must be called from a background thread.
   */
  public boolean scheduleRecording(TvShow show) throws IOException, JSONException, ParseException
  {
    boolean preExist = false;
    
    // check whether there's a recording for chanid and starttime
    tvShow = show;
    recording = getRecording(show);
    if (recording != null)
    {
      preExist = true;
    }
    else
    {
      // schedule the recording
      URL addRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/AddRecordSchedule?" + show.getChanIdStartTimeParams()
          + "&EndTime=" + show.getEndTimeParam() + "&Title=" + show.getEncodedTitle() + "&Station=" + show.getCallsign() + "&FindDay=0&FindTime=00:00:00");
        
      String addRecJson = new String(getServiceHelper(addRecUrl).post());
      recRuleId = new MythTvParser(appSettings, addRecJson).parseUint();
      if (recRuleId <= 0)
        throw new IOException("Problem scheduling recording for: " + show.getTitle());
    }

    return preExist;
  }
  
  /**
   * Wait for recording to be available.
   */
  public void waitAvailable() throws IOException, JSONException, ParseException, InterruptedException
  {
    // wait for content to be available
    int timeout = appSettings.getTunerTimeout() * 1000;
    while (recording == null && timeout > 0)
    {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "Awaiting recording ...");
      long before = System.currentTimeMillis();
      recording = getRecording(tvShow);
      long elapsed = System.currentTimeMillis() - before;
      if (elapsed < 1000)
      {
        Thread.sleep(1000 - elapsed);
        timeout -= 1000;
      }
      else
      {
        timeout -= elapsed;
      }
    }
    
    if (recording == null)
    {
      if (recRuleId > 0)
      {
        // remove the recording rule
        URL remRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/RemoveRecordSchedule?RecordId=" + recRuleId);
        getServiceHelper(remRecUrl).post();
      }
      throw new FileNotFoundException("No recording available (there may be a scheduling conflict, or possibly a tuner timeout occurred).");
    }
    
    // wait a few seconds
    int lagSeconds = 10;  // TODO: prefs
    Thread.sleep(lagSeconds * 1000);
  }
  
  public void deleteRecording(Recording recording) throws IOException, JSONException, InterruptedException
  {
    // delete the recording
    URL delRecUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Dvr/RemoveRecorded?ChanId=" + recording.getChannelId() + "&StartTime=" + recording.getStartTimeParam());
    String delRecRes = new String(getServiceHelper(delRecUrl).post());
    if (BuildConfig.DEBUG)
      Log.d(TAG, "Delete recording result: " + delRecRes);
    
    boolean deleteResult = new MythTvParser(appSettings, delRecRes).parseBool();
    if (!deleteResult)
      throw new IOException("Problem deleting recording for: " + recording.getTitle());

    // wait for recording to be deleted
    int lagSeconds = 5; // TODO: prefs
    Thread.sleep(lagSeconds * 1000);
  }
  
  private Recording getRecording(TvShow tvShow) throws IOException, JSONException, ParseException
  {
    HttpHelper recordingsHelper = appSettings.getMediaListDownloader(new URL[]{appSettings.getMediaListUrl(MediaType.recordings)});
    String recordingsListJson = new String(recordingsHelper.get());
    MediaListParser jsonParser = appSettings.getMediaListParser(recordingsListJson);
    MediaList recordingsList = jsonParser.parseMediaList(MediaType.recordings, storageGroups);
    Date now = new Date();
    for (Category cat : recordingsList.getCategories())
    {
      for (Item item : cat.getItems())
      {
        Recording rec = ((Recording)item);
        if (rec.getChannelId() == tvShow.getChannelId() && rec.getProgramStart().equals(tvShow.getStartTimeRaw()))
        {
          recording = rec;
          recording.setPath("");
          return recording;
        }
        else if (!"Deleted".equals(rec.getRecordingGroup()) && rec.getStartTime().compareTo(now) <= 0 && rec.getEndTime().compareTo(now) >= 0)
        {
          if (rec.getRecordId() != 0)
          {
            TunerInUseException ex = new TunerInUseException(rec.toString());
            ex.setRecording(rec);
            throw ex;
          }
        }
      }
    }
    return null;
  }
  
  private HttpHelper getServiceHelper(URL url) throws MalformedURLException
  {
    HttpHelper helper = new HttpHelper(appSettings.getUrls(url), appSettings.getMythTvServicesAuthType(), appSettings.getPrefs());
    helper.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
    return helper;
  }
}
