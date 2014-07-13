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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefsActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

public class WebViewActivity extends Activity
{
  private static final String TAG = WebViewActivity.class.getSimpleName();
  
  private WebView webView;

  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.webview);
    getActionBar().setDisplayHomeAsUpEnabled(true);

    webView = (WebView) findViewById(R.id.webview);
    
    try
    {
      webView.loadUrl(URLDecoder.decode(getIntent().getDataString(), "UTF-8"));
    }
    catch (UnsupportedEncodingException ex)
    {
      if (BuildConfig.DEBUG)
        Log.e(TAG, ex.getMessage(), ex);
      Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.webview, menu);    
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
    else if (item.getItemId() == R.id.menu_refresh)
    {
      webView.reload();
      return true;
    }    
    else if (item.getItemId() == R.id.menu_settings)
    {
      startActivity(new Intent(this, PrefsActivity.class));
      return true;
    }
    else if (item.getItemId() == R.id.menu_mythweb)
    {
      AppSettings appSettings = new AppSettings(getApplicationContext());
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.getMythWebUrl())));
      return true;
    }
    
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  public void onBackPressed()
  {
    if (webView.canGoBack() == true)
      webView.goBack();
    else
      super.onBackPressed();
  }
  
}
