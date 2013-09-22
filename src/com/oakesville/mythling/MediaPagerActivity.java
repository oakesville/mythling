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

import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Item;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.app.MediaList;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.util.HttpHelper;

/**
 * Activity for side-scrolling through paged detail views of MythTV media.
 *
 */
public class MediaPagerActivity extends MediaActivity
{
  private static final String TAG = MediaPagerActivity.class.getSimpleName();

  private String path;
  private MediaList mediaList;

  private ViewPager pager;
  private MoviePagerAdapter pagerAdapter;
  private List<Listable> items;
  private int currentPosition;
  private SeekBar positionBar;
  private int[] ratingViewIds = new int[5];

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pager);
    
    createProgressBar();
    
    pager = (ViewPager) findViewById(R.id.pager);

    try
    {
      String newPath = URLDecoder.decode(getIntent().getDataString(), "UTF-8");
      if (newPath != null && !newPath.isEmpty())
        path = newPath;
      
      if (!"TV".equals(path))
        getActionBar().setDisplayHomeAsUpEnabled(true);
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

  public void populate() throws IOException, JSONException
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
    items = mediaList.getListables(path);

    pagerAdapter = new MoviePagerAdapter(getFragmentManager());
    pager.setAdapter(pagerAdapter);
    pager.setOnPageChangeListener(new OnPageChangeListener()
    {
      public void onPageSelected(int position)
      {
        currentPosition = position;
        getAppSettings().setMovieCurrentPosition(path, currentPosition);
        positionBar.setProgress(currentPosition + 1);
        TextView curItemView = (TextView) findViewById(R.id.currentItem);
        curItemView.setText(String.valueOf(currentPosition + 1));
      }

      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
      {
      }

      public void onPageScrollStateChanged(int state)
      {
      }
    });
    
    positionBar = (SeekBar) findViewById(R.id.pagerPosition);
    positionBar.setMax(items.size());
    positionBar.setProgress(1);
    positionBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
    {
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
      {
        if (fromUser)
          currentPosition = progress;
      }
      public void onStartTrackingTouch(SeekBar seekBar)
      {
      }
      public void onStopTrackingTouch(SeekBar seekBar)
      {
        pager.setCurrentItem(currentPosition);
      }
    });

    ImageButton button = (ImageButton) findViewById(R.id.gotoFirst);
    button.setOnClickListener(new OnClickListener()
    {
      public void onClick(View v)
      {
        pager.setCurrentItem(0);
        positionBar.setProgress(1);
      }
    });
    button = (ImageButton) findViewById(R.id.gotoLast);
    button.setOnClickListener(new OnClickListener()
    {
      public void onClick(View v)
      {
        pager.setCurrentItem(items.size() - 1);
        positionBar.setProgress(items.size());
      }
    });
    
    TextView tv = (TextView) findViewById(R.id.lastItem);
    tv.setText(String.valueOf(items.size()));
    
    ratingViewIds[0] = R.id.star_1;
    ratingViewIds[1] = R.id.star_2;
    ratingViewIds[2] = R.id.star_3;
    ratingViewIds[3] = R.id.star_4;
    ratingViewIds[4] = R.id.star_5;

    currentPosition = getAppSettings().getMovieCurrentPosition(path);
    if (items.size() > currentPosition)
    {
      pager.setCurrentItem(currentPosition);
      positionBar.setProgress(currentPosition);
      TextView curItemView = (TextView) findViewById(R.id.currentItem);
      if (curItemView != null)
        curItemView.setText(String.valueOf(currentPosition + 1));
    }
    else
    {
      getAppSettings().setMovieCurrentPosition(path, 0);
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
    return true;
  }  
  
  public void refresh()
  {
    getAppSettings().setLastLoad(0);
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    finish();
  }
  
  protected void goListView()
  {
    Uri.Builder builder = new Uri.Builder();
    builder.path(path);
    Uri uri = builder.build();
    startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaListActivity.class));
  }
  
  public ListView getListView()
  {
    return null;
  }
      
  
  private class MoviePagerAdapter extends FragmentPagerAdapter
  {
    public MoviePagerAdapter(FragmentManager fm)
    {
      super(fm);
    }
    
    public int getCount()
    {
      return items.size();
    }
    
    public Fragment getItem(int position)
    {
      Fragment frag = new MovieListFragment();
      Bundle args = new Bundle();
      args.putInt("idx", position);
      frag.setArguments(args);
      return frag;
    }
  }

  public static class MovieListFragment extends Fragment
  {
    private MediaPagerActivity pagerActivity;
    private AppSettings appSettings;
    private View movieView;
    private ImageView posterView;
    private Listable listable;
    private int idx;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      idx = getArguments() != null ? getArguments().getInt("idx") : 1;
    }
    
    @Override
    public void onAttach(Activity activity)
    {
      super.onAttach(activity);
      pagerActivity = (MediaPagerActivity) activity;
      appSettings = pagerActivity.getAppSettings();
    }

    @Override
    public void onDetach()
    {
      pagerActivity = null;
      appSettings = null;
      super.onDetach();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
      movieView = inflater.inflate(R.layout.movie, container, false);
      return movieView;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
      super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume()
    {
      super.onResume();
      
      listable = pagerActivity.items.get(idx);
      
      TextView titleView = (TextView) movieView.findViewById(R.id.titleText);
      titleView.setText(listable.getLabel());
      
      if (listable instanceof Item)
      {
        Item item = (Item) listable;
        // TODO other types
        if (item.isMovie())
        {
          if (item.getPoster() != null)
          {
            posterView = (ImageView) movieView.findViewById(R.id.posterImage);
            // http://mythbe:6544/Content/GetImageFile?StorageGroup=Coverart&FileName=acp.jpg
            String posterStorageGroup = "Coverart"; // TODO prefs
            try
            {
              String filePath = pagerActivity.path + "/" + item.getPoster();
              Bitmap bitmap = getAppData().getImageBitMap(filePath);
              if (bitmap == null)
              {
                URL url = new URL(appSettings.getServicesBaseUrl() + "/Content/GetImageFile?StorageGroup=" + posterStorageGroup + "&FileName=" + item.getPoster());
                new ImageRetrievalTask().execute(url);
              }
              else
              {
                posterView.setImageBitmap(bitmap);
              }
            }
            catch (Exception ex)
            {
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
              Toast.makeText(pagerActivity, ex.toString(), Toast.LENGTH_LONG).show();
            }
          }
          // rating
          for (int i = 0; i < item.getRating(); i++)
          {
            ImageView star = (ImageView) movieView.findViewById(pagerActivity.ratingViewIds[i]);
            if (i <= item.getRating() - 1)
              star.setImageResource(R.drawable.rating_full);
            else
              star.setImageResource(R.drawable.rating_half);
          }
          // director
          if (item.getDirector() != null)
          {
            TextView tv = (TextView) movieView.findViewById(R.id.directorText);
            tv.setText("Directed by: " + item.getDirector());
          }
          // actors
          if (item.getActors() != null)
          {
            TextView tv = (TextView) movieView.findViewById(R.id.actorsText);
            tv.setText("Starring: " + item.getActors());
          }
          // summary
          if (item.getSummary() != null)
          {
            TextView tv = (TextView) movieView.findViewById(R.id.summaryText);
            tv.setText(item.getSummary());
          }
          if (item.getPageUrl() != null && getAppData().getMediaList().getPageLinkTitle() != null)
          {
            // page link
            TextView tv = (TextView) movieView.findViewById(R.id.oakesvilleLink);
            tv = (TextView) movieView.findViewById(R.id.pageLink);
            tv.setText(Html.fromHtml("<a href='" + item.getPageUrl() + "'>" + getAppData().getMediaList().getPageLinkTitle() + "</a>"));
            tv.setMovementMethod(LinkMovementMethod.getInstance());
          }
          
          Button button = (Button) movieView.findViewById(R.id.pagerPlay);
          if (!pagerActivity.path.equals("Horror"))
          {
            button.setOnClickListener(new OnClickListener()
            {
              public void onClick(View v)
              {
                Item oldItem = (Item)listable;
                Item item = new Item(oldItem);
                item.setPath(getAppData().getMediaList().getBasePath() + "/" + pagerActivity.path);
                pagerActivity.playItem(item);
              }
            });
          }
          else
          {
            button.setEnabled(false);
            button.setVisibility(View.INVISIBLE);
          }
        }
      }
    }
    
    public class ImageRetrievalTask extends AsyncTask<URL,Integer,Long>
    {
      private Exception ex;
      private String filePath;
      private Bitmap bitmap;
      
      protected Long doInBackground(URL... urls)
      {
        try
        {
          String file = urls[0].getFile();
          // assumes FileName is last URL parameter
          file = file.substring(urls[0].getFile().lastIndexOf("&FileName=") + 10);
          
          filePath = pagerActivity.path + "/" + file;
          bitmap = getAppData().readImageBitmap(filePath);
          if (bitmap == null)
          {
            if (BuildConfig.DEBUG)
              Log.d(TAG, "Loading image from url: " + urls[0]);
            HttpHelper downloader = new HttpHelper(urls, appSettings.getServicesAuthType(), appSettings.getPrefs());
            downloader.setCredentials(appSettings.getServicesAccessUser(), appSettings.getServicesAccessPassword());
            try
            {
              byte[] imageBytes = downloader.get();
              getAppData().writeImage(filePath, imageBytes);
            }
            catch (EOFException ex)
            {
              /// try again
              byte[] imageBytes = downloader.get();
              getAppData().writeImage(filePath, imageBytes);
            }
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
        if (result != 0L)
        {
          if (ex != null)
          {
            if (BuildConfig.DEBUG)
              Log.e(TAG, ex.getMessage(), ex);
            Toast.makeText(pagerActivity, ex.toString(), Toast.LENGTH_LONG).show();
          }
        }
        else
        {
          try
          {
            posterView.setImageBitmap(getAppData().readImageBitmap(filePath));
          }
          catch (Exception ex)
          {
            if (BuildConfig.DEBUG)
              Log.e(TAG, ex.getMessage(), ex);
          }
        }
      }    
    }
    
  }

  @Override
  public void sort()
  {
    startProgress();
    refreshMediaList();
    pager.setCurrentItem(0);
    positionBar.setProgress(1);
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
