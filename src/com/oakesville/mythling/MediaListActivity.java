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
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.FragmentBreadCrumbs;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.Item;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.MediaSettings.ViewType;

/**
 * Displays a list of listables (either categories or items).
 */
public class MediaListActivity extends MediaActivity
{
  private static final String TAG = MediaListActivity.class.getSimpleName();
  
  private String path; 
  private FragmentBreadCrumbs breadCrumbs;
  private ListView listView;
  public ListView getListView() { return listView; }

  private ArrayAdapter<Listable> adapter;
  List<Listable> listables;
  
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sub_cats);
    
    createProgressBar();
    
    listView = (ListView) findViewById(R.id.sub_cats);
    try
    {
      String newPath = URLDecoder.decode(getIntent().getDataString(), "UTF-8");
      if (newPath != null && !newPath.isEmpty())
        path = newPath;
      
      if (!"TV".equals(path))
        getActionBar().setDisplayHomeAsUpEnabled(true);
      
      breadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
      breadCrumbs.setActivity(this);
      breadCrumbs.setTitle(path, path);
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }
  }
  
  @Override
  protected void onResume()
  {
    try
    {
      populate();
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }
    
    super.onResume();
  }

  public void populate() throws IOException, JSONException, ParseException
  {
    if (getAppData() == null)
    {
      startProgress();
      AppData appData = new AppData(getApplicationContext());
      appData.readMediaList();
      setAppData(appData);
      stopProgress();
    }
    else if (getMediaType() != null && getMediaType() != getAppData().getMediaList().getMediaType())
    {
      // media type was changed, then back button was pressed
      getAppSettings().setMediaType(getMediaType());
      refresh();
      return;
    }
    mediaList = getAppData().getMediaList();
    setMediaType(mediaList.getMediaType());
    showViewMenu(mediaList.getMediaType() == MediaType.movies);
    showSortMenu(mediaList.getMediaType() == MediaType.movies);
    listables = mediaList.getListables(path);
    if (getAppSettings().getMediaSettings().getViewType() == ViewType.pager)
    {
      for (Listable listable : listables)
      {
        if (listable instanceof Item)
        {
          goPagerView();
          return;
        }
      }
    }
    
    if ("TV".equals(path))
    {
      String title = "TV  (at " + mediaList.getRetrieveTimeDisplay() + " on " + mediaList.getRetrieveDateDisplay() + ")";
      breadCrumbs.setTitle(title, title);
    }
   
    adapter = new ArrayAdapter<Listable>(MediaListActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, listables.toArray(new Listable[0]));
    listView.setAdapter(adapter);
    if (listables.size() > 0)
    {
      listView.setOnItemClickListener(new OnItemClickListener()
      {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
          final String text = ((TextView)view).getText().toString();
          boolean isItem = listables.get(position) instanceof Item;
          if (isItem)
          {
            Item item = new Item((Item)listables.get(position));
            if (item.isRecording() || item.isTv())
              item.setPath(mediaList.getBasePath());
            else
              item.setPath(mediaList.getBasePath() + "/" + path);
            playItem(item);
          }
          else
          {
            // must be category
            Uri.Builder builder = new Uri.Builder();
            builder.path(path + "/" + text);
            Uri uri = builder.build();
            startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaListActivity.class));
          }
        }
      });
      
      listView.setOnItemLongClickListener(new OnItemLongClickListener()
      {
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
        {
          boolean isItem = listables.get(position) instanceof Item;
          if (isItem)
          {
            final Item item = new Item((Item)listables.get(position));
            if (item.isRecording() || item.isTv())
            {
              item.setPath(mediaList.getBasePath());
              new AlertDialog.Builder(view.getContext())
              .setIcon(android.R.drawable.ic_dialog_info)
              .setTitle("Transcode")
              .setMessage("Begin transcoding " + item.getTitle() + "?")
              .setPositiveButton("OK", new DialogInterface.OnClickListener()
              {
                public void onClick(DialogInterface dialog, int which)
                {
                  try
                  {
                    // TODO: if TV, schedule recording and start transcode
                    if (item.isRecording())
                      new TranscodeVideoTask(item).execute(getAppSettings().getMythTvServicesBaseUrl());
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
          }
          return true;
        }
      });
    }
  }

  @Override
  protected boolean supportsViewSelection()
  {
    return true;
  }
  
  @Override
  protected boolean supportsSort()
  {
    return getAppSettings().isMythlingMediaServices();
  }  

  @Override
  public void sort()
  {
    startProgress();
    refreshMediaList();
    getAppSettings().setMovieCurrentPosition(path, 0);
  }

  public void refresh()
  {
    getAppSettings().setLastLoad(0);
    startActivity(new Intent(this, MainActivity.class));
  }

  protected void goPagerView()
  {
    Uri.Builder builder = new Uri.Builder();
    builder.path(path);
    Uri uri = builder.build();
    startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaPagerActivity.class));
  }

}
