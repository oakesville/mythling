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
package com.oakesville.mythling.util;

import java.io.IOException;
import java.net.URL;

import org.json.JSONException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.util.HttpHelper.AuthType;

public class ServiceFrontendPlayer implements FrontendPlayer
{
  private static final String TAG = ServiceFrontendPlayer.class.getSimpleName();
  
  private AppSettings appSettings;
  private Item item;
  
  private String state;
  
  public ServiceFrontendPlayer(AppSettings appSettings, Item item)
  {
    this.appSettings = appSettings;
    this.item = item;
  }

  public boolean checkIsPlaying() throws IOException, JSONException
  {
    int timeout = 5000;  // TODO: pref
    
    state = null;
    new StatusTask().execute();
    while (state == null && timeout > 0)
    {
      try
      {
        Thread.sleep(100);
        timeout -= 100;
      }
      catch (InterruptedException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
      }
    }
    if (state == null)
      throw new IOException("Unable to connect to mythfrontend: " + appSettings.getFrontendServiceBaseUrl());
    
    return !state.equals("idle");
  }

  public void play()
  {
    new PlayItemTask().execute();
  }

  public void stop()
  {
    new StopTask().execute();
  }
  
  private class StatusTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        URL url = new URL(appSettings.getFrontendServiceBaseUrl() + "/Frontend/GetStatus");        
        HttpHelper downloader = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
        String frontendStatusJson = new String(downloader.get(), "UTF-8");
        state = new MythTvParser(frontendStatusJson, appSettings).parseFrontendStatus("state");
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error checking status: " + ex.toString(), Toast.LENGTH_LONG).show();
      }
    }
  }
  
  private class PlayItemTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        
        URL url = appSettings.getFrontendServiceBaseUrl();
        if (item.isRecording())
          url = new URL(url + "/Frontend/PlayRecording?ChanId" + ((Recording)item).getChanIdStartTimeParams());
        else if (item.isLiveTv())
          throw new UnsupportedOperationException("LiveTV not supported by ServiceFrontendPlayer");
        else if (item.isMusic())
          throw new UnsupportedOperationException("Music playback not supported by ServiceFrontendPlayer");
        else
          url = new URL(url + "/Frontend/PlayVideo?Id" + item.getId());
              
        HttpHelper poster = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
        String frontendStatusJson = new String(poster.post(), "UTF-8");
        state = new MythTvParser(frontendStatusJson, appSettings).parseFrontendStatus("state");
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error checking status: " + ex.toString(), Toast.LENGTH_LONG).show();
      }
    }
  }

  private class StopTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        
        URL url = new URL(appSettings.getFrontendServiceBaseUrl() + "/Frontend/SendAction?Action=STOPPLAYBACK");              
        HttpHelper poster = new HttpHelper(new URL[]{url}, AuthType.None.toString(), appSettings.getPrefs());
        String frontendStatusJson = new String(poster.get(), "UTF-8");
        state = new MythTvParser(frontendStatusJson, appSettings).parseFrontendStatus("state");
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error checking status: " + ex.toString(), Toast.LENGTH_LONG).show();
      }
    }
  }
  
}
