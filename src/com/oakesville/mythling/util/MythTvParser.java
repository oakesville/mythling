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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.LiveStreamInfo;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.Movie;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TvEpisode;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.media.Video;

public class MythTvParser implements MediaListParser
{
  private static final String TAG = MythTvParser.class.getSimpleName();

  private String json;
  private AppSettings appSettings;
  
  public MythTvParser(String json, AppSettings appSettings)
  {
    this.json = json;
    this.appSettings = appSettings;
  }
  
  public MediaList parseMediaList(MediaType mediaType) throws JSONException, ParseException
  {
    return parseMediaList(mediaType, null, null, null);
  }

  /**
   * @param mediaType
   * @param storageGroup media storage group
   */
  public MediaList parseMediaList(MediaType mediaType, StorageGroup storageGroup, StorageGroup artworkStorageGroup) throws JSONException, ParseException
  {
    return parseMediaList(mediaType, storageGroup, null, artworkStorageGroup);
  }

  /**
   * @param mediaType
   * @param basePath base path to trim from filename (when no storage group)
   */
  public MediaList parseMediaList(MediaType mediaType, String basePath, StorageGroup artworkStorageGroup) throws JSONException, ParseException
  {
    return parseMediaList(mediaType, null, basePath, artworkStorageGroup);
  }
  
  /**
   * @param mediaType
   * @param storageGroup media storage group
   * @param basePath base path to trim from filename (when no storage group)
   */
  private MediaList parseMediaList(MediaType mediaType, StorageGroup storageGroup, String basePath, StorageGroup artworkStorageGroup) throws JSONException, ParseException
  {
    MediaList mediaList = new MediaList();
    mediaList.setMediaType(mediaType);
    mediaList.setStorageGroup(storageGroup);
    mediaList.setBasePath(basePath);
    long startTime = System.currentTimeMillis();
    JSONObject list = new JSONObject(json);
    SortType sortType = appSettings.getMediaSettings().getSortType();
    if (list.has("VideoMetadataInfoList"))
    {
      // videos, movies, or tvSeries
      MediaSettings mediaSettings = appSettings.getMediaSettings();
      JSONObject infoList = list.getJSONObject("VideoMetadataInfoList");
      mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
      JSONArray vids = infoList.getJSONArray("VideoMetadataInfos");
      
      String[] movieDirs = appSettings.getMovieDirs();
      String[] tvSeriesDirs = appSettings.getTvSeriesDirs();
      String[] vidExcludeDirs = appSettings.getVidExcludeDirs();
      
      int count = 0;
      for (int i = 0; i < vids.length(); i++)
      {
        JSONObject vid = (JSONObject) vids.get(i);
        MediaType type = MediaType.videos;
        // determine type
        if (mediaSettings.getTypeDeterminer() == MediaTypeDeterminer.directories)
        {
          if (vid.has("FileName"))
          {
            String filePath = vid.getString("FileName");
            if (mediaList.getStorageGroup() == null && filePath.startsWith(basePath + "/"))
              filePath = filePath.substring(basePath.length() + 1);
            
            for (String movieDir : movieDirs)
            {
              if (filePath.startsWith(movieDir))
              {
                type = MediaType.movies;
                break;
              }
            }
            for (String tvDir : tvSeriesDirs)
            {
              if (filePath.startsWith(tvDir))
              {
                type = MediaType.tvSeries;
                break;
              }
            }
            for (String vidExcludeDir : vidExcludeDirs)
            {
              if (filePath.startsWith(vidExcludeDir))
              {
                type = null;
                break;
              }
            }
          }
        }
        else if (mediaSettings.getTypeDeterminer() == MediaTypeDeterminer.metadata)
        {
          if (vid.has("Season"))
          {
            String season = vid.getString("Season");
            if (!season.isEmpty() && !season.equals("0"))
              type = MediaType.tvSeries;
          }
          if (type != MediaType.tvSeries && vid.has("Inetref"))
          {
            String inetref = vid.getString("Inetref");
            if (!inetref.isEmpty() && !inetref.equals("00000000"))
              type = MediaType.movies;
          }
        }
        
        if (type == mediaType)
        {
          mediaList.addItemUnderPathCategory(buildVideoItem(type, vid, artworkStorageGroup));
          count++;
        }
      }
      mediaList.setCount(count);
    }
    else if (list.has("ProgramList"))
    {
      // recordings
      JSONObject infoList = list.getJSONObject("ProgramList");
      mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
      mediaList.setCount(infoList.getString("Count"));
      JSONArray recs = infoList.getJSONArray("Programs");
      for (int i = 0; i < recs.length(); i++)
      {
        JSONObject rec = (JSONObject) recs.get(i);
        Item recItem = buildRecordingItem(rec);
        ViewType viewType = appSettings.getMediaSettings().getViewType(); 
        if (viewType == ViewType.list && (sortType == null || sortType == SortType.byTitle))
        {
          // categorize by title
          Category cat = mediaList.getCategory(recItem.getTitle());
          if (cat == null)
          {
            cat = new Category(recItem.getTitle(), MediaType.recordings);
            mediaList.addCategory(cat);
          }
          cat.addItem(recItem);
        }
        else
        {
          mediaList.addItem(recItem);
        }
      }
    }
    else if (list.has("ProgramGuide"))
    {
      // live tv
      JSONObject infoList = list.getJSONObject("ProgramGuide");
      mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
      mediaList.setCount(infoList.getString("Count"));
      JSONArray chans = infoList.getJSONArray("Channels");
      for (int i = 0; i < chans.length(); i++)
      {
        JSONObject chanInfo = (JSONObject) chans.get(i);
        Item show = buildLiveTvItem(chanInfo);
        if (show != null)
          mediaList.addItem(show);
      }
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, " -> media list parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    if (sortType != null)
    {
      startTime = System.currentTimeMillis();
      mediaList.sort(sortType, true);
      if (BuildConfig.DEBUG)
        Log.d(TAG, " -> media list sort time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    return mediaList;    
  }
  
  private Video buildVideoItem(MediaType type, JSONObject vid, StorageGroup artworkStorageGroup) throws JSONException, ParseException
  {
    Video item;
    if (type == MediaType.movies)
    {
      item = new Movie(vid.getString("Id"), vid.getString("Title"));
    }
    else if (type == MediaType.tvSeries)
    {
      item = new TvEpisode(vid.getString("Id"), vid.getString("Title"));
      if (vid.has("Season"))
      {
        String season = vid.getString("Season");
        if (!season.isEmpty() && !season.equals("0"))
          ((TvEpisode)item).setSeason(Integer.parseInt(season));
      }
      if (vid.has("Episode"))
      {
        String episode = vid.getString("Episode");
        if (!episode.isEmpty() && !episode.equals("0"))
          ((TvEpisode)item).setEpisode(Integer.parseInt(episode));
      }
    }
    else
    {
      item = new Video(vid.getString("Id"), vid.getString("Title"));
    }
    
    String filename = vid.getString("FileName");
    int lastdot = filename.lastIndexOf('.');
    item.setFileBase(filename.substring(0, lastdot));
    item.setFormat(filename.substring(lastdot + 1));
    if (vid.has("SubTitle"))
    {
      String subtitle = vid.getString("SubTitle");
      if (!subtitle.isEmpty())
        item.setSubTitle(subtitle);
    }
    if (vid.has("Director"))
    {
      String director = vid.getString("Director");
      if (!director.isEmpty() && !director.equals("Unknown"))
        item.setDirector(director);
    }
    if (vid.has("Description"))
    {
      String description = vid.getString("Description");
      if (!description.equals("None"))
        item.setSummary(description);
    }
    
    if (vid.has("Inetref"))
    {
      String inetref = vid.getString("Inetref");
      if (!inetref.isEmpty() && !inetref.equals("00000000"))
        item.setInternetRef(inetref);
    }
    if (vid.has("HomePage"))
    {
      String pageUrl = vid.getString("HomePage");
      if (!pageUrl.isEmpty())
        item.setPageUrl(pageUrl);
    }
    if (vid.has("ReleaseDate"))
    {
      String releaseDate = vid.getString("ReleaseDate");
      if (!releaseDate.isEmpty())
      {
        String dateStr = releaseDate.replace('T', ' ');
        if (dateStr.endsWith("Z"))
          dateStr = dateStr.substring(0, dateStr.length() - 1);
        Date date = DateTimeFormats.SERVICE_DATE_FORMAT.parse(dateStr + " UTC");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        item.setYear(cal.get(Calendar.YEAR));
      }
    }
    if (vid.has("UserRating"))
    {
      String rating = vid.getString("UserRating");
      if (!rating.isEmpty() && !rating.equals("0"))
        item.setRating(Float.parseFloat(rating)/2);
    }
    
    if (vid.has("Artwork") && artworkStorageGroup != null)
    {
      JSONObject artwork = vid.getJSONObject("Artwork");
      if (artwork.has("ArtworkInfos"))
      {
        JSONArray artworkInfos = artwork.getJSONArray("ArtworkInfos");
        for (int i = 0; i < artworkInfos.length(); i++)
        {
          JSONObject artworkInfo = (JSONObject)artworkInfos.get(i);
          if (artworkInfo.has("StorageGroup") && artworkStorageGroup.getName().equals(artworkInfo.getString("StorageGroup")))
          {
            if (artworkInfo.has("URL"))
            {
              String url = artworkInfo.getString("URL");
              // assumes FileName is last parameter
              int amp = url.lastIndexOf("&FileName=");
              if (amp > 0)
              {
                String artPath = url.substring(amp + 10);
                if (artPath.startsWith("/")) // indicates no video storage group
                {
                  for (String artDir : artworkStorageGroup.getDirectories())
                  {
                    if (artPath.startsWith(artDir))
                    {
                      artPath = artPath.substring(artDir.length() + 1);
                      break;
                    }
                  }
                }
                item.setArtwork(artPath);
              }
            }
          }
        }
      }
    }
    
    return item;
  }
  
  private Recording buildRecordingItem(JSONObject rec) throws JSONException, ParseException
  {
    JSONObject channel = rec.getJSONObject("Channel");
    String chanId = channel.getString("ChanId");
    JSONObject recObj = rec.getJSONObject("Recording");
    String startTime = recObj.getString("StartTs").replace('T', ' ');
    String id = chanId + "~" + startTime;
    Recording recording = new Recording(id, rec.getString("Title"));
    recording.setStartTime(parseMythDateTime(startTime));
    if (recObj.has("RecordId"))
      recording.setRecordRuleId(Integer.parseInt(recObj.getString("RecordId")));
    if (recObj.has("RecGroup"))
      recording.setRecordingGroup(recObj.getString("RecGroup"));
    String filename = rec.getString("FileName");
    int lastdot = filename.lastIndexOf('.');
    recording.setFileBase(filename.substring(0, lastdot));
    recording.setFormat(filename.substring(lastdot + 1));
    if (channel.has("CallSign"))
      recording.setCallsign(channel.getString("CallSign"));
    addProgramInfo(recording, rec);
    return recording;
  }
  
  private TvShow buildLiveTvItem(JSONObject chanInfo) throws JSONException, ParseException
  {
    String chanId = chanInfo.getString("ChanId");
    if (!chanInfo.has("Programs"))
      return null;
    JSONArray progs = chanInfo.getJSONArray("Programs");
    if (progs.length() == 0)
      return null;
    JSONObject prog = (JSONObject) progs.get(0);
    String startTime = prog.getString("StartTime").replace('T', ' ');
    String id = chanId + "~" + startTime;
    TvShow tvShow = new TvShow(id, prog.getString("Title"));
    tvShow.setStartTime(parseMythDateTime(startTime));
    tvShow.setProgramStart(startTime);
    if (chanInfo.has("CallSign"))
      tvShow.setCallsign(chanInfo.getString("CallSign"));
    addProgramInfo(tvShow, prog);
    return tvShow;
  }

  private void addProgramInfo(TvShow tvShow, JSONObject jsonObj) throws JSONException, ParseException
  {
    if (jsonObj.has("SubTitle"))
    {
      String subtit = jsonObj.getString("SubTitle");
      if (!subtit.isEmpty())
        tvShow.setSubTitle(subtit);
    }
    if (jsonObj.has("Description"))
    {
      String description = jsonObj.getString("Description");
      if (!description.isEmpty())
        tvShow.setDescription(description);
    }
    if (jsonObj.has("Airdate"))
    {
      String airdate = jsonObj.getString("Airdate");
      if (!airdate.isEmpty())
        tvShow.setOriginallyAired(DateTimeFormats.SERVICE_DATE_FORMAT.parse(airdate));
    }
    if (jsonObj.has("StartTime"))
    {
      String startTime = jsonObj.getString("StartTime");
      if (!startTime.isEmpty())
        tvShow.setProgramStart(startTime.replace('T', ' '));
    }
    if (jsonObj.has("EndTime"))
    {
      String endtime = jsonObj.getString("EndTime");
      if (!endtime.isEmpty())
        tvShow.setEndTime(parseMythDateTime(endtime));
    }
    if (jsonObj.has("Stars"))
    {
      // mythconverg stores program.stars and recorded.stars as a fraction of one
      Float rating = Float.parseFloat(jsonObj.getString("Stars")) * 10;
      rating = (float)Math.round(rating) / 2;
      tvShow.setRating(rating);
    }
    if (tvShow instanceof Recording)
    {
      if (jsonObj.has("Inetref"))
      {
        String inetref = jsonObj.getString("Inetref");
        if (!inetref.isEmpty() && !inetref.equals("00000000"))
          ((Recording)tvShow).setInternetRef(inetref);
      }
      if (jsonObj.has("Season"))
      {
        String season = jsonObj.getString("Season");
        if (!season.isEmpty() && !season.equals("0"))
          ((Recording)tvShow).setSeason(Integer.parseInt(season));
      }
      
    }
    
  }
  
  public List<LiveStreamInfo> parseStreamInfoList()
  {
    List<LiveStreamInfo> streamList = new ArrayList<LiveStreamInfo>();
    
    try
    {
      long startTime = System.currentTimeMillis();

      JSONObject list = new JSONObject(json).getJSONObject("LiveStreamInfoList");
      
      if (list.has("LiveStreamInfos"))
      {
        JSONArray infos = list.getJSONArray("LiveStreamInfos");
        for (int i = 0; i < infos.length(); i++)
        {
          JSONObject info = (JSONObject)infos.get(i);
          streamList.add(buildLiveStream(info));
        }
      }
      if (BuildConfig.DEBUG)
        Log.d(TAG, " -> live stream info parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      if (appSettings.isErrorReportingEnabled())
        new Reporter(ex).send();      
    }
    
    return streamList;
  }

  public LiveStreamInfo parseStreamInfo()
  {
    try
    {
      long startTime = System.currentTimeMillis();
      LiveStreamInfo info = buildLiveStream(new JSONObject(json).getJSONObject("LiveStreamInfo"));
      if (BuildConfig.DEBUG)
        Log.d(TAG, " -> live stream info parse time: " + (System.currentTimeMillis() - startTime) + " ms");
      return info;
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      if (appSettings.isErrorReportingEnabled())
        new Reporter(ex).send();            
      return new LiveStreamInfo();
    }
  }
  
  private LiveStreamInfo buildLiveStream(JSONObject obj) throws JSONException
  {
    LiveStreamInfo streamInfo = new LiveStreamInfo();
    
    if (obj.has("Id"))
      streamInfo.setId(obj.getLong("Id"));
    if (obj.has("StatusInt"))
      streamInfo.setStatusCode(obj.getInt("StatusInt"));
    if (obj.has("StatusStr"))
      streamInfo.setStatus(obj.getString("StatusStr"));
    if (obj.has("StatusMessage"))
      streamInfo.setMessage(obj.getString("StatusMessage"));
    if (obj.has("PercentComplete"))
      streamInfo.setPercentComplete(obj.getInt("PercentComplete"));
    if (obj.has("RelativeURL"))
      streamInfo.setRelativeUrl(obj.getString("RelativeURL").replaceAll(" ", "%20"));
    if (obj.has("SourceFile"))
      streamInfo.setFile(obj.getString("SourceFile"));
    if (obj.has("Width"))
      streamInfo.setWidth(obj.getInt("Width"));
    if (obj.has("Height"))
      streamInfo.setHeight(obj.getInt("Height"));
    if (obj.has("Bitrate"))
      streamInfo.setVideoBitrate(obj.getInt("Bitrate"));
    if (obj.has("AudioBitrate"))
      streamInfo.setAudioBitrate(obj.getInt("AudioBitrate"));
    
    return streamInfo;
  }
  
  public Map<String,StorageGroup> parseStorageGroups() throws JSONException
  {
    Map<String,StorageGroup> storageGroups = new HashMap<String,StorageGroup>();
    
    JSONObject dirList = new JSONObject(json).getJSONObject("StorageGroupDirList");
    if (dirList.has("StorageGroupDirs"))
    {
      JSONArray dirs = dirList.getJSONArray("StorageGroupDirs");
      for (int i = 0; i < dirs.length(); i++)
      {
        JSONObject dir = (JSONObject) dirs.get(i);
        String name = dir.getString("GroupName");
        StorageGroup storageGroup = storageGroups.get(name);
        if (storageGroup == null)
        {
          storageGroup = new StorageGroup(name);
          storageGroups.put(name, storageGroup);
        }

        String dirPath = dir.getString("DirName");
        if (dirPath.endsWith("/"))
          dirPath = dirPath.substring(0, dirPath.length() - 1);
        storageGroup.addDirectory(dirPath);
        if (dir.has("HostName"))
          storageGroup.setHost(dir.getString("HostName"));
      }
    }
    
    return storageGroups;
  }
  
  public String parseMythTvSetting(String key) throws JSONException
  {
    JSONObject settingsList = new JSONObject(json).getJSONObject("SettingList");
    JSONObject settings = settingsList.getJSONObject("Settings");
    if (settings.has(key))
      return settings.getString(key);
    else
      return null;
  }
 
  public Date parseMythDateTime(String dt) throws ParseException
  {
    String str = dt.replace('T',  ' ');
    if (str.endsWith("Z"))
      str = str.substring(0, str.length() - 1);
    return DateTimeFormats.SERVICE_DATE_TIME_RAW_FORMAT.parse(str + " UTC");    
  }
  
  public String parseFrontendStatus() throws JSONException
  {
    JSONObject frontendStatus = new JSONObject(json).getJSONObject("FrontendStatus");
    JSONObject stateObj = frontendStatus.getJSONObject("State");
    return stateObj.getString("state");
  }
  
  public String parseString() throws JSONException
  {
    return new JSONObject(json).getString("String");
  }

  public int parseInt() throws JSONException
  {
    return Integer.parseInt(new JSONObject(json).getString("int"));
  }

  public int parseUint() throws JSONException
  {
    return Integer.parseInt(new JSONObject(json).getString("uint"));
  }
  
  public boolean parseBool() throws JSONException
  {
    return Boolean.parseBoolean(new JSONObject(json).getString("bool"));
  }  
  
}
