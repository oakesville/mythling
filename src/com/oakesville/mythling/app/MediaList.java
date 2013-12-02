/**
 * Copyright 2013 Donald Oakes
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
package com.oakesville.mythling.app;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.oakesville.mythling.app.MediaSettings.MediaType;

public class MediaList
{
  private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss Z");
  private DateFormat dateDisplay = new SimpleDateFormat("EEE, MMM d");  
  private DateFormat timeDisplay = new SimpleDateFormat("h:mm aa");
  
  private MediaType mediaType;
  public MediaType getMediaType() { return mediaType; }
  public void setMediaType(MediaType mt) { this.mediaType = mt; }
  
  private Date retrieveDate;
  public Date getRetrieveDate() { return retrieveDate; }
  public void setRetrieveDate(Date d) { this.retrieveDate = d; }
  public void setRetrieveDate(String d) throws ParseException
  {
    retrieveDate = dateFormat.parse(d);    
  }
  public String getRetrieveDateDisplay() 
  {
    return dateDisplay.format(retrieveDate);
  }
  public String getRetrieveTimeDisplay() 
  {
    return timeDisplay.format(retrieveDate);
  }
  
  private int count;
  public int getCount() { return count; }
  public void setCount(int ct) { this.count = ct; }
  public void setCount(String ct)
  {
    count = Integer.parseInt(ct);
  }
  
  private String basePath;
  public String getBasePath() { return basePath; }
  public void setBasePath(String bp) { this.basePath = bp; }
  
  private String pageLinkTitle;
  public String getPageLinkTitle() { return pageLinkTitle; }
  public void setPageLinkTitle(String plt) { this.pageLinkTitle = plt; }
  
  private List<Item> items = new ArrayList<Item>();
  public List<Item> getItems() { return items; }
  public void setItems(List<Item> items) { this.items = items; }
  public void addItem(Item item)
  {
    items.add(item);
  }
  
  private List<Category> categories = new ArrayList<Category>();
  public List<Category> getCategories() { return categories; }
  public void setCategories(List<Category> cats) { this.categories = cats; }
  public void addCategory(Category cat)
  {
    categories.add(cat);
  }
  
  public List<Listable> getTopCategoriesAndItems()
  {
    List<Listable> all = new ArrayList<Listable>();
    if (items != null)
      all.addAll(items);
    if (categories != null)
      all.addAll(categories);
    return all;
  }  
  
  public Category getCategory(String name)
  {
    for (Category cat : categories)
    {
      if (cat.getName().equals(name))
        return cat;
    }
    return null;
  }
  
  public List<Listable> getListables(String path)
  {
    Category curCat = null;
    for (Category cat : getCategories())
    {
      if (path.startsWith(cat.getName()))
        curCat = cat;
    }
    StringTokenizer st = new StringTokenizer(path.substring(curCat.getName().length()), "/");
    while (st.hasMoreTokens())
    {
      String segment = st.nextToken();
      for (Category cat : curCat.getChildren())
      {
        if (cat.getName().equals(segment))
        {
          curCat = cat;
        }
      }
    }
    return curCat.getList();
  }  
}
