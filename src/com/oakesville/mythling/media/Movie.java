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

public class Movie extends Video
{
  public Movie(String id, String title)
  {
    super(id, title);
  }

  public MediaType getType()
  {
    return MediaType.movies;
  }
  
  public String getTypeTitle()
  {
    return "Movie";
  }
  
  public String getLabel()
  {
    String label = getTitle();
    if (getYear() > 0)
      label += " (" + getYear() + ")";
    return label;
  }

  @Override
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX + getTitle());
    if (getYear() > 0)
      buf.append(" (").append(getYear()).append(")");
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
    if (getYear() > 0)
      buf.append(" (").append(getYear()).append(")");
    return buf.toString();
  }
  
  @Override
  protected Comparator<Item> getDateComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
        Movie movie1 = (Movie) item1;
        Movie movie2 = (Movie) item2;
        if (movie1.getYear() == movie2.getYear())
          return movie1.getTitle().compareTo(movie2.getTitle());
        else
          return movie1.getYear() - movie2.getYear();
      }
    };
  }
}
