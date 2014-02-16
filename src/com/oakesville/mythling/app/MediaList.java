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
package com.oakesville.mythling.app;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.MediaSettings.SortType;

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
  
  private String basePath;
  public String getBasePath() { return basePath; }
  public void setBasePath(String bp) { this.basePath = bp; }
  
  private StorageGroup storageGroup;
  public StorageGroup getStorageGroup() { return storageGroup; }
  public void setStorageGroup(StorageGroup sg) { this.storageGroup = sg; }
  
  private String artworkStorageGroup;
  public String getArtworkStorageGroup() { return artworkStorageGroup; }
  public void setArtworkStorageGroup(String asg) { this.artworkStorageGroup = asg; }
  
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
    String filepath = item.getFile();
    if (getStorageGroup() == null && filepath.startsWith(basePath + "/"))
    {
      filepath = filepath.substring(basePath.length() + 1);
      item.setFile(filepath);
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
          item.setFile(seg);
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
  
  public void sort(SortType sortType)
  {
    for (Category cat : getCategories())
      sortCategory(cat, new ItemSortComparator(sortType));
  }
  
  public void sortCategory(Category category, Comparator<Item> comparator)
  {
    category.sortItems(comparator);
    for (Category child : category.getChildren())
      sortCategory(child, comparator);
  }
  
  private class ItemSortComparator implements Comparator<Item>
  {
    private SortType sort;
    ItemSortComparator(SortType sort)
    {
      this.sort = sort;
    }
    
    public int compare(Item item1, Item item2)
    {
      if (sort == SortType.byDate)
      {
        if (item1.isMovie())
        {
          if (item1.getYear() == item2.getYear())
            return item1.toString().compareTo(item2.toString());
          else
            return item1.getYear() - item2.getYear();
        }
        else if (item1.isTvSeries())
        {
          if (item1.getSeason() == item2.getSeason())
            return item1.getEpisode() - item2.getEpisode();
          else
            return item1.getSeason() - item2.getSeason();
        }
        else if (item1.isRecording())
        {
          if (item1.getStartTime() == null)
          {
            if (item2.getStartTime() == null)
              return item1.toString().compareTo(item2.toString());
            else return 1;
          }
          else if (item2.getStartTime() == null)
          {
            return -1;
          }
          else
          {
            if (item1.getStartTime().equals(item2.getStartTime()))
              return item1.toString().compareTo(item2.toString());
            else
              return item2.getStartTime().compareTo(item1.getStartTime());
          }
        }
        else
        {
          return item1.toString().compareTo(item2.toString());
        }
      }
      else if (sort == SortType.byRating)
      {
        float f = item2.getRating() - item1.getRating();
        if (f > 0)
          return 1;
        else if (f < 0)
          return -1;
        else
          return 0;
      }
      else
      {
        return item1.toString().compareTo(item2.toString());
      }
    }
  }
}
