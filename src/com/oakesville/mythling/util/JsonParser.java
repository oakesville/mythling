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
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.oakesville.mythling.app.Category;
import com.oakesville.mythling.app.LiveStreamInfo;
import com.oakesville.mythling.app.SearchResults;
import com.oakesville.mythling.app.Work;
import com.oakesville.mythling.app.WorksList;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.BuildConfig;

public class JsonParser
{
  public static final String TAG = JsonParser.class.getSimpleName();

  private String json;
  public JsonParser(String json)
  {
    this.json = json;
  }
  
  public WorksList parseWorksList()
  {
    dateTimeRawFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    WorksList worksList = new WorksList();
    try
    {
      long startTime = System.currentTimeMillis();
      JSONObject list = new JSONObject(json);
      JSONObject summary = list.getJSONObject("summary");
      worksList.setMediaType(MediaType.valueOf(summary.getString("type")));
      worksList.setRetrieveDate(summary.getString("date"));
      worksList.setTimeZone(summary.getString("timeZone"));
      worksList.setCount(summary.getString("count"));
      worksList.setBasePath(summary.getString("base"));
      
      JSONArray cats = list.getJSONArray("categories");
      for (int i = 0; i < cats.length(); i++)
      {
        JSONObject cat = (JSONObject) cats.get(i);
        worksList.addCategory(buildCategory(cat, null, worksList.getMediaType()));
      }
      if (BuildConfig.DEBUG)
        Log.d(TAG, "WorksList parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    return worksList;    
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
    if (cat.has("works"))
    {
      JSONArray works = cat.getJSONArray("works");
      for (int i = 0; i < works.length(); i++)
      {
        JSONObject work = (JSONObject) works.get(i);
        category.addWork(buildWork(work, type));
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
        searchResults.addVideo(buildWork(vid, MediaType.videos));
      }
      
      JSONArray recordings = list.getJSONArray("recordings");
      for (int i = 0; i < recordings.length(); i++)
      {
        JSONObject recording = (JSONObject) recordings.get(i);
        recording.put("path", "");
        searchResults.addRecording(buildWork(recording, MediaType.recordings));
      }

      JSONArray tvShows = list.getJSONArray("tv");
      for (int i = 0; i < tvShows.length(); i++)
      {
        JSONObject tvShow = (JSONObject) tvShows.get(i);
        tvShow.put("path", "");
        searchResults.addTvShow(buildWork(tvShow, MediaType.tv));
      }

      JSONArray movies = list.getJSONArray("movies");
      for (int i = 0; i < movies.length(); i++)
      {
        JSONObject movie = (JSONObject) movies.get(i);
        searchResults.addMovie(buildWork(movie, MediaType.movies));
      }
      
      JSONArray songs = list.getJSONArray("songs");
      for (int i = 0; i < songs.length(); i++)
      {
        JSONObject song = (JSONObject) songs.get(i);
        searchResults.addSong(buildWork(song, MediaType.songs));
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

  public List<Work> parseQueue(MediaType type)
  {
    List<Work> queue = new ArrayList<Work>();
    try
    {
      long startTime = System.currentTimeMillis();
      JSONObject list = new JSONObject(json);      
      JSONArray vids = list.getJSONArray(type.toString());
      for (int i = 0; i < vids.length(); i++)
      {
        JSONObject vid = (JSONObject) vids.get(i);
        queue.add(buildWork(vid, type));
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
  
  private Work buildWork(JSONObject w, MediaType t) throws JSONException, ParseException
  {
    Work work = new Work(w.getString("id"), t, w.getString("title"));
    int squigIdx = work.getId().indexOf('~');
    if (squigIdx > 0)
      work.setStartTime(dateTimeRawFormat.parse(work.getId().substring(squigIdx + 1)));

    if (w.has("format"))
      work.setFormat(w.getString("format"));
    if (w.has("path"))
      work.setPath(w.getString("path"));
    if (w.has("file"))
      work.setFile(w.getString("file"));
    if (w.has("artist"))
      work.setArtist(w.getString("artist"));
    if (w.has("extra"))
      work.setExtra(w.getString("extra"));
    if (w.has("callsign"))
      work.setCallsign(w.getString("callsign"));
    if (w.has("subtitle"))
      work.setSubTitle(w.getString("subtitle"));
    if (w.has("description"))
      work.setDescription(w.getString("description"));
    if (w.has("airdate"))
      work.setOriginallyAired(dateFormat.parse(w.getString("airdate")));
    if (w.has("endtime"))
      work.setEndTime(dateTimeRawFormat.parse(w.getString("endtime")));
    if (w.has("recordid"))
      work.setRecordingRuleId(w.getInt("recordid"));
    if (w.has("programStart"))
      work.setProgramStart(w.getString("programStart"));
    if (w.has("year"))
      work.setYear(Integer.parseInt(w.getString("year")));
    if (w.has("rating"))
      work.setRating((float)Integer.parseInt(w.getString("rating"))/2);
    if (w.has("director"))
      work.setDirector(w.getString("director"));
    if (w.has("actors"))
      work.setActors(w.getString("actors"));
    if (w.has("poster"))
      work.setPoster(w.getString("poster"));
    if (w.has("imdbId"))
      work.setImdbId(w.getString("imdbId"));
    return work;
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
  
  public int parseInt() throws JSONException
  {
    return Integer.parseInt(new JSONObject(json).getString("int"));
  }
  
  public boolean parseBool() throws JSONException
  {
    return Boolean.parseBoolean(new JSONObject(json).getString("bool"));
  }
  
}
