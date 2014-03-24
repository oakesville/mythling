package com.oakesville.mythling.media;

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Song extends Item
{
  public Song(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.music;
  }

  public String getTypeTitle()
  {
    return "Song";
  }
}
