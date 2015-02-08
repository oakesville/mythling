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
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;

import org.json.JSONException;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.util.Reporter;

/**
 * Activity for side-scrolling through paged detail views of MythTV media w/artwork.
 */
public class MediaPagerActivity extends MediaActivity {
    private static final String TAG = MediaPagerActivity.class.getSimpleName();

    private String path;
    String getPath() { return path; }
    private ViewPager pager;
    private MediaPagerAdapter pagerAdapter;
    private List<Listable> items;
    List<Listable> getItems() { return items; }
    private int currentPosition;
    private SeekBar positionBar;

    public String getCharSet() {
        return mediaList.getCharSet();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);

        createProgressBar();

        pager = (ViewPager) findViewById(R.id.pager);

        try {
            String newPath = getIntent().getDataString();
            if (newPath == null)
                path = "";
            else
                path = URLDecoder.decode(newPath, "UTF-8");

            modeSwitch = getIntent().getBooleanExtra("modeSwitch", false);

            getActionBar().setDisplayHomeAsUpEnabled(!path.isEmpty());
        } catch (Exception ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }
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
            Toast.makeText(getApplicationContext(), "Error: " + ex.toString(), Toast.LENGTH_LONG).show();
        }

        super.onResume();
    }

    public void populate() throws IOException, JSONException, ParseException, BadSettingsException {
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
        setMediaType(mediaList.getMediaType());

        items = mediaList.getListables(path);

        pagerAdapter = new MediaPagerAdapter(getFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageSelected(int position) {
                currentPosition = position;
                getAppSettings().setPagerCurrentPosition(mediaList.getMediaType(), path, currentPosition);
                positionBar.setProgress(currentPosition + 1);
                TextView curItemView = (TextView) findViewById(R.id.currentItem);
                curItemView.setText(String.valueOf(currentPosition + 1));
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageScrollStateChanged(int state) {
            }
        });

        positionBar = (SeekBar) findViewById(R.id.pagerPosition);
        positionBar.setMax(items.size());
        positionBar.setProgress(1);
        positionBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                pager.setCurrentItem(currentPosition);
            }
        });

        ImageButton button = (ImageButton) findViewById(R.id.gotoFirst);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pager.setCurrentItem(0);
                positionBar.setProgress(1);
            }
        });
        button = (ImageButton) findViewById(R.id.gotoLast);
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                pager.setCurrentItem(items.size() - 1);
                positionBar.setProgress(items.size());
            }
        });

        TextView tv = (TextView) findViewById(R.id.lastItem);
        tv.setText(String.valueOf(items.size()));

        updateActionMenu();

        currentPosition = getAppSettings().getPagerCurrentPosition(mediaList.getMediaType(), path);
        if (items.size() > currentPosition) {
            pager.setCurrentItem(currentPosition);
            positionBar.setProgress(currentPosition);
            TextView curItemView = (TextView) findViewById(R.id.currentItem);
            if (curItemView != null)
                curItemView.setText(String.valueOf(currentPosition + 1));
        } else {
            getAppSettings().setPagerCurrentPosition(mediaList.getMediaType(), path, 0);
        }
    }

    public void refresh() throws BadSettingsException {
        path = "";
        mediaList = new MediaList();

        startProgress();
        getAppSettings().validate();

        refreshMediaList();
    }

    protected void goListView() {
        goListView("list");
    }

    protected void goSplitView() {
        goListView("split");
    }

    protected void goListView(String mode) {
        if (mediaList.getMediaType() == MediaType.recordings && getAppSettings().getMediaSettings().getSortType() == SortType.byTitle)
            getAppSettings().clearCache(); // refresh since we're switching from flattened hierarchy

        if (path == null || path.isEmpty()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("modeSwitch", mode);
            startActivity(intent);
        } else {
            Uri.Builder builder = new Uri.Builder();
            builder.path(path);
            Uri uri = builder.build();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), MediaListActivity.class);
            intent.putExtra("modeSwitch", mode);
            startActivity(intent);
        }
    }

    public ListView getListView() {
        return null;
    }

    @Override
    public void onBackPressed() {
        if (modeSwitch) {
            modeSwitch = false;
            Intent intent = new Intent(this, MediaPagerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private class MediaPagerAdapter extends FragmentPagerAdapter {
        public MediaPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public int getCount() {
            return items.size();
        }

        public Fragment getItem(int position) {
            Fragment frag = new ItemDetailFragment();
            Bundle args = new Bundle();
            args.putInt("idx", position);
            frag.setArguments(args);
            return frag;
        }
    }

    @Override
    public void sort() throws IOException, JSONException, ParseException {
        super.sort();
        pager.setCurrentItem(0);
        positionBar.setProgress(1);
    }

}
