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
package com.oakesville.mythling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
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
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.LiveStreamInfo;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.Movie;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TunerInUseException;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.FrontendPlayer;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.Recorder;
import com.oakesville.mythling.util.ServiceFrontendPlayer;
import com.oakesville.mythling.util.SocketFrontendPlayer;
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
  
  public String getCharSet() { return "UTF-8"; }
  
  private MenuItem mediaMenuItem;
  protected MenuItem getMediaMenuItem() { return mediaMenuItem; }
  private MenuItem searchMenuItem;
  private MenuItem viewMenuItem;
  private MenuItem sortMenuItem;
  private MenuItem moviesMenuItem;
  private MenuItem tvSeriesMenuItem;
  private MenuItem musicMenuItem;
  private MenuItem mythwebMenuItem;
  
  private static MediaPlayer mediaPlayer;
  
  private ProgressBar progressBar;

  private ProgressDialog countdownDialog;
  private int count;
  private Timer timer;
  
  protected boolean modeSwitch;
  
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
    progressBar.setScaleX(0.20f);
    progressBar.setScaleY(0.20f);
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
      else if (mediaSettings.isLiveTv())
        mediaMenuItem.getSubMenu().findItem(R.id.media_tv).setChecked(true);
      else if (mediaSettings.isMovies())
        mediaMenuItem.getSubMenu().findItem(R.id.media_movies).setChecked(true);
      else if (mediaSettings.isTvSeries())
        mediaMenuItem.getSubMenu().findItem(R.id.media_tv_series).setChecked(true);
      else if (mediaSettings.isVideos())
        mediaMenuItem.getSubMenu().findItem(R.id.media_videos).setChecked(true);
      else
        mediaMenuItem.getSubMenu().findItem(R.id.media_recordings).setChecked(true);
      
      moviesMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_movies);
      showMoviesMenuItem(supportsMovies());
      tvSeriesMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_tv_series);
      showTvSeriesMenuItem(supportsTvSeries());
      musicMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_music);
      showMusicMenuItem(supportsMusic());
    }

    searchMenuItem = menu.findItem(R.id.menu_search);
    showSearchMenu(supportsSearch());
    
    sortMenuItem = menu.findItem(R.id.menu_sort);
    showSortMenu(supportsSort());

    viewMenuItem = menu.findItem(R.id.menu_view);
    showViewMenu(supportsViewMenu());

    mythwebMenuItem = menu.findItem(R.id.menu_mythweb);
    showMythwebMenu(supportsMythwebMenu());
    
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

  protected void showMoviesMenuItem(boolean show)
  {
    if (moviesMenuItem != null)
    {
      moviesMenuItem.setEnabled(show);
      moviesMenuItem.setVisible(show);
    }
  }
  
  protected void showTvSeriesMenuItem(boolean show)
  {
    if (tvSeriesMenuItem != null)
    {
      tvSeriesMenuItem.setEnabled(show);
      tvSeriesMenuItem.setVisible(show);
    }
  }

  protected void showMusicMenuItem(boolean show)
  {
    if (musicMenuItem != null)
    {
      musicMenuItem.setEnabled(show);
      musicMenuItem.setVisible(show);
    }
  }
  
  protected void showViewMenu(boolean show)
  {
    if (viewMenuItem != null)
    {
      MediaSettings mediaSettings = appSettings.getMediaSettings();
      if (show)
      {
        viewMenuItem.setTitle("");
        viewMenuItem.setIcon(mediaSettings.getViewIcon());
        if (mediaSettings.getViewType() == ViewType.detail)
          viewMenuItem.getSubMenu().findItem(R.id.view_detail).setChecked(true);
        else
          viewMenuItem.getSubMenu().findItem(R.id.view_list).setChecked(true);
      }
      else
      {
        mediaSettings.setViewType(ViewType.list);
      }
      
      viewMenuItem.setEnabled(show);
      viewMenuItem.setVisible(show);
    }
  }

  protected void showMythwebMenu(boolean show)
  {
    if (mythwebMenuItem != null)
    {
      mythwebMenuItem.setEnabled(show);
      mythwebMenuItem.setVisible(show);
    }
  }
  
  protected void showSortMenu(boolean show)
  {
    if (sortMenuItem != null)
    {
      if (show)
      {
        MediaSettings mediaSettings = appSettings.getMediaSettings();
        sortMenuItem.setTitle(mediaSettings.getSortTypeTitle());
        if (mediaSettings.getSortType() == SortType.byDate)
          sortMenuItem.getSubMenu().findItem(R.id.sort_byDate).setChecked(true);
        else if (mediaSettings.getSortType() == SortType.byRating)
          sortMenuItem.getSubMenu().findItem(R.id.sort_byRating).setChecked(true);
        else
          sortMenuItem.getSubMenu().findItem(R.id.sort_byTitle).setChecked(true);
      }
      sortMenuItem.setEnabled(show);
      sortMenuItem.setVisible(show);
    }
  }  
  
  protected boolean supportsSearch()
  {
    return getAppSettings().isMythlingMediaServices();
  }

  protected boolean supportsSort()
  {
    return mediaList != null && mediaList.supportsSort();
  }
  
  protected boolean supportsViewMenu()
  {
    return mediaList != null && mediaList.canHaveArtwork();    
  }
  
  protected boolean supportsMovies()
  {
    return getAppSettings().isVideosCategorization();
  }

  protected boolean supportsTvSeries()
  {
    return getAppSettings().isVideosCategorization();
  }
  
  protected boolean supportsMusic()
  {
    return getAppSettings().isMythlingMediaServices();
  }

  protected boolean supportsMythwebMenu()
  {
    return getAppSettings().isMythWebAccessEnabled();    
  }
  
  protected boolean isListView()
  {
    return !isDetailView();
  }
  
  protected boolean isDetailView()
  {
    return false;
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
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.music));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_videos)
      {
        appSettings.setMediaType(MediaType.videos);
        item.setChecked(true);
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.videos));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_recordings)
      {
        appSettings.setMediaType(MediaType.recordings);
        item.setChecked(true);
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.recordings));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_tv)
      {
        appSettings.setMediaType(MediaType.liveTv);
        item.setChecked(true);
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.liveTv));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_movies)
      {
        appSettings.setMediaType(MediaType.movies);
        item.setChecked(true);
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.movies));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
        refresh();
        return true;
      }
      else if (item.getItemId() == R.id.media_tv_series)
      {
        appSettings.setMediaType(MediaType.tvSeries);
        item.setChecked(true);
        mediaMenuItem.setTitle(MediaSettings.getMediaTitle(MediaType.tvSeries));
        showViewMenu(supportsViewMenu());
        showSortMenu(supportsSort());
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
      else if (item.getItemId() == R.id.sort_byDate)
      {
        appSettings.setSortType(SortType.byDate);
        item.setChecked(true);
        sortMenuItem.setTitle(R.string.menu_byDate);
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
      else if (item.getItemId() == R.id.view_detail)
      {
        appSettings.setViewType(ViewType.detail);
        item.setChecked(true);
        viewMenuItem.setIcon(R.drawable.ic_menu_detail);
        goDetailView();
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
      else if (item.getItemId() == R.id.menu_mythweb)
      {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.getMythWebUrl())));
        return true;
      }
      else if (item.getItemId() == R.id.menu_help)
      {
        String url = getResources().getString(R.string.url_help);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class));
        return true;
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
            mediaPlayer.setOnCompletionListener(new OnCompletionListener()
            {
              public void onCompletion(MediaPlayer mp)
              {
                mediaPlayer.reset();
                onResume();
              }
            });
          }
          String musicUrl = appSettings.getMythTvServicesBaseUrlWithCredentials() + "/Content/GetMusic?Id=" + item.getId();
          mediaPlayer.setDataSource(appSettings.getAppContext(), Uri.parse(musicUrl));
          // TODO async?
          mediaPlayer.prepare();
          mediaPlayer.start();
          stopProgress();
        }
        else
        {
          if (item.isLiveTv() || item.isMovie())
          {
            if (item.isLiveTv() && ((TvShow)item).getEndTime().compareTo(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()) < 0)
            {
              new AlertDialog.Builder(this)
              .setIcon(android.R.drawable.ic_dialog_alert)
              .setTitle("Live TV")
              .setMessage("Show has already ended: " + ((TvShow)item).getShowInfo())
              .setPositiveButton("OK", null)
              .show();
              stopProgress();
              onResume();
              return;
            }
            String msg = null;
            if (item.isLiveTv())
              msg = ((TvShow)item).getShowInfo() + "\n\nRecording will be scheduled if necessary.";
            else
              msg = ((Movie)item).getShowInfo();
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(item.getTitle())
            .setMessage(msg)
            .setPositiveButton("Watch", new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                try
                {
                  if (item.isLiveTv())
                    new StreamTvTask((TvShow)item).execute(getAppSettings().getMythTvServicesBaseUrl());
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
      else // frontend playback
      {
        final FrontendPlayer player;
        if (item.isSearchResult())
        {
          SearchResults searchResults = ((SearchActivity)this).searchResults;
          StorageGroup storageGroup = searchResults.getStorageGroups().get(AppSettings.getStorageGroup(item.getType()));
          String basePath = null;
          if (item.isMusic())
            basePath = searchResults.getMusicBase();
          else if (storageGroup == null)
            basePath = searchResults.getVideoBase();
          if (basePath != null)
            player = new SocketFrontendPlayer(appSettings, basePath, item, getCharSet());
          else
            player = new ServiceFrontendPlayer(appSettings, item);
        }
        else
        {
          if (item.isMusic() || mediaList.getStorageGroup() == null) // frontend services require storage groups
            player = new SocketFrontendPlayer(appSettings, mediaList.getBasePath(), item, getCharSet());
          else
            player = new ServiceFrontendPlayer(appSettings, item);
        }
        
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
              player.stop();
              startFrontendPlayback(item, player);
            }
          })
          .setNegativeButton("No", null)
          .show();
        }
        else
        {
          startFrontendPlayback(item, player);
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
      {
        mediaPlayer.stop();
        // mediaPlayer.release();
      }
      
      mediaPlayer.reset();
    }
  }
  
  protected void goDetailView()
  {
    // default does nothing
  }
  
  protected void goListView()
  {
    // default does nothing
  }
  
  public void sort() throws IOException, JSONException, ParseException
  {
    startProgress();
    refreshMediaList();
  }
  
  private void startFrontendPlayback(Item item, final FrontendPlayer player)
  {
    if (item.isLiveTv())
    {
      new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle(item.getTitle())
      .setMessage("TODO: Frontend Live TV playback not yet supported.")
      .setPositiveButton("OK", null)
      .show();
    }
    else if (item.isRecording())
    {
      new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle(item.getTitle())
      .setMessage(((Recording)item).getShowInfo())
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
        countdownDialog.setMessage("Playing " + MediaSettings.getMediaLabel(item.getType()) + ": " + item.getLabel());
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
        // countdownDialog.setProgressDrawable(getResources().getDrawable(R.drawable.countdown_bar));
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
      getAppSettings().clearMediaSettings(); // in case prefs changed
      if (getAppSettings().getMediaSettings().getType() == MediaType.movies && !supportsMovies())
        getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));
      if (getAppSettings().getMediaSettings().getType() == MediaType.tvSeries && !supportsTvSeries())
        getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));
      if (getAppSettings().getMediaSettings().getType() == MediaType.music && !supportsMusic())
        getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));

      new RefreshTask().execute(getAppSettings().getUrls(getAppSettings().getMediaListUrl()));      
    }
    catch (Exception ex)
    {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
  
  protected void populate() throws IOException, JSONException, ParseException, BadSettingsException
  {
    // default does nothing
  }
  
  protected Transcoder getTranscoder(Item item)
  {
    if (mediaList.getStorageGroup() == null)
      return new Transcoder(getAppSettings(), mediaList.getBasePath());
    else
      return new Transcoder(getAppSettings(), mediaList.getStorageGroup());
  }
  
  /**
   * must be run in background thread
   */
  protected Map<String,StorageGroup> retrieveStorageGroups() throws IOException, JSONException
  {
    URL baseUrl = getAppSettings().getMythTvServicesBaseUrl();
    HttpHelper downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetStorageGroupDirs")));
    String sgJson = new String(downloader.get());
    return new MythTvParser(sgJson, getAppSettings()).parseStorageGroups();          
  }
  
  private class RefreshTask extends AsyncTask<URL,Integer,Long>
  {
    private String mediaListJson;

    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        MediaSettings mediaSettings = getAppSettings().getMediaSettings();
        
        HttpHelper downloader = getAppSettings().getMediaListDownloader(urls);
        byte[] bytes = downloader.get();
        mediaListJson = new String(bytes, downloader.getCharSet());
        if (mediaListJson.startsWith("<"))
        {
          // just display html
          ex = new IOException(mediaListJson);
          return -1L;
        }
        MediaListParser mediaListParser = getAppSettings().getMediaListParser(mediaListJson);

        
        if (getAppSettings().isMythlingMediaServices())
        {
          mediaList = mediaListParser.parseMediaList(mediaSettings.getType());
          if (mediaList.getBasePath() == null) // otherwise can avoid retrieving storage groups at least until MythTV 0.28
          {
            mediaList.setStorageGroup(retrieveStorageGroups().get(getAppSettings().getStorageGroup()));
          }
        }
        else
        {
          Map<String,StorageGroup> storageGroups = retrieveStorageGroups();
          StorageGroup mediaStorageGroup = storageGroups.get(getAppSettings().getStorageGroup());
          if (mediaStorageGroup != null)
          {
            mediaList = ((MythTvParser)mediaListParser).parseMediaList(mediaSettings.getType(), mediaStorageGroup, null, null);
          }
          else
          {
            // no storage group for media type
            URL baseUrl = getAppSettings().getMythTvServicesBaseUrl();
            String basePath = null;
            if (mediaSettings.getType() == MediaType.videos || mediaSettings.getType() == MediaType.movies || mediaSettings.getType() == MediaType.tvSeries)
            {
              // handle videos by getting the base path setting
              downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetHostName")));
              String hostName = new MythTvParser(new String(downloader.get()), getAppSettings()).parseString();
              String key = "VideoStartupDir";
              downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetSetting?Key=" + key + "&HostName=" + hostName)));
              basePath = new MythTvParser(new String(downloader.get()), getAppSettings()).parseMythTvSetting(key);
              if (basePath == null)
              {
                // try without host name
                downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetSetting?Key=" + key)));
                basePath = new MythTvParser(new String(downloader.get()), getAppSettings()).parseMythTvSetting(key);
              }
            }
            StorageGroup artworkStorageGroup = storageGroups.get(getAppSettings().getArtworkStorageGroup());
            mediaList = ((MythTvParser)mediaListParser).parseMediaList(mediaSettings.getType(), null, basePath, artworkStorageGroup);
          }
        }
        
        mediaList.setArtworkStorageGroup(getAppSettings().getArtworkStorageGroup(mediaSettings.getType()));
        mediaList.setCharSet(downloader.getCharSet());
        getAppSettings().clearPagerCurrentPosition(mediaList.getMediaType(), "");
        
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
          ViewType viewType = getAppSettings().getMediaSettings().getViewType();
          if (viewType == ViewType.list && isDetailView())
          {
            goListView();
          }
          else if (viewType == ViewType.detail && isListView())
          {
            goDetailView();
          }
          else
          {
            stopProgress();
            populate();
          }
        }
        catch (Exception ex)
        {
          stopProgress();          
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
        Transcoder transcoder = getTranscoder(item);
        
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
        Transcoder transcoder = getTranscoder(item);
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
    private TvShow tvShow;
    private Recording recordingToDelete;
    private LiveStreamInfo streamInfo;
    private Exception ex;
    
    public StreamTvTask(TvShow tvShow)
    {
      this.tvShow = tvShow;
    }
    
    public StreamTvTask(TvShow tvShow, Recording recordingToDelete)
    {
      this.tvShow = tvShow;
      this.recordingToDelete = recordingToDelete;
    }
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        Recorder recorder = new Recorder(getAppSettings());
        if (recordingToDelete != null)
          recorder.deleteRecording(recordingToDelete);
        boolean recordAvail = recorder.scheduleRecording(tvShow);
        
        if (!recordAvail)
          recorder.waitAvailable();
        
        Transcoder transcoder = getTranscoder(tvShow);
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
          new AlertDialog.Builder(MediaActivity.this)
          .setIcon(android.R.drawable.ic_dialog_info)
          .setTitle("Recording Conflict")
          .setMessage("Tuner already in use recording:\n" + ex.getMessage() + "\nDelete this recording and proceed?")
          .setPositiveButton("Delete", new DialogInterface.OnClickListener()
          {
            public void onClick(DialogInterface dialog, int which)
            {
              final Recording inProgressRecording = ((TunerInUseException)ex).getRecording();
              new AlertDialog.Builder(MediaActivity.this)
              .setIcon(android.R.drawable.ic_dialog_alert)
              .setTitle("Confirm Delete")
              .setMessage("Delete in-progress recording?\n" + inProgressRecording)
              .setPositiveButton("Yes", new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface dialog, int which)
                {
                  try
                  {
                    startProgress();
                    new StreamTvTask(tvShow, inProgressRecording).execute(getAppSettings().getMythTvServicesBaseUrl());
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
              .setNegativeButton("No", new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface dialog, int which)
                {
                  stopProgress();
                  onResume();
                }
              })
              .show();              
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
    String streamUrl = appSettings.getMythTvServicesBaseUrlWithCredentials() + streamInfo.getRelativeUrl();

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
      // XXX internal player
    }
  }
  
  protected void startProgress()
  {
    progressBar.setVisibility(View.VISIBLE);
  }
  
  protected void stopProgress()
  {
    progressBar.setVisibility(View.GONE);
  }
}
