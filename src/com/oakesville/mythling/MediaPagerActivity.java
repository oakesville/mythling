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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.ArtworkDescriptor;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.Song;
import com.oakesville.mythling.media.TvEpisode;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.media.Video;
import com.oakesville.mythling.util.HttpHelper;

/**
 * Activity for side-scrolling through paged detail views of MythTV media w/artwork.
 *
 */
public class MediaPagerActivity extends MediaActivity
{
  private static final String TAG = MediaPagerActivity.class.getSimpleName();

  private String path;

  private ViewPager pager;
  private MediaPagerAdapter pagerAdapter;
  private List<Listable> items;
  private int currentPosition;
  private SeekBar positionBar;
  private int[] ratingViewIds = new int[] {R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5};
  
  public String getCharSet()
  {
    return mediaList.getCharSet();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pager);
    
    createProgressBar();
    
    pager = (ViewPager) findViewById(R.id.pager);

    try
    {
      String newPath = getIntent().getDataString();
      if (newPath == null)
        path = "";
      else
        path = URLDecoder.decode(newPath, "UTF-8");
      
      modeSwitch = getIntent().getBooleanExtra("modeSwitch", false);
      
      getActionBar().setDisplayHomeAsUpEnabled(!path.isEmpty());
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

  public void populate() throws IOException, JSONException, ParseException, BadSettingsException
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
    showMoviesMenuItem(supportsMovies());
    showTvSeriesMenuItem(supportsTvSeries());
    showMusicMenuItem(supportsMusic());
    showSortMenu(supportsSort());
    showViewMenu(supportsViewMenu());
    showSearchMenu(supportsSearch());
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
  
  public void refresh() throws BadSettingsException
  {
    path = "";
    mediaList = new MediaList();
    
    startProgress();
    getAppSettings().validate();
    
    refreshMediaList();
  }
  
  protected void goListView()
  {
    if (mediaList.getMediaType() == MediaType.recordings && getAppSettings().getMediaSettings().getSortType() == SortType.byTitle)
      getAppSettings().clearCache(); // refresh since we're switching from flattened hierarchy
    
    if (path == null || path.isEmpty())
    {
      Intent intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.putExtra("modeSwitch", true);
      startActivity(intent);
    }
    else
    {        
      Uri.Builder builder = new Uri.Builder();
      builder.path(path);
      Uri uri = builder.build();
      Intent intent = new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(),  MediaListActivity.class);
      intent.putExtra("modeSwitch", true);
      startActivity(intent);
    }
  }
  
  public ListView getListView()
  {
    return null;
  }
  
  @Override
  public void onBackPressed()
  {
    if (modeSwitch)
    {
      modeSwitch = false;
      Intent intent = new Intent(this, MediaPagerActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
      finish();
    }
    else
    {
      super.onBackPressed();
    }
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
      
      if (listable instanceof Category)
      {
        Category category = (Category) listable;

        titleView.setText(category.getName());
        titleView.setMovementMethod(LinkMovementMethod.getInstance());        
        Spannable spans = (Spannable) titleView.getText();
        ClickableSpan clickSpan = new ClickableSpan()
        {
           public void onClick(View v)
           {
             ((TextView)v).setBackgroundColor(Color.GRAY);
             Uri.Builder builder = new Uri.Builder();
             builder.path(pagerActivity.path.length() == 0 ? listable.toString() : pagerActivity.path + "/" + listable.toString());
             Uri uri = builder.build();
             startActivity(new Intent(Intent.ACTION_VIEW, uri, pagerActivity.getApplicationContext(),  MediaPagerActivity.class));
           }
        };
        spans.setSpan(clickSpan, 0, spans.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);        
        
        artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
        Drawable folder = getResources().getDrawable(R.drawable.folder);
        artworkView.setImageDrawable(folder);
        artworkView.setClickable(true);
        artworkView.setOnClickListener(new View.OnClickListener()
        {
          public void onClick(View v)
          {
            ((ImageView)v).setBackgroundResource(R.drawable.rounded_frame_active);
            Uri.Builder builder = new Uri.Builder();
            builder.path(pagerActivity.path.length() == 0 ? listable.toString() : pagerActivity.path + "/" + listable.toString());
            Uri uri = builder.build();
            startActivity(new Intent(Intent.ACTION_VIEW, uri, pagerActivity.getApplicationContext(),  MediaPagerActivity.class));
          }
        });        
      }
      else if (listable instanceof Item)
      {
        Item item = (Item) listable;
        
        titleView.setText(item.getLabel());
            
        // rating
        if (item.getRating() > 0)
        {
          for (int i = 0; i < 5; i++)
          {
            ImageView star = (ImageView) detailView.findViewById(pagerActivity.ratingViewIds[i]);
            if (i <= item.getRating() - 1)
              star.setImageResource(R.drawable.rating_full);
            else if (i < item.getRating())
              star.setImageResource(R.drawable.rating_half);
            else
              star.setImageResource(R.drawable.rating_empty);
          }
        }
        
        if (item instanceof Video)
        {
          Video video = (Video) item;
          // director
          if (video.getDirector() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.directorText);
            tv.setText("Directed by: " + video.getDirector());
          }
          // actors
          if (video.getActors() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.actorsText);
            tv.setText("Starring: " + video.getActors());
          }
          // summary
          if (video.getSummary() != null)
          {
            TextView tv = (TextView) detailView.findViewById(R.id.summaryText);
            String summary = video.getSummary();
            if (item instanceof TvEpisode)
            {
              TvEpisode tve = (TvEpisode) item;
              if (tve.getSeason() != 0)
                summary = "Season " + tve.getSeason() + ", Episode " + tve.getEpisode() + ":\n" + summary; 
            }
            tv.setText(summary);
          }
          
          // custom link (only for movies and tv series)
          if ((item.isMovie() || item.isTvSeries())
              && appSettings.getCustomBaseUrl() != null && !appSettings.getCustomBaseUrl().isEmpty())
          {
            try
            {
              String encodedTitle = URLEncoder.encode(item.getTitle(), "UTF-8");
              URL url = new URL(appSettings.getCustomBaseUrl() + pagerActivity.path + "/" + encodedTitle);
              TextView tv = (TextView) detailView.findViewById(R.id.customLink);
              String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
              tv.setText(Html.fromHtml("<a href='" + url + "'>" + host + "</a>"));
              tv.setMovementMethod(LinkMovementMethod.getInstance());
              tv.setOnClickListener(new OnClickListener()
              {
                public void onClick(View v)
                {
                  ((TextView)v).setBackgroundColor(Color.GRAY);
                }
              });
            }
            catch (IOException ex)
            {
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            }
          }
          
          // page link
          if (video.getPageUrl() != null || video.getInternetRef() != null)
          {
            try
            {
              String pageUrl = video.getPageUrl();
              if (pageUrl == null || pageUrl.isEmpty())
              {
                String baseUrl = getAppData().getMediaList().getMediaType() == MediaType.tvSeries ? appSettings.getTvBaseUrl() : appSettings.getMovieBaseUrl();
                pageUrl = baseUrl + video.getInternetRef();
              }
              URL url = new URL(pageUrl);
              TextView tv = (TextView) detailView.findViewById(R.id.pageLink);
              String host = url.getHost().startsWith("www") ? url.getHost().substring(4) : url.getHost();
              tv.setText(Html.fromHtml("<a href='" + pageUrl + "'>" + host + "</a>"));
              tv.setMovementMethod(LinkMovementMethod.getInstance());
              tv.setOnClickListener(new OnClickListener()
              {
                public void onClick(View v)
                {
                  ((TextView)v).setBackgroundColor(Color.GRAY);
                }
              });
            }
            catch (MalformedURLException ex)
            {
              if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
              Toast.makeText(pagerActivity, ex.toString(), Toast.LENGTH_LONG).show();
            }
          }
        }
        else
        {
          if (item instanceof TvShow)
          {
            TvShow tvShow = (TvShow) item;
            // summary
            StringBuffer summary = new StringBuffer();
            summary.append(tvShow.getShowTimeInfo());
            if (!tvShow.isShowMovie())
              summary.append(tvShow.getAirDateInfo());
            String details = tvShow.getShowDescription();
            if (details != null)
              summary.append("\n").append(details);
            
            TextView tv = (TextView) detailView.findViewById(R.id.summaryText);
            tv.setText(summary.toString());
          }
        }
            
        Button button = (Button) detailView.findViewById(R.id.pagerPlay);
        button.setVisibility( android.view.View.VISIBLE);
        button.setOnClickListener(new OnClickListener()
        {
          public void onClick(View v)
          {
            Item item = (Item)listable;
            item.setPath(pagerActivity.path);
            pagerActivity.playItem(item);
          }
        });
        
        String artSg;
        if (item.isMusic())
          artSg = appSettings.isAlbumArtAlbumLevel() ? Song.ARTWORK_LEVEL_ALBUM : Song.ARTWORK_LEVEL_SONG;
        else
          artSg = getAppData().getMediaList().getArtworkStorageGroup();
        ArtworkDescriptor art = item.getArtworkDescriptor(artSg);
        if (art != null)
        {
          artworkView = (ImageView) detailView.findViewById(R.id.posterImage);
          try
          {
            String filePath = item.getType() + pagerActivity.path + "/" + art.getArtworkPath();
            Bitmap bitmap = getAppData().getImageBitMap(filePath);
            if (bitmap == null)
            {
              URL url = new URL(appSettings.getMythTvContentServiceBaseUrl() + "/" + art.getArtworkContentServicePath());
              new ImageRetrievalTask(item, art).execute(url);
            }
            else
            {
              artworkView.setImageBitmap(bitmap);
            }
            artworkView.setClickable(true);
            artworkView.setOnClickListener(new View.OnClickListener()
            {
              public void onClick(View v)
              {
                Item item = (Item)listable;
                item.setPath(pagerActivity.path);
                pagerActivity.playItem(item);
              }
            });            
          }
          catch (Exception ex)
          {
            if (BuildConfig.DEBUG)
              Log.e(TAG, ex.getMessage(), ex);
            Toast.makeText(pagerActivity, ex.toString(), Toast.LENGTH_LONG).show();
          }
        }
      }
    }
    
    public class ImageRetrievalTask extends AsyncTask<URL,Integer,Long>
    {
      private Exception ex;
      private String filePath;
      private Bitmap bitmap;
      private Item item;
      private ArtworkDescriptor descriptor;
      
      ImageRetrievalTask(Item item, ArtworkDescriptor descriptor)
      {
        this.item = item;
        this.descriptor = descriptor;
      }
      
      protected Long doInBackground(URL... urls)
      {
        try
        {
          filePath = item.getType() + "/" + pagerActivity.path + "/" + descriptor.getArtworkPath();
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
  protected boolean isDetailView()
  {
    return true;
  }  

}
