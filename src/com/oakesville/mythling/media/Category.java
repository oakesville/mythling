/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;

public class Category implements Listable, Comparable<Category> {

    public Category(String name, MediaType type) {
        this.name = name;
        this.type = type;
    }

    public Category(String name, Category parent) {
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

    public boolean isTopLevel() {
        return parent == null;
    }

    private List<Category> children = new ArrayList<Category>();
    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }

    public void addChild(Category childCat) {
        children.add(childCat);
    }

    public Category getChild(String name) {
        for (Category child : children) {
            if (child.getName().equals(name))
                return child;
        }
        return null;
    }

    private List<Item> items = new ArrayList<Item>();
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public void addItem(Item item) {
        items.add(item);
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public boolean hasItems() {
        return items != null && items.size() > 0;
    }

    public void setDownloadIds(Map<String,Long> downloads) {
        if (items != null) {
            for (Item item : items)
                item.setDownloadId(downloads.get(item.getId()));
        }
        if (children != null) {
            for (Category child : children)
                child.setDownloadIds(downloads);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public List<Listable> getList() {
        List<Listable> listable = new ArrayList<Listable>();
        for (Category cat : getChildren())
            listable.add(cat);
        for (Item item : getItems())
            listable.add(item);
        return listable;
    }

    public void sortItems(SortType sort) {
        if (!items.isEmpty())
            Collections.sort(items, items.get(0).getComparator(sort));
    }

    public String getLabel() {
        return name;
    }

    @Override
    public int compareTo(Category another) {
        String n1 = Localizer.stripLeadingArticle(this.name);
        String n2 = Localizer.stripLeadingArticle(another.name);
        return n1.compareToIgnoreCase(n2);
    }
}
