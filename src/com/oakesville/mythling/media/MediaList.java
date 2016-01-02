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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;

public class MediaList {

    private MediaType mediaType;
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mt) { this.mediaType = mt; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    public void setRetrieveDate(String d) throws ParseException {
        retrieveDate = Localizer.SERVICE_DATE_TIME_ZONE_FORMAT.parse(d);
    }

    public String getRetrieveDateDisplay() {
        return Localizer.getWeekdayDateFormat().format(retrieveDate);
    }

    public String getRetrieveTimeDisplay() {
        return Localizer.getTimeFormat().format(retrieveDate);
    }

    private String charSet;
    public String getCharSet() { return charSet; }
    public void setCharSet(String charSet) { this.charSet = charSet; }

    private int count;
    public int getCount() { return count; }
    public void setCount(int ct) { this.count = ct; }

    public void setCount(String ct) {
        count = Integer.parseInt(ct);
    }

    /**
     * basePath should be populated if no storage group for media type
     * (music and possibly videos)
     */
    private String basePath;
    public String getBasePath() { return basePath; }
    public void setBasePath(String bp) { this.basePath = bp;  }

    private List<Item> items = new ArrayList<Item>();
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public void addItem(Item item) {
        items.add(item);
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    private List<Category> categories = new ArrayList<Category>();
    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> cats) { this.categories = cats; }

    public void addCategory(Category cat) {
        categories.add(cat);
    }

    public void addItemUnderPathCategory(Item item) {
        String filepath = item.getFileBase();
        if (item.getStorageGroup() == null && filepath.startsWith(basePath + "/")) {
            filepath = filepath.substring(basePath.length() + 1);
            item.setFileBase(filepath);
        }
        String[] segments = filepath.split("/");
        if (segments.length > 1 && !filepath.startsWith("/")) {
            Category cat = null;
            for (int i = 0; i < segments.length; i++) {
                String seg = segments[i];
                if (i < segments.length - 1) {
                    Category subcat = cat == null ? getCategory(seg) : cat.getChild(seg);
                    if (subcat == null) {
                        if (cat == null)
                            addCategory(subcat = new Category(seg, item.getType()));
                        else
                            cat.addChild(subcat = new Category(seg, cat));
                    }
                    cat = subcat;
                } else {
                    item.setFileBase(seg);
                    item.setPath(filepath.substring(0, filepath.length() - seg.length() - 1));
                }
            }
            cat.addItem(item);
        } else {
            addItem(item);
            item.setPath("");
        }
    }

    public List<Listable> getTopCategoriesAndItems() {
        List<Listable> all = new ArrayList<Listable>();
        if (categories != null)
            all.addAll(categories);
        if (items != null)
            all.addAll(items);
        return all;
    }

    public boolean hasTopLevelItems() {
        return items != null && items.size() > 0;
    }

    public Category getCategory(String name) {
        for (Category cat : categories) {
            if (cat.getName().equals(name))
                return cat;
        }
        return null;
    }

    public List<Listable> getListables(String path) {
        return getListables(path, true);
    }

    public List<Listable> getListables(String path, boolean lenient) {
        if (path == null || "".equals(path))
            return getTopCategoriesAndItems();
        if (path.startsWith("/"))
            path = path.substring(1);

        Category curCat = null;
        for (Category cat : getCategories()) {
            if (path.startsWith(cat.getName()))
                curCat = cat;
        }

        if (curCat == null) {
            if (lenient)
                return getTopCategoriesAndItems();
            else
                return null;
        }

        StringTokenizer st = new StringTokenizer(path.substring(curCat.getName().length()), "/");
        while (st.hasMoreTokens()) {
            String segment = st.nextToken();
            for (Category cat : curCat.getChildren()) {
                if (cat.getName().equals(segment)) {
                    curCat = cat;
                }
            }
        }
        return curCat.getList();
    }

    public boolean hasItems(String path) {
        for (Listable listable : getListables(path)) {
            if (listable instanceof Item)
                return true;
        }
        return false;
    }

    public boolean supportsSort() {
        return mediaType != MediaType.liveTv && mediaType != MediaType.music;
    }

    public boolean canHaveArtwork() {
        return mediaType != MediaType.liveTv;
    }

    public boolean supportsTranscode() {
        return mediaType == MediaType.recordings || mediaType == MediaType.movies || mediaType == MediaType.tvSeries;
    }

    public void sort(SortType sortType, boolean includeItems) {
        if (includeItems && !items.isEmpty())
            Collections.sort(items, items.get(0).getComparator(sortType));
        Collections.sort(getCategories());
        // the categories themselves are always sorted by title
        for (Category cat : getCategories())
            sortCategory(cat, sortType, includeItems);
    }

    public void sortCategory(Category category, SortType sort, boolean includeItems) {
        if (includeItems)
            category.sortItems(sort);
        // the categories themselves are always sorted by title
        Collections.sort(category.getChildren());
        for (Category child : category.getChildren())
            sortCategory(child, sort, includeItems);
    }

    public void setDownloadIds(Map<String,Long> downloads) {
        if (items != null) {
            for (Item item : items)
                item.setDownloadId(downloads.get(item.getId()));
        }
        if (categories != null) {
            for (Category cat : categories)
                cat.setDownloadIds(downloads);
        }
    }

    public List<Item> getAllItems() {
        List<Item> allItems = new ArrayList<Item>();
        if (items != null)
            allItems.addAll(items);
        if (categories != null) {
            for (Category cat : categories)
                allItems.addAll(cat.getAllItems());
        }
        return allItems;
    }
}
