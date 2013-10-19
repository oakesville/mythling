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
  
  public MediaList parseMediaList(boolean mythlingFormat) throws JSONException, ParseException
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
      if (summary.has("pageLinkTitle"))
        mediaList.setPageLinkTitle(summary.getString("pageLinkTitle"));
      
      JSONArray cats = list.getJSONArray("categories");
      for (int i = 0; i < cats.length(); i++)
      {
        JSONObject cat = (JSONObject) cats.get(i);
        mediaList.addCategory(buildCategory(cat, null, mediaList.getMediaType()));
      }
    }
    else
    {
      JSONObject infoList = list.getJSONObject("VideoMetadataInfoList");
      mediaList.setMediaType(MediaType.videos);
      mediaList.setRetrieveDateMyth(infoList.getString("AsOf"));
      mediaList.setCount(infoList.getString("Count"));
      Category vidCat = new Category("Videos", MediaType.videos);
      mediaList.addCategory(vidCat);
      JSONArray vids = infoList.getJSONArray("VideoMetadataInfos");
      for (int i = 0; i < vids.length(); i++)
      {
        JSONObject vid = (JSONObject) vids.get(i);
        vidCat.addItem(buildMythVideoItem(vid));
      }
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, "MediaList parse time: " + (System.currentTimeMillis() - startTime) + " ms");
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

      JSONArray tvShows = list.getJSONArray("tv");
      for (int i = 0; i < tvShows.length(); i++)
      {
        JSONObject tvShow = (JSONObject) tvShows.get(i);
        tvShow.put("path", "");
        searchResults.addTvShow(buildItem(tvShow, MediaType.tv));
      }

      JSONArray movies = list.getJSONArray("movies");
      for (int i = 0; i < movies.length(); i++)
      {
        JSONObject movie = (JSONObject) movies.get(i);
        searchResults.addMovie(buildItem(movie, MediaType.movies));
      }
      
      JSONArray songs = list.getJSONArray("songs");
      for (int i = 0; i < songs.length(); i++)
      {
        JSONObject song = (JSONObject) songs.get(i);
        searchResults.addSong(buildItem(song, MediaType.music));
      }
      
      if (BuildConfig.DEBUG)
        Log.d(TAG, "SearchResults parse time: " + (System.currentTimeMillis() - startTime) + " ms");
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
        Log.d(TAG, type + " queue parse time: " + (System.currentTimeMillis() - startTime) + " ms");
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
  
  Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  private Item buildMythVideoItem(JSONObject vid) throws JSONException, ParseException
  {
    Item item = new Item(vid.getString("Id"), MediaType.videos, vid.getString("Title"));
    item.setFile(vid.getString("FileName"));
    if (vid.has("Director"))
    {
      String director = vid.getString("Director");
      if (!director.equals("Unknown"))
        item.setDirector(director);
    }
    if (vid.has("Description"))
    {
      String description = vid.getString("Description");
      if (!description.equals("None"))
        item.setDescription(description);
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
        cal.setTime(date);
        item.setYear(cal.get(Calendar.YEAR));
      }
    }
    if (vid.has("UserRating"))
    {
      String rating = vid.getString("UserRating");
      if (!rating.equals("0"))
        item.setRating((float)Integer.parseInt(rating)/2);
    }
    if (vid.has("Coverart"))
    {
      String art = vid.getString("Coverart");
      if (!art.isEmpty())
        item.setPoster(art);
    }
    
    return item;
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
    if (w.has("year"))
      item.setYear(Integer.parseInt(w.getString("year")));
    if (w.has("rating"))
      item.setRating((float)Integer.parseInt(w.getString("rating"))/2);
    if (w.has("director"))
      item.setDirector(w.getString("director"));
    if (w.has("actors"))
      item.setActors(w.getString("actors"));
    if (w.has("summary"))
      item.setSummary(w.getString("summary"));
    if (w.has("poster"))
      item.setPoster(w.getString("poster"));
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
        Log.d(TAG, "LiveStreamInfos parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
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
        Log.d(TAG, "LiveStreamInfo parse time: " + (System.currentTimeMillis() - startTime) + " ms");
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
