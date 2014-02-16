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

import java.io.EOFException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
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
  private MediaPagerAdapter pagerAdapter;
  private List<Listable> items;
  private int currentPosition;
  private SeekBar positionBar;
  private int[] ratingViewIds = new int[5];
  
  public String getCharSet()
  {
    return mediaList.getCharSet();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pager);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    
    createProgressBar();
    
    pager = (ViewPager) findViewById(R.id.pager);

    try
    {
      String newPath = URLDecoder.decode(getIntent().getDataString(), "UTF-8");
      if (newPath != null && !newPath.isEmpty())
        path = newPath;
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
      if (getAppData() == null || getAppData().isExpired())
        refresh();
      else
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
      appData.readMediaList(getMediaType());
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
    showViewMenu(mediaList.getMediaType() == MediaType.movies || mediaList.getMediaType() == MediaType.tvSeries);
    showSortMenu(mediaList.getMediaType() == MediaType.movies || mediaList.getMediaType() == MediaType.tvSeries);
    showMusicMenuItem(getAppSettings().isMythlingMediaServices());
    items = mediaList.getListables(path);

    pagerAdapter = new MediaPagerAdapter(getFragmentManager());
    pager.setAdapter(pagerAdapter);
    pager.setOnPageChangeListener(new OnPageChangeListener()
    {
      public void onPageSelected(int position)
      {
        currentPosition = position;
        getAppSettings().setPagerCurrentPosition(mediaList.getMediaType(), path, currentPosition);
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

    currentPosition = getAppSettings().getPagerCurrentPosition(mediaList.getMediaType(), path);
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
      getAppSettings().setPagerCurrentPosition(mediaList.getMediaType(), path, 0);
    }
  }
  
  protected boolean supportsViewSelection()
  {
    return true;
  }
  
  public void refresh()
  {
    getAppSettings().setLastLoad(0);
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
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
      
  
  private class MediaPagerAdapter extends FragmentPagerAdapter
  {
    public MediaPagerAdapter(FragmentManager fm)
    {
      super(fm);
    }
    
    public int getCount()
    {
      return items.size();
    }
    
    public Fragment getItem(int position)
    {
      Fragment frag = new MediaPagerFragment();
      Bundle args = new Bundle();
      args.putInt("idx", position);
      frag.setArguments(args);
      return frag;
    }
  }

  public static class MediaPagerFragment extends Fragment
  {
    private MediaPagerActivity pagerActivity;
    private AppSettings appSettings;
    private View detailView;
    private ImageView artworkView;
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
      detailView = inflater.inflate(R.layout.detail, container, false);
      return detailView;
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
      appSettings = pagerActivity.getAppSettings();  // somehow this was set to null
      
      TextView titleView = (TextView) detailView.findViewById(R.id.titleText);
      titleView.setText(listable.getLabel());
      
      if (listable instanceof Item)
      {
        Item item = (Item) listable;
        if (item.isMovie() || item.isTvSeries())
        {
          if (item.getArtwork() != null)
          {
            artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
            String artworkStorageGroup = item.getArtworkStorageGroup();
            if (artworkStorageGroup == null)
              artworkStorageGroup = getAppData().getMediaList().getArtworkStorageGroup();
            try
            {
              String filePath = pagerActivity.path + "/" + item.getArtwork();
              Bitmap bitmap = getAppData().getImageBitMap(filePath);
              if (bitmap == null)
              {
                URL url = appSettings.getArtworkUrl(artworkStorageGroup, item.getArtwork());
                new ImageRetrievalTask().execute(url);
              }
              else
              {
                artworkView.setImageBitmap(bitmap);
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
            ImageView star = (ImageView) detailView.findViewById(pagerActivity.ratingViewIds[i]);
            if (i <= item.getRating() - 1)
              star.setImageResource(R.drawable.rating_full);
            else
              star.setImageResource(R.drawable.rating_half);
          }
          // director
          if (item.getDirector() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.directorText);
            tv.setText("Directed by: " + item.getDirector());
          }
          // actors
          if (item.getActors() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.actorsText);
            tv.setText("Starring: " + item.getActors());
          }
          // summary
          if (item.getSummary() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.summaryText);
            String summary = item.getSummary();
            if (item.getSeason() != 0)
              summary = "Season " + item.getSeason() + ", Episode " + item.getEpisode() + ":\n" + summary; 
            tv.setText(summary);
          }
          
          // custom link
          if (appSettings.getCustomBaseUrl() != null && !appSettings.getCustomBaseUrl().isEmpty())
          {
            try
            {
              String encodedTitle = URLEncoder.encode(item.getTitle(), "UTF-8");
              URL url = new URL(appSettings.getCustomBaseUrl() + pagerActivity.path + "/" + encodedTitle);
              TextView tv = (TextView) detailView.findViewById(R.id.customLink);
              String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
              tv.setText(Html.fromHtml("<a href='" + url + "'>" + host + "</a>"));
              tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
            catch (IOException ex)
            {
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            }
          }
          
          // page link
          if (item.getPageUrl() != null || item.getInternetRef() != null)
          {
            try
            {
              String pageUrl = item.getPageUrl();
              if (pageUrl == null || pageUrl.isEmpty())
              {
                String baseUrl = getAppData().getMediaList().getMediaType() == MediaType.tvSeries ? appSettings.getTvBaseUrl() : appSettings.getMovieBaseUrl();
                pageUrl = baseUrl + item.getInternetRef();
              }
              URL url = new URL(pageUrl);
              TextView tv = (TextView) detailView.findViewById(R.id.pageLink);
              String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
              tv.setText(Html.fromHtml("<a href='" + pageUrl + "'>" + host + "</a>"));
              tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
            catch (MalformedURLException ex)
            {
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
              Toast.makeText(pagerActivity, ex.toString(), Toast.LENGTH_LONG).show();
            }
          }
          
          Button button = (Button) detailView.findViewById(R.id.pagerPlay);
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
            HttpHelper downloader = new HttpHelper(urls, appSettings.getMythTvServicesAuthType(), appSettings.getPrefs(), true);
            downloader.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
            try
            {
              byte[] imageBytes = downloader.get();
              getAppData().writeImage(filePath, imageBytes);
            }
            catch (EOFException ex)
            {
              // try again
              byte[] imageBytes = downloader.get();
              getAppData().writeImage(filePath, imageBytes);
            }
            catch (IOException ex)
            {
              // fail silently
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
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
            artworkView.setImageBitmap(getAppData().readImageBitmap(filePath));
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
  public void sort() throws IOException, JSONException, ParseException
  {
    super.sort();
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
