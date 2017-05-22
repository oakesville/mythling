package com.oakesville.mythling;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.Reporter;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class WelcomeActivity extends WebViewActivity {

    private static final String TAG = WelcomeActivity.class.getSimpleName();

    static final String WELCOME_PAGE = "file:///android_asset/mythling/welcome.html";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getAppSettings().isFireTv())
            getActionBar().hide();

        getActionBar().setDisplayHomeAsUpEnabled(false);

        getWebView().addJavascriptInterface(new JsHandler(), "jsHandler");
    }

    @Override
    protected String getUrl() throws UnsupportedEncodingException {
        return WELCOME_PAGE;
    }

    protected boolean isJavaScriptEnabled() {
        return true;
    }

    protected class JsHandler {
        @JavascriptInterface
        public void backendHostSubmitted(String host) {
            Log.d(TAG, "backend host: " + host);
            getAppSettings().setInternalBackendHost(host);
            new CheckBackendHost().execute();
        }
    }

    protected class CheckBackendHost extends AsyncTask<URL,Integer,Long> {

        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                URL sgUrl = new URL(getAppSettings().getMythTvServicesBaseUrl() + "/Myth/GetStorageGroupDirs");
                HttpHelper sgDownloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(sgUrl));
                sgDownloader.get();
                return 0L;
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                this.ex = ex;
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                String msg = getString(R.string.unable_to_connect_to_backend_) + ex.getMessage();
                getWebView().loadUrl("javascript:showMessage('" + msg + "')");
            }
            else {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        }
    }
}
