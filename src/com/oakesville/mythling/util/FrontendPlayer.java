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
package com.oakesville.mythling.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Work;
import com.oakesville.mythling.BuildConfig;

public class FrontendPlayer
{
  public static final String TAG = FrontendPlayer.class.getSimpleName();
  
  private AppSettings appSettings;
  private Work work;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String status;
  
  public FrontendPlayer(AppSettings settings, Work work)
  {
    this.appSettings = settings;
    this.work = work;
  }
  
  public boolean checkIsPlaying() throws IOException
  {
    int timeout = 5000;
    
    status = null;
    new StatusTask().execute();
    while (status == null && timeout > 0)
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
    if (status == null)
      throw new IOException("Unable to connect to mythfrontend: " + appSettings.getFrontendIp() + ":" + appSettings.getFrontendControlPort());
      
    return status.startsWith("Playback");
  }
  
  public void play()
  {
    new PlayWorkTask().execute();
  }
  
  public void stop()
  {
    new StopTask().execute();
  }
  
  private class PlayWorkTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        open();
        run("play stop");
        run("play music stop");
        String filepath = work.getFilePath();
        if (work.isMusic())
          run("play music file " + filepath);
        else if (work.isRecording())
          run("play program " + work.getChannelId() + " " + work.getStartTimeParam());
        else
          run("play file " + filepath);
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
      finally
      {
        try
        {
          close();
        }
        catch (IOException ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error playing file '" + work.getFileName() + "': " + ex.toString(), Toast.LENGTH_LONG).show();
      }
      else
      {
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
        open();
        if (work.isMusic())
          run("play music stop");
        else
          run("play stop\n");
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
      finally
      {
        try
        {
          close();
        }
        catch (IOException ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error stopping playback: " + ex.toString(), Toast.LENGTH_LONG).show();
      }
      else
      {
      }
    }
  }
  
  private class StatusTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        open();
        status = run("query location");
        return 0L;
      }
      catch (Exception ex)
      {
        this.ex = ex;
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        return -1L;
      }
      finally
      {
        try
        {
          close();
        }
        catch (IOException ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }
      }
    }

    protected void onPostExecute(Long result)
    {
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(appSettings.getAppContext(), "Error checking status: " + ex.toString(), Toast.LENGTH_LONG).show();
      }
      else
      {
      }
    }
  }
  
  private String run(String command) throws IOException
  {
    out.println(command);
    out.flush();
    String line = null;
    while ((line = in.readLine()) != null)
    {
      if (line.startsWith("#"))
        return line.substring(2);
    }
    return null;
  }
  
  private void open() throws IOException
  {
    String frontendIp = appSettings.getFrontendIp();
    InetAddress serverAddr = InetAddress.getByName(frontendIp);
    int frontendPort = appSettings.getFrontendControlPort();
    socket = new Socket(serverAddr, frontendPort);
    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    in =  new BufferedReader(new InputStreamReader(socket.getInputStream()));    
  }
  
  private void close() throws IOException
  {
    if (out != null)
      out.close();
    if (in != null)
      in.close();
    if (socket != null)
      socket.close();
  }
}
