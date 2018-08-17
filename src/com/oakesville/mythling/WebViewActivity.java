/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.firetv.FireTvEpgActivity;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = WebViewActivity.class.getSimpleName();
    public static final String BACK_TO = "back_to";
    protected static final String CONSOLE_ERROR_TAG = "ERROR: "; // must match epg.js

    private WebView webView;
    protected WebView getWebView() { return webView; }
    private AppSettings appSettings;
    protected AppSettings getAppSettings() { return appSettings; }
    private String backTo;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = new AppSettings(getApplicationContext());
        if (appSettings.isPhone())
            getSupportActionBar().hide(); // TODO immersive
        else
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        backTo = getIntent().getStringExtra(BACK_TO);

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
                WebView.setWebContentsDebuggingEnabled(true);
                // do not cache in debug
                webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            }

            // print javascript console output
            webView.setWebChromeClient(new WebChromeClient() {
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    if (consoleMessage.message().startsWith(CONSOLE_ERROR_TAG)) {
                        String msg = consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n -> " + consoleMessage.message();
                        Log.e(TAG, msg);
                        if (appSettings.isErrorReportingEnabled())
                            new Reporter(msg).send();
                        Toast.makeText(getApplicationContext(), consoleMessage.message(), Toast.LENGTH_LONG).show();
                        return true;
                    }
                    else if (BuildConfig.DEBUG) {
                        Log.i(TAG, consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n -> " + consoleMessage.message());
                        return true;
                    }
                    else {
                        return super.onConsoleMessage(consoleMessage);
                    }
                }
            });


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
        if (shouldReload()) {
            if (useDefaultWebView()) {
                try {
                    if (getUrl() != null) { // null url indicates don't load yet
                        String url = getUrl() + getParams();
                        Log.d(TAG, "Loading URL: " + url);
                        webView.loadUrl(url);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (appSettings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected boolean shouldReload() {
        return true;
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
        return super.onCreateOptionsMenu(menu);
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
        if (useDefaultWebView() && webView.canGoBack()) {
            webView.goBack();
        }
        else {
            if (backTo != null) {
                try {
                    startActivity(new Intent(this, Class.forName(backTo)));
                    return;
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (new AppSettings(getApplicationContext()).isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
            super.onBackPressed();
        }
    }

    protected Map<String,String> getParameters() {
        return null;
    }

    protected String getParams() throws UnsupportedEncodingException {
        StringBuffer params = new StringBuffer();
        Map<String,String> parameters = getParameters();
        if (parameters != null) {
            boolean hasOne = false;
            for (String key : parameters.keySet()) {
                if (!hasOne) {
                    params.append("?");
                    hasOne = true;
                }
                else {
                    params.append("&");
                }
                params.append(key);
                params.append("=");
                params.append(URLEncoder.encode(parameters.get(key), "UTF-8"));
            }
        }
        return params.toString();
    }
}
