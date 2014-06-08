package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Song extends Item
{
  public Song(String id, String title)
  {
    super(id, title);
  }
  
  private int albumArtId;
  public int getAlbumArtId() { return albumArtId; }
  public void setAlbumArtId(int id) { this.albumArtId = id; }
  
  public MediaType getType()
  {
    return MediaType.music;
  }

  public String getTypeTitle()
  {
    return "Song";
  }
  
  @Override
  public boolean hasArtwork()
  {
    return getAlbumArtId() != 0;
  }
  
  @Override
  public String getArtworkPath()
  {
    return "";  // TODO: forces caching at ALBUM level (not supporting individual song artwork -- if there is such a thing)
  }
  
  @Override
  public String getArtworkContentServicePath() throws UnsupportedEncodingException
  {
    return "GetAlbumArt?Id=" + getAlbumArtId();
  }
}
