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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends Activity {
    private static final String TAG = WebViewActivity.class.getSimpleName();

    private WebView webView;
    protected WebView getWebView() { return webView; }
    private AppSettings appSettings;
    protected AppSettings getAppSettings() { return appSettings; }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = new AppSettings(getApplicationContext());
        if (appSettings.isPhone())
            getActionBar().hide(); // TODO immersive
        else
            getActionBar().setDisplayHomeAsUpEnabled(true);

        if (useDefaultWebView()) {

            setContentView(R.layout.webview);
            webView = (WebView) findViewById(R.id.webview);

            if (isJavaScriptEnabled())
            	webView.getSettings().setJavaScriptEnabled(true);

            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            webView.getSettings().setUseWideViewPort(true);

            if (supportZoom()) {
                webView.getSettings().setSupportZoom(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setDisplayZoomControls(false);
            }

            if (BuildConfig.DEBUG) {
                // allow debugging with chrome dev tools
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    WebView.setWebContentsDebuggingEnabled(true);

                // print javascript console output
                webView.setWebChromeClient(new WebChromeClient() {
                    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                        Log.e(TAG, consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n" + consoleMessage.message());
                        return true;
                    }
                });

                // do not cache in debug
                webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            }

            if (!appSettings.deviceSupportsWebLinks()) {
                webView.setWebViewClient(new WebViewClient() {
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            webView.loadUrl(getUrl());
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected boolean useDefaultWebView() {
        return true;
    }

    protected boolean isJavaScriptEnabled() {
    	return false;
    }

    protected boolean supportZoom() {
    	return true;
    }

    protected String getUrl() throws UnsupportedEncodingException {
        return URLDecoder.decode(getIntent().getDataString(), "UTF-8");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_refresh && useDefaultWebView()) {
            webView.reload();
            return true;
        } else if (item.getItemId() == R.id.menu_guide) {
            startActivity(new Intent(this, appSettings.isFireTv() ? FireTvEpgActivity.class : EpgActivity.class));
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

    @Override
    public void onBackPressed() {
        if (useDefaultWebView() && webView.canGoBack() == true)
            webView.goBack();
        else
            super.onBackPressed();
    }

}
