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
package com.oakesville.mythling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Item;
import com.oakesville.mythling.app.LiveStreamInfo;
import com.oakesville.mythling.app.MediaList;
import com.oakesville.mythling.app.MediaSettings;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.MediaSettings.SortType;
import com.oakesville.mythling.app.MediaSettings.ViewType;
import com.oakesville.mythling.app.TunerInUseException;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.FrontendPlayer;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.JsonParser;
import com.oakesville.mythling.util.Recorder;
import com.oakesville.mythling.util.Transcoder;

/**
 * Base class for the two different ways to view collections of MythTV media.
 */
public abstract class MediaActivity extends Activity
{
  private static final String TAG = MediaActivity.class.getSimpleName();
  
  protected MediaList mediaList;

  private static AppData appData;
  public static AppData getAppData() { return appData; }
  public static void setAppData(AppData data) { appData = data; }
  
  private AppSettings appSettings;
  public AppSettings getAppSettings() { return appSettings; }
  
  private MediaType mediaType;
  protected MediaType getMediaType() { return mediaType; }
  protected void setMediaType(MediaType mt) { this.mediaType = mt; }
  
  private MenuItem mediaMenuItem;
  private MenuItem searchMenuItem;
  private MenuItem viewMenuItem;
  private MenuItem sortMenuItem;
  private MenuItem musicMenuItem;
  
  private static MediaPlayer mediaPlayer;
  
  private ProgressBar progressBar;

  private ProgressDialog countdownDialog;
  private int count;
  private Timer timer;
  
  public abstract void refresh() throws BadSettingsException;
  public abstract ListView getListView();

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);    
    appSettings = new AppSettings(getApplicationContext());
  }
  
  protected ProgressBar createProgressBar()
  {
    progressBar = (ProgressBar)findViewById(R.id.progress);
    progressBar.setVisibility(View.GONE);
    progressBar.setIndeterminate(true);
    progressBar.setScaleX(0.10f);
    progressBar.setScaleY(0.10f);
    return progressBar;
  }
  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    MediaSettings mediaSettings = appSettings.getMediaSettings();

    mediaMenuItem = menu.findItem(R.id.menu_media);
    if (mediaMenuItem != null)
    {
      mediaMenuItem.setTitle(mediaSettings.getTitle());
      if (mediaSettings.isMusic())
        mediaMenuItem.getSubMenu().findItem(R.id.media_music).setChecked(true);
      else if (mediaSettings.isTv())
        mediaMenuItem.getSubMenu().findItem(R.id.media_tv).setChecked(true);
      else if (mediaSettings.isMovies())
        mediaMenuItem.getSubMenu().findItem(R.id.media_movies).setChecked(true);
      else if (mediaSettings.isVideos())
        mediaMenuItem.getSubMenu().findItem(R.id.media_videos).setChecked(true);
      else
        mediaMenuItem.getSubMenu().findItem(R.id.media_recordings).setChecked(true);
      
      musicMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_music);
      showMusicMenuItem(supportsMusic());
    }

    searchMenuItem = menu.findItem(R.id.menu_search);
    showSearchMenu(supportsSearch());
    
    sortMenuItem = menu.findItem(R.id.menu_sort);
    showSortMenu(supportsSort() && MediaType.movies.equals(getMediaType()));

    viewMenuItem = menu.findItem(R.id.menu_view);
    showViewMenu(supportsViewSelection() && MediaType.movies.equals(getMediaType()));

    return super.onPrepareOptionsMenu(menu);
  }
  
  protected void showSearchMenu(boolean show)
  {
    if (searchMenuItem != null)
    {
      searchMenuItem.setEnabled(show);
      searchMenuItem.setVisible(show);
    }
  }  

  protected void showMusicMenuItem(boolean show)
  {
    if (musicMenuItem != null)
    {
      musicMenuItem.setEnabled(show);
      musicMenuItem.setVisible(show);
      if (appSettings.getMediaSettings().isMusic())
      {
        appSettings.getMediaSettings().setType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));
        
      }
    }
  }
  
  protected void showViewMenu(boolean show)
  {
    if (viewMenuItem != null)
    {
      if (show)
      {
        viewMenuItem.setTitle("");
        viewMenuItem.setIcon(appSettings.getMediaSettings().getViewIcon());
        if (appSettings.getMediaSettings().getViewType() == ViewType.pager)
          viewMenuItem.getSubMenu().findItem(R.id.view_pager).setChecked(true);
        else
          viewMenuItem.getSubMenu().findItem(R.id.view_list).setChecked(true);
      }
      viewMenuItem.setEnabled(show);
      viewMenuItem.setVisible(show);
    }
  }
  
  protected void showSortMenu(boolean show)
  {
    if (sortMenuItem != null)
    {
      if (show)
      {
        sortMenuItem.setTitle(appSettings.getMediaSettings().getSortTypeTitle());
        if (appSettings.getMediaSettings().getSortType() == SortType.byYear)
          sortMenuItem.getSubMenu().findItem(R.id.sort_byYear).setChecked(true);
        else if (appSettings.getMediaSettings().getSortType() == SortType.byRating)
          sortMenuItem.getSubMenu().findItem(R.id.sort_byRating).setChecked(true);
        else
          sortMenuItem.getSubMenu().findItem(R.id.sort_byTitle).setChecked(true);
      }
      sortMenuItem.setEnabled(show);
      sortMenuItem.setVisible(show);
    }
  }  
  
  protected boolean supportsViewSelection()
  {
    return false;
  }
  
  protected boolean supportsSort()
  {
    return false;
  }
  
  protected boolean supportsSearch()
  {
    return getAppSettings().isMythlingMediaServices();
  }
  
  protected boolean supportsMusic()
  {
    return getAppSettings().isMythlingMediaServices();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      NavUtils.navigateUpFromSameTask(this);
      return true;
    }
    try
    {
      if (sortMenuItem != null)
        sortMenuItem.setTitle(appSettings.getMediaSettings().getSortTypeTitle());
      if (viewMenuItem != null)
        viewMenuItem.setIcon(appSettings.getMediaSettings().getViewIcon());
      
      if (item.getItemId() == R.id.media_music)
      {
        appSettings.setMediaType(MediaType.music);
        item.setChecked(true);
        mediaMenuItem.setTitle(appSettings.getMediaSettings().getTitle());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_videos)
      {
        appSettings.setMediaType(MediaType.videos);
        item.setChecked(true);
        mediaMenuItem.setTitle(appSettings.getMediaSettings().getTitle());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_recordings)
      {
        appSettings.setMediaType(MediaType.recordings);
        item.setChecked(true);
        mediaMenuItem.setTitle(appSettings.getMediaSettings().getTitle());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_tv)
      {
        appSettings.setMediaType(MediaType.tv);
        item.setChecked(true);
        mediaMenuItem.setTitle(appSettings.getMediaSettings().getTitle());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_movies)
      {
        appSettings.setMediaType(MediaType.movies);
        item.setChecked(true);
        mediaMenuItem.setTitle(appSettings.getMediaSettings().getTitle());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.sort_byTitle)
      {
        appSettings.setSortType(SortType.byTitle);
        item.setChecked(true);
        sortMenuItem.setTitle(R.string.menu_byTitle);
        sort();
        return true;
      }
      else if (item.getItemId() == R.id.sort_byYear)
      {
        appSettings.setSortType(SortType.byYear);
        item.setChecked(true);
        sortMenuItem.setTitle(R.string.menu_byYear);
        sort();
        return true;
      }
      else if (item.getItemId() == R.id.sort_byRating)
      {
        appSettings.setSortType(SortType.byRating);
        item.setChecked(true);
        sortMenuItem.setTitle(R.string.menu_byRating);
        sort();
        return true;
      }
      else if (item.getItemId() == R.id.view_list)
      {
        appSettings.setViewType(ViewType.list);
        item.setChecked(true);
        viewMenuItem.setIcon(R.drawable.ic_menu_list);
        goListView();
        return true;
      }
      else if (item.getItemId() == R.id.view_pager)
      {
        appSettings.setViewType(ViewType.pager);
        item.setChecked(true);
        viewMenuItem.setIcon(R.drawable.ic_menu_detail);
        goPagerView();
        return true;
      }
      else if (item.getItemId() == R.id.menu_refresh)
      {
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.menu_settings)
      {
        startActivity(new Intent(this, PrefsActivity.class));
        return true;
      }
      else if (item.getItemId() == R.id.menu_search)
      {
        return onSearchRequested();
      }
    }
    catch (BadSettingsException ex)
    {
      stopProgress();
      Toast.makeText(getApplicationContext(), "Bad or missing setting:\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      stopProgress();
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }

    return super.onOptionsItemSelected(item);
  }
  
  protected void playItem(final Item item)
  {
    try
    {
      AppSettings appSettings = getAppSettings();
      
      if (appSettings.isDevicePlayback())
      {
        if (getListView() != null)  // TODO what about pager activity?
        {
          String msg = (item.isMusic() ? "Playing: '" : "Loading: '") + item.getTitle() + "'";
          ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new String[]{msg});
          getListView().setAdapter(adapter);
        }
        
        startProgress();
        
        stopMediaPlayer(); // music
        
        if (item.isMusic())
        {
          if (mediaPlayer == null)
          {
            mediaPlayer = new MediaPlayer();
          }
          mediaPlayer.setOnCompletionListener(new OnCompletionListener()
          {
            public void onCompletion(MediaPlayer mp)
            {
              onResume();
            }
          });
          String musicUrl = appSettings.getMythTvServicesBaseUrl() + "/Content/GetMusic?Id=" + item.getId();
          Map<String,String> headers = new HashMap<String,String>();
          String credentials = Base64.encodeToString((appSettings.getMythTvServicesUser() + ":" + appSettings.getMythTvServicesPassword()).getBytes(), Base64.DEFAULT);
          headers.put("Authorization", "Basic " + credentials);
          mediaPlayer.setDataSource(appSettings.getAppContext(), Uri.parse(musicUrl), headers);
          // TODO async?
          mediaPlayer.prepare();
          mediaPlayer.start();
          stopProgress();
        }
        else
        {
          if (item.isTv() || item.isRecording() || item.isMovie())
          {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(item.getTitle())
            .setMessage(item.getShowInfo())
            .setPositiveButton("Watch", new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                try
                {
                  if (item.isTv())
                    new StreamTvTask(item).execute(getAppSettings().getMythTvServicesBaseUrl());
                  else
                    new StreamVideoTask(item).execute(getAppSettings().getMythTvServicesBaseUrl());
                }
                catch (MalformedURLException ex)
                {
                  stopProgress();
                  if (BuildConfig.DEBUG)
                    Log.e(TAG, ex.getMessage(), ex);
                  Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
                }
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                stopProgress();
                onResume();
              }
            })
            .show();
          }
          else
          {
            new StreamVideoTask(item).execute(appSettings.getMythTvServicesBaseUrl());
          }
        }
      }
      else
      {
        final FrontendPlayer player = new FrontendPlayer(appSettings, item);
        if (player.checkIsPlaying())
        {
          new AlertDialog.Builder(this)
          .setIcon(android.R.drawable.ic_dialog_alert)
          .setTitle("Interrupt")
          .setMessage("Stop current playback?")
          .setPositiveButton("Yes", new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface dialog, int which)
            {
              startPlayback(item, player);
            }
          })
          .setNegativeButton("No", null)
          .show();
        }
        else
        {
          startPlayback(item, player);
        }
      }
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }
  }
  
  private void stopMediaPlayer()
  {
    if (mediaPlayer != null)
    {
      if (mediaPlayer.isPlaying())
        mediaPlayer.stop();
      
      mediaPlayer.reset();
    }
  }
  
  protected void goPagerView()
  {
    // default does nothing
  }
  
  protected void goListView()
  {
    // default does nothing
  }
  
  protected void sort()
  {
    // default does nothing
  }
  
  private void startPlayback(Item item, final FrontendPlayer player)
  {
    if (item.isRecording())
    {
      new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle(item.getTitle())
      .setMessage(item.getShowInfo())
      .setPositiveButton("Play", new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          player.play();
        }
      })
      .setNegativeButton("Cancel", null)
      .show();
    }
    else
    {
      try
      {
        player.play();
        
        // reset progress
        count = 0;
        // prepare for a progress bar dialog
        countdownDialog = new ProgressDialog(this);
        countdownDialog.setCancelable(true);
        countdownDialog.setMessage("Playing " + getAppSettings().getMediaSettings().getLabel() + ": " + item);
        countdownDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        countdownDialog.setProgressPercentFormat(null);
        countdownDialog.setProgressNumberFormat(null);
        countdownDialog.setMax(10);
        countdownDialog.setCancelable(true);
        countdownDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface dialog, int which)
          {
            dialog.dismiss();
            stopTimer();
          }
        });
        countdownDialog.setButton(Dialog.BUTTON_NEGATIVE, "Stop", new DialogInterface.OnClickListener()
        {
          public void onClick(DialogInterface dialog, int which)
          {
            player.stop();
            dialog.dismiss();
            stopTimer();
          }
        });
        countdownDialog.setCanceledOnTouchOutside(true);
        countdownDialog.show();
        countdownDialog.setProgress(10);
    
        tick();
      }
      catch (Exception ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        stopTimer();
      }
    }
  }
  
  @Override
  protected void onStop()
  {
    if (countdownDialog != null && countdownDialog.isShowing())
      countdownDialog.dismiss();
    stopTimer();
    super.onStop();
  }
  

  private void stopTimer()
  {
    if (timer != null)
      timer.cancel();
    count = 0;
  }
  
  private void tick()
  {
    if (timer != null)
      timer.cancel();
    timer = new Timer();
    timer.schedule(new TimerTask()
    {
      public void run()
      {
        countdownDialog.setProgress(10 - count);
        if (count == 10)
        {
          countdownDialog.dismiss();
          stopTimer();
        }
        else
        {
          count++;
          tick();
        }
      }
    }, 1000);
  }
  
  protected void refreshMediaList()
  {
    try
    {
      new RefreshTask().execute(getAppSettings().getUrls(getAppSettings().getMediaListUrl()));
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
  
  protected void populate() throws IOException, JSONException, ParseException
  {
    // default does nothing
  }
  
  private class RefreshTask extends AsyncTask<URL,Integer,Long>
  {
    private String mediaListJson;

    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        HttpHelper downloader = getMediaListDownloader(urls);
        mediaListJson = new String(downloader.get());
        if (mediaListJson.startsWith("<"))
        {
          // just display html
          ex = new IOException(mediaListJson);
          return -1L;
        }
        mediaList = new JsonParser(mediaListJson).parseMediaList(getAppSettings().isMythlingMediaServices());
        if (!getAppSettings().isMythlingMediaServices())
        {
          downloader = getMediaListDownloader(getAppSettings().getUrls(new URL(getAppSettings().getMythTvServicesBaseUrl() + "/Myth/GetStorageGroupDirs")));
          String storageGroupsJson = new String(downloader.get());
          mediaList.setBasePath(new JsonParser(storageGroupsJson).parseStorageGroupDir(getAppSettings().getMediaSettings().getStorageGroup()));
        }
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
      stopProgress();
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
      }
      else
      {
        AppData appData = new AppData(getApplicationContext());
        appData.setMediaList(mediaList);
        setAppData(appData);
        setMediaType(appData.getMediaList().getMediaType());
        getAppSettings().setLastLoad(System.currentTimeMillis());
        
        try
        {
          appData.writeMediaList(mediaListJson);
          populate();
        }
        catch (Exception ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }
      }
    }    
  }
  
  private class StreamVideoTask extends AsyncTask<URL,Integer,Long>
  {
    private Item item;
    private LiveStreamInfo streamInfo;
    private Exception ex;
    
    public StreamVideoTask(Item item)
    {
      this.item = item;
    }
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        Transcoder transcoder = new Transcoder(getAppSettings());

        // TODO: do this retry for tv playback
        int ct = 0;
        int maxTries = 3;
        // empty relative url i think means myth has not started transcoding
        while ((streamInfo == null || streamInfo.getRelativeUrl().isEmpty()) && ct < maxTries )
        {
          transcoder.beginTranscode(item);
          streamInfo = transcoder.getStreamInfo();
          ct++;
          Thread.sleep(1000);
        }
        
        if (streamInfo == null || streamInfo.getRelativeUrl().isEmpty())
          throw new IOException("Transcoding does not seem to have started");
        
        transcoder.waitAvailable();
        
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
        stopProgress();
        if (ex != null)
          Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        onResume();
      }
      else
      {
        try
        {
          playLiveStream(streamInfo);
        }
        catch (Exception ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
          Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        }
      }
    }
  }
  
  protected class TranscodeVideoTask extends AsyncTask<URL,Integer,Long>
  {
    private Item item;
    private Exception ex;
    
    public TranscodeVideoTask(Item item)
    {
      this.item = item;
    }
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        Transcoder transcoder = new Transcoder(getAppSettings());
        transcoder.beginTranscode(item);
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
      stopProgress();
      if (result != 0L)
      {
        if (ex != null)
          Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        onResume();
      }
    }    
  }
  
  protected class StreamTvTask extends AsyncTask<URL,Integer,Long>
  {
    private Item item;
    private Item recordingToDelete;
    private LiveStreamInfo streamInfo;
    private Exception ex;
    
    public StreamTvTask(Item item)
    {
      this.item = item;
    }
    
    public StreamTvTask(Item item, Item recordingToDelete)
    {
      this.item = item;
      this.recordingToDelete = recordingToDelete;
    }
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        Recorder recorder = new Recorder(getAppSettings());
        if (recordingToDelete != null)
          recorder.deleteRecording(recordingToDelete);
        boolean recordAvail = recorder.scheduleRecording(item);
        
        if (!recordAvail)
          recorder.waitAvailable();
        
        Transcoder transcoder = new Transcoder(getAppSettings());
        boolean streamAvail = transcoder.beginTranscode(recorder.getRecording());
        
        streamInfo = transcoder.getStreamInfo();
        
        if (!streamAvail)
          transcoder.waitAvailable();
        
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
      stopProgress();
      if (result != 0L)
      {
        if (ex instanceof TunerInUseException)
        {
          final Item inProgressRecording = ((TunerInUseException)ex).getRecording();
          new AlertDialog.Builder(MediaActivity.this)
          .setIcon(android.R.drawable.ic_dialog_info)
          .setTitle("Recording Conflict")
          .setMessage("Tuner already in use recording:\n" + ex.getMessage() + "\nDelete this recording and proceed?")
          .setPositiveButton("Delete", new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface dialog, int which)
            {
              try
              {
                startProgress();
                new StreamTvTask(item, inProgressRecording).execute(getAppSettings().getMythTvServicesBaseUrl());
              }
              catch (MalformedURLException ex)
              {
                stopProgress();
                if (BuildConfig.DEBUG)
                  Log.e(TAG, ex.getMessage(), ex);
                Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
              }
            }
          })
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface dialog, int which)
            {
              stopProgress();
              onResume();
            }
          })
          .show();
          
        }
        else 
        {
          if (ex != null)
            Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
          onResume();
        }
      }
      else
      {
        try
        {
          playLiveStream(streamInfo);
        }
        catch (Exception ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
          Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
        }
      }
    }    
  }

  protected void playLiveStream(LiveStreamInfo streamInfo) throws IOException
  {
    String streamUrl = appSettings.getMythTvServicesBaseUrl() + streamInfo.getRelativeUrl();

    // avoid retrieving unnecessary audio-only streams
    int lastDot = streamUrl.lastIndexOf('.');
    streamUrl = streamUrl.substring(0, lastDot) + ".av" + streamUrl.substring(lastDot);
    
    stopProgress();
    if (appSettings.isExternalPlayer())
    {
      Intent toStart = new Intent(Intent.ACTION_VIEW);
      toStart.setDataAndType(Uri.parse(streamUrl), "video/*");
      startActivity(toStart);
    }
    else
    {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl), getApplicationContext(),  VideoActivity.class));
    }
  }
  
  protected HttpHelper getMediaListDownloader(URL[] urls)
  {
    HttpHelper downloader;
    if (getAppSettings().isMythlingMediaServices())
    {
      downloader = new HttpHelper(urls, getAppSettings().getMythlingServicesAuthType(), getAppSettings().getPrefs());
      downloader.setCredentials(getAppSettings().getMythlingServicesUser(), getAppSettings().getMythlingServicesPassword());
    }
    else
    {
      downloader = new HttpHelper(urls, getAppSettings().getMythTvServicesAuthType(), getAppSettings().getPrefs());
      downloader.setCredentials(getAppSettings().getMythTvServicesUser(), getAppSettings().getMythTvServicesPassword());
    }
    return downloader;
  }
  
  protected void startProgress()
  {
    progressBar.setScaleX(0.50f);
    progressBar.setScaleY(0.50f);
    progressBar.setVisibility(View.VISIBLE);
  }
  
  protected void stopProgress()
  {
    progressBar.setVisibility(View.GONE);
  }
  
  
}