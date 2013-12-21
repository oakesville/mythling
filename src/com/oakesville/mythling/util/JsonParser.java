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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.Category;
import com.oakesville.mythling.app.Item;
import com.oakesville.mythling.app.LiveStreamInfo;
import com.oakesville.mythling.app.MediaList;
import com.oakesville.mythling.app.MediaSettings;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.SearchResults;

public class JsonParser
{
  private static final String TAG = JsonParser.class.getSimpleName();

  private String json;
  public JsonParser(String json)
  {
    this.json = json;
  }
  
  public MediaList parseMediaList(boolean mythlingFormat, MediaType mediaType) throws JSONException, ParseException
  {
    dateTimeRawFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    MediaList mediaList = new MediaList();
    long startTime = System.currentTimeMillis();
    JSONObject list = new JSONObject(json);
    if (mythlingFormat)
    {
      JSONObject summary = list.getJSONObject("summary");
      mediaList.setMediaType(MediaType.valueOf(summary.getString("type")));
      mediaList.setRetrieveDate(summary.getString("date"));
      mediaList.setCount(summary.getString("count"));
      mediaList.setBasePath(summary.getString("base"));
      mediaList.setArtworkStorageGroup(summary.getString("artworkStorageGroup"));
      if (list.has("items"))
      {
        JSONArray items = list.getJSONArray("items");
        for (int i = 0; i < items.length(); i++)
        {
          JSONObject item = (JSONObject) items.get(i);
          mediaList.addItem(buildItem(item, mediaList.getMediaType()));
        }
      }
      if (list.has("categories"))
      {
        JSONArray cats = list.getJSONArray("categories");
        for (int i = 0; i < cats.length(); i++)
        {
          JSONObject cat = (JSONObject) cats.get(i);
          mediaList.addCategory(buildCategory(cat, null, mediaList.getMediaType()));
        }
      }
    }
    else
    {
      if (list.has("VideoMetadataInfoList"))
      {
        // videos, movies, or tvSeries
        mediaList.setMediaType(mediaType);
        JSONObject infoList = list.getJSONObject("VideoMetadataInfoList");
        mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
        mediaList.setCount(infoList.getString("Count"));
        Category vidCat = new Category("Videos", MediaType.videos);
        mediaList.addCategory(vidCat);
        JSONArray vids = infoList.getJSONArray("VideoMetadataInfos");
        for (int i = 0; i < vids.length(); i++)
        {
          JSONObject vid = (JSONObject) vids.get(i);
          // determine type
          MediaType type = MediaType.videos;
          if (vid.has("Inetref"))
          {
            String inetref = vid.getString("Inetref");
            if (!inetref.isEmpty() && !inetref.equals("00000000"))
            {
              type = MediaType.movies;
              if (vid.has("Season"))
              {
                String season = vid.getString("Season");
                if (!season.isEmpty() && !season.equals("0"))
                  type = MediaType.tvSeries;
              }
            }
          }
          
          if (type == mediaType)
            vidCat.addItem(buildMythVideoItem(vid, type));
        }
      }
      else if (list.has("ProgramList"))
      {
        // recordings
        mediaList.setMediaType(MediaType.recordings);
        JSONObject infoList = list.getJSONObject("ProgramList");
        mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
        mediaList.setCount(infoList.getString("Count"));
        JSONArray recs = infoList.getJSONArray("Programs");
        for (int i = 0; i < recs.length(); i++)
        {
          JSONObject rec = (JSONObject) recs.get(i);
          Item recItem = buildMythRecordingItem(rec);
          Category cat = mediaList.getCategory(recItem.getTitle());
          if (cat == null)
          {
            cat = new Category(recItem.getTitle(), MediaType.recordings);
            mediaList.addCategory(cat);
          }
          cat.addItem(recItem);
          Collections.sort(mediaList.getCategories());
        }
      }
      else if (list.has("ProgramGuide"))
      {
        // live tv
        mediaList.setMediaType(MediaType.liveTv);
        JSONObject infoList = list.getJSONObject("ProgramGuide");
        mediaList.setRetrieveDate(parseMythDateTime(infoList.getString("AsOf")));
        mediaList.setCount(infoList.getString("Count"));
        Category tvCat = new Category(MediaSettings.getMediaTitle(MediaType.liveTv), MediaType.liveTv);
        mediaList.addCategory(tvCat);
        JSONArray chans = infoList.getJSONArray("Channels");
        for (int i = 0; i < chans.length(); i++)
        {
          JSONObject chanInfo = (JSONObject) chans.get(i);
          Item show = buildMythLiveTvItem(chanInfo);
          if (show != null)
            tvCat.addItem(show);
        }
      }
      else
      {
        throw new JSONException("Unsupported MediaList Content: " + list.keys().next());
      }
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, " -> media list parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    return mediaList;    
  }
  
  private Category buildCategory(JSONObject cat, Category parent, MediaType type) throws JSONException, ParseException
  {
    String name = cat.getString("name");
    Category category = parent == null ? new Category(name, type) : new Category(name, parent);
    if (cat.has("categories"))
    {
      JSONArray childs = cat.getJSONArray("categories");
      for (int i = 0; i < childs.length(); i++)
      {
        JSONObject childCat = (JSONObject) childs.get(i);
        category.addChild(buildCategory(childCat, category, type));
      }
    }
    if (cat.has("items"))
    {
      JSONArray items = cat.getJSONArray("items");
      for (int i = 0; i < items.length(); i++)
      {
        JSONObject item = (JSONObject) items.get(i);
        category.addItem(buildItem(item, type));
      }
    }
    return category;
  }
  
  public SearchResults parseSearchResults()
  {
    dateTimeRawFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    SearchResults searchResults = new SearchResults();
    try
    {
      long startTime = System.currentTimeMillis();
      JSONObject list = new JSONObject(json);
      JSONObject summary = list.getJSONObject("summary");
      searchResults.setRetrieveDate(summary.getString("date"));
      searchResults.setQuery(summary.getString("query"));
      searchResults.setVideoBase(summary.getString("videoBase"));
      searchResults.setMusicBase(summary.getString("musicBase"));
      searchResults.setRecordingsBase(summary.getString("recordingsBase"));
      searchResults.setMoviesBase(summary.getString("moviesBase"));
      
      JSONArray vids = list.getJSONArray("videos");
      for (int i = 0; i < vids.length(); i++)
      {
        JSONObject vid = (JSONObject) vids.get(i);
        searchResults.addVideo(buildItem(vid, MediaType.videos));
      }
      
      JSONArray recordings = list.getJSONArray("recordings");
      for (int i = 0; i < recordings.length(); i++)
      {
        JSONObject recording = (JSONObject) recordings.get(i);
        recording.put("path", "");
        searchResults.addRecording(buildItem(recording, MediaType.recordings));
      }

      JSONArray tvShows = list.getJSONArray("liveTv");
      for (int i = 0; i < tvShows.length(); i++)
      {
        JSONObject tvShow = (JSONObject) tvShows.get(i);
        tvShow.put("path", "");
        searchResults.addLiveTvItem(buildItem(tvShow, MediaType.liveTv));
      }

      JSONArray movies = list.getJSONArray("movies");
      for (int i = 0; i < movies.length(); i++)
      {
        JSONObject movie = (JSONObject) movies.get(i);
        searchResults.addMovie(buildItem(movie, MediaType.movies));
      }

      JSONArray tvSeries = list.getJSONArray("tvSeries");
      for (int i = 0; i < tvSeries.length(); i++)
      {
        JSONObject tvSeriesItem = (JSONObject) tvSeries.get(i);
        searchResults.addTvSeriesItem(buildItem(tvSeriesItem, MediaType.tvSeries));
      }

      JSONArray songs = list.getJSONArray("songs");
      for (int i = 0; i < songs.length(); i++)
      {
        JSONObject song = (JSONObject) songs.get(i);
        searchResults.addSong(buildItem(song, MediaType.music));
      }
      
      if (BuildConfig.DEBUG)
        Log.d(TAG, " -> search results parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    return searchResults; 
  }

  public List<Item> parseQueue(MediaType type)
  {
    List<Item> queue = new ArrayList<Item>();
    try
    {
      long startTime = System.currentTimeMillis();
      JSONObject list = new JSONObject(json);      
      JSONArray vids = list.getJSONArray(type.toString());
      for (int i = 0; i < vids.length(); i++)
      {
        JSONObject vid = (JSONObject) vids.get(i);
        queue.add(buildItem(vid, type));
      }
      if (BuildConfig.DEBUG)
        Log.d(TAG, " -> (" + type + ") queue parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    return queue; 
  }

  private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static DateFormat dateTimeRawFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  private Item buildMythVideoItem(JSONObject vid, MediaType type) throws JSONException, ParseException
  {
    Item item = new Item(vid.getString("Id"), type, vid.getString("Title"));
    String filename = vid.getString("FileName");
    int lastdot = filename.lastIndexOf('.');
    item.setFile(filename.substring(0, lastdot));
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
    if (type != MediaType.videos)
    {
      // we don't make use of the following data for videos, so don't waste time parsing
      if (vid.has("Inetref"))
      {
        String inetref = vid.getString("Inetref");
        if (!inetref.isEmpty() && !inetref.equals("00000000"))
        {
          item.setInternetRef(inetref);
          if (type == MediaType.tvSeries && vid.has("Season"))
          {
            String season = vid.getString("Season");
            if (!season.isEmpty() && !season.equals("0"))
            {
              item.setSeason(Integer.parseInt(season));
              if (vid.has("Episode"))
              {
                String episode = vid.getString("Episode");
                if (!episode.isEmpty() && !episode.equals("0"))
                  item.setEpisode(Integer.parseInt(episode));
              }
            }
          }
        }
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
          Date date = dateFormat.parse(dateStr + " UTC");
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
      if (vid.has("Coverart"))
      {
        String art = vid.getString("Coverart");
        if (!art.isEmpty())
        {
          item.setArtwork(art);
          item.setArtworkStorageGroup("Coverart");
        }
      }
      else if (vid.has("Fanart"))
      {
        String art = vid.getString("Fanart");
        if (!art.isEmpty())
        {
          item.setArtwork(art);
          item.setArtworkStorageGroup("Fanart");
        }
      }
      else if (vid.has("Screenshot"))
      {
        String art = vid.getString("Screenshot");
        if (!art.isEmpty())
        {
          item.setArtwork(art);
          item.setArtworkStorageGroup("Screenshots");
        }
      }
      else if (vid.has("Banner"))
      {
        String art = vid.getString("Banner");
        if (!art.isEmpty())
        {
          item.setArtwork(art);
          item.setArtworkStorageGroup("Banners");
        }
      }
    }
    
    return item;
  }
  
  private Item buildMythRecordingItem(JSONObject rec) throws JSONException, ParseException
  {
    JSONObject channel = rec.getJSONObject("Channel");
    String chanId = channel.getString("ChanId");
    String startTime = rec.getString("StartTime").replace('T', ' ');
    String id = chanId + "~" + startTime;
    Item item = new Item(id, MediaType.recordings, rec.getString("Title"));
    item.setStartTime(parseMythDateTime(startTime));
    String filename = rec.getString("FileName");
    int lastdot = filename.lastIndexOf('.');
    item.setFile(filename.substring(0, lastdot));
    item.setFormat(filename.substring(lastdot + 1));
    item.setProgramStart(startTime);
    if (channel.has("CallSign"))
      item.setCallsign(channel.getString("CallSign"));
    addMythProgramInfo(item, rec);
    return item;
  }
  
  private Item buildMythLiveTvItem(JSONObject chanInfo) throws JSONException, ParseException
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
    Item item = new Item(id, MediaType.liveTv, prog.getString("Title"));
    item.setStartTime(parseMythDateTime(startTime));
    item.setProgramStart(startTime);
    if (chanInfo.has("CallSign"))
      item.setCallsign(chanInfo.getString("CallSign"));
    addMythProgramInfo(item, prog);
    return item;
  }

  private void addMythProgramInfo(Item item, JSONObject show) throws JSONException, ParseException
  {
    if (show.has("SubTitle"))
    {
      String subtit = show.getString("SubTitle");
      if (!subtit.isEmpty())
        item.setSubTitle(subtit);
    }
    if (show.has("Description"))
    {
      String description = show.getString("Description");
      if (!description.isEmpty())
        item.setDescription(description);
    }
    if (show.has("Airdate"))
    {
      String airdate = show.getString("Airdate");
      if (!airdate.isEmpty())
        item.setOriginallyAired(dateFormat.parse(airdate));
    }
    if (show.has("EndTime"))
    {
      String endtime = show.getString("EndTime");
      if (!endtime.isEmpty())
        item.setEndTime(parseMythDateTime(endtime));
    }    
  }

  private Item buildItem(JSONObject w, MediaType t) throws JSONException, ParseException
  {
    Item item = new Item(w.getString("id"), t, w.getString("title"));
    int squigIdx = item.getId().indexOf('~');
    if (squigIdx > 0)
      item.setStartTime(dateTimeRawFormat.parse(item.getId().substring(squigIdx + 1)));

    if (w.has("format"))
      item.setFormat(w.getString("format"));
    if (w.has("path"))
      item.setPath(w.getString("path"));
    if (w.has("file"))
      item.setFile(w.getString("file"));
    if (w.has("artist"))
      item.setArtist(w.getString("artist"));
    if (w.has("extra"))
      item.setExtra(w.getString("extra"));
    if (w.has("callsign"))
      item.setCallsign(w.getString("callsign"));
    if (w.has("subtitle"))
      item.setSubTitle(w.getString("subtitle"));
    if (w.has("description"))
      item.setDescription(w.getString("description"));
    if (w.has("airdate"))
      item.setOriginallyAired(dateFormat.parse(w.getString("airdate")));
    if (w.has("endtime"))
      item.setEndTime(dateTimeRawFormat.parse(w.getString("endtime")));
    if (w.has("recordid"))
      item.setRecordingRuleId(w.getInt("recordid"));
    if (w.has("programStart"))
      item.setProgramStart(w.getString("programStart"));
    if (w.has("season"))
      item.setSeason(Integer.parseInt(w.getString("season")));
    if (w.has("episode"))
      item.setEpisode(Integer.parseInt(w.getString("episode")));
    if (w.has("year"))
      item.setYear(Integer.parseInt(w.getString("year")));
    if (w.has("rating"))
      item.setRating(Float.parseFloat(w.getString("rating"))/2);
    if (w.has("director"))
      item.setDirector(w.getString("director"));
    if (w.has("actors"))
      item.setActors(w.getString("actors"));
    if (w.has("summary"))
      item.setSummary(w.getString("summary"));
    if (w.has("artwork"))
      item.setArtwork(w.getString("artwork"));
    if (w.has("internetRef"))
      item.setInternetRef(w.getString("internetRef"));
    if (w.has("pageUrl"))
      item.setPageUrl(w.getString("pageUrl"));
    return item;
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
    }
    
    return streamList;
  }
  
  public Date parseMythDateTime(String dt) throws ParseException
  {
    String str = dt.replace('T',  ' ');
    if (str.endsWith("Z"))
      str = str.substring(0, str.length() - 1);
    return dateTimeRawFormat.parse(str + " UTC");    
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
  
  public String parseStorageGroupDir(String name) throws JSONException
  {
    JSONObject dirList = new JSONObject(json).getJSONObject("StorageGroupDirList");
    JSONArray dirs = dirList.getJSONArray("StorageGroupDirs");
    for (int i = 0; i < dirs.length(); i++)
    {
      JSONObject dir = (JSONObject) dirs.get(i);
      if (dir.getString("GroupName").equals(name))
      {
        String dirPath = dir.getString("DirName");
        if (dirPath.endsWith("/"))
          dirPath = dirPath.substring(0, dirPath.length() - 1);
        return dirPath;
      }
    }
    return null;
  }
  
  public int parseInt() throws JSONException
  {
    return Integer.parseInt(new JSONObject(json).getString("int"));
  }
  
  public boolean parseBool() throws JSONException
  {
    return Boolean.parseBoolean(new JSONObject(json).getString("bool"));
  }  
}
