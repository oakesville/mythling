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
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.amazon.android.webkit.AmazonConsoleMessage;
import com.amazon.android.webkit.AmazonWebChromeClient;
import com.amazon.android.webkit.AmazonWebKitFactories;
import com.amazon.android.webkit.AmazonWebKitFactory;
import com.amazon.android.webkit.AmazonWebResourceResponse;
import com.amazon.android.webkit.AmazonWebView;
import com.amazon.android.webkit.AmazonWebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class FireTvEpgActivity extends EpgActivity {
    private static final String TAG = FireTvEpgActivity.class.getSimpleName();

    private static boolean factoryInited = false;

    private AmazonWebKitFactory factory;
    private AmazonWebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            public boolean shouldOverrideUrlLoading(AmazonWebView view, String url) {
                boolean should = super.shouldOverrideUrlLoading(view, url);
                return should;
            }

            @Override
            public AmazonWebResourceResponse shouldInterceptRequest(AmazonWebView webview, String url) {
                AmazonWebResourceResponse response = super.shouldInterceptRequest(webview, url);
                return response;
//                if (!getScale().equals("1.0") && getUrl().equals(url))
//                    return new AmazonWebResourceResponse("text/html", "UTF-8", getGuideInputStreamScaled(url, getScale()));
//                else
//                    return super.shouldInterceptRequest(webview, url);
            }
        });

        if (BuildConfig.DEBUG) {
            webView.setWebChromeClient(new AmazonWebChromeClient() {
                public boolean onConsoleMessage(AmazonConsoleMessage consoleMessage) {
                    Log.e(TAG, consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "\n" + consoleMessage.message());
                    return true;
                }
            });
        }
    }

    @Override
    protected boolean useDefaultWebView() {
        return false;
    }

    @Override
    protected String getScale() {
        return "1.5";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//            if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
//                System.out.println("LEFT...");
//                return false;
//            }
//            else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
//                System.out.println("RIGHT...");
//                webView.loadUrl("javascript:arrow('right')");
//                return true;
//            }
//        }
//        return super.dispatchKeyEvent(event);
//    }


}

