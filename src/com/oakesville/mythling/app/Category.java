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

import java.util.ArrayList;
import java.util.List;

import com.oakesville.mythling.app.MediaSettings.MediaType;


public class Category implements Item
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
  
  private List<Work> works = new ArrayList<Work>();
  public List<Work> getWorks() { return works; }
  public void setWorks(List<Work> works) { this.works = works; }
  public void addWork(Work work)
  {
    works.add(work);
  }
  
  @Override
  public String toString()
  {
    return name;
  }
  
  
  public List<Item> getItems()
  {
    List<Item> items = new ArrayList<Item>();
    for (Category cat : getChildren())
      items.add(cat);
    for (Work work : getWorks())
      items.add(work);
    return items;
  }
  
  public String getLabel()
  {
    return name;
  }

}
