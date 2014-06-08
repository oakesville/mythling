package com.oakesville.mythling.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.Movie;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.media.Song;
import com.oakesville.mythling.media.TvEpisode;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.media.Video;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public class MythlingParser implements MediaListParser
{
  private static final String TAG = MythlingParser.class.getSimpleName();
  
  private String json;
  
  public MythlingParser(String json)
  {
    this.json = json;
  }

  public MediaList parseMediaList(MediaType mediaType) throws JSONException, ParseException
  {
    MediaList mediaList = new MediaList();
    mediaList.setMediaType(mediaType);

    long startTime = System.currentTimeMillis();
    JSONObject list = new JSONObject(json);
    JSONObject summary = list.getJSONObject("summary");
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
        mediaList.addItem(buildItem(mediaList.getMediaType(), item));
      }
    }
    if (list.has("categories"))
    {
      JSONArray cats = list.getJSONArray("categories");
      for (int i = 0; i < cats.length(); i++)
      {
        JSONObject cat = (JSONObject) cats.get(i);
        mediaList.addCategory(buildCategory(mediaList.getMediaType(), cat, null));
      }
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, " -> media list parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    return mediaList;    
  }
  
  private Category buildCategory(MediaType type, JSONObject cat, Category parent) throws JSONException, ParseException
  {
    String name = cat.getString("name");
    Category category = parent == null ? new Category(name, type) : new Category(name, parent);
    if (cat.has("categories"))
    {
      JSONArray childs = cat.getJSONArray("categories");
      for (int i = 0; i < childs.length(); i++)
      {
        JSONObject childCat = (JSONObject) childs.get(i);
        category.addChild(buildCategory(type, childCat, category));
      }
    }
    if (cat.has("items"))
    {
      JSONArray items = cat.getJSONArray("items");
      for (int i = 0; i < items.length(); i++)
      {
        JSONObject item = (JSONObject) items.get(i);
        category.addItem(buildItem(type, item));
      }
    }
    return category;
  }
  
  public SearchResults parseSearchResults()
  {
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
        searchResults.addVideo(buildItem(MediaType.videos, vid));
      }
      
      JSONArray recordings = list.getJSONArray("recordings");
      for (int i = 0; i < recordings.length(); i++)
      {
        JSONObject recording = (JSONObject) recordings.get(i);
        recording.put("path", "");
        searchResults.addRecording(buildItem(MediaType.recordings, recording));
      }

      JSONArray tvShows = list.getJSONArray("liveTv");
      for (int i = 0; i < tvShows.length(); i++)
      {
        JSONObject tvShow = (JSONObject) tvShows.get(i);
        tvShow.put("path", "");
        searchResults.addLiveTvItem(buildItem(MediaType.liveTv, tvShow));
      }

      JSONArray movies = list.getJSONArray("movies");
      for (int i = 0; i < movies.length(); i++)
      {
        JSONObject movie = (JSONObject) movies.get(i);
        searchResults.addMovie(buildItem(MediaType.movies, movie));
      }

      JSONArray tvSeries = list.getJSONArray("tvSeries");
      for (int i = 0; i < tvSeries.length(); i++)
      {
        JSONObject tvSeriesItem = (JSONObject) tvSeries.get(i);
        searchResults.addTvSeriesItem(buildItem(MediaType.tvSeries, tvSeriesItem));
      }

      JSONArray songs = list.getJSONArray("songs");
      for (int i = 0; i < songs.length(); i++)
      {
        JSONObject song = (JSONObject) songs.get(i);
        searchResults.addSong(buildItem(MediaType.music, song));
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

  private Item buildItem(MediaType type, JSONObject jsonObj) throws JSONException, ParseException
  {
    Item item;
    if (type == MediaType.movies)
    {
      item = new Movie(jsonObj.getString("id"), jsonObj.getString("title"));
      addVideoInfo((Video)item, jsonObj);
    }
    else if (type == MediaType.tvSeries)
    {
      item = new TvEpisode(jsonObj.getString("id"), jsonObj.getString("title"));
      addVideoInfo((Video)item, jsonObj);
      if (jsonObj.has("season"))
        ((TvEpisode)item).setSeason(Integer.parseInt(jsonObj.getString("season")));
      if (jsonObj.has("episode"))
        ((TvEpisode)item).setEpisode(Integer.parseInt(jsonObj.getString("episode")));
    }
    else if (type == MediaType.videos)
    {
      item = new Video(jsonObj.getString("id"), jsonObj.getString("title"));
      addVideoInfo((Video)item, jsonObj);
    }
    else if (type == MediaType.liveTv)
    {
      item = new TvShow(jsonObj.getString("id"), jsonObj.getString("title"));
      addProgramInfo((TvShow)item, jsonObj);
    }
    else if (type == MediaType.recordings)
    {
      item = new Recording(jsonObj.getString("id"), jsonObj.getString("title"));
      addProgramInfo((TvShow)item, jsonObj);
      if (jsonObj.has("recordid"))
        ((Recording)item).setRecordingRuleId(jsonObj.getInt("recordid"));
    }
    else if (type == MediaType.music)
    {
      item = new Song(jsonObj.getString("id"), jsonObj.getString("title"));
      if (jsonObj.has("albumArtId"))
        ((Song)item).setAlbumArtId(jsonObj.getInt("albumArtId"));
    }
    else
    {
      throw new IllegalArgumentException("Unsupported media type: " + type);
    }
    

    if (jsonObj.has("format"))
      item.setFormat(jsonObj.getString("format"));
    if (jsonObj.has("path"))
      item.setSearchPath(jsonObj.getString("path"));
    if (jsonObj.has("file"))
      item.setFile(jsonObj.getString("file"));
    if (jsonObj.has("artist"))
      item.setArtist(jsonObj.getString("artist"));
    if (jsonObj.has("extra"))
      item.setExtra(jsonObj.getString("extra"));
    if (jsonObj.has("subtitle"))
      item.setSubTitle(jsonObj.getString("subtitle"));
    return item;
  }
  
  private void addProgramInfo(TvShow item, JSONObject jsonObj) throws JSONException, ParseException
  {
    item.setStartTime(DateTimeFormats.SERVICE_DATE_TIME_RAW_FORMAT.parse(item.getId().substring(item.getId().indexOf('~') + 1)));
    if (jsonObj.has("callsign"))
      item.setCallsign(jsonObj.getString("callsign"));
    if (jsonObj.has("description"))
      item.setDescription(jsonObj.getString("description"));
    if (jsonObj.has("airdate"))
      item.setOriginallyAired(DateTimeFormats.SERVICE_DATE_FORMAT.parse(jsonObj.getString("airdate")));
    if (jsonObj.has("endtime"))
      item.setEndTime(DateTimeFormats.SERVICE_DATE_TIME_RAW_FORMAT.parse(jsonObj.getString("endtime")));
    if (jsonObj.has("programStart"))
      item.setProgramStart(jsonObj.getString("programStart"));
    if (jsonObj.has("rating"))
    {
      // seems that latest mythconverg stores program.stars and recorded.stars as a fraction of one, but try to be compatible
      Float rating = Float.parseFloat(jsonObj.getString("rating"));
      if (rating <= 1)
        rating = rating * 5;
      item.setRating(rating);
    }
  }
  
  private void addVideoInfo(Video item, JSONObject jsonObj) throws JSONException, ParseException
  {
    if (jsonObj.has("year"))
      item.setYear(Integer.parseInt(jsonObj.getString("year")));
    if (jsonObj.has("rating"))
      item.setRating(Float.parseFloat(jsonObj.getString("rating"))/2);
    if (jsonObj.has("director"))
      item.setDirector(jsonObj.getString("director"));
    if (jsonObj.has("actors"))
      item.setActors(jsonObj.getString("actors"));
    if (jsonObj.has("summary"))
      item.setSummary(jsonObj.getString("summary"));
    if (jsonObj.has("artwork"))
      item.setArtwork(jsonObj.getString("artwork"));
    if (jsonObj.has("internetRef"))
      item.setInternetRef(jsonObj.getString("internetRef"));
    if (jsonObj.has("pageUrl"))
      item.setPageUrl(jsonObj.getString("pageUrl"));
  }
  
  public List<Item> parseQueue(MediaType type) throws JSONException, ParseException
  {
    List<Item> queue = new ArrayList<Item>();
    long startTime = System.currentTimeMillis();
    JSONObject list = new JSONObject(json);      
    JSONArray vids = list.getJSONArray(type.toString());
    for (int i = 0; i < vids.length(); i++)
    {
      JSONObject vid = (JSONObject) vids.get(i);
      queue.add(buildItem(type, vid));
    }
    if (BuildConfig.DEBUG)
      Log.d(TAG, " -> (" + type + ") queue parse time: " + (System.currentTimeMillis() - startTime) + " ms");
    return queue; 
  }  
}
