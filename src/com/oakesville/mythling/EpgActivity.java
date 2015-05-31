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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.Reporter;

public class EpgActivity extends WebViewActivity {
    private static final String TAG = EpgActivity.class.getSimpleName();

    static final String VIEWPORT
      = "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,maximum-scale=1.0,minimum-scale=1.0,user-scalable=no\" />";

    // refreshed from appSettings in onResume()
    private String epgUrl;
    private String epgBaseUrl;
    protected String getEpgBaseUrl() { return epgBaseUrl; }
    private String scale;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (useDefaultWebView()) {
            getWebView().setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (url.startsWith(epgBaseUrl)) {
                        if (getAppSettings().isHostedEpg()) {
                            Log.d(TAG, "Loading hosted: " + url);
                            if (!getScale().equals("1.0") && getUrl().equals(url)) {
                                InputStream responseStream = null;
                                WebResourceResponse response = super.shouldInterceptRequest(view, url);
                                if (response == null) {
                                    try {
                                        HttpHelper helper = new HttpHelper(new URL[]{new URL(url)}, getAppSettings().getMythTvServicesAuthType(), getAppSettings().getPrefs());
                                        responseStream = getHostedGuideScaled(new ByteArrayInputStream(helper.get()));
                                    }
                                    catch (Exception ex) {
                                        if (BuildConfig.DEBUG)
                                            Log.e(TAG, ex.getMessage(), ex);
                                        if (getAppSettings().isErrorReportingEnabled())
                                            new Reporter(ex).send();
                                        return response;
                                    }
                                }
                                else
                                    responseStream = getHostedGuideScaled(response.getData());
                                return new WebResourceResponse("text/html", "UTF-8", responseStream);
                            }
                        }
                        else {
                            Log.d(TAG, "Loading embedded: " + url);
                            String localPath = AppSettings.MYTHLING_EPG + url.substring(epgBaseUrl.length());
                            String contentType = getLocalContentType(localPath);
                            if (!getScale().equals("1.0") && getUrl().equals(url))
                                return new WebResourceResponse(contentType, "UTF-8", getLocalGuideScaled(localPath));
                            else
                                return new WebResourceResponse(contentType, "UTF-8", getLocalAsset(localPath));
                        }
                    }
                    return super.shouldInterceptRequest(view, url);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        try {
            epgBaseUrl = getAppSettings().getEpgBaseUrl().toString();
            epgUrl = getAppSettings().getEpgUrl().toString();
            scale = getAppSettings().getEpgScale();
        }
        catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
        super.onResume();
    }


    @Override
    protected String getUrl() {
        return epgUrl;
    }

    protected String getScale() {
        return scale;
    }

    @Override
    protected boolean isJavaScriptEnabled() {
        return true;
    }

    @Override
    protected boolean supportZoom() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getAppSettings().isPhone())
            getMenuInflater().inflate(R.menu.guide_fs, menu); // otherwise menu items hidden
        else
            getMenuInflater().inflate(R.menu.guide, menu);
        return true;
    }

    protected InputStream getLocalGuideScaled(String path) {
        try {
            InputStream inStream = getAssets().open(path, AssetManager.ACCESS_STREAMING);
            StringBuilder strBuf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null) {
                if (str.equals(VIEWPORT))
                    strBuf.append(str.replaceAll("1\\.0", scale));
                else
                    strBuf.append(str);
                strBuf.append('\n');
            }
            in.close();
            return new ByteArrayInputStream(strBuf.toString().getBytes());
        }
        catch (IOException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            return null;
        }
    }

    protected InputStream getHostedGuideScaled(InputStream responseStream) {
        if (responseStream == null)
            return null;
        try {
            StringBuilder strBuf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null) {
                if (str.equals(VIEWPORT))
                    strBuf.append(str.replaceAll("1\\.0", scale));
                else
                    strBuf.append(str);
                strBuf.append('\n');
            }
            in.close();
            return new ByteArrayInputStream(strBuf.toString().getBytes());
        }
        catch (IOException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            return null;
        }
    }

    protected InputStream getLocalAsset(String path) {
        try {
            return getAssets().open(path, AssetManager.ACCESS_STREAMING);
        }
        catch (IOException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            return null;
        }
    }

    protected String getLocalContentType(String path) {
        if (path.endsWith(".html"))
            return "text/html";
        else if (path.endsWith(".js"))
            return "application/javascript";
        else if (path.endsWith(".css"))
            return "text/css";
        else if (path.endsWith(".png"))
            return "image/png";
        else
            return "text/plain";
    }
}
