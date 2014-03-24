package com.oakesville.mythling.media;

import java.text.ParseException;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Recording extends TvShow
{
  private static final String TAG = Recording.class.getSimpleName();

  private int recordingRuleId;
  public int getRecordingRuleId() { return recordingRuleId; }
  public void setRecordingRuleId(int rrid) { this.recordingRuleId = rrid; }

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
  
}
