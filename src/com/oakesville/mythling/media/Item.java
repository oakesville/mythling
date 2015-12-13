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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;

import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.util.TextBuilder;

public abstract class Item implements Listable {

    public abstract MediaType getType();

    public String getTypeLabel() {
        return Localizer.getItemTypeLabel(getType());
    }

    private String id;
    public String getId() { return id; }

    private Long downloadId;
    public Long getDownloadId() { return downloadId; }
    public void setDownloadId(Long id) { this.downloadId = id; }
    public boolean isDownloaded() {
        return downloadId != null && downloadId > 0;
    }

    private boolean transcoded;
    public boolean isTranscoded() { return transcoded; }
    public void setTranscoded(boolean transcoded) { this.transcoded = transcoded; }

    // searchPath is populated for search results
    private String searchPath;
    public String getSearchPath() { return searchPath; }
    public void setSearchPath(String searchPath) { this.searchPath = searchPath; }

    /**
     * item path (excluding filename)
     */
    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFilePath() {
        String itemPath = isSearchResult() ? searchPath : path;
        return itemPath.isEmpty() ? getFileName() : itemPath + "/" + getFileName();
    }

    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    private StorageGroup storageGroup;
    public StorageGroup getStorageGroup() { return storageGroup; }
    public void setStorageGroup(StorageGroup storageGroup) { this.storageGroup = storageGroup; }

    public String getEncodedTitle() {
        try {
            return URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Just the base part of the filename.
     */
    private String fileBase;
    public String getFileBase() { return fileBase; }
    public void setFileBase(String file) { this.fileBase = file; }

    private String format;
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    private String subTitle;
    public String getSubTitle() { return subTitle; }
    public void setSubTitle(String subTitle) { this.subTitle = subTitle; }

    private float rating;
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public Item(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getFileName() {
        if (fileBase != null) {
            return fileBase + "." + format;
        } else {
            return title + "." + format;
        }
    }

    public boolean isMusic() {
        return getType() == MediaType.music;
    }

    public boolean isRecording() {
        return getType() == MediaType.recordings;
    }

    public boolean isLiveTv() {
        return getType() == MediaType.liveTv;
    }

    public boolean isMovie() {
        return getType() == MediaType.movies;
    }

    public boolean isTvSeries() {
        return getType() == MediaType.tvSeries;
    }

    /**
     * Used by ListableListAdapter
     */
    public String toString() {
        if (isSearchResult())
            return getSearchResultText();
        else
            return getListText();
    }

    public String getListText() {
        return getTitle();
    }

    public String getListSubText() {
        if (subTitle == null)
            return null;
        return new TextBuilder().appendQuoted(subTitle).toString();
    }

    /**
     * Identifies the item in dialog and context menu title bars.
     */
    public String getDialogTitle() {
        return new TextBuilder(title).appendDashed(getSubLabel()).toString();
    }

    public String getDialogText() {
        return getTitle() + getListSubText();
    }

    public String getSubLabel() {
        return new TextBuilder().appendQuoted(subTitle).toString();
    }

    public int getIconResourceId() {
        return 0;
    }

    public String getSearchResultText() {
        TextBuilder tb = new TextBuilder();
        tb.appendParen(getTypeLabel());
        tb.append(getTitle());
        return tb.toString();
    }

    public boolean isSearchResult() {
        return getSearchPath() != null;
    }

    /**
     * Item length in ms (-1 if unknown).
     */
    public long getLength() {
        return -1;
    }

    /**
     * Default supports by title or rating.
     */
    public Comparator<Item> getComparator(SortType sort) {
        if (isLiveTv())
            return getChannelNumberComparator();
        else if (sort == SortType.byRating)
            return getRatingComparator();
        else if (sort == SortType.byDate)
            return getDateComparator();
        else
            return getTitleComparator();
    }

    protected Comparator<Item> getTitleComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                String t1 = Localizer.stripLeadingArticle(item1.getTitle());
                String t2 = Localizer.stripLeadingArticle(item2.getTitle());
                return t1.compareToIgnoreCase(t2);
            }
        };
    }

    protected Comparator<Item> getRatingComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                float f = item2.getRating() - item1.getRating();
                if (f > 0) {
                    return 1;
                } else if (f < 0) {
                    return -1;
                } else {
                    String t1 = Localizer.stripLeadingArticle(item1.getTitle());
                    String t2 = Localizer.stripLeadingArticle(item2.getTitle());
                    return t1.compareToIgnoreCase(t2);
                }
            }
        };
    }

    protected Comparator<Item> getDateComparator() {
        return getTitleComparator(); // supported only for specific types
    }

    protected Comparator<Item> getChannelNumberComparator() {
        return getTitleComparator(); // supported only for specific types
    }

    public ArtworkDescriptor getArtworkDescriptor(String storageGroup) {
        return null;
    }
}
