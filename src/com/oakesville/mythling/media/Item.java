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

import java.util.Comparator;

import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;

public abstract class Item implements Listable
{
  public static final char ARROW = 0x25BA;
  public static final char STAR = 0x2605;
  public static final char HALF_STAR = 0x00BD;
  public static final String PREFIX = String.valueOf(ARROW) + " ";
  
  public abstract MediaType getType();
  public abstract String getTypeTitle();

  private String id;
  public String getId() { return id; }

  // searchPath is populated for search results
  private String searchPath;
  public String getSearchPath() { return searchPath; }
  public void setSearchPath(String searchPath) { this.searchPath = searchPath; }

  /**
   * Path for item playback (set when item is played)
   */
  private String path;
  public String getPath() { return path; }
  public void setPath(String path) { this.path = path; }
  
  private String title;
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  
  /**
   * Just the base part of the filename.
   */
  private String file;
  public String getFile() { return file; }
  public void setFile(String file) { this.file = file; }
  
  private String artist;
  public String getArtist() { return artist; }
  public void setArtist(String artist) { this.artist = artist; }
  
  private String format;
  public String getFormat() { return format; }
  public void setFormat(String format) { this.format = format; }
  
  private String extra;
  public String getExtra() { return extra; }
  public void setExtra(String extra) { this.extra = extra; }
  
  private String subTitle;
  public String getSubTitle() { return subTitle; }
  public void setSubTitle(String subTitle) { this.subTitle = subTitle; }
  
  private float rating;
  public float getRating() { return rating; }
  public void setRating(float rating) { this.rating = rating; }
  
  public Item(String id, String title)
  {
    this.id = id;
    this.title = title;
  }

  public String getFileName()
  {
    if (file != null)
    {
      return file + "." + format;
    }
    else
    {
      // reconstruct
      String str = title;
      if (extra != null)
        str += " (" + extra + ")";
      if (artist != null)
        str += " - " + artist;
      return str + "." + format;
    }
  }
  
  /**
   * full file path for item 
   */
  public String getFilePath()
  {
    return getPath() + "/" + getFileName();
  }
  
  public boolean isMusic()
  {
    return getType() == MediaType.music;
  }
  
  public boolean isRecording()
  {
    return getType() == MediaType.recordings;
  }
  
  public boolean isLiveTv()
  {
    return getType() == MediaType.liveTv;
  }
  
  public boolean isMovie()
  {
    return getType() == MediaType.movies;
  }
  
  public boolean isTvSeries()
  {
    return getType() == MediaType.tvSeries;
  }
  
  public String toString()
  {
    if (isSearchResult()) 
      return getSearchResultText();
    else
      return getText();
  }
  
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX + getTitle());
    if (getExtra() != null)
      buf.append(" (").append(getExtra()).append(")");
    if (getArtist() != null)
      buf.append(" - ").append(getArtist());
    else if (getSubTitle() != null)
      buf.append(" - \"").append(getSubTitle()).append("\"");
    return buf.toString();
  }

  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getPath() != null && getPath().length() > 0)
      buf.append(getPath()).append("/");
    buf.append(getTitle());
    if (getExtra() != null)
      buf.append(" (").append(getExtra()).append(")");
    if (getArtist() != null)
      buf.append(" - ").append(getArtist());
    else if (getSubTitle() != null)
      buf.append(" - \"").append(getSubTitle()).append("\"");
    return buf.toString();
  }
  
  public boolean isSearchResult()
  {
    return getSearchPath() != null;
  }

  public String getLabel()
  {
    String label = title;
    if (subTitle != null)
      label += " - \"" + subTitle + "\"";
    return label;
  }
  
  public String getRatingString(float stars)
  {
    String str = "";
    for (int i = 0; i < stars; i++)
    {
      if (i <= stars - 1)
        str += String.valueOf(STAR);
      else
        str += String.valueOf(HALF_STAR);
    }
    return str;
  }  
  
  /**
   * Default supports by title or rating.
   */
  public Comparator<Item> getComparator(SortType sort)
  {
    if (sort == SortType.byRating)
      return getRatingComparator();
    else if (sort == SortType.byDate)
      return getDateComparator();
    else
      return getTitleComparator();
  }
  
  protected Comparator<Item> getTitleComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        return item1.getTitle().compareTo(item2.getTitle());
      }
    };
  }
  
  protected Comparator<Item> getRatingComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        float f = item2.getRating() - item1.getRating();
        if (f > 0)
          return 1;
        else if (f < 0)
          return -1;
        else
          return 0;
      }
    };
  }
  
  protected Comparator<Item> getDateComparator()
  {
    return getTitleComparator(); // supported only for specific types
  }  
}
