package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Recording extends TvShow
{
  private static final String TAG = Recording.class.getSimpleName();

  private int recordingRuleId;
  public int getRecordingRuleId() { return recordingRuleId; }
  public void setRecordingRuleId(int rrid) { this.recordingRuleId = rrid; }
  
  private String internetRef;
  public String getInternetRef() { return internetRef; }
  public void setInternetRef(String inetRef) { this.internetRef = inetRef; } 
  
  private int season;
  public int getSeason() { return season; }
  public void setSeason(int season) { this.season = season; }

  public Recording(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.recordings;
  }
  
  public String getTypeTitle()
  {
    return "Recording";
  }
  
  @Override
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    try
    {
      buf.append(getStartDateTimeFormatted()).append(" - ");
      buf.append(getChannelNumber()).append(" (").append(getCallsign()).append(") ");
    }
    catch (ParseException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    buf.append("\n").append(getTitle());
    buf.append("\n").append(getShowInfo());
    return buf.toString();
  }

  @Override
  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getPath() != null && getPath().length() > 0)
      buf.append(getPath()).append("/");
    buf.append(getTitle());
    try
    {
      buf.append(getStartDateTimeFormatted()).append(" - ");
      buf.append(getChannelNumber()).append(" (").append(getCallsign()).append(") ");
    }
    catch (ParseException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
    }
    return buf.toString();
  }
  
  @Override
  public ArtworkDescriptor getArtworkDescriptor(String storageGroup)
  {
    final boolean usePreviewImage = AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS.equals(storageGroup);
    if (getInternetRef() == null && !usePreviewImage)
      return null;

    return new ArtworkDescriptor(storageGroup)
    {
      public String getArtworkPath()
      {
        return getStorageGroup() + "/" + getId();
      }
      
      public String getArtworkContentServicePath() throws UnsupportedEncodingException
      {
        if (usePreviewImage)
        {
          return "GetPreviewImage?ChanId=" + getChannelId() + "&StartTime=" + getStartTimeParam();
        }
        else
        {
          String type = "coverart";
          if ("Fanart".equals(getStorageGroup()))
            type = "fanart";
          else if ("Banners".equals(getStorageGroup()))
            type = "banners";
          String path = "GetRecordingArtwork?Inetref=" + getInternetRef() + "&Type=" + type;
          if (season > 0)
            path += "&Season=" + season;
          return path;
        }
      }
    };
  }
}
