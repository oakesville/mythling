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

import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.oakesville.mythling.util.Reporter;

public class EpgActivity extends WebViewActivity {
    private static final String TAG = EpgActivity.class.getSimpleName();

    static final String INTERNAL_BASE_URL = "file:///android_asset/";
    static final String VIEWPORT
      = "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0,maximum-scale=1.0,minimum-scale=1.0,user-scalable=no\" />";

    @Override
    protected String getUrl() throws UnsupportedOperationException {
        boolean external = false; // TODO settings
        String externalBaseUrl = "http://192.168.0.69:6544/"; // TODO settings

        String baseUrl = external ? externalBaseUrl : INTERNAL_BASE_URL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT || getAppSettings().isFireTv()) {
        	return baseUrl + "mythling-epg/guide.html";
        }
        else {
            // no params: https://code.google.com/p/android/issues/detail?id=17535
            return baseUrl + "mythling-epg/guide-omb.html";
        }
    }

    protected String getScale() {
        return "1.0";
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
            getMenuInflater().inflate(R.menu.guide_fs, menu);
        else
            getMenuInflater().inflate(R.menu.guide, menu);
        return true;
    }

    protected InputStream getGuideInputStreamScaled(String url, String scale) {
        try {
            InputStream inStream = getAssets().open(url, AssetManager.ACCESS_STREAMING);
            StringBuilder strBuf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String str;
            while ((str=in.readLine()) != null)
              strBuf.append(str).append('\n');
            in.close();
            int idx = strBuf.indexOf(VIEWPORT);
            if (idx > 0)
                strBuf = new StringBuilder(strBuf.substring(0, idx)).append(strBuf.substring(idx));
            return new ByteArrayInputStream(strBuf.toString().getBytes());
        }
        catch (IOException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            return null;
        }
    }
}
