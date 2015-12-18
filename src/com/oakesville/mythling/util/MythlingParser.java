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
package com.oakesville.mythling.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.Movie;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.media.Song;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TvEpisode;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.media.Video;

import android.util.Log;

/**
 * Artist and title may be reversed for some folks
 * but the reversal will be counteracted so that they'll
 * be displayed in the same order as in the filename.
 */
public class MythlingParser implements MediaListParser {
    private static final String TAG = MythlingParser.class.getSimpleName();

    private AppSettings appSettings;
    private String json;

    public MythlingParser(AppSettings appSettings, String json) {
        this.appSettings = appSettings;
        this.json = json;
    }

    public MediaList parseMediaList(MediaType mediaType, Map<String,StorageGroup> storageGroups) throws JSONException, ParseException, ServiceException {
        MediaList mediaList = new MediaList();
        mediaList.setMediaType(mediaType);

        long startTime = System.currentTimeMillis();
        JSONObject list = new JSONObject(json);
        if (list.has("error"))
            throw new ServiceException(list.getString("error"));
        JSONObject summary = list.getJSONObject("summary");
        mediaList.setRetrieveDate(summary.getString("date"));
        mediaList.setCount(summary.getString("count"));
        if (summary.has("base"))
            mediaList.setBasePath(summary.getString("base"));
        if (list.has("items")) {
            JSONArray its = list.getJSONArray("items");
            for (int i = 0; i < its.length(); i++) {
                JSONObject it = (JSONObject) its.get(i);
                Item item = buildItem(mediaList.getMediaType(), it, storageGroups);
                item.setPath("");
                mediaList.addItem(item);
            }
        }
        if (list.has("categories")) {
            JSONArray cats = list.getJSONArray("categories");
            for (int i = 0; i < cats.length(); i++) {
                JSONObject cat = (JSONObject) cats.get(i);
                mediaList.addCategory(buildCategory(mediaList.getMediaType(), cat, null, storageGroups));
            }
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, " -> media list parse time: " + (System.currentTimeMillis() - startTime) + " ms");

        SortType sortType = appSettings.getMediaSettings().getSortType();
        if ((mediaType == MediaType.movies || mediaType == MediaType.tvSeries)
                && (sortType == SortType.byDate || sortType == SortType.byRating)) {
            // media.php will have sorted by sortType within categories, but categories must be sorted by title
            startTime = System.currentTimeMillis();
            mediaList.sort(SortType.byTitle, false);
            if (BuildConfig.DEBUG)
                Log.d(TAG, " -> media list sort time: " + (System.currentTimeMillis() - startTime) + " ms");
        }

        return mediaList;
    }

    private Category buildCategory(MediaType type, JSONObject cat, Category parent, Map<String,StorageGroup> storageGroups) throws JSONException, ParseException {
        String name = cat.getString("name");
        Category category = parent == null ? new Category(name, type) : new Category(name, parent);
        if (cat.has("categories")) {
            JSONArray childs = cat.getJSONArray("categories");
            for (int i = 0; i < childs.length(); i++) {
                JSONObject childCat = (JSONObject) childs.get(i);
                category.addChild(buildCategory(type, childCat, category, storageGroups));
            }
        }
        if (cat.has("items")) {
            JSONArray its = cat.getJSONArray("items");
            for (int i = 0; i < its.length(); i++) {
                JSONObject it = (JSONObject) its.get(i);
                Item item = buildItem(type, it, storageGroups);
                item.setPath(category.getPath());
                category.addItem(item);
            }
        }
        return category;
    }

    public SearchResults parseSearchResults(Map<String,StorageGroup> storageGroups) throws JSONException, ParseException, ServiceException {
        SearchResults searchResults = new SearchResults();

        long startTime = System.currentTimeMillis();
        JSONObject list = new JSONObject(json);
        if (list.has("error"))
            throw new ServiceException(list.getString("error"));

        JSONObject summary = list.getJSONObject("summary");
        searchResults.setRetrieveDate(summary.getString("date"));
        searchResults.setQuery(summary.getString("query"));
        if (summary.has("videoBase"))
            searchResults.setVideoBase(summary.getString("videoBase"));
        if (summary.has("musicBase"))
            searchResults.setMusicBase(summary.getString("musicBase"));

        JSONArray vids = list.getJSONArray("videos");
        for (int i = 0; i < vids.length(); i++) {
            JSONObject vid = (JSONObject) vids.get(i);
            searchResults.addVideo(buildItem(MediaType.videos, vid, storageGroups));
        }

        JSONArray recordings = list.getJSONArray("recordings");
        for (int i = 0; i < recordings.length(); i++) {
            JSONObject recording = (JSONObject) recordings.get(i);
            recording.put("path", "");
            searchResults.addRecording(buildItem(MediaType.recordings, recording, storageGroups));
        }

        JSONArray tvShows = list.getJSONArray("liveTv");
        for (int i = 0; i < tvShows.length(); i++) {
            JSONObject tvShow = (JSONObject) tvShows.get(i);
            tvShow.put("path", "");
            searchResults.addLiveTvItem(buildItem(MediaType.liveTv, tvShow, storageGroups));
        }

        if (list.has("movies")) // if no videos categorization
        {
            JSONArray movies = list.getJSONArray("movies");
            for (int i = 0; i < movies.length(); i++) {
                JSONObject movie = (JSONObject) movies.get(i);
                searchResults.addMovie(buildItem(MediaType.movies, movie, storageGroups));
            }
        }

        if (list.has("tvSeries")) // if no videos categorization
        {
            JSONArray tvSeries = list.getJSONArray("tvSeries");
            for (int i = 0; i < tvSeries.length(); i++) {
                JSONObject tvSeriesItem = (JSONObject) tvSeries.get(i);
                searchResults.addTvSeriesItem(buildItem(MediaType.tvSeries, tvSeriesItem, storageGroups));
            }
        }

        if (list.has("songs")) {
            JSONArray songs = list.getJSONArray("songs");
            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = (JSONObject) songs.get(i);
                searchResults.addSong(buildItem(MediaType.music, song, storageGroups));
            }
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, " -> search results parse time: " + (System.currentTimeMillis() - startTime) + " ms");

        return searchResults;
    }

    private Item buildItem(MediaType type, JSONObject jsonObj, Map<String,StorageGroup> storageGroups) throws JSONException, ParseException {
        Item item;
        if (type == MediaType.movies) {
            item = new Movie(jsonObj.getString("id"), jsonObj.getString("title"));
            if (storageGroups != null)
                item.setStorageGroup(storageGroups.get(appSettings.getVideoStorageGroup()));
            addVideoInfo((Video) item, jsonObj);
        } else if (type == MediaType.tvSeries) {
            item = new TvEpisode(jsonObj.getString("id"), jsonObj.getString("title"));
            if (storageGroups != null)
                item.setStorageGroup(storageGroups.get(appSettings.getVideoStorageGroup()));
            addVideoInfo((Video) item, jsonObj);
            if (jsonObj.has("season"))
                ((TvEpisode) item).setSeason(Integer.parseInt(jsonObj.getString("season")));
            if (jsonObj.has("episode"))
                ((TvEpisode) item).setEpisode(Integer.parseInt(jsonObj.getString("episode")));
        } else if (type == MediaType.videos) {
            item = new Video(jsonObj.getString("id"), jsonObj.getString("title"));
            if (storageGroups != null)
                item.setStorageGroup(storageGroups.get(appSettings.getVideoStorageGroup()));
            addVideoInfo((Video) item, jsonObj);
        } else if (type == MediaType.liveTv) {
            item = new TvShow(jsonObj.getString("id"), jsonObj.getString("title"));
            addProgramInfo((TvShow) item, jsonObj);
        } else if (type == MediaType.recordings) {
            item = new Recording(jsonObj.getString("id"), jsonObj.getString("title"));
            addProgramInfo((TvShow) item, jsonObj);
            if (jsonObj.has("recordId"))
                ((Recording) item).setRecordRuleId(jsonObj.getInt("recordId"));
            if (storageGroups != null && jsonObj.has("storageGroup"))
                item.setStorageGroup(storageGroups.get(jsonObj.getString("storageGroup")));
            if (jsonObj.has("recGroup"))
                ((Recording) item).setRecordingGroup(jsonObj.getString("recGroup"));
            if (jsonObj.has("internetRef"))
                ((Recording) item).setInternetRef(jsonObj.getString("internetRef"));
            if (jsonObj.has("recStatus")) {
                String recStatus = jsonObj.getString("recStatus");
                ((Recording) item).setRecorded(recStatus.equals("-3") || recStatus.equals("Recorded"));
            }
        } else if (type == MediaType.music) {
            item = new Song(jsonObj.getString("id"), jsonObj.getString("title"));
            if (jsonObj.has("albumArtId"))
                ((Song) item).setAlbumArtId(jsonObj.getInt("albumArtId"));
        } else {
            throw new IllegalArgumentException("Unsupported media type: " + type);
        }

        if (jsonObj.has("format"))
            item.setFormat(jsonObj.getString("format"));
        if (jsonObj.has("path"))
            item.setSearchPath(jsonObj.getString("path"));
        if (jsonObj.has("file"))
            item.setFileBase(jsonObj.getString("file"));
        if (jsonObj.has("subtitle"))
            item.setSubTitle(jsonObj.getString("subtitle"));
        if (jsonObj.has("transcoded"))
            item.setTranscoded("true".equalsIgnoreCase(jsonObj.getString("transcoded")));
        return item;
    }

    private void addProgramInfo(TvShow item, JSONObject jsonObj) throws JSONException, ParseException {
        item.setStartTime(Localizer.SERVICE_DATE_TIME_RAW_FORMAT.parse(item.getId().substring(item.getId().indexOf('~') + 1)));
        if (jsonObj.has("callsign"))
            item.setCallsign(jsonObj.getString("callsign"));
        if (jsonObj.has("description"))
            item.setDescription(jsonObj.getString("description"));
        if (jsonObj.has("airdate")) {
            String ad = jsonObj.getString("airdate");
            if (ad.length() == 4) // year only (for movies)
                item.setOriginallyAired(Localizer.getYearFormat().parse(ad));
            else
                item.setOriginallyAired(Localizer.SERVICE_DATE_FORMAT.parse(ad));
        }
        if (jsonObj.has("endtime"))
            item.setEndTime(Localizer.SERVICE_DATE_TIME_RAW_FORMAT.parse(jsonObj.getString("endtime")));
        if (jsonObj.has("programStart"))
            item.setProgramStart(jsonObj.getString("programStart"));
        if (jsonObj.has("rating")) {
            // mythconverg stores program.stars and recorded.stars as a fraction of one
            Float rating = Float.parseFloat(jsonObj.getString("rating")) * 10;
            rating = (float) Math.round(rating) / 2;
            item.setRating(rating);
        }
    }

    private void addVideoInfo(Video item, JSONObject jsonObj) throws JSONException, ParseException {
        if (jsonObj.has("year"))
            item.setYear(Integer.parseInt(jsonObj.getString("year")));
        if (jsonObj.has("rating"))
            item.setRating(Float.parseFloat(jsonObj.getString("rating")) / 2);
        if (jsonObj.has("director"))
            item.setDirector(jsonObj.getString("director"));
        if (jsonObj.has("actors"))
            item.setActors(jsonObj.getString("actors"));
        if (jsonObj.has("summary"))
            item.setSummary(jsonObj.getString("summary"));
        if (jsonObj.has("artwork"))
            item.setArtwork(jsonObj.getString("artwork"));
        if (jsonObj.has("internetRef"))
            item.setInternetRef(jsonObj.getString("internetRef"));
        if (jsonObj.has("pageUrl"))
            item.setPageUrl(jsonObj.getString("pageUrl"));
    }

    public List<Item> parseQueue(MediaType type, Map<String,StorageGroup> storageGroups) throws JSONException, ParseException {
        List<Item> queue = new ArrayList<Item>();
        long startTime = System.currentTimeMillis();
        JSONObject list = new JSONObject(json);
        JSONArray vids = list.getJSONArray(type.toString());
        for (int i = 0; i < vids.length(); i++) {
            JSONObject vid = (JSONObject) vids.get(i);
            queue.add(buildItem(type, vid, storageGroups));
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, " -> (" + type + ") queue parse time: " + (System.currentTimeMillis() - startTime) + " ms");
        return queue;
    }
}
