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
import java.util.List;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.LiveStreamInfo;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.StorageGroup;

public class Transcoder
{
  private static final String TAG = Transcoder.class.getSimpleName();

  private AppSettings appSettings;
  
  // for matching up existing transcodes
  private StorageGroup storageGroup;
  private String basePath;
  
  /**
   * transcoder needs to know about the storage group to match-up
   * currently-executing jobs versus the requested playback item
   */
  public Transcoder(AppSettings appSettings, StorageGroup storageGroup)
  {
    this.appSettings = appSettings;
    this.storageGroup = storageGroup;
  }

  public Transcoder(AppSettings appSettings, String basePath)
  {
    this.appSettings = appSettings;
    this.basePath = basePath;
  }
  
  private LiveStreamInfo streamInfo;
  public LiveStreamInfo getStreamInfo() { return streamInfo; }
  
  /**
   * Returns true if a matching live stream already existed.  Must be called from a background thread.
   */
  public boolean beginTranscode(Item item) throws IOException
  {
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
    List<LiveStreamInfo> liveStreams = new MythTvParser(liveStreamJson, appSettings).parseStreamInfoList();
    int inProgress = 0;
    for (LiveStreamInfo liveStream : liveStreams)
    {
      if (liveStreamMatchesItemAndQuality(liveStream, item))
      {
        streamInfo = liveStream;
        preExist = true;
      }
      else
      {
        if ("Transcoding".equals(liveStream.getMessage()))
        {
          if (liveStreamMatchesItem(liveStream, item))
          {
            // stop and delete in-progress transcoding jobs for same file
            try
            {
              //getDownloader(new URL(baseUrl + "/Content/StopLiveStream?Id=" + liveStream.getId())).retrieve();
              getServiceDownloader(new URL(baseUrl + "/Content/RemoveLiveStream?Id=" + liveStream.getId())).get();
            }
            catch (Exception ex)
            {
              // don't let this stop us
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            }
          }
          else
          {
            inProgress++;
          }
        }
      }
    }
    
    if (streamInfo == null)
    {
      if (inProgress >= maxTranscodes)
      {
        throw new RuntimeException("Already " + inProgress + " transcode jobs running");
      }
      // add the stream
      URL addStreamUrl;
      if (item.isRecording())
        addStreamUrl = new URL(baseUrl + "/Content/AddRecordingLiveStream?" + ((Recording)item).getChanIdStartTimeParams() + "&" + appSettings.getVideoQualityParams());
      else
        addStreamUrl = new URL(baseUrl + "/Content/AddVideoLiveStream?Id=" + item.getId() + "&" + appSettings.getVideoQualityParams());
      String addStreamJson = new String(getServiceDownloader(addStreamUrl).get(), "UTF-8");
      streamInfo = new MythTvParser(addStreamJson, appSettings).parseStreamInfo();
      
      // get the actual streamInfo versus requested
      URL getStreamUrl = new URL(baseUrl + "/Content/GetLiveStream?Id=" + streamInfo.getId());
      String getStreamJson = new String(getServiceDownloader(getStreamUrl).get(), "UTF-8");
      streamInfo = new MythTvParser(getStreamJson, appSettings).parseStreamInfo();
      if (streamInfo.getRelativeUrl().isEmpty())
        throw new IOException("No live stream found.");
    }    
    
    return preExist; 
  }
  
  /**
   * Wait for content to be available.
   */
  public void waitAvailable() throws IOException, InterruptedException
  {
    // wait for content to be available
    String streamUrl = appSettings.getMythTvServicesBaseUrl() + streamInfo.getRelativeUrl();
    // avoid retrieving unnecessary audio-only streams
    int lastDot = streamUrl.lastIndexOf('.');
    streamUrl = streamUrl.substring(0, lastDot) + ".av" + streamUrl.substring(lastDot);

    byte[] streamBytes = null;
    int timeout = appSettings.getTranscodeTimeout() * 1000;
    boolean hasTs = false;
    while ((streamBytes == null || !hasTs) && timeout > 0)
    {
      if (BuildConfig.DEBUG)
      {
        Log.d(TAG, "Awaiting transcode...");
        Log.d(TAG, "streamInfo.getRelativeUrl(): " + streamInfo.getRelativeUrl());
        Log.d(TAG, "streamUrl: " + streamUrl);
      }
      long before = System.currentTimeMillis();
      try
      {
        streamBytes = getServiceDownloader(new URL(streamUrl)).get();
        if (streamBytes != null)
        {
          hasTs = new String(streamBytes).indexOf(".ts") > 0;
          if (BuildConfig.DEBUG)
            Log.d(TAG, "streamBytes:\n" + new String(streamBytes));
        }
      }
      catch (IOException ex)
      {
        // keep trying
      }
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
    
    if (!hasTs)
      throw new FileNotFoundException("No stream available: " + streamUrl);
    
    // wait one more second for good measure
    int lagSeconds = 1;  // TODO: prefs?
    Thread.sleep(lagSeconds * 1000);
  }
  
  private HttpHelper getServiceDownloader(URL url) throws MalformedURLException
  {
    HttpHelper downloader = new HttpHelper(appSettings.getUrls(url), appSettings.getMythTvServicesAuthType(), appSettings.getPrefs());
    downloader.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
    return downloader;
  }
  
  private boolean liveStreamMatchesItem(LiveStreamInfo liveStream, Item item)
  {
    String itemPath = item.getPath().isEmpty() ? item.getFileName() : item.getPath() + "/" + item.getFileName();
    if (storageGroup == null)
    {
      return liveStream.getFile().equals(basePath + "/" + itemPath);
    }
    else
    {
      for (String dir : storageGroup.getDirectories())
      {
        if (liveStream.getFile().equals(dir + "/" + itemPath))
          return true;
      }
    }
    return false;
  }
  
  private boolean liveStreamMatchesItemAndQuality(LiveStreamInfo liveStream, Item item)
  {
    if (!liveStreamMatchesItem(liveStream, item))
      return false;
    
    int desiredRes = appSettings.getVideoRes();
    int resDiff = Math.abs(liveStream.getHeight() - desiredRes);
    int[] resValues = appSettings.getVideoResValues();
    for (int i = 0; i < resValues.length; i++)
    {
      if (resDiff > Math.abs(liveStream.getHeight() - resValues[i]))
        return false;
    }
    
    int desiredVidBr = appSettings.getVideoBitrate();
    int vidBrDiff = Math.abs(liveStream.getVideoBitrate() - desiredVidBr);
    int[] vidBrValues = appSettings.getVideoBitrateValues();
    for (int i = 0; i < vidBrValues.length; i++)
    {
      if (vidBrDiff > Math.abs(liveStream.getVideoBitrate() - vidBrValues[i]))
        return false;
    }
    
    int desiredAudBr = appSettings.getAudioBitrate();
    int audBrDiff = Math.abs(liveStream.getAudioBitrate() - desiredAudBr);
    int[] audBrValues = appSettings.getAudioBitrateValues();
    for (int i = 0; i < audBrValues.length; i++)
    {
      if (audBrDiff > Math.abs(liveStream.getAudioBitrate() - audBrValues[i]))
        return false;
    }
    
    return true;
  }

}
