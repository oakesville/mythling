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
package com.oakesville.mythling.prefs;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.oakesville.mythling.R;
import com.oakesville.mythling.WebViewActivity;
import com.oakesville.mythling.app.AppSettings;

public class PrefsActivity extends PreferenceActivity {
    private List<Header> headers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle("Settings");
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        this.loadHeadersFromResource(R.xml.prefs_headers, target);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (item.getItemId() == R.id.menu_mythweb) {
            AppSettings appSettings = new AppSettings(getApplicationContext());
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.getMythWebUrl())));
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            String url = getResources().getString(R.string.url_help);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (headers == null) {
            headers = new ArrayList<Header>();
            // when the saved state provides the list of headers, onBuildHeaders() is not called
            // so we build it from the adapter proveded, then use our own adapter

            for (int i = 0; i < adapter.getCount(); i++)
                headers.add((Header) adapter.getItem(i));
        }

        super.setListAdapter(new HeaderListAdapter(this, headers));
    }
}
