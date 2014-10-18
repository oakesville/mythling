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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
  
  public String getEncodedTitle()
  {
    try
    {
      return URLEncoder.encode(title, "UTF-8");
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
  
  /**
   * Just the base part of the filename.
   */
  private String fileBase;
  public String getFileBase() { return fileBase; }
  public void setFileBase(String file) { this.fileBase = file; }
  
  private String format;
  public String getFormat() { return format; }
  public void setFormat(String format) { this.format = format; }
  
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
    if (fileBase != null)
    {
      return fileBase + "." + format;
    }
    else
    {
      return title + "." + format;
    }
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
    if (getSubTitle() != null)
      buf.append(" - \"").append(getSubTitle()).append("\"");
    return buf.toString();
  }

  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getSearchPath() != null && getSearchPath().length() > 0)
      buf.append(getSearchPath()).append("/");
    buf.append(getTitle());
    if (getSubTitle() != null)
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
      label += "\n\"" + subTitle + "\"";
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
    if (isLiveTv())
      return getChannelNumberComparator();
    else if (sort == SortType.byRating)
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
        String t1 = stripLeadingArticle(item1.getTitle());
        String t2 = stripLeadingArticle(item2.getTitle());
        return t1.compareToIgnoreCase(t2);
      }
    };
  }
  
  private String stripLeadingArticle(String inStr)
  {
    if (inStr.startsWith("The "))
      return inStr.substring(4);
    if (inStr.startsWith("A "))
      return inStr.substring(2);
    if (inStr.startsWith("An "))
      return inStr.substring(3);
    return inStr;
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

  protected Comparator<Item> getChannelNumberComparator()
  {
    return getTitleComparator(); // supported only for specific types
  }
  
  public ArtworkDescriptor getArtworkDescriptor(String storageGroup)
  {
    return null;
  }  
}
