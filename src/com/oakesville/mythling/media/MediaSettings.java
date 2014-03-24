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

import com.oakesville.mythling.R;

public class MediaSettings
{
  public enum MediaType
  {
    videos,
    music,
    recordings,
    liveTv,
    movies,
    tvSeries,
    images  // only used for cover art
  }
  
  public enum MediaTypeDeterminer
  {
    none,
    metadata,
    directories
  }
  
  public enum ViewType
  {
    list,
    pager
  }
  
  public enum SortType
  {
    byTitle,
    byDate,
    byRating
  }
  
  private MediaType type = MediaType.videos;
  public MediaType getType() { return type; }
  
  private MediaTypeDeterminer typeDeterminer = MediaTypeDeterminer.metadata;
  public MediaTypeDeterminer getTypeDeterminer() { return typeDeterminer; }
  public void setTypeDeterminer(MediaTypeDeterminer determiner) { this.typeDeterminer = determiner; }
  public void setTypeDeterminer(String determiner)
  {
    this.typeDeterminer = MediaTypeDeterminer.valueOf(determiner);
  }
  public String getTypeDeterminerLabel()
  {
    if (MediaTypeDeterminer.metadata == typeDeterminer)
      return "MetaData";
    else if (MediaTypeDeterminer.directories == typeDeterminer)
      return "Directories";
    else if (MediaTypeDeterminer.none == typeDeterminer)
      return "None";
    else
      return null;
  }
  
  private ViewType viewType = ViewType.list;
  public ViewType getViewType() { return viewType; }
  public void setViewType(ViewType vt) { this.viewType = vt; }
  public void setViewType(String type)
  {
    this.viewType = ViewType.valueOf(type);
  }
  
  private SortType sortType = SortType.byTitle;
  public SortType getSortType() { return sortType; }
  public void setSortType(SortType st) { this.sortType = st; }
  public void setSortType(String type)
  {
    this.sortType = SortType.valueOf(type);
  }
  
  public MediaSettings(MediaType type)
  {
    this.type = type;
  }
  
  public MediaSettings(String type)
  {
    this.type = MediaType.valueOf(type); 
  }
  
  public static String getMediaTitle(MediaType type)
  {
    if (type == MediaType.music)
      return "Music";
    else if (type == MediaType.videos)
      return "Videos";
    else if (type == MediaType.recordings)
      return "Recordings";
    else if (type == MediaType.liveTv)
      return "Live TV";
    else if (type == MediaType.movies)
      return "Movies";
    else if (type == MediaType.tvSeries)
      return "TV Series";
    else
      return "";
  }
  
  public String getTitle()
  {
    return getMediaTitle(type);
  }
  
  public String getViewTypeTitle()
  {
    if (viewType == ViewType.pager)
      return "Pager";
    else
      return "List";
  }
  
  public String getSortTypeTitle()
  {
    if (sortType == SortType.byDate)
      return "By Date";
    else if (sortType == SortType.byRating)
      return "By Rating";
    else
      return "By Title";
  }
  
  public String getLabel()
  {
    return getMediaLabel(type);
  }
  
  public static String getMediaLabel(MediaType type)
  {
    if (type == MediaType.music)
      return "Song";
    else if (type == MediaType.videos)
      return "Video";
    else if (type == MediaType.recordings)
      return "Recording";
    else if (type == MediaType.liveTv)
      return "Live TV";
    else if (type == MediaType.movies)
      return "Movie";
    else if (type == MediaType.tvSeries)
      return "TV Show";
    else
      return "";
  }
  
  public int getViewIcon()
  {
    if (getViewType() == ViewType.pager)
      return R.drawable.ic_menu_detail;
    else
      return R.drawable.ic_menu_list;
  }

  public boolean isMusic()
  {
    return type == MediaType.music;
  }
  
  public boolean isVideos()
  {
    return type == MediaType.videos;
  }
  
  public boolean isRecordings()
  {
    return type == MediaType.recordings;
  }
  
  public boolean isLiveTv()
  {
    return type == MediaType.liveTv;
  }

  public boolean isMovies()
  {
    return type == MediaType.movies;
  }
  
  public boolean isTvSeries()
  {
    return type == MediaType.tvSeries;
  }
  
  public String toString()
  {
    return type.toString();
  }
  
  public String getStorageGroup()
  {
    return getStorageGroup(type);
  }
  
  /**
   * TODO: prefs
   */
  public static String getStorageGroup(MediaType mediaType)
  {
    if (mediaType == MediaType.movies || mediaType == MediaType.videos || mediaType == MediaType.tvSeries)
      return "Videos";
    else if (mediaType == MediaType.images)
      return "Coverart";
    else
      return "Default";
  }
}
