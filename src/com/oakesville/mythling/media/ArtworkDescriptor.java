package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;

public abstract class ArtworkDescriptor
{
  private String storageGroup;
  protected String getStorageGroup() { return storageGroup; }
  
  public ArtworkDescriptor(String storageGroup)
  {
    this.storageGroup = storageGroup;
  }
  
  /**
   * For local storage
   */
  public abstract String getArtworkPath();
  
  public abstract String getArtworkContentServicePath() throws UnsupportedEncodingException;
}
