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
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.util.JsonParser;

public class AppData
{
  public static final int BUILD_ID = 7;
  private static final String TAG = AppData.class.getSimpleName();
  
  private Context appContext;
  public AppData(Context appContext)
  {
    this.appContext = appContext;
  }
  
  public boolean isExpired()
  {
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
  public void setMediaList(MediaList wl) { this.mediaList = wl; }
  
  private SearchResults searchResults;
  public SearchResults getSearchResults() { return searchResults; }
  public void setSearchResults(SearchResults results) { this.searchResults = results; }
  
  private static final String MEDIA_LIST_JSON_FILE = "mediaList.json";
  private static final String QUEUE_FILE_SUFFIX = "Queue.json";
  public MediaList readMediaList() throws IOException, JSONException, ParseException
  {
    File cacheDir = appContext.getCacheDir();
    File appDataJsonFile = new File(cacheDir.getPath() + "/" + MEDIA_LIST_JSON_FILE);
    if (appDataJsonFile.exists())
    {
      String mediaListJson = new String(readFile(appDataJsonFile));
      JsonParser parser = new JsonParser(mediaListJson);
      mediaList = parser.parseMediaList(new AppSettings(appContext).isMythlingMediaServices());
    }
    return mediaList;
  }
  
  public void writeMediaList(String json) throws IOException, JSONException
  {
    File cacheDir = appContext.getCacheDir();
    File appDataJsonFile = new File(cacheDir.getPath() + "/" + MEDIA_LIST_JSON_FILE);
    writeFile(appDataJsonFile, json.getBytes());
  }
  
  private Map<MediaType,List<Item>> queues = new HashMap<MediaType,List<Item>>();
  public List<Item> getQueue(MediaType type) { return queues.get(type); }
  public void setQueue(MediaType type, List<Item> queue) { queues.put(type, queue); }
  
  public String getQueueJson(MediaType type) throws JSONException
  {
    List<Item> itemsList = getQueue(type);
    if (itemsList == null)
      return null;
    JSONObject jsonObj = new JSONObject();
    JSONArray items = new JSONArray();
    for (Item item : itemsList)
    {
      JSONObject w = new JSONObject();
      w.put("path", item.getPath());
      w.put("title", item.getTitle());
      w.put("format", item.getFormat());
      items.put(w);
    }
    jsonObj.put(type.toString(), items);
    
    return jsonObj.toString(2);
  }
  
  public List<Item> readQueue(MediaType type) throws IOException, JSONException
  {
    File cacheDir = appContext.getCacheDir();
    File queueFile = new File(cacheDir.getPath() + "/" + type + QUEUE_FILE_SUFFIX);
    if (queueFile.exists())
    {
      String queueJson = new String(readFile(queueFile));
      JsonParser parser = new JsonParser(queueJson);
      List<Item> queue = parser.parseQueue(type);
      queues.put(type, queue);
    }
    return queues.get(type);
  }
  
  public void writeQueue(MediaType type, String json) throws IOException, JSONException
  {
    File cacheDir = appContext.getCacheDir();
    File jsonFile = new File(cacheDir.getPath() + "/" + type + QUEUE_FILE_SUFFIX);
    writeFile(jsonFile, json.getBytes());
  }
  
  private byte[] readFile(File file) throws IOException
  {
    FileInputStream fis = null;
    try
    {
      fis = new FileInputStream(file);
      byte[] bytes = new byte[(int)file.length()];
      fis.read(bytes);
      return bytes;
    } 
    finally
    {
      if (fis != null)
        fis.close();
    }    
  }
  
  private void writeFile(File file, byte[] contents) throws IOException
  {
    
    FileOutputStream fos = null;
    try
    {
      fos = new FileOutputStream(file);
      fos.write(contents);
    } 
    finally
    {
      if (fos != null)
        fos.close();
    }
  }
  
  private static int bitmapCacheMem = (int) ((Runtime.getRuntime().maxMemory() / 1024) / 4); // one quarter of total (kb)
  private static LruCache<String,Bitmap> bitmapCache = new LruCache<String,Bitmap>(bitmapCacheMem)
  {
    protected int sizeOf(String key, Bitmap value)
    {
      return value.getByteCount() / 1024;  // kb taken up by this entry
    }
  };

  public Bitmap getImageBitMap(String path)
  {
    Bitmap bitmap = (Bitmap) bitmapCache.get(path);
    if (bitmap == null)
    {
      bitmap = readImageBitmap(path);
      if (bitmap != null)
        bitmapCache.put(path, bitmap);
    }
    return bitmap;
  }
  
  public Bitmap readImageBitmap(String path)
  {
    File cacheDir = appContext.getCacheDir();
    File imageFile = new File(cacheDir.getPath() + "/" + path);
    if (imageFile.exists())
    {
      if (BuildConfig.DEBUG)
        Log.d(TAG, "reading image from file: " + imageFile);
      return BitmapFactory.decodeFile(imageFile.getPath());
    }
    else
    {
      return null;
    }
  }
  
  public void writeImage(String path, byte[] bytes) throws IOException
  {
    File cacheDir = appContext.getCacheDir();
    String dirPath = path.substring(0, path.lastIndexOf('/'));
    File dir = new File(cacheDir + "/" + dirPath);
    if (!dir.exists() && !dir.mkdirs())
      throw new IOException("Unable to create directories: " + dir);
    File imageFile = new File(cacheDir.getPath() + "/" + path);
    if (BuildConfig.DEBUG)
      Log.d(TAG, "writing image to file: " + imageFile);
    writeFile(imageFile, bytes);
  }
}
