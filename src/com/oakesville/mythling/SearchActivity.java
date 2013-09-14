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

import java.net.URL;
import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.SearchResults;
import com.oakesville.mythling.app.Item;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.JsonParser;
import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;

public class SearchActivity extends MediaActivity
{
  public static final String TAG = SearchActivity.class.getSimpleName();
  
  private String searchQuery;
  private String resultsJson;
  private SearchResults searchResults;
  private ListView listView;
  public ListView getListView() { return listView; }

  private ArrayAdapter<Item> adapter;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    
    createProgressBar();

    listView = (ListView) findViewById(R.id.search_list);
    
    // Get the intent, verify the action and get the query
    Intent intent = getIntent();
    if (Intent.ACTION_SEARCH.equals(intent.getAction()))
    {
      searchQuery = intent.getStringExtra(SearchManager.QUERY);
      search();
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.search, menu);    
    return true;
  }  
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      startActivity(new Intent(this, MainActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void search()
  {
    searchResults = new SearchResults();

    adapter = new ArrayAdapter<Item>(SearchActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, searchResults.getAll().toArray(new Item[0]));
    listView.setAdapter(adapter);
    
    startProgress();
        
    try
    {
      new SearchTask().execute(getAppSettings().getUrls(getAppSettings().getSearchUrl(searchQuery)));
    }
    catch (Exception ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
    }
  }
  
  private class SearchTask extends AsyncTask<URL,Integer,Long>
  {
    private Exception ex;
    
    protected Long doInBackground(URL... urls)
    {
      try
      {
        HttpHelper downloader = new HttpHelper(urls, getAppSettings().getWebAuthType(), getAppSettings().getPrefs());
        downloader.setCredentials(getAppSettings().getWebAccessUser(), getAppSettings().getWebAccessPassword());
        resultsJson = new String(downloader.get());
        searchResults = new JsonParser(resultsJson).parseSearchResults();
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
        try
        {
          final List<Item> items = searchResults.getAll();
          adapter = new ArrayAdapter<Item>(SearchActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, items.toArray(new Item[0]));
          listView.setAdapter(adapter);
          listView.setOnItemClickListener(new OnItemClickListener()
          {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
              Item oldItem = (Item)items.get(position);
              Item item = new Item(oldItem);
              if (item.isMusic())
                item.setPath(searchResults.getMusicBase());
              else if (item.isRecording() || item.isTv())
                item.setPath(searchResults.getRecordingsBase());
              else
                item.setPath(searchResults.getVideoBase());
              playItem(item);
            }
          });
        }
        catch (Exception ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
          Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
      }
    }    
  }
  
  public void refresh() throws BadSettingsException
  {
    startActivity(new Intent(this, MainActivity.class));
  } 
  
  @Override
  public void onBackPressed()
  {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    finish();
  }  
  
}