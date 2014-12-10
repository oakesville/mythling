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

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Song extends Item
{
  public static final String ARTWORK_LEVEL_ALBUM = "albumArtwork";
  public static final String ARTWORK_LEVEL_SONG = "songArtwork";
  
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
  public ArtworkDescriptor getArtworkDescriptor(String storageGroup)
  {
    if (albumArtId == 0)
      return null;

    // actually storageGroup is artwork level (album or song)
    final boolean songLevelArt = ARTWORK_LEVEL_SONG.equals(storageGroup);

    return new ArtworkDescriptor(storageGroup) 
    {
      public String getArtworkPath()
      {
        // cache at album level 
        return getStorageGroup() + (songLevelArt ? ("/" + getId()) : "");
      }
      
      public String getArtworkContentServicePath() throws UnsupportedEncodingException
      {
        return "GetAlbumArt?Id=" + getAlbumArtId();
      }
    };
  }
  
  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (!getSearchPath().isEmpty())
      buf.append(getSearchPath()).append("\n");
    buf.append(getTitle());
    return buf.toString();
  }
  
}
