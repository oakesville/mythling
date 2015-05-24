/**
 * Copyright 2015 Donald Oakes
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

@SuppressLint("SetJavaScriptEnabled")
public class EpgActivity extends Activity {
    private static final String TAG = EpgActivity.class.getSimpleName();

    private WebView webView;
    private AppSettings appSettings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);

        appSettings = new AppSettings(getApplicationContext());
        if (appSettings.isPhone())
            getActionBar().hide(); // TODO immersive
        else
            getActionBar().setDisplayHomeAsUpEnabled(true);

        webView = (WebView)findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);

        // String url = "file:///android_asset/mythling-epg/guide.html";
        String url = "http://192.168.0.69:6544/mythling-epg/guide.html";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (BuildConfig.DEBUG)
              WebView.setWebContentsDebuggingEnabled(true);
        }
        else {
            // use omb
            // no params: https://code.google.com/p/android/issues/detail?id=17535
            //url = "file:///android_asset/mythling-epg/guide-omb.html";
            url = "http://192.168.0.69:6544/mythling-epg/guide-omb.html";
        }

        if (BuildConfig.DEBUG) {
            webView.setWebChromeClient(new WebChromeClient() {
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.e(TAG, consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n" + consoleMessage.message());
                    return true;
                }
            });
        }

        try {
            webView.loadUrl(url);
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (appSettings.isPhone())
            getMenuInflater().inflate(R.menu.guide_fs, menu);
        else
            getMenuInflater().inflate(R.menu.guide, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_refresh) {
            webView.reload();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, PrefsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            String url = getString(R.string.url_help);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
