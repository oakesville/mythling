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
package com.oakesville.mythling.media;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchResults
{
  private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
  
  private Date retrieveDate;
  public Date getRetrieveDate() { return retrieveDate; }
  public void setRetrieveDate(Date d) { this.retrieveDate = d; }
  public void setRetrieveDate(String d) throws ParseException
  {
    retrieveDate = dateFormat.parse(d);    
  }
  
  private String charSet;
  public String getCharSet() { return charSet; }
  public void setCharSet(String charSet) { this.charSet = charSet; }
  
  private String query;
  public String getQuery() { return query; }
  public void setQuery(String q) { this.query = q; }
  
  private String videoBase;
  public String getVideoBase() { return videoBase; }
  public void setVideoBase(String vb) { this.videoBase = vb; }
  
  private String musicBase;
  public String getMusicBase() { return musicBase; }
  public void setMusicBase(String mb) { this.musicBase = mb; }
  
  private String recordingsBase;
  public String getRecordingsBase() { return recordingsBase; }
  public void setRecordingsBase(String rb) { this.recordingsBase = rb; }
  
  private String moviesBase;
  public String getMoviesBase() { return moviesBase; }
  public void setMoviesBase(String mb) { this.moviesBase = mb; }
  
  private List<Item> videos = new ArrayList<Item>();
  public List<Item> getVideos() { return videos; }
  public void setVideos(List<Item> videos) { this.videos = videos; }
  
  private List<Item> recordings = new ArrayList<Item>();
  public List<Item> getRecordings() { return recordings; }
  public void setRecordings(List<Item> recordings) { this.recordings = recordings; }
  
  private List<Item> liveTvItems = new ArrayList<Item>();
  public List<Item> getLiveTvItems() { return liveTvItems; }
  public void setLiveTvItems(List<Item> liveTvItems) { this.liveTvItems = liveTvItems; }
  
  private List<Item> movies = new ArrayList<Item>();
  public List<Item> getMovies() { return movies; }
  public void setMovies(List<Item> movies) { this.movies = movies; }
  
  private List<Item> tvSeriesItems = new ArrayList<Item>();
  public List<Item> getTvSeriesItems() { return tvSeriesItems; }
  public void setTvSeriesItems(List<Item> tvSeriesItems) { this.tvSeriesItems = tvSeriesItems; }

  private List<Item> songs = new ArrayList<Item>();
  public List<Item> getSongs() { return songs; }
  public void setSongs(List<Item> songs) { this.songs = songs; }
  

  public void addVideo(Item video)
  {
    videos.add(video);
  }
  
  public void addRecording(Item recording)
  {
    recordings.add(recording);
  }
  
  public void addLiveTvItem(Item liveTvItem)
  {
    liveTvItems.add(liveTvItem);
  }

  public void addMovie(Item movie)
  {
    movies.add(movie);
  }
  
  public void addTvSeriesItem(Item tvSeriesItem)
  {
    tvSeriesItems.add(tvSeriesItem);
  }
  
  public void addSong(Item song)
  {
    songs.add(song);
  }
  

  public List<Item> getAll()
  {
    List<Item> all = new ArrayList<Item>();
    all.addAll(videos);
    all.addAll(recordings);
    all.addAll(liveTvItems);
    all.addAll(movies);
    all.addAll(tvSeriesItems);
    all.addAll(songs);
    return all;
  }
  

}
