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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.WindowManager;
import io.oakesville.media.ChannelGroup;
import io.oakesville.media.Cut;
import io.oakesville.media.Download;
import io.oakesville.media.Item;
import io.oakesville.media.MediaSettings.MediaType;
import io.oakesville.media.StorageGroup;

public class AppData {
    private static final String TAG = AppData.class.getSimpleName();

    private static final String MEDIA_LIST_JSON_FILE = "mediaList.json";
    private static final String STORAGE_GROUPS_JSON_FILE = "storageGroups.json";
    private static final String CHANNEL_GROUPS_JSON_FILE = "channelGroups.json";
    private static final String DOWNLOADS_JSON_FILE = "downloadedItems.json";

    private final Context appContext;
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

    private final Map<String,Download> downloads = new HashMap<>();

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
                mediaList.setDownloads(getDownloads());
            }
        }
        return mediaList;
    }

    private Map<String,StorageGroup> readStorageGroups() throws IOException, JSONException {
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

    public Map<String,ChannelGroup> readChannelGroups() throws IOException, JSONException {
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

    public void writeMediaList(String json) throws IOException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + MEDIA_LIST_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    public void writeStorageGroups(String json) throws IOException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + STORAGE_GROUPS_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    public void writeChannelGroups(String json) throws IOException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + CHANNEL_GROUPS_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    private void readDownloads() throws IOException, JSONException, ParseException {
        downloads.clear();
        File cacheDir = appContext.getCacheDir();
        File downloadsJsonFile = new File(cacheDir.getPath() + "/" + DOWNLOADS_JSON_FILE);
        if (downloadsJsonFile.exists()) {
            String downloadsJson = new String(readFile(downloadsJsonFile));
            JSONArray downloadsArr = new JSONArray(downloadsJson);
            for (int i = 0; i < downloadsArr.length(); i++) {
                JSONObject downloadObj = downloadsArr.getJSONObject(i);
                Download download = new Download(downloadObj);
                downloads.put(download.getItemId(), download);
            }
        }
    }

    public Map<String,Download> getDownloads() throws IOException, JSONException, ParseException {
        readDownloads();
        DownloadManager dm = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        assert dm != null;
        Map<String,Download> filtered = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        long lastWeek = cal.getTimeInMillis();
        List<String> itemsToRemove = null;
        for (String itemId : downloads.keySet()) {
            Download download = downloads.get(itemId);
            boolean found = false;
            if (download.getDownloadId() == download.getPath().hashCode()) {
                // it's one of ours
                found = new File(download.getPath()).isFile();
            }
            else {
                if (dm.getUriForDownloadedFile(download.getDownloadId()) != null) {
                    // the downloaded file exists
                    found = true;
                }
            }
            if (found) {
                filtered.put(itemId, download);
            }
            else if (lastWeek > download.getStarted().getTime()) { // remove missing items older than a week
                if (itemsToRemove == null)
                    itemsToRemove = new ArrayList<>();
                itemsToRemove.add(download.getItemId());
            }
        }
        if (itemsToRemove != null) {
            for (String itemToRemove : itemsToRemove)
                downloads.remove(itemToRemove);
            persistDownloads();
        }

        // append downloads that bypassed DownloadManager

        return filtered;
    }

    public Download getDownload(String path) throws IOException, JSONException, ParseException {
        for (Download download : getDownloads().values()) {
            if (download.getPath() != null && path.endsWith(download.getPath()))
                return download;
        }
        return null;
    }

    private void writeDownloads(String json) throws IOException {
        File cacheDir = appContext.getCacheDir();
        File jsonFile = new File(cacheDir.getPath() + "/" + DOWNLOADS_JSON_FILE);
        writeFile(jsonFile, json.getBytes());
    }

    public void addDownload(Download download) throws IOException, JSONException {
        downloads.put(download.getItemId(), download);
        persistDownloads();
    }

    public boolean addDownloadCutList(String itemId, ArrayList<Cut> cutList) throws IOException, JSONException {
        Download download = downloads.get(itemId);
        if (download != null) {
            download.setCutList(cutList);
            persistDownloads();
            return true;
        }
        return false;
    }

    private void persistDownloads() throws IOException, JSONException {
        JSONArray downloadsArr = new JSONArray();
        for (Download download : downloads.values())
            downloadsArr.put(download.toJson());
        writeDownloads(downloadsArr.toString());
    }

    public Long removeDownload(String itemId) throws IOException, JSONException {
        Download download = downloads.remove(itemId);
        persistDownloads();
        return download.getDownloadId();
    }

    private final Map<MediaType,List<Item>> queues = new HashMap<>();

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

    private static final int bitmapCacheMem = (int) ((Runtime.getRuntime().maxMemory() / 1024) / 4); // one quarter of total (kb)
    private static final LruCache<String, Bitmap> bitmapCache = new LruCache<String, Bitmap>(bitmapCacheMem) {
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
            // avoid out-of-memory errors by checking image dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getPath(), options);
            // calculate sample size (based on screen size)
            options.inSampleSize = calculateImageSampleSize(options.outWidth, options.outHeight, getScreenSize().x, getScreenSize().y);
            options.inJustDecodeBounds = false;
            // TODO: save resampled image to save disk space
            return BitmapFactory.decodeFile(imageFile.getPath(), options);
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

    private static Point screenSize;
    private Point getScreenSize() {
        if (screenSize == null) {
            WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            assert wm != null;
            Display display = wm.getDefaultDisplay();
            screenSize = new Point();
            display.getSize(screenSize);
        }
        return screenSize;
    }

    private int calculateImageSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // get the largest sample size that's a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / sampleSize) > reqHeight
                    && (halfWidth / sampleSize) > reqWidth) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }
}
