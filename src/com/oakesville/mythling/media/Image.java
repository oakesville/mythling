package com.oakesville.mythling.media;

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Image extends Item
{
  public Image(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.images;
  }
  
  public String getTypeTitle()
  {
    return "Image";
  }
}
