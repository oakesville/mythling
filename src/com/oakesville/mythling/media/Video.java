package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Video extends Item
{
  private int year;
  public int getYear() { return year; }
  public void setYear(int year) { this.year = year; }
  
  private String director;
  public String getDirector() { return director; }
  public void setDirector(String director) { this.director = director; }
  
  private String actors;
  public String getActors() { return actors; }
  public void setActors(String actors) { this.actors = actors; }
  
  private String summary;
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }

  private String artwork;
  public String getArtwork() { return artwork; }
  public void setArtwork(String artwork) { this.artwork = artwork; }
  
  private String internetRef;
  public String getInternetRef() { return internetRef; }
  public void setInternetRef(String inetRef) { this.internetRef = inetRef; }
  
  private String pageUrl;
  public String getPageUrl() { return pageUrl; }
  public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }  
  
  public Video(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.videos;
  }

  public String getTypeTitle()
  {
    return "Video";
  }
  
  public String getShowInfo()
  {
    String str = (getYear() == 0 ? "" : getYear() + "   ") + getRatingString(getRating()) + "\n";
    str += getDirector() == null ? "" : "Directed By: " + getDirector() + "\n";
    str += getActors() == null ? "" : "Starring: " + getActors() + "\n\n";
    str += getSummary() == null ? "" : getSummary();
    return str;
  }
  
  @Override
  public ArtworkDescriptor getArtworkDescriptor(String storageGroup)
  {
    if (artwork == null)
      return null;

    return new ArtworkDescriptor(storageGroup)
    {
      public String getArtworkPath()
      {
        return getStorageGroup() + "/" + artwork;
      }
      
      public String getArtworkContentServicePath() throws UnsupportedEncodingException
      {
        return "GetImageFile?StorageGroup=" + URLEncoder.encode(getStorageGroup(), "UTF-8") + "&FileName=" + URLEncoder.encode(artwork, "UTF-8");    
      }
    };
  }  
}
