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
package com.oakesville.mythling.prefs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.oakesville.mythling.R;
import com.oakesville.mythling.WebViewActivity;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.util.Reporter;

import java.util.ArrayList;
import java.util.List;

public class PrefsActivity extends AppCompatPreferenceActivity {
    public static final String BACK_TO = "back_to";
    private static final String TAG = PrefsActivity.class.getSimpleName();

    private List<Header> headers;
    private String backTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewGroup rootView = (ViewGroup)findViewById(R.id.action_bar_root); // id from appcompat

        if (rootView != null) {
            View view = getLayoutInflater().inflate(R.layout.settings_bar, rootView, false);
            rootView.addView(view, 0);

            Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        backTo = getIntent().getStringExtra(BACK_TO);
    }

    public void setActionBarTitle(int titleRes) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(titleRes);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        int headerResource = R.xml.prefs_headers;
        AppSettings appSettings = new AppSettings(getApplicationContext());
        DevicePrefsSpec deviceConstraints = appSettings.getDevicePrefsConstraints();
        if (deviceConstraints != null && deviceConstraints.getPrefsHeadersResource() != 0)
            headerResource = deviceConstraints.getPrefsHeadersResource();
        loadHeadersFromResource(headerResource, target);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getTitle().equals(getString(R.string.title_activity_prefs))) {
                NavUtils.navigateUpFromSameTask(this);
            } else {
                Intent intent = new Intent(this, PrefsActivity.class);
                startActivity(intent);
            }
            return true;
        } else if (item.getItemId() == R.id.menu_help) {
            String url = getResources().getString(R.string.url_help);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (headers == null) {
            headers = new ArrayList<Header>();
            // when the saved state provides the list of headers, onBuildHeaders() is not called
            // so we build it from the adapter provided, then use our own adapter

            for (int i = 0; i < adapter.getCount(); i++)
                headers.add((Header) adapter.getItem(i));
        }

        super.setListAdapter(new HeaderListAdapter(this, headers));
    }

    @Override
    public void onBackPressed() {
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
