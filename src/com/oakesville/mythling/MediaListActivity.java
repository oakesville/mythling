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
package com.oakesville.mythling;

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONException;

import android.app.FragmentBreadCrumbs;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.media.Listable;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.util.Reporter;

/**
 * Displays a list of listables (either categories or items).
 */
public class MediaListActivity extends MediaActivity {
    private static final String TAG = MediaListActivity.class.getSimpleName();

    private FragmentBreadCrumbs breadCrumbs;

    private ListView listView;
    public ListView getListView() { return listView; }

    public String getCharSet() {
        return mediaList.getCharSet();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getAppSettings().isFireTv() ? R.layout.firetv_split : R.layout.split);

        createProgressBar();

        setPathFromIntent();

        listView = (ListView) findViewById(R.id.split_cats);

        setSelItemIndex(getIntent().getIntExtra(SEL_ITEM_INDEX, 0));
        setTopOffset(getIntent().getIntExtra(TOP_OFFSET, 0));
        setCurrentTop(getIntent().getIntExtra(CURRENT_TOP, 0));

        String mode = getIntent().getStringExtra(MODE_SWITCH);
        modeSwitch = mode != null;
        if (mode == null)
            mode = getAppSettings().getMediaSettings().getViewType().toString();
        if (ViewType.list.toString().equals(mode))
            goListView();
        else if (ViewType.split.toString().equals(mode))
            goSplitView();

        if (!MediaSettings.getMediaTitle(MediaType.liveTv).equals(getPath()))
            getActionBar().setDisplayHomeAsUpEnabled(true);

        breadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
        breadCrumbs.setActivity(this);
        breadCrumbs.setTitle(getPath(), getPath());
    }

    @Override
    protected void onResume() {
        try {
            if (getAppData() == null || getAppData().isExpired())
                refresh();
            else
                populate();
        } catch (Exception ex) {
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
        if (item.getItemId() == R.id.view_list && getAppSettings().getMediaSettings().getViewType() == ViewType.split
                || item.getItemId() == R.id.view_split && getAppSettings().getMediaSettings().getViewType() == ViewType.list)
            modeSwitch = true;  // in case back button to MainActivity
        return super.onOptionsItemSelected(item);
    }

    public void populate() throws IOException, JSONException, ParseException {
        if (getAppData() == null) {
            startProgress();
            AppData appData = new AppData(getApplicationContext());
            appData.readMediaList(getMediaType());
            setAppData(appData);
            stopProgress();
        } else if (getMediaType() != null && getMediaType() != getAppData().getMediaList().getMediaType()) {
            // media type was changed, then back button was pressed
            getAppSettings().setMediaType(getMediaType());
            refresh();
            return;
        }
        mediaList = getAppData().getMediaList();
        storageGroups = getAppData().getStorageGroups();
        setMediaType(mediaList.getMediaType());

        if (MediaSettings.getMediaTitle(MediaType.liveTv).equals(getPath())) {
            String title = getString(R.string.tv) + " (" + mediaList.getRetrieveDateDisplay() + " " + mediaList.getRetrieveTimeDisplay() + ")";
            breadCrumbs.setTitle(title, title);
        }

        setListAdapter(new ListableListAdapter(MediaListActivity.this, getListables().toArray(new Listable[0])));

        initListViewOnItemClickListener();
        initListViewOnItemSelectedListener();
        initListViewDpadHandler();
        registerForContextMenu(listView);

        updateActionMenu();

        if (getListables().size() > 0) {
            listView.setSelectionFromTop(getCurrentTop(), getTopOffset());
            getListAdapter().setSelection(getSelItemIndex());
            getListView().setSelection(getSelItemIndex());

            if (isSplitView())
                initSplitView();
            listView.requestFocus();
        } else {
            handleEmptyMediaList(); // really shouldn't happen
        }
    }

    public void refresh() {
        super.refresh();
        getAppSettings().setLastLoad(0);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void sort() throws IOException, JSONException, ParseException {
        super.refresh();
        getAppSettings().setLastLoad(0);
        Uri uri = new Uri.Builder().path(getPath()).build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (modeSwitch) {
            modeSwitch = false;
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
