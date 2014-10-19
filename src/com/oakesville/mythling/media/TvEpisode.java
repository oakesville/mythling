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

import java.util.Comparator;

import com.oakesville.mythling.media.MediaSettings.MediaType;

/**
 * A TV episode from MythVideo.
 */
public class TvEpisode extends Video
{
  private int season;
  public int getSeason() { return season; }
  public void setSeason(int season) { this.season = season; }
  
  private int episode;
  public int getEpisode() { return episode; }
  public void setEpisode(int episode) { this.episode = episode; }
  
  public TvEpisode(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.tvSeries;
  }
  
  public String getTypeTitle()
  {
    return "TV Episode";
  }
  
  @Override
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX + getTitle());
    buf.append(" (s").append(getSeason()).append("e").append(getEpisode()).append(")");
    if (getRating() > 0)
      buf.append(" ").append(getRatingString(getRating()));
    return buf.toString();
  }

  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getPath() != null && getPath().length() > 0)
      buf.append(getPath()).append("/");
    buf.append(getTitle());
    buf.append(" (s").append(getSeason()).append("e").append(getEpisode()).append(")");
    return buf.toString();
  }
  
  public String getSummary()
  {
    if (getSeason() != 0)
    {
      StringBuffer sum = new StringBuffer();
      sum.append("Season ").append(getSeason()).append(", Episode ").append(getEpisode());
      if (super.getSummary() != null)
        sum.append("\n").append(super.getSummary());
      return sum.toString();
    }
    else
    {
      return super.getSummary();
    }
  }
  
  @Override
  protected Comparator<Item> getDateComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        TvEpisode episode1 = (TvEpisode) item1;
        TvEpisode episode2 = (TvEpisode) item2;
        if (episode1.getSeason() == episode2.getSeason())
          return episode1.getEpisode() - episode2.getEpisode();
        else
          return episode1.getSeason() - episode2.getSeason();
      }
    };
  }
}
