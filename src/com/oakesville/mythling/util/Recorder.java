/**
 * Copyright 2013 Donald Oakes
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
import java.util.Date;

import org.json.JSONException;

import android.util.Log;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Category;
import com.oakesville.mythling.app.TunerInUseException;
import com.oakesville.mythling.app.Work;
import com.oakesville.mythling.app.WorksList;
import com.oakesville.mythling.BuildConfig;

public class Recorder
{
  public static final String TAG = Recorder.class.getSimpleName();

  private AppSettings appSettings;
  
  public Recorder(AppSettings appSettings)
  {
    this.appSettings = appSettings;
  }
  
  private Work tvShow;
  private int recRuleId;
  private Work recording;
  public Work getRecording() { return recording; }
  
  /**
   * Returns true if a matching recording was already scheduled.  Must be called from a background thread.
   */
  public boolean scheduleRecording(Work work) throws IOException, JSONException
  {
    boolean preExist = false;
    
    // check whether there's a recording for chanid and starttime
    tvShow = work;
    recording = getRecording(work);
    if (recording != null)
    {
      preExist = true;
    }
    else
    {
      // schedule the recording
      URL addRecUrl = new URL(appSettings.getServicesBaseUrl() + "/Dvr/AddRecordSchedule?ChanId=" + work.getChannelId() + "&StartTime=" + work.getStartTimeParam());
        
      String addRecJson = new String(getServiceHelper(addRecUrl).post());
      recRuleId = new JsonParser(addRecJson).parseInt();
      if (recRuleId <= 0)
        throw new IOException("Problem scheduling recording for: " + work.getTitle());
    }

    return preExist;
  }
  
  /**
   * Wait for recording to be available.
   */
  public void waitAvailable() throws IOException, InterruptedException
  {
    // wait for content to be available
    int timeout = appSettings.getTunerTimeout() * 1000;
    while (recording == null && timeout > 0)
    {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "Awaiting recording ...");
      Thread.sleep(1000);
      recording = getRecording(tvShow);
      timeout -= 1000;
    }
    
    if (recording == null)
    {
      if (recRuleId > 0)
      {
        // remove the recording rule
        URL remRecUrl = new URL(appSettings.getServicesBaseUrl() + "/Dvr/RemoveRecordSchedule?RecordId=" + recRuleId);
        getServiceHelper(remRecUrl).post();
      }
      throw new FileNotFoundException("No recording available.");
    }
    
    // wait a few seconds
    int lagSeconds = 10;  // TODO: prefs
    Thread.sleep(lagSeconds * 1000);
  }
  
  public void deleteRecording(Work recording) throws IOException, JSONException, InterruptedException
  {
    // delete the recording
    URL delRecUrl = new URL(appSettings.getServicesBaseUrl() + "/Dvr/RemoveRecorded?ChanId=" + recording.getChannelId() + "&StartTime=" + recording.getStartTimeParam());
    String delRecRes = new String(getServiceHelper(delRecUrl).post());
    if (BuildConfig.DEBUG)
      Log.d(TAG, "Delete recording result: " + delRecRes);
    
    boolean deleteResult = new JsonParser(delRecRes).parseBool();
    if (!deleteResult)
      throw new IOException("Problem deleting recording for: " + recording.getTitle());

    // wait for recording to be deleted
    int lagSeconds = 5; // TODO: prefs
    Thread.sleep(lagSeconds * 1000);
  }
  
  private Work getRecording(Work tvShow) throws IOException
  {
    HttpHelper recordingsHelper = getWebHelper(new URL(appSettings.getWebBaseUrl() + "/works.php?type=recordings"));
    String recordingsListJson = new String(recordingsHelper.get());
    WorksList recordingsList = new JsonParser(recordingsListJson).parseWorksList();
    Date now = new Date();
    for (Category cat : recordingsList.getCategories())
    {
      for (Work rec : cat.getWorks())
      {
        if (rec.getChannelId() == tvShow.getChannelId() && rec.getProgramStart().equals(tvShow.getStartTimeRaw()))
        {
          recording = rec;
          recording.setPath(recordingsList.getBasePath());
          return recording;
        }
        else if (rec.getStartTime().compareTo(now) <= 0 && rec.getEndTime().compareTo(now) >= 0)
        {
          if (rec.getRecordingRuleId() != 0)
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
  
  private HttpHelper getWebHelper(URL url) throws MalformedURLException
  {
    HttpHelper downloader = new HttpHelper(appSettings.getUrls(url), appSettings.getWebAuthType(), appSettings.getPrefs());
    downloader.setCredentials(appSettings.getWebAccessUser(), appSettings.getWebAccessPassword());
    return downloader;
  }
  
  private HttpHelper getServiceHelper(URL url) throws MalformedURLException
  {
    HttpHelper helper = new HttpHelper(appSettings.getUrls(url), appSettings.getServicesAuthType(), appSettings.getPrefs());
    helper.setCredentials(appSettings.getServicesAccessUser(), appSettings.getServicesAccessPassword());
    return helper;
  }
}
