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

// XXX vitamio
//import io.vov.vitamio.MediaPlayer;
//import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
//import io.vov.vitamio.MediaPlayer.OnCompletionListener;
//import io.vov.vitamio.MediaPlayer.OnErrorListener;
//import io.vov.vitamio.widget.MediaController;
//import io.vov.vitamio.widget.VideoView;

//import java.net.URLDecoder;

import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.res.Configuration;
//import android.net.TrafficStats;
//import android.os.Bundle;
//import android.widget.Toast;
//import android.util.Log;
//import com.oakesville.mythling.app.AppSettings;

/**
 * This is the embedded video viewer which uses the proprietary Vitamio library (http://www.vitamio.org/en).
 * Search for and uncomment all code sections flagged with 'XXX vitamio' to enable this playback,
 * which is disabled by default due to Vitamio licensing restrictions.
 */
public class VideoActivity extends Activity
{
  public static final String TAG = VideoActivity.class.getSimpleName();

//  private String path;
//  private VideoView videoView;
//  private long myTrafficStartBytes;
//  private long allTrafficStartBytes;
//  private boolean loggedBufFullStats;
//
//  @Override
//  public void onCreate(Bundle savedInstanceState)
//  {
//    super.onCreate(savedInstanceState);
//    if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
//      return;
//
//    setContentView(R.layout.videoview);
//    videoView = (VideoView) findViewById(R.id.surface_view);
//    
//    myTrafficStartBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
//    allTrafficStartBytes = TrafficStats.getTotalRxBytes();
//
//    try
//    {
//      path = URLDecoder.decode(getIntent().getDataString(), "UTF-8");
//      path = path.replaceAll(" ", "%20");
//      
//      if (BuildConfig.DEBUG)
//        Log.d(TAG, "Video path: " + path);
//      
//      videoView.setVideoURI(null);
//      videoView.setVideoPath(path);
//      videoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
//      videoView.setOnCompletionListener(new OnCompletionListener()
//      {
//        public void onCompletion(MediaPlayer mp)
//        {
//          long myTrafficEndBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
//          long myTrafficKb = ((myTrafficEndBytes - myTrafficStartBytes)/1024);
//          if (BuildConfig.DEBUG)
//            Log.d(TAG, "my traffic kb: " + myTrafficKb);
//          long allTrafficEndBytes = TrafficStats.getTotalRxBytes();
//          long allTrafficKb = ((allTrafficEndBytes - allTrafficStartBytes)/1024);
//          if (BuildConfig.DEBUG)
//            Log.d(TAG, "all traffic kb: " + allTrafficKb);
//          onBackPressed();
//        }
//      });
//      videoView.setOnBufferingUpdateListener(new OnBufferingUpdateListener ()
//      {
//        @Override
//        public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent)
//        {
//          if (percent > 98 && !loggedBufFullStats)
//          {
//            long myTrafficBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid());
//            long myTrafficKb = ((myTrafficBytes - myTrafficStartBytes)/1024);
//            if (BuildConfig.DEBUG)
//              Log.d(TAG, "buff full: my traffic kb: " + myTrafficKb);
//            long allTrafficBytes = TrafficStats.getTotalRxBytes();
//            long allTrafficKb = ((allTrafficBytes - allTrafficStartBytes)/1024);
//            if (BuildConfig.DEBUG)
//              Log.d(TAG, "buff full: all traffic kb: " + allTrafficKb);
//            loggedBufFullStats = true;
//          }
//        }
//      });
//      final Context ctx = this; 
//      videoView.setOnErrorListener(new OnErrorListener()
//      {
//        public boolean onError(MediaPlayer mp, int what, int extra)
//        {
//          String msg = what + "/" + extra;
//          if (BuildConfig.DEBUG)
//            Log.e(TAG, "Playback Error: " + msg);
//          new AlertDialog.Builder(ctx).setTitle("Playback Error").setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener()
//          {
//            public void onClick(DialogInterface dialog, int whichButton)
//            {
//              onBackPressed();
//            }
//          }).setCancelable(false).show();
//          return true;
//        }
//      });
//      
//      // TODO: this is not honored
//      videoView.setBufferSize(new AppSettings(getApplicationContext()).getBuiltInPlayerBufferSize() * 1024);
//      
//      MediaController mediaController = new MediaController(this);
//      videoView.setMediaController(mediaController);
//      if (BuildConfig.DEBUG)
//        Log.d(TAG, "Playback started");
//      while (videoView.isBuffering())
//      {
//        if (BuildConfig.DEBUG)
//          Log.d(TAG, "Buffering...");
//        Thread.sleep(100);
//      }
//    }
//    catch (Exception ex)
//    {
//      if (BuildConfig.DEBUG)
//        Log.e(TAG, ex.getMessage(), ex);
//      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();      
//    }
//  }
//
//  @Override
//  public void onConfigurationChanged(Configuration newConfig)
//  {
//    if (videoView != null)
//      videoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
//    super.onConfigurationChanged(newConfig);
//  }
}
