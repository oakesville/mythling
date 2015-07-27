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
package com.oakesville.mythling.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.media.ChannelGroup;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.MythlingParser;

public class AppData {
    public static final int BUILD_ID = 7;
    private static final String TAG = AppData.class.getSimpleName();

    private static final String MEDIA_LIST_JSON_FILE = "mediaList.json";
    private static final String STORAGE_GROUPS_JSON_FILE = "storageGroups.json";
    private static final String CHANNEL_GROUPS_JSON_FILE = "channelGroups.json";
    private static final String QUEUE_FILE_SUFFIX = "Queue.json";

    private Context appContext;
    public AppData(Context appContext) { this.appContext = appContext;  }

    public boolean isExpired() {
        AppSettings appSettings = new AppSettings(appContext);
        long last = appSettings.getLastLoad();
        if (last <= 0)
            return true;

        long current = System.currentTimeMillis();
        long expiry = appSettings.getExpiryMinutes() * 60 * 1000;

        return (current - last) > expiry;
    }

    private MediaList mediaList;
    public MediaList getMediaList() { return mediaList; }
    public void setMediaList(MediaList mediaList) { this.mediaList = mediaList; }

    private Map<String,StorageGroup> storageGroups;
    public Map<String,StorageGroup> getStorageGroups() { return storageGroups; }
    public void setStorageGroups(Map<String,StorageGroup> sgs) { this.storageGroups = sgs; }

    private Map<String,ChannelGroup> channelGroups;
    public Map<String,ChannelGroup> getChannelGroups() { return channelGroups; }
    public void setChannelGroups(Map<String,ChannelGroup> chgroups) { this.channelGroups = chgroups; }

    private SearchResults searchResults;
    public SearchResults getSearchResults() { return searchResults; }
    public void setSearchResults(SearchResults results) { this.searchResults = results; }

    public MediaList readMediaList(MediaType mediaType) throws IOException, JSONException, ParseException {
        Map<String,StorageGroup> storageGroups = readStorageGroups();
        if (storageGroups != null) {
            File cacheDir = appContext.getCacheDir();
            File mediaListJsonFile = new File(cacheDir.getPath() + "/" + MEDIA_LIST_JSON_FILE);
            if (mediaListJsonFile.exists()) {
                AppSettings appSettings = new AppSettings(appContext);
                String mediaListJson = new String(readFile(mediaListJsonFile));
                MediaListParser mediaListParser = appSettings.getMediaListParser(mediaListJson);
                mediaList = mediaListParser.parseMediaList(mediaType, storageGroups);
            }
        }
        return mediaList;
    }

    public Map<String,StorageGroup> readStorageGroups() throws IOException, JSONException, ParseException {
        File cacheDir = appContext.getCacheDir();
        File storageGroupsJsonFile = new File(cacheDir.getPath() + "/" + STORAGE_GROUPS_JSON_FILE);
        if (storageGroupsJsonFile.exists()) {
            AppSettings appSettings = new AppSettings(appContext);
            String storageGroupsJson = new String(readFile(storageGroupsJsonFile));
            MythTvParser storageGroupParser = new MythTvParser(appSettings, storageGroupsJson);
            storageGroups = storageGroupParser.parseStorageGroups();
        }
        return storageGroups;
    }

    public Map<String,ChannelGroup> readChannelGroups() throws IOException, JSONException, ParseException {
        File cacheDir = appContext.getCacheDir();
        File channelGroupsJsonFile = new File(cacheDir.getPath() + "/" + CHANNEL_GROUPS_JSON_FILE);
        if (channelGroupsJsonFile.exists()) {
            AppSettings appSettings = new AppSettings(appContext);
            String channelGroupsJson = new String(readFile(channelGroupsJsonFile));
            MythTvParser channelGroupParser = new MythTvParser(appSettings, channelGroupsJson);
            channelGroups = channelGroupParser.parseChannelGroups();
        }
        return channelGroups;
    }

    public void clearChannelGroups() {
        channelGroups = null;
        File cacheDir = appContext.getCacheDir();
        File channelGroupsJsonFile = new File(cacheDir.getPath() + "/" + CHANNEL_GROUPS_JSON_FILE);
        if (channelGroupsJsonFile.exists())
            channelGroupsJsonFile.delete();
    }

    public void writeMediaList(String json) throws IOException, JSONException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + MEDIA_LIST_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    public void writeStorageGroups(String json) throws IOException, JSONException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + STORAGE_GROUPS_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    public void writeChannelGroups(String json) throws IOException, JSONException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + CHANNEL_GROUPS_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    private Map<MediaType,List<Item>> queues = new HashMap<MediaType, List<Item>>();

    public List<Item> getQueue(MediaType type) {
        return queues.get(type);
    }

    public void setQueue(MediaType type, List<Item> queue) {
        queues.put(type, queue);
    }

    public String getQueueJson(MediaType type) throws JSONException {
        List<Item> itemsList = getQueue(type);
        if (itemsList == null)
            return null;
        JSONObject jsonObj = new JSONObject();
        JSONArray items = new JSONArray();
        for (Item item : itemsList) {
            JSONObject w = new JSONObject();
            w.put("path", item.getPath());
            w.put("title", item.getTitle());
            w.put("format", item.getFormat());
            items.put(w);
        }
        jsonObj.put(type.toString(), items);

        return jsonObj.toString(2);
    }

    public List<Item> readQueue(MediaType type) throws IOException, JSONException, ParseException {
        File cacheDir = appContext.getCacheDir();
        File queueFile = new File(cacheDir.getPath() + "/" + type + QUEUE_FILE_SUFFIX);
        if (queueFile.exists()) {
            String queueJson = new String(readFile(queueFile));
            MythlingParser parser = new MythlingParser(new AppSettings(appContext), queueJson);
            List<Item> queue = parser.parseQueue(type, storageGroups);
            queues.put(type, queue);
        }
        return queues.get(type);
    }

    public void writeQueue(MediaType type, String json) throws IOException, JSONException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + type + QUEUE_FILE_SUFFIX);
        writeFile(jsonFile, json.getBytes());
    }

    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        } finally {
            if (fis != null)
                fis.close();
        }
    }

    private void writeFile(File file, byte[] contents) throws IOException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(contents);
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    private static int bitmapCacheMem = (int) ((Runtime.getRuntime().maxMemory() / 1024) / 4); // one quarter of total (kb)
    private static LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(bitmapCacheMem) {
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;  // kb taken up by this entry
        }
    };

    public Bitmap getImageBitMap(String path) {
        Bitmap bitmap = bitmapCache.get(path);
        if (bitmap == null) {
            bitmap = readImageBitmap(path);
            if (bitmap != null)
                bitmapCache.put(path, bitmap);
        }
        return bitmap;
    }

    public Bitmap readImageBitmap(String path) {
        File cacheDir = appContext.getCacheDir();
        File imageFile = new File(cacheDir.getPath() + "/" + path);
        if (imageFile.exists()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "reading image from file: " + imageFile);
            return BitmapFactory.decodeFile(imageFile.getPath());
        } else {
            return null;
        }
    }

    public void writeImage(String path, byte[] bytes) throws IOException {
        File cacheDir = appContext.getCacheDir();
        String dirPath = path.substring(0, path.lastIndexOf('/'));
        File dir = new File(cacheDir + "/" + dirPath);
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException(appContext.getString(R.string.unable_to_create_directories_) + dir);
        File imageFile = new File(cacheDir.getPath() + "/" + path);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "writing image to file: " + imageFile);
        writeFile(imageFile, bytes);
    }
}
