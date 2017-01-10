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

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONException;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.util.Reporter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import io.oakesville.media.Listable;
import io.oakesville.media.MediaSettings.MediaType;
import io.oakesville.media.MediaSettings.SortType;
import io.oakesville.media.MediaSettings.ViewType;

/**
 * Displays a list of listables (either categories or items).
 */
public class MediaListActivity extends MediaActivity {
    private static final String TAG = MediaListActivity.class.getSimpleName();

    private TextView breadCrumbs;

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

        if (!Localizer.getInstance().getMediaLabel(MediaType.liveTv).equals(getPath()))
            getActionBar().setDisplayHomeAsUpEnabled(true);

        breadCrumbs = (TextView) findViewById(R.id.breadcrumbs);
        breadCrumbs.setText(getPath());
    }

    @Override
    protected void onResume() {
        try {
            if (getAppData() == null || getAppData().isExpired())
                refresh();
            else
                populate();
        } catch (Exception ex) {
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

        if (Localizer.getInstance().getMediaLabel(MediaType.liveTv).equals(getPath())) {
            String bc = getString(R.string.tv) + " (" + mediaList.getRetrieveDateDisplay() + " " + mediaList.getRetrieveTimeDisplay() + ")";
            breadCrumbs.setText(bc);
        }

        setListAdapter(new ListableListAdapter(MediaListActivity.this, getListables().toArray(new Listable[0]), isTv()));

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
            handleEmptyMediaList(); // can happen when item deleted from detail frag in split mode
        }
    }

    protected void handleEmptyMediaList() {
        // go to main
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
        SortType sortType = getAppSettings().getMediaSettings().getSortType();
        if (getMediaType() == MediaType.recordings && (sortType == SortType.byDate || sortType == SortType.byRating))
            setPath(""); // recordings list will be flattened
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
