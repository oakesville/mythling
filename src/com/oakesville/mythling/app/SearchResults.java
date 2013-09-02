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
package com.oakesville.mythling.app;

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
  
  private List<Work> videos = new ArrayList<Work>();
  public List<Work> getVideos() { return videos; }
  public void setVideos(List<Work> videos) { this.videos = videos; }
  
  private List<Work> recordings = new ArrayList<Work>();
  public List<Work> getRecordings() { return recordings; }
  public void setRecordings(List<Work> recordings) { this.recordings = recordings; }
  
  private List<Work> tvShows = new ArrayList<Work>();
  public List<Work> getTvShows() { return tvShows; }
  public void setTvShows(List<Work> tvShows) { this.tvShows = tvShows; }
  
  private List<Work> movies = new ArrayList<Work>();
  public List<Work> getMovies() { return movies; }
  public void setMovies(List<Work> movies) { this.movies = movies; }

  private List<Work> songs = new ArrayList<Work>();
  public List<Work> getSongs() { return songs; }
  public void setSongs(List<Work> songs) { this.songs = songs; }
  

  public void addVideo(Work video)
  {
    videos.add(video);
  }
  
  public void addRecording(Work recording)
  {
    recordings.add(recording);
  }
  
  public void addTvShow(Work tvShow)
  {
    tvShows.add(tvShow);
  }

  public void addMovie(Work movie)
  {
    movies.add(movie);
  }
  
  public void addSong(Work song)
  {
    songs.add(song);
  }
  

  public List<Work> getAll()
  {
    List<Work> all = new ArrayList<Work>();
    all.addAll(videos);
    all.addAll(recordings);
    all.addAll(tvShows);
    all.addAll(movies);
    all.addAll(songs);
    return all;
  }
  

}
