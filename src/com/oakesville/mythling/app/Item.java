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
import java.util.Date;

import android.util.Log;

import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.BuildConfig;

public class Item implements Listable
{
  private static final String TAG = Item.class.getSimpleName();

  private String id;
  public String getId() { return id; }

  // path is populated for search results
  private String path;
  public String getPath() { return path; }
  public void setPath(String path) { this.path = path; }
  
  private MediaType type;
  public MediaType getType() { return type; }
  
  private String title;
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  
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
  
  private String callsign;
  public String getCallsign() { return callsign; }
  public void setCallsign(String callsign) { this.callsign = callsign; }
  
  private String subTitle;
  public String getSubTitle() { return subTitle; }
  public void setSubTitle(String subTitle) { this.subTitle = subTitle; }
  
  private String description;
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  
  private Date originallyAired;
  public Date getOriginallyAired() { return originallyAired; }
  public void setOriginallyAired(Date aired) { this.originallyAired = aired; }
  
  private int year;
  public int getYear() { return year; }
  public void setYear(int year) { this.year = year; }
  
  private float rating;
  public float getRating() { return rating; }
  public void setRating(float rating) { this.rating = rating; }
  
  private String director;
  public String getDirector() { return director; }
  public void setDirector(String director) { this.director = director; }
  
  private String actors;
  public String getActors() { return actors; }
  public void setActors(String actors) { this.actors = actors; }
  
  private String summary;
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  
  private String poster;
  public String getPoster() { return poster; }
  public void setPoster(String poster) { this.poster = poster; }

  private String pageUrl;
  public String getPageUrl() { return pageUrl; }
  public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
  
  private Date startTime;
  public Date getStartTime() { return startTime; }
  public void setStartTime(Date startTime) { this.startTime = startTime; }

  private Date endTime;
  public Date getEndTime() { return endTime; }
  public void setEndTime(Date endTime) { this.endTime = endTime; }
  
  private int recordingRuleId;
  public int getRecordingRuleId() { return recordingRuleId; }
  public void setRecordingRuleId(int rrid) { this.recordingRuleId = rrid; }
  
  private String programStart;
  public String getProgramStart() { return programStart; }
  public void setProgramStart(String programStart) { this.programStart = programStart; }
  
  public Item(String id, MediaType type, String title)
  {
    this.id = id;
    this.type = type;
    this.title = title;
  }
  
  public Item(Item other)
  {
    this.id = other.id;
    this.type = other.type;
    this.title = other.title;
    this.file = other.file;
    this.format = other.format;
    this.artist = other.artist;
    this.extra = other.extra;
    this.callsign = other.callsign;
    this.startTime = other.startTime;
    this.endTime = other.endTime;
    this.programStart = other.programStart;
    this.subTitle = other.subTitle;
    this.description = other.description;
    this.originallyAired = other.originallyAired;
    this.year = other.year;
    this.rating = other.rating;
    this.director = other.director;
    this.actors = other.actors;
    this.poster = other.poster;
    this.pageUrl = other.pageUrl;
  }
  
  public String getFileName()
  {
    String str = file == null ? title : file;
    if (extra != null)
      str += " (" + extra + ")";
    if (artist != null)
      str += " - " + artist;
    return str + "." + format;
  }
  
  public String getFilePath()
  {
    return getPath() + "/" + getFileName();
  }
  
  public int getChannelId()
  {
    if (!isRecording() && !isTv())
      return -1;
    else
      return Integer.parseInt(getId().substring(0,  getId().indexOf('~')));
  }
  
  public int getChannelNumber()
  {
    if (!isRecording() && !isTv())
      return -1;
    else
      return Integer.parseInt(getId().substring(1,  getId().indexOf('~')));
  }
  
  public String getStartTimeParam()
  {
    if (!isRecording() && !isTv())
      return null;
    else
      return getStartTimeRaw().replace(' ', 'T');
  }
  public String getStartTimeRaw()
  {
    if (!isRecording() && !isTv())
      return null;
    return getId().substring(getId().indexOf('~') + 1);
  }

  private static DateFormat dateFormat = new SimpleDateFormat("MMM d yyyy");
  private static DateFormat timeFormat = new SimpleDateFormat("h:mm a");
  private static DateFormat dateTimeFormat = new SimpleDateFormat("MMM d  h:mm a");
  public String getStartDateTimeFormatted() throws ParseException
  {
    if (!isRecording() && !isTv())
      return null;
    return dateTimeFormat.format(getStartTime());
  }
  public String getStartTimeFormatted() throws ParseException
  {
    if (!isRecording() && !isTv())
      return null;
    return timeFormat.format(getStartTime());
  }
    
  public String getEndDateTimeFormatted() throws ParseException
  {
    if (!isRecording() && !isTv())
      return null;
    return dateTimeFormat.format(getEndTime());    
  }
  public String getEndTimeFormatted() throws ParseException
  {
    if (!isRecording() && !isTv())
      return null;
    return timeFormat.format(getEndTime());
  }
  
  public String getShowInfo()
  {
    if (!isRecording() && !isTv() && !isMovie())
      return null;
    
    String str = "";
    
    if (isMovie())
    {
      str += (getYear() == 0 ? "" : getYear() + "   ") + getRatingString() + "\n";
      str += getDirector() == null ? "" : "Directed By: " + getDirector() + "\n";
      str += getActors() == null ? "" : "Starring: " + getActors() + "\n\n";
      str += getSummary() == null ? "" : getSummary();
    }
    else
    {
      if (isTv())
      {
        str += getChannelNumber() + " (" + getCallsign() + ") ";
        try
        {
          str += getStartTimeFormatted() + " - " + getEndTimeFormatted();
        }
        catch (ParseException ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }
        str += "\n";
      }
      
      if (subTitle != null)
        str += "\"" + subTitle + "\"\n";
      if (originallyAired != null)
        str += "(Originally Aired " + dateFormat.format(originallyAired) + ")\n";
      if (description != null)
        str += description;
    }
    
    return str;
  }
  
  public boolean isMusic()
  {
    return type == MediaType.music;
  }
  
  public boolean isRecording()
  {
    return type == MediaType.recordings;
  }
  
  public boolean isTv()
  {
    return type == MediaType.liveTv;
  }
  
  public boolean isMovie()
  {
    return type == MediaType.movies;
  }
  
  private static final int arrowChar = 0x25BA;
  private static final int starChar = 0x2605;
  private static final int halfChar = 0x00BD;
  
  public String toString()
  {
    String str = String.valueOf((char)arrowChar) + " ";
    
    if (path != null)
    {
      // indicates search result
      if (isMusic())
        str += "(Song)";
      else if (isRecording())
        str += "(Recording)";
      else if (isTv())
        str += "(Live TV)";
      else if (isMovie())
        str += "(Movie)";
      else
        str += "(Video)";
      
      str += " " + (path.length() == 0 ? "" : path + "/");
    }
    
    if (isRecording())
    {
      if (path != null)
        str += title + " - ";
      try
      {
        str += getStartDateTimeFormatted() + " - ";
        str += getChannelNumber() + " (" + getCallsign() + ") ";
      }
      catch (ParseException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
      }
      if (path == null)
      {
        str += "\n" + title;
        str += "\n" + getShowInfo();
      }
    }
    else if (isTv())
    {
      str += getChannelNumber() + " (" + getCallsign() + ") ";
      str += title;
      try
      {
        str += " (" + getStartTimeFormatted() + " - " + getEndTimeFormatted() + ")";
      }
      catch (ParseException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
      }
    }
    else
    {
      str += title;
      if (extra != null)
        str += " (" + extra + ")";
      if (artist != null)
        str += " - " + artist;
      else if (isMovie())
      {
        str += " (" + getYear() + ")  " + getRatingString();
      }
      else if (subTitle != null)
        str += " - \"" + subTitle + "\"";
    }
    return str;
  }
  
  public String getRatingString()
  {
    String str = "";
    for (int i = 0; i < getRating(); i++)
    {
      if (i <= getRating() - 1)
        str += String.valueOf((char)starChar);
      else
        str += String.valueOf((char)halfChar);
    }
    return str;
  }

  public String getLabel()
  {
    String label = title;
    if (isMovie() && year > 0)
      return title + " (" + year + ")";
    
    return label;
  }
}
