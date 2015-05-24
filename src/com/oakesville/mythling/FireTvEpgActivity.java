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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.amazon.android.webkit.AmazonConsoleMessage;
import com.amazon.android.webkit.AmazonWebChromeClient;
import com.amazon.android.webkit.AmazonWebKitFactories;
import com.amazon.android.webkit.AmazonWebKitFactory;
import com.amazon.android.webkit.AmazonWebView;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

@SuppressLint("SetJavaScriptEnabled")
public class FireTvEpgActivity extends Activity {
    private static final String TAG = FireTvEpgActivity.class.getSimpleName();

    private static boolean factoryInited = false;

    private AmazonWebKitFactory factory;
    private AmazonWebView webView;
    private AppSettings appSettings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firetv_webview);

        if (!factoryInited) {
            factory = AmazonWebKitFactories.getDefaultFactory();
            if (factory.isRenderProcess(this)) {
                return; // Do nothing if this is on render process
            }
            factory.initialize(this.getApplicationContext());

//            if (BuildConfig.DEBUG && factory.getWebKitCapabilities().isDeveloperToolsSupported()) {
//                //factory.enableDeveloperToolsUnix(this.getPackageName() + ".devtools");
//                factory.enableDeveloperToolsTcp(9223);
//            }

            // factory configuration is done here, for example:
            // factory.getCookieManager().setAcceptCookie(true);
            factoryInited = true;
        } else {
            factory = AmazonWebKitFactories.getDefaultFactory();
        }

        appSettings = new AppSettings(getApplicationContext());
        getActionBar().setDisplayHomeAsUpEnabled(true);

        webView = (AmazonWebView)findViewById(R.id.firetv_webview);
        factory.initializeWebView(webView, 0xFFFFFF, false, null);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);

        String url = "http://192.168.0.69:6544/mythling-epg/guide.html";

//        webView.getSettings().setLoadWithOverviewMode(false);
//        webView.getSettings().setSupportZoom(false);
//        webView.setInitialScale(200);

        if (BuildConfig.DEBUG) {
            webView.setWebChromeClient(new AmazonWebChromeClient() {
                public boolean onConsoleMessage(AmazonConsoleMessage consoleMessage) {
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

