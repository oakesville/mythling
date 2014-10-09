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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;

public class Category implements Listable, Comparable<Category>
{
  public Category(String name, MediaType type)
  {
    this.name = name;
    this.type = type;
  }
  
  public Category(String name, Category parent)
  {
    this.name = name;
    this.parent = parent;
    this.type = parent.type;
  }
  
  private String name;
  public String getName() { return name; }

  private MediaType type;
  public MediaType getType() { return type; }
  
  private Category parent;
  public Category getParent() { return parent; }
  
  public boolean isTopLevel()
  {
    return parent == null;
  }
  
  private List<Category> children = new ArrayList<Category>();
  public List<Category> getChildren() { return children; }
  public void setChildren(List<Category> children) { this.children = children; }
  public void addChild(Category childCat)
  {
    children.add(childCat);
  }
  public Category getChild(String name)
  {
    for (Category child : children)
    {
      if (child.getName().equals(name))
        return child;
    }
    return null;
  }
  
  private List<Item> items = new ArrayList<Item>();
  public List<Item> getItems() { return items; }
  public void setItems(List<Item> items) { this.items = items; }
  public void addItem(Item item)
  {
    items.add(item);
  }
  
  public boolean hasItems()
  {
    return items != null && items.size() > 0;
  }
  
  @Override
  public String toString()
  {
    return name;
  }
  
  public List<Listable> getList()
  {
    List<Listable> listable = new ArrayList<Listable>();
    for (Category cat : getChildren())
      listable.add(cat);
    for (Item item : getItems())
      listable.add(item);
    return listable;
  }
  
  public void sortItems(SortType sort)
  {
    if (!items.isEmpty())
      Collections.sort(items, items.get(0).getComparator(sort));
  }
  
  public String getLabel()
  {
    return name;
  }

  @Override
  public int compareTo(Category another)
  {
    String n1 = stripLeadingArticle(this.name);
    String n2 = stripLeadingArticle(another.name);
    return n1.compareToIgnoreCase(n2);
  }
  
  /**
   * TODO duplicated in Item.java 
   */
  private String stripLeadingArticle(String inStr)
  {
    if (inStr.startsWith("The "))
      return inStr.substring(4);
    if (inStr.startsWith("A "))
      return inStr.substring(2);
    if (inStr.startsWith("An "))
      return inStr.substring(3);
    return inStr;
  }
}
