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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.HttpAuthHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.ChannelGroup;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.Reporter;

public class EpgActivity extends WebViewActivity {
    private static final String TAG = EpgActivity.class.getSimpleName();

    protected static final String VIEWPORT
      = "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,user-scalable=no\">";

    AppData appData;

    // refreshed from appSettings in onResume()
    private String epgUrl;
    private String epgBaseUrl;
    protected String getEpgBaseUrl() { return epgBaseUrl; }
    private String scale;
    private Map<String,String> parameters;
    protected long lastLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appData = new AppData(getApplicationContext());

        try {
            String channelGroup = getAppSettings().getEpgChannelGroup();
            if ((appData.getChannelGroups() != null || appData.readChannelGroups() != null)
                    && appData.getChannelGroups().get(channelGroup) == null)
                appData.clearChannelGroups(); // force re-retrieve
        }
        catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            appData.clearChannelGroups();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }

        if (savedInstanceState != null)
            getWebView().restoreState(savedInstanceState);
        else
            getAppSettings().setEpgLastLoad(0);
        if (useDefaultWebView()) {
            getWebView().setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (url.startsWith(epgBaseUrl)) {
                        if (getAppSettings().isHostedEpg()) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Loading hosted: " + url);
                            if (!getScale().equals("1.0") && url.startsWith(getUrl())) {
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
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Loading embedded: " + url);
                            String localPath = AppSettings.MYTHLING_EPG + url.substring(epgBaseUrl.length());
                            if (localPath.indexOf('?') > 0)
                                localPath = localPath.substring(0, localPath.indexOf('?'));
                            String contentType = getLocalContentType(localPath);
                            if (!getScale().equals("1.0") && url.startsWith(getUrl()))
                                return new WebResourceResponse(contentType, "UTF-8", getLocalGuideScaled(localPath));
                            else
                                return new WebResourceResponse(contentType, "UTF-8", getLocalAsset(localPath));
                        }
                    }
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Loading: " + url);
                    return super.shouldInterceptRequest(view, url);
                }

                @Override
                public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                    if (getAppSettings().isMythlingMediaServices())
                        handler.proceed(getAppSettings().getBackendWebUser(), getAppSettings().getBackendWebPassword());
                    else
                        handler.proceed(getAppSettings().getMythTvServicesUser(), getAppSettings().getMythTvServicesPassword());
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    lastLoad = System.currentTimeMillis();
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    System.out.println("Failed loading: " + failingUrl + " (" + errorCode + ": " + description + ")");
                    Log.e(TAG, "Failed loading: " + failingUrl + " (" + errorCode + ": " + description + ")");
                    super.onReceivedError(view, errorCode, description, failingUrl);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        getAppSettings().setEpgLastLoad(lastLoad);
        super.onPause();
    }

    @Override
    protected void onResume() {
        lastLoad = getAppSettings().getEpgLastLoad();
        try {
            epgBaseUrl = getAppSettings().getEpgBaseUrl().toString();
            epgUrl = getAppSettings().getEpgUrl().toString();
            scale = getAppSettings().getEpgScale();
            parameters = new HashMap<String,String>();
            String epgParams = getAppSettings().getEpgParams();
            if (epgParams != null && epgParams.length() > 0) {
                if (epgParams.startsWith("?"))
                    epgParams = epgParams.substring(1);
                for (String param : epgParams.split("&")) {
                    int eq = param.indexOf('=');
                    if (eq > 0 && param.length() > eq + 1)
                        parameters.put(param.substring(0, eq), param.substring(eq + 1));
                }
            }
            if (!parameters.containsKey("mythlingServices")) { // honor explicitly-set parameter first
                if (getAppSettings().isMythlingMediaServices())
                    parameters.put("mythlingServices", "true");
                else
                    parameters.remove("mythlingServices");
            }
            String channelGroup = getAppSettings().getEpgChannelGroup();
            if (channelGroup == null || channelGroup.isEmpty()) {
                parameters.remove("channelGroupId");
            }
            else {
                Map<String,ChannelGroup> channelGroups = appData.getChannelGroups();
                if (channelGroups == null)
                    channelGroups = appData.readChannelGroups();
                if (channelGroups == null) {
                    // needs async channel group retrieval
                    epgUrl = null; // postpone load in super.onResume()
                    new PopulateChannelGroupParamTask().execute((URL)null);
                }
                else {
                    ChannelGroup group = channelGroups.get(channelGroup);
                    parameters.put("channelGroupId", group == null ? "999" : group.getId());
                }
            }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            backToMythling();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, PrefsActivity.class);
            intent.putExtra(PrefsActivity.BACK_TO, this.getClass().getName());
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            String url = getString(R.string.url_help);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class);
            intent.putExtra(WebViewActivity.BACK_TO, this.getClass().getName());
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        backToMythling();
    }

    protected void backToMythling() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        getWebView().saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        getWebView().restoreState(savedInstanceState);
    }

    @Override
    protected boolean shouldReload() {
        lastLoad = getAppSettings().getEpgLastLoad();
        if (lastLoad > 0) {
            // if crossed the next half-hour threshold
            Calendar llCal = Calendar.getInstance();
            llCal.setTime(new Date(lastLoad));
            int mins = llCal.get(Calendar.MINUTE);
            if (mins >= 30)
                llCal.set(Calendar.MINUTE, 30);
            else
                llCal.set(Calendar.MINUTE, 0);
            return System.currentTimeMillis() - llCal.getTimeInMillis() >= 1800000;
        } else {
            return true;
        }
    }

    @Override
    protected Map<String,String> getParameters() {
        if ("0".equals(parameters.get("channelGroupId"))) {
            // couldn't find channel group; exclude
            Map<String,String> params = new HashMap<String,String>();
            for (String key : parameters.keySet()) {
                if (!"channelGroupId".equals(key))
                    params.put(key, parameters.get(key));
            }
            return params;
        }
        else {
            return parameters;
        }
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
        if (getAppSettings().isPhone() || getAppSettings().isTv())
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
                if (str.trim().equals(VIEWPORT))
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

    protected class PopulateChannelGroupParamTask extends AsyncTask<URL,Integer,Long> {
        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                String url = getAppSettings().getMythTvServicesBaseUrlWithCredentials() + "/Guide/GetChannelGroupList";
                Log.d(TAG, "Retrieving channel groups: " + url);
                HttpHelper helper = new HttpHelper(new URL[]{new URL(url)}, getAppSettings().getMythTvServicesAuthType(), getAppSettings().getPrefs());
                helper.setCredentials(getAppSettings().getMythTvServicesUser(), getAppSettings().getMythTvServicesPassword());
                String json = new String(helper.get());
                new AppData(getApplicationContext()).writeChannelGroups(json);
                return 0L;
            } catch (Exception ex) {
                try {
                    new AppData(getApplicationContext()).writeChannelGroups("{}"); // prevent infinite re-retrieve
                    this.ex = ex;
                }
                catch (Exception ex2) {
                    this.ex = ex2;
                }
                if (BuildConfig.DEBUG)
                    Log.e(TAG, this.ex.getMessage(), this.ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(this.ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L && ex != null && !(ex instanceof IOException)) // IOException for 0.27
                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
            onResume();
        }
    }
}
