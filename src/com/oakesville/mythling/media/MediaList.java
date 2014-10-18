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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;

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
  
  private String charSet;
  public String getCharSet() { return charSet; }
  public void setCharSet(String charSet) { this.charSet = charSet; }
  
  private int count;
  public int getCount() { return count; }
  public void setCount(int ct) { this.count = ct; }
  public void setCount(String ct)
  {
    count = Integer.parseInt(ct);
  }
  
  /**
   * either storageGroup or basePath should be populated 
   */
  private String basePath;
  public String getBasePath() { return basePath; }
  public void setBasePath(String bp) { this.basePath = bp; }
  private StorageGroup storageGroup;
  public StorageGroup getStorageGroup() { return storageGroup; }
  public void setStorageGroup(StorageGroup sg) { this.storageGroup = sg; }
  
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
  
  public void addItemUnderPathCategory(Item item)
  {
    String filepath = item.getFileBase();
    if (getStorageGroup() == null && filepath.startsWith(basePath + "/"))
    {
      filepath = filepath.substring(basePath.length() + 1);
      item.setFileBase(filepath);
    }
    String[] segments = filepath.split("/");
    if (segments.length > 1 && !filepath.startsWith("/"))
    {
      Category cat = null;
      for (int i = 0; i < segments.length; i++)
      {
        String seg = segments[i];
        if (i < segments.length - 1)
        {
          Category subcat = cat == null ? getCategory(seg) : cat.getChild(seg);
          if (subcat == null)
          {
            if (cat == null)
              addCategory(subcat = new Category(seg, item.getType()));
            else
              cat.addChild(subcat = new Category(seg, cat));
          }
          cat = subcat;
        }
        else
        {
          item.setFileBase(seg);
        }
      }
      cat.addItem(item);
    }
    else
    {
      addItem(item);
    }
  }
  
  public List<Listable> getTopCategoriesAndItems()
  {
    List<Listable> all = new ArrayList<Listable>();
    if (categories != null)
      all.addAll(categories);
    if (items != null)
      all.addAll(items);
    return all;
  }
  
  public boolean hasTopLevelItems()
  {
    return items != null && items.size() > 0;
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
    if (path == null || "".equals(path))
      return getTopCategoriesAndItems();
    if (path.startsWith("/"))
      path = path.substring(1);
          
    Category curCat = null;
    for (Category cat : getCategories())
    {
      if (path.startsWith(cat.getName()))
        curCat = cat;
    }
    
    if (curCat == null)  // how?
      return getTopCategoriesAndItems();
    
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
  
  public boolean hasItems(String path)
  {
    for (Listable listable : getListables(path))
    {
      if (listable instanceof Item)
        return true;
    }
    return false;
  }
  
  public boolean supportsSort()
  {
    return mediaType != MediaType.liveTv && mediaType != MediaType.music;
  }
  
  public boolean canHaveArtwork()
  {
    return mediaType != MediaType.liveTv;
  }
  
  public void sort(SortType sortType)
  {
    if (!items.isEmpty())
      Collections.sort(items, items.get(0).getComparator(sortType));
    for (Category cat : getCategories())
      sortCategory(cat, sortType);
  }
  
  public void sortCategory(Category category, SortType sort)
  {
    category.sortItems(sort);
    for (Category child : category.getChildren())
      sortCategory(child, sort);
  }
}
