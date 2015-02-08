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
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Reporter;

public class MainActivity extends MediaActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ListView listView;

    public ListView getListView() {
        return listView;
    }

    private int currentTop = 0;  // top item in the list
    private int topOffset = 0;
    protected int selItemIndex = 0;

    private ListableListAdapter adapter;

    public String getCharSet() {
        return mediaList.getCharSet();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSettings.loadDevicePrefsConstraints();

        try {
            getAppSettings().initMythlingVersion();
        } catch (NameNotFoundException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
        }

        if (!getAppSettings().isPrefsInitiallySet()) {
            String msg = "Please access Network settings to initialize connection info.";
            if (getAppSettings().isFireTv()) {
                new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Setup Required")
                .setMessage(msg)
                .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, PrefsActivity.class));
                    }
                })
                .show();
            } else {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }

        setContentView(R.layout.split);

        findViewById(R.id.breadcrumbs).setVisibility(View.GONE);

        createProgressBar();

        String mode = getIntent().getStringExtra("modeSwitch");
        modeSwitch = mode != null;
        if (mode == null)
            mode = getAppSettings().getMediaSettings().getViewType().toString();
        if (ViewType.list.toString().equals(mode))
            goListView();
        else if (ViewType.split.toString().equals(mode))
            goSplitView();

        listView = (ListView) findViewById(R.id.split_cats);

        if (getAppSettings().getMediaSettings().getViewType() == ViewType.detail) {
            Intent intent = new Intent(this, MediaPagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
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
            Toast.makeText(getApplicationContext(), "Bad or missing setting:\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            stopProgress();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        super.onResume();
    }


    public void refresh() throws BadSettingsException {
        currentTop = 0;
        topOffset = 0;
        mediaList = new MediaList();
        adapter = new ListableListAdapter(this, mediaList.getTopCategoriesAndItems().toArray(new Listable[0]));
        listView.setAdapter(adapter);

        startProgress();
        getAppSettings().validate();

        refreshMediaList();
    }

    protected void populate() throws IOException, JSONException, ParseException, BadSettingsException {
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

        adapter = new ListableListAdapter(MainActivity.this, mediaList.getTopCategoriesAndItems().toArray(new Listable[0]));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentTop = listView.getFirstVisiblePosition();
                View topV = listView.getChildAt(0);
                topOffset = (topV == null) ? 0 : topV.getTop();
                selItemIndex = position;
                List<Listable> listables = mediaList.getTopCategoriesAndItems();
                boolean isMediaItem = listables.get(position) instanceof Item;
                if (isMediaItem) {
                    Item item = (Item) listables.get(position);
                    if (isSplitView()) {
                        adapter.setSelection(selItemIndex);
                        listView.setItemChecked(selItemIndex, true);
                        showItemInDetailPane(position);
                    } else {
                        item.setPath("");
                        playItem(item);
                    }
                } else {
                    // must be category
                    String cat = ((TextView) view).getText().toString();
                    if (isSplitView()) {
                        adapter.setSelection(position);
                        listView.setItemChecked(position, true);
                        showSubListPane(cat);
                    } else {
                        Uri.Builder builder = new Uri.Builder();
                        builder.path(cat);
                        Uri uri = builder.build();
                        startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), MediaListActivity.class));
                    }
                }
            }
        });
        updateActionMenu();
        stopProgress();
        listView.setSelectionFromTop(currentTop, topOffset);
        if (isSplitView()) {
            adapter.setSelection(selItemIndex);
            listView.setItemChecked(selItemIndex, true);
            if (selItemIndex != -1) {
                Listable preSel = getItems().get(selItemIndex);
                if (preSel instanceof Item)
                    showItemInDetailPane(selItemIndex);
                else
                    showSubListPane(getPath() + "/" + preSel.getLabel());
            }
        }
    }
}
