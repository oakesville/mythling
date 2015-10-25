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
package com.oakesville.mythling.firetv;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;

import com.amazon.android.webkit.AmazonConsoleMessage;
import com.amazon.android.webkit.AmazonUrlRequest;
import com.amazon.android.webkit.AmazonWebChromeClient;
import com.amazon.android.webkit.AmazonWebKitFactories;
import com.amazon.android.webkit.AmazonWebKitFactory;
import com.amazon.android.webkit.AmazonWebResourceResponse;
import com.amazon.android.webkit.AmazonWebView;
import com.amazon.android.webkit.AmazonWebViewClient;
import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.EpgActivity;
import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.prefs.PrefDismissDialog;
import com.oakesville.mythling.util.Reporter;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class FireTvEpgActivity extends EpgActivity {
    private static final String TAG = FireTvEpgActivity.class.getSimpleName();

    private static final String EPG_FIRETV_JS = "<script src=\"js/epg-firetv.js\"></script>";
    private static final String MYTHLING_FIRETV_CSS = "<link rel=\"stylesheet\" href=\"css/mythling-firetv.css\">";

    private static final String SHOW_GUIDE_HINT_PREF = "show_guide_hint";
    private static final String SHOW_SEARCH_HINT_PREF = "show_search_hint";

    private static boolean factoryInited = false;

    private AmazonWebKitFactory factory;
    private AmazonWebView webView;

    private int skipInterval;
    private Calendar startTime;
    private boolean showGuideHint;
    private boolean showSearchHint;

    private int menuItemFromBtm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.firetv_webview);

        if (!factoryInited) {
            factory = AmazonWebKitFactories.getDefaultFactory();
            if (factory.isRenderProcess(this)) {
                return; // do nothing if this is on render process
            }
            factory.initialize(this.getApplicationContext());
            factoryInited = true;
        } else {
            factory = AmazonWebKitFactories.getDefaultFactory();
        }

        webView = (AmazonWebView)findViewById(R.id.firetv_webview);
        factory.initializeWebView(webView, 0xFFFFFF, false, null);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setUseWideViewPort(true);

        if (savedInstanceState != null)
            webView.restoreState(savedInstanceState);
        else
            getAppSettings().setEpgLastLoad(0);

        webView.setWebViewClient(new AmazonWebViewClient() {

            @Override
            public AmazonWebResourceResponse shouldInterceptRequest(AmazonWebView view, String url) {
                if (getEpgBaseUrl() == null)
                    populateParams();
                if (url.startsWith(getEpgBaseUrl())) {
                    String localPath = AppSettings.MYTHLING_EPG + url.substring(getEpgBaseUrl().length());
                    if (localPath.indexOf('?') > 0)
                        localPath = localPath.substring(0, localPath.indexOf('?'));
                    String contentType = getLocalContentType(localPath);
                    if (!getScale().equals("1.0") && url.startsWith(getUrl()))
                        return new AmazonWebResourceResponse(contentType, "UTF-8", getLocalGuide(localPath));
                    else
                        return new AmazonWebResourceResponse(contentType, "UTF-8", getLocalAsset(localPath));
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onCompletedUrlRequest(AmazonUrlRequest request) {
                super.onCompletedUrlRequest(request);
                // wait until ajax request is complete before showing any hints
                if (isGuideServiceUrl(request.getUrl())) {
                    if (showGuideHint) {
                        // don't show again for this session regardless of pref setting
                        showGuideHint = false;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                String title = getString(R.string.epg_hint);
                                String msg = getString(R.string.epg_ff_rew_hint_) + " " +
                                        getAppSettings().getEpgSkipInterval() + " " + getString(R.string.abbrev_hrs) + ".";
                                PrefDismissDialog hintDlg = new PrefDismissDialog(getAppSettings(), title, msg, SHOW_GUIDE_HINT_PREF);
                                hintDlg.show(getFragmentManager(), SHOW_GUIDE_HINT_PREF);
                            }
                        });
                    }
                    else if (showSearchHint && getPopups().contains("search")) {
                        // don't show again for this session regardless of pref setting
                        showSearchHint = false;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                String title = getString(R.string.epg_hint);
                                final String msg = getString(R.string.epg_search_res_hint);
                                PrefDismissDialog hintDlg = new PrefDismissDialog(getAppSettings(), title, msg, SHOW_SEARCH_HINT_PREF);
                                hintDlg.show(getFragmentManager(), SHOW_SEARCH_HINT_PREF);
                            }
                        });
                    }
                }
            }

            @Override
            public void onPageFinished(AmazonWebView view, String url) {
                super.onPageFinished(view, url);
                setLastLoad(System.currentTimeMillis());
                popups = new ArrayList<String>();
                menuItemFromBtm = 0;
            }
        });

        webView.addJavascriptInterface(new JsHandler(), "jsHandler");

        if (BuildConfig.DEBUG) {
            // do not cache in debug
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

        webView.setWebChromeClient(new AmazonWebChromeClient() {
            public boolean onConsoleMessage(AmazonConsoleMessage consoleMessage) {
                if (consoleMessage.message().startsWith(CONSOLE_ERROR_TAG)) {
                    String msg = consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n -> " + consoleMessage.message();
                    Log.e(TAG, msg);
                    if (getAppSettings().isErrorReportingEnabled())
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTime = Calendar.getInstance();
        startTime.set(Calendar.MINUTE, startTime.get(Calendar.MINUTE) >= 30 ? 30 : 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        skipInterval = getAppSettings().getEpgSkipInterval();
        showGuideHint = getAppSettings().getBooleanPref(SHOW_GUIDE_HINT_PREF, true);
        showSearchHint = getAppSettings().getBooleanPref(SHOW_SEARCH_HINT_PREF, true);

        load();
    }

    private void load() {
        try {
            webView.loadUrl(getUrl() + getParams());
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String getParams() throws UnsupportedEncodingException {
        String startDateParam = Localizer.getIsoDateFormat().format(startTime.getTime());
        return super.getParams() + "&guideInterval=" + skipInterval + "&startTime=" + startDateParam;
    }

    @Override
    protected boolean useDefaultWebView() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected InputStream getLocalGuide(String path) {
        try {
            InputStream inStream = getAssets().open(path, AssetManager.ACCESS_STREAMING);
            StringBuilder strBuf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null) {
                if (str.trim().equals(VIEWPORT) && !getScale().equals("1.0"))
                    strBuf.append(str.replaceAll("1\\.0", getScale()));
                else if (str.trim().equals(EPG_JS)) {
                    strBuf.append(str).append('\n').append(EPG_DEVICE_JS).append('\n');
                    strBuf.append(str).append('\n').append(EPG_FIRETV_JS).append('\n');
                }
                else if (str.trim().equals(MYTHLING_CSS))
                    strBuf.append(str).append('\n').append(MYTHLING_FIRETV_CSS).append('\n');
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

    private boolean isGuideServiceUrl(String url) {
        try {
            return url != null && url.startsWith(getAppSettings().getGuideServiceUrl().toString());
        }
        catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * However, dispatchKeyEvent() takes precedence if popup != null.
     */
    @Override
    public void onBackPressed() {
        backToMythling();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_REWIND) {
                if (getPopups().contains("search")) {
                    webView.loadUrl("javascript:searchBackward()");
                }
                else {
                    startTime.add(Calendar.HOUR, -skipInterval);
                    load();
                    return true;
                }
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                if (getPopups().contains("search")) {
                    webView.loadUrl("javascript:searchForward()");
                }
                else {
                    startTime.add(Calendar.HOUR, skipInterval);
                    load();
                    return true;
                }
            }
            else if (getPopups() != null && !getPopups().isEmpty()) {
                if (getPopups().contains("menu") || getPopups().contains("details")) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        webView.loadUrl("javascript:closePopup()");
                        return true;
                    }
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true; // not allowed
                    }
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true; // not allowed
                    }
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                        if (getPopups().contains("details") || menuItemFromBtm == menuItems - 1) {
                            return true; // no further
                        }
                        else {
                            menuItemFromBtm++;
                            return super.dispatchKeyEvent(event);
                        }
                    }
                    else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (getPopups().contains("details") || menuItemFromBtm == 0) {
                            return true; // no further
                        }
                        else {
                            menuItemFromBtm--;
                            return super.dispatchKeyEvent(event);
                        }
                    }
                }
            }
            else {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                    webView.loadUrl("javascript:webViewKey('left')");
                    return true;
                }
                else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    webView.loadUrl("javascript:webViewKey('right')");
                    return true;
                }
                else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    webView.loadUrl("javascript:webViewKey('up')");
                    return true;
                }
                else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    webView.loadUrl("javascript:webViewKey('down')");
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

}

