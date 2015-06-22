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
package com.oakesville.mythling.firetv;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.widget.Toast;

import com.amazon.android.webkit.AmazonConsoleMessage;
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
import com.oakesville.mythling.util.Reporter;

@SuppressLint("SetJavaScriptEnabled")
public class FireTvEpgActivity extends EpgActivity {
    private static final String TAG = FireTvEpgActivity.class.getSimpleName();

    private static final String EPG_JS = "<script src=\"js/epg.js\"></script>";
    private static final String EPG_FIRETV_JS = "<script src=\"js/epg-firetv.js\"></script>";
    private static final String MYTHLING_CSS = "<link rel=\"stylesheet\" href=\"css/mythling.css\">";
    private static final String MYTHLING_FIRETV_CSS = "<link rel=\"stylesheet\" href=\"css/mythling-firetv.css\">";

    private static boolean factoryInited = false;

    private AmazonWebKitFactory factory;
    private AmazonWebView webView;
    private boolean popupOpen = false;
    private JsHandler jsHandler;

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

        webView.setWebViewClient(new AmazonWebViewClient() {
            @Override
            public AmazonWebResourceResponse shouldInterceptRequest(AmazonWebView view, String url) {
                if (url.startsWith(getEpgBaseUrl())) {
                    String localPath = AppSettings.MYTHLING_EPG + url.substring(getEpgBaseUrl().length());
                    if (localPath.indexOf('?') > 0)
                        localPath = localPath.substring(0, localPath.indexOf('?'));
                    String contentType = getLocalContentType(localPath);
                    if (!getScale().equals("1.0") && url.startsWith(getUrl()))
                        return new AmazonWebResourceResponse(contentType, "UTF-8", getLocalGuideScaled(localPath));
                    else
                        return new AmazonWebResourceResponse(contentType, "UTF-8", getLocalAsset(localPath));
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(AmazonWebView view, String url) {
                super.onPageFinished(view, url);
                popupOpen = false;
            }
        });

        jsHandler = new JsHandler();
        webView.addJavascriptInterface(jsHandler, "jsHandler");

        if (BuildConfig.DEBUG) {
            webView.setWebChromeClient(new AmazonWebChromeClient() {
                public boolean onConsoleMessage(AmazonConsoleMessage consoleMessage) {
                    Log.e(TAG, consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n" + consoleMessage.message());
                    return true;
                }
            });

            // do not cache in debug
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    protected InputStream getLocalGuideScaled(String path) {
        try {
            InputStream inStream = getAssets().open(path, AssetManager.ACCESS_STREAMING);
            StringBuilder strBuf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null) {
                if (str.equals(VIEWPORT))
                    strBuf.append(str.replaceAll("1\\.0", getScale()));
                else if (str.equals(EPG_JS))
                    strBuf.append(str).append('\n').append(EPG_FIRETV_JS).append('\n');
                else if (str.equals(MYTHLING_CSS))
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_REWIND) {
                // next day
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                // previous day
                return true;
            }
            else if (popupOpen) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    webView.loadUrl("javascript:closePopups()");
                    popupOpen = false;
                    return true;
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

    class JsHandler {
        @JavascriptInterface
        public void setPopupOpen(boolean isOpen) {
            popupOpen = isOpen;
        }
    }
}

