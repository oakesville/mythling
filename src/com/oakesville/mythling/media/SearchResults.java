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
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.oakesville.mythling.app.Localizer;

import io.oakesville.media.Download;
import io.oakesville.media.Item;
import io.oakesville.media.Recording;
import io.oakesville.media.StorageGroup;

public class SearchResults {

    private String mythTvVersion = "Unknown";
    public String getMythTvVersion() { return mythTvVersion; }
    public void setMythTvVersion(String mythTvVersion) { this.mythTvVersion = mythTvVersion; }

    private Date retrieveDate;
    public Date getRetrieveDate() { return retrieveDate; }
    public void setRetrieveDate(Date d) { this.retrieveDate = d; }

    public void setRetrieveDate(String d) throws ParseException {
        retrieveDate = Localizer.SERVICE_DATE_TIME_ZONE_FORMAT.parse(d);
    }

    private String charSet;
    public String getCharSet() { return charSet; }
    public void setCharSet(String charSet) { this.charSet = charSet; }

    private String query;
    public String getQuery() { return query; }
    public void setQuery(String q) { this.query = q; }

    private String videoBase;
    public String getVideoBase() { return videoBase; }
    public void setVideoBase(String vb) { this.videoBase = vb; }

    private String musicBase;
    public String getMusicBase() { return musicBase; }
    public void setMusicBase(String mb) { this.musicBase = mb; }

    private Map<String,StorageGroup> storageGroups;
    public Map<String,StorageGroup> getStorageGroups() { return storageGroups; }
    public void setStorageGroups(Map<String,StorageGroup> sgs) { this.storageGroups = sgs;  }

    private List<Item> videos = new ArrayList<>();
    public List<Item> getVideos() { return videos; }
    public void setVideos(List<Item> videos) { this.videos = videos; }

    private List<Item> recordings = new ArrayList<>();
    public List<Item> getRecordings() { return recordings; }
    public void setRecordings(List<Item> recordings) { this.recordings = recordings;  }

    private List<Item> liveTvItems = new ArrayList<>();
    public List<Item> getLiveTvItems() { return liveTvItems; }
    public void setLiveTvItems(List<Item> liveTvItems) { this.liveTvItems = liveTvItems; }

    private List<Item> movies = new ArrayList<>();
    public List<Item> getMovies() { return movies; }
    public void setMovies(List<Item> movies) { this.movies = movies; }

    private List<Item> tvSeriesItems = new ArrayList<>();
    public List<Item> getTvSeriesItems() { return tvSeriesItems; }
    public void setTvSeriesItems(List<Item> tvSeriesItems) { this.tvSeriesItems = tvSeriesItems; }

    private List<Item> songs = new ArrayList<>();
    public List<Item> getSongs() { return songs; }
    public void setSongs(List<Item> songs) { this.songs = songs; }

    public void addVideo(Item video) {
        videos.add(video);
    }

    public void addRecording(Item recording) {
        recordings.add(recording);
    }

    public void addLiveTvItem(Item liveTvItem) {
        liveTvItems.add(liveTvItem);
    }

    public void addMovie(Item movie) {
        movies.add(movie);
    }

    public void addTvSeriesItem(Item tvSeriesItem) {
        tvSeriesItems.add(tvSeriesItem);
    }

    public void addSong(Item song) {
        songs.add(song);
    }

    public int getCount() {
        return videos.size() + recordings.size() + liveTvItems.size() + movies.size() + tvSeriesItems.size() + songs.size();
    }

    public List<Item> getAll() {
        List<Item> all = new ArrayList<>();
        all.addAll(videos);
        all.addAll(recordings);
        all.addAll(liveTvItems);
        all.addAll(movies);
        all.addAll(tvSeriesItems);
        all.addAll(songs);
        return all;
    }

    public void setDownloads(Map<String,Download> downloads) {
        for (Item item : getAll()) {
            Download download = downloads.get(item.getId());
            if (download != null) {
                item.setDownloadId(download.getDownloadId());
                if (item.isRecording() && download.getCutList() != null)
                    ((Recording)item).setCutList(download.getCutList());
            }
        }
    }
}
