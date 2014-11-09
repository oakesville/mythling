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
import java.text.ParseException;
import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.util.Reporter;

public class MainActivity extends MediaActivity
{
  private static final String TAG = MainActivity.class.getSimpleName();
  
  private ListView listView;
  public ListView getListView() { return listView; }
  private int currentTop = 0;  // top item in the list
  private int topOffset = 0;
  private ArrayAdapter<Listable> adapter;

  public String getCharSet()
  {
    return mediaList.getCharSet();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    
    PreferenceManager.setDefaultValues(this, R.xml.cache_prefs, false);
    PreferenceManager.setDefaultValues(this, R.xml.credentials_prefs, false);
    PreferenceManager.setDefaultValues(this, R.xml.network_prefs, false);
    PreferenceManager.setDefaultValues(this, R.xml.playback_prefs, false);
    PreferenceManager.setDefaultValues(this, R.xml.quality_prefs, false);
    
    try
    {
      getAppSettings().initMythlingVersion();
    }
    catch (NameNotFoundException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      if (getAppSettings().isErrorReportingEnabled())
        new Reporter(ex).send();      
    }

    setContentView(R.layout.categories);
    
    createProgressBar();
    
    listView = (ListView) findViewById(R.id.categories);

    if (getAppSettings().getMediaSettings().getViewType() == ViewType.detail)
    {
      Intent intent = new Intent(this, MediaPagerActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
      finish();
      return;
    }
  }
  
  @Override
  protected void onResume()
  {
    try
    {
      if (getAppData() == null || getAppData().isExpired())
        refresh();
      else
        populate();
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
      if (getAppSettings().isErrorReportingEnabled())
        new Reporter(ex).send();      
      stopProgress();
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }
    
    super.onResume();
  }


  public void refresh() throws BadSettingsException
  {
    currentTop = 0;
    topOffset = 0;
    mediaList = new MediaList();
    adapter = new ArrayAdapter<Listable>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mediaList.getTopCategoriesAndItems().toArray(new Listable[0]));
    listView.setAdapter(adapter);
    
    startProgress();
    getAppSettings().validate();
    
    refreshMediaList();
  }
  
  protected void populate() throws IOException, JSONException, ParseException, BadSettingsException
  {
    startProgress();
    if (getAppData() == null)
    {
      AppData appData = new AppData(getApplicationContext());
      appData.readMediaList(getMediaType());
      setAppData(appData);
    }
    else if (getMediaType() != null && getMediaType() != getAppData().getMediaList().getMediaType())
    {
      // media type was changed, then back button was pressed
      getAppSettings().setMediaType(getMediaType());
      getAppSettings().setLastLoad(0);
      onResume();
    }
    
    mediaList = getAppData().getMediaList();
    showMoviesMenuItem(supportsMovies());
    showTvSeriesMenuItem(supportsTvSeries());
    showMusicMenuItem(supportsMusic());
    showSortMenu(supportsSort());
    showViewMenu(supportsViewMenu());
    showSearchMenu(supportsSearch());
    
    MenuItem mediaMenuItem = getMediaMenuItem();
    if (mediaMenuItem != null)
    {
      String title = MediaSettings.getMediaTitle(getAppSettings().getMediaSettings().getType());
      mediaMenuItem.setTitle(title + " (" + mediaList.getCount() + ")");
    }
    
    adapter = new ArrayAdapter<Listable>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, mediaList.getTopCategoriesAndItems().toArray(new Listable[0]));
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
        currentTop = listView.getFirstVisiblePosition();
        View topV = listView.getChildAt(0);
        topOffset = (topV == null) ? 0 : topV.getTop();        
        List<Listable> listables = mediaList.getTopCategoriesAndItems();
        boolean isMediaItem = listables.get(position) instanceof Item;
        if (isMediaItem)
        {
          Item item = (Item)listables.get(position);
          item.setPath("");
          playItem(item);
        }
        else
        {
          // must be category
          String cat = ((TextView)view).getText().toString();
          Uri.Builder builder = new Uri.Builder();
          builder.path(cat);
          Uri uri = builder.build();
          startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaListActivity.class));
        }
      }
    });
    stopProgress();
    listView.setSelectionFromTop(currentTop, topOffset);
  }

  protected void goDetailView()
  {
    if (mediaList.getMediaType() == MediaType.recordings && getAppSettings().getMediaSettings().getSortType() == SortType.byTitle)
      getAppSettings().clearCache(); // refresh since we're switching to flattened hierarchy
    
    Uri.Builder builder = new Uri.Builder();
    builder.path("");
    Uri uri = builder.build();
    Intent intent = new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaPagerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }
  
}
