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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.DateTimeFormats;

/**
 * A live or recorded TV show.
 */
public class TvShow extends Item
{
  private static final String TAG = TvShow.class.getSimpleName();
  
  private String callsign;
  public String getCallsign() { return callsign; }
  public void setCallsign(String callsign) { this.callsign = callsign; }
 
  private String description;
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
    
  private Date originallyAired;
  public Date getOriginallyAired() { return originallyAired; }
  public void setOriginallyAired(Date aired) { this.originallyAired = aired; }
  
  private Date startTime;
  public Date getStartTime() { return startTime; }
  public void setStartTime(Date startTime) { this.startTime = startTime; }

  private Date endTime;
  public Date getEndTime() { return endTime; }
  public void setEndTime(Date endTime) { this.endTime = endTime; }

  private String programStart;
  public String getProgramStart() { return programStart; }
  public void setProgramStart(String programStart) { this.programStart = programStart; }
  
  public int getChannelId()
  {
    return Integer.parseInt(getId().substring(0,  getId().indexOf('~')));
  }
  
  public int getChannelNumber()
  {
    return Integer.parseInt(getId().substring(1,  getId().indexOf('~')));
  }
  
  public String getStartTimeParam()
  {
    return getStartTimeRaw().replace(' ', 'T');
  }
  public String getStartTimeRaw()
  {
    return getId().substring(getId().indexOf('~') + 1);
  }
  public String getStartDateTimeFormatted() throws ParseException
  {
    String startYear = DateTimeFormats.YEAR_FORMAT.format(getStartTime()); 
    String sdtf = DateTimeFormats.DATE_TIME_FORMAT.format(getStartTime()).replace(" AM", "a").replace(" PM", "p");
    if (!startYear.equals(DateTimeFormats.YEAR_FORMAT.format(new Date())))
      sdtf += ", " + startYear;
    return sdtf;
  }
  public String getStartTimeFormatted() throws ParseException
  {
    return DateTimeFormats.TIME_FORMAT.format(getStartTime()).replace(" AM", "a").replace(" PM", "p");
  }
    
  public String getEndDateTimeFormatted() throws ParseException
  {
    return DateTimeFormats.DATE_TIME_FORMAT.format(getEndTime()).replace(" AM", "a").replace(" PM", "p");    
  }
  public String getEndTimeFormatted() throws ParseException
  {
    return DateTimeFormats.TIME_FORMAT.format(getEndTime()).replace(" AM", "a").replace(" PM", "p");
  }
  public String getEndTimeParam()
  {
    return DateTimeFormats.SERVICE_DATE_TIME_RAW_FORMAT.format(getEndTime()).replace(' ', 'T');
  }
  
  public String getChanIdStartTimeParams()
  {
    return "ChanId=" + getChannelId() + "&StartTime=" + getStartTimeParam();
  }
  
  public TvShow(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.liveTv;
  }
  
  public String getTypeTitle()
  {
    return "Live TV";
  }  

  public String getShowDateTimeInfo()
  {
    StringBuffer buf = new StringBuffer();
    try
    {
      buf.append("\n").append(getStartDateTimeFormatted()).append("-");
      buf.append(getEndTimeFormatted()).append(" ");
    }
    catch (ParseException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    
    return buf.toString();
  }
  
  public String getShowTimeInfo()
  {
    StringBuffer buf = new StringBuffer();
    try
    {
      buf.append(getStartTimeFormatted()).append("-");
      buf.append(getEndTimeFormatted()).append(" ");
    }
    catch (ParseException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    
    return buf.toString();
  }
  
  public String getChannelInfo()
  {
    return new StringBuffer().append(getChannelNumber()).append(" (").append(getCallsign()).append(")").toString();
  }
  
  public String getAirDateInfo()
  {
    StringBuffer buf = new StringBuffer();
    if (isRepeat())
    {
      if (isShowMovie())
        buf.append("(").append(DateTimeFormats.YEAR_FORMAT.format(originallyAired)).append(")");
      else
        buf.append("\n(Originally Aired ").append(DateTimeFormats.DATE_FORMAT.format(originallyAired)).append(")");
    }
    return buf.toString();
  }
  
  public String getFormat()
  {
    if (getType() == MediaType.liveTv)
      return "Live TV"; // for remembering stream option
    else
      return super.getFormat();
  }
  
  public String getLabel()
  {
    String label = getTitle();
    if (isShowMovie() && getYear() > 0)
      label += " (" + getYear() + ")";    
    if (getSubTitle() != null)
      label += "\n\"" + getSubTitle() + "\"";
    return label;
  }  
  
  public boolean isShowMovie()
  {
    return getRating() > 0; // proxy for determining the show is a movie
  }
  
  public int getYear()
  {
    if (originallyAired == null)
      return 0;
    return Integer.parseInt(DateTimeFormats.YEAR_FORMAT.format(originallyAired));
  }
  
  protected boolean isRepeat()
  {
    if (originallyAired == null)
      return false;
    
    Calendar origCal = Calendar.getInstance();
    origCal.setTime(originallyAired);
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(startTime);
    return !(origCal.get(Calendar.YEAR) == startCal.get(Calendar.YEAR)
        && origCal.get(Calendar.MONTH) == startCal.get(Calendar.MONTH)
        && origCal.get(Calendar.DAY_OF_MONTH) == startCal.get(Calendar.DAY_OF_MONTH));
  }

  /**
   * Overridden for Recordings, so this is for LiveTV.
   */
  @Override
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append(getChannelInfo());
    buf.append(" ").append(getShowTimeInfo());
    buf.append("\n").append(getTitle());
    if (isShowMovie() && getYear() > 0)
      buf.append(" (").append(getYear()).append(")");    
    if (getRating() > 0)
      buf.append(" ").append(getRatingString(getRating()));
    if (getSubTitle() != null)
      buf.append("\n\"").append(getSubTitle()).append("\"");
    if (!isShowMovie() && isRepeat())
      buf.append(getAirDateInfo());    
    return buf.toString();
  }
  
  @Override
  public String getDialogText()
  {
    StringBuffer buf = new StringBuffer(getTitle());
    if (isShowMovie() && getYear() > 0)
      buf.append(" (").append(getYear()).append(")");    
    if (getRating() > 0)
      buf.append(" ").append(getRatingString(getRating()));
    buf.append(getSummary());
    return buf.toString();
  }

  public String getSummary()
  {
    StringBuffer summary = new StringBuffer();
    summary.append("\n").append(getChannelInfo()).append(" ").append(getShowTimeInfo());
    if (getSubTitle() != null)
      summary.append("\n\"").append(getSubTitle()).append("\"");
    if (!isShowMovie() && isRepeat())
      summary.append(getAirDateInfo());
    if (getDescription() != null)
      summary.append("\n").append(getDescription());
    return summary.toString();
  }
    
  @Override
  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getPath() != null && getPath().length() > 0)
      buf.append(getPath()).append("/");
    buf.append(getChannelNumber()).append(" (").append(getCallsign()).append(") ");
    buf.append(getTitle());
    try
    {
      buf.append(" (").append(getStartTimeFormatted()).append(" - ").append(getEndTimeFormatted()).append(")");
    }
    catch (ParseException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    return buf.toString();
  }
  
  @Override
  protected Comparator<Item> getDateComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        TvShow show1 = (TvShow) item1;
        TvShow show2 = (TvShow) item2;
        if (show1.getStartTime() == null)
        {
          if (show2.getStartTime() == null)
            return show1.toString().compareTo(show2.toString());
          else return 1;
        }
        else if (show2.getStartTime() == null)
        {
          return -1;
        }
        else
        {
          if (show1.getStartTime().equals(show2.getStartTime()))
          {
            String t1 = stripLeadingArticle(show1.getTitle());
            String t2 = stripLeadingArticle(show2.getTitle());
            return t1.compareToIgnoreCase(t2);
          }
          else
          {
            return show2.getStartTime().compareTo(show1.getStartTime());
          }
        }
      }
    };
  }
  
  @Override
  protected Comparator<Item> getChannelNumberComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        TvShow show1 = (TvShow) item1;
        TvShow show2 = (TvShow) item2;
        return show1.getChannelNumber() - show2.getChannelNumber();
      }
    };
  }
  
}
