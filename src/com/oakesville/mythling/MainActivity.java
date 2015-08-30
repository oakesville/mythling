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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.firetv.FireTvEpgActivity;
import com.oakesville.mythling.media.Listable;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

public class MainActivity extends MediaActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView listView;
    public ListView getListView() { return listView; }

    private String backTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getAppSettings().initialize();
        } catch (NameNotFoundException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
        }

        if (!getAppSettings().isPrefsInitiallySet()) {
            String msg = getString(R.string.access_network_settings);
            if (getAppSettings().isTv()) {
                new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.setup_required))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.go_to_settings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, PrefsActivity.class));
                    }
                })
                .show();
            } else {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }

        setContentView(getAppSettings().isTv() ? R.layout.firetv_split : R.layout.split);

        findViewById(R.id.breadcrumbs).setVisibility(View.GONE);

        createProgressBar();

        backTo = getIntent().getStringExtra("back_to");

        setPathFromIntent();

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

        if (getAppSettings().getMediaSettings().getViewType() == ViewType.detail) {
            Intent intent = new Intent(this, MediaPagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (backTo != null)
                intent.putExtra("back_to", backTo);
            startActivity(intent);
            finish();
            return;
        }

        listView = (ListView) findViewById(R.id.split_cats);

    }

    @Override
    protected void onResume() {
        try {
            if (getAppData() == null || getAppData().isExpired())
                refresh();
            else
                populate();
        } catch (BadSettingsException ex) {
            stopProgress();
            Toast.makeText(getApplicationContext(), getString(R.string.bad_setting_) + "\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            stopProgress();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }

        super.onResume();
    }

    public void onBackPressed() {
        if (EpgActivity.class.getName().equals(backTo)|| FireTvEpgActivity.class.getName().equals(backTo)) {
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        } else {
            super.onBackPressed();
        }
    }

    public void refresh() {
        super.refresh();
        mediaList = new MediaList();
        setListAdapter(new ListableListAdapter(this, mediaList.getTopCategoriesAndItems().toArray(new Listable[0])));

        startProgress();
        getAppSettings().validate();

        refreshMediaList();
    }

    protected void populate() throws IOException, JSONException, ParseException {
        startProgress();
        if (getAppData() == null) {
            AppData appData = new AppData(getApplicationContext());
            appData.readMediaList(getMediaType());
            setAppData(appData);
        } else if (getMediaType() != null && getMediaType() != getAppData().getMediaList().getMediaType()) {
            // media type was changed, then back button was pressed
            getAppSettings().setMediaType(getMediaType());
            getAppSettings().setLastLoad(0);
            onResume();
        }

        mediaList = getAppData().getMediaList();
        storageGroups = getAppData().getStorageGroups();
        if (getPath() != null && !getPath().isEmpty()) {
            // proceed to MediaListActivity
            Uri uri = new Uri.Builder().path(getPath()).build();
            startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), MediaListActivity.class));
            finish();
            return;
        }

        setListAdapter(new ListableListAdapter(MainActivity.this, getListables().toArray(new Listable[0])));

        initListViewOnItemClickListener();
        initListViewOnItemSelectedListener();
        initListViewDpadHandler();
        registerForContextMenu(listView);

        stopProgress();
        updateActionMenu();

        if (getListables().size() > 0) {
            listView.setSelectionFromTop(getCurrentTop(), getTopOffset());
            getListAdapter().setSelection(getSelItemIndex());
            getListView().setSelection(getSelItemIndex());

            if (isSplitView())
                initSplitView();
            listView.requestFocus();
        } else {
            handleEmptyMediaList();
        }
    }
}
