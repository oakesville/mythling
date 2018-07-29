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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.oakesville.mythling.VideoPlaybackDialog.PlaybackDialogListener;
import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.firetv.FireTvEpgActivity;
import com.oakesville.mythling.media.AllTunersInUseException;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.PlaybackOptions;
import com.oakesville.mythling.media.PlaybackOptions.PlaybackOption;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.prefs.PrefDismissDialog;
import com.oakesville.mythling.prefs.PrefDismissDialog.PrefDismissListener;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.Downloader;
import com.oakesville.mythling.util.FrontendPlayer;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.HttpHelper.AuthType;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MediaStreamProxy;
import com.oakesville.mythling.util.MediaStreamProxy.ProxyInfo;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.Recorder;
import com.oakesville.mythling.util.Reporter;
import com.oakesville.mythling.util.ServiceFrontendPlayer;
import com.oakesville.mythling.util.SocketFrontendPlayer;
import com.oakesville.mythling.util.Transcoder;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import io.oakesville.media.Category;
import io.oakesville.media.Cut;
import io.oakesville.media.Download;
import io.oakesville.media.Item;
import io.oakesville.media.Listable;
import io.oakesville.media.LiveStreamInfo;
import io.oakesville.media.MediaSettings;
import io.oakesville.media.MediaSettings.MediaType;
import io.oakesville.media.MediaSettings.SortType;
import io.oakesville.media.MediaSettings.ViewType;
import io.oakesville.media.Recording;
import io.oakesville.media.StorageGroup;
import io.oakesville.media.TvShow;

/**
 * Base class for the different ways to view collections of MythTV media.
 */
public abstract class MediaActivity extends ActionBarActivity {
    private static final String TAG = MediaActivity.class.getSimpleName();

    static final String DETAIL_FRAGMENT = "detailFragment";
    static final String LIST_FRAGMENT = "listFragment";
    static final int MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID = 1;
    static final int LIST_FRAGMENT_CONTEXT_MENU_GROUP_ID = 2;
    /**
     * from R.array.item_long_click_menu
     */
    static final int LONG_CLICK_MENU_PLAY = 0;
    static final int LONG_CLICK_MENU_TRANSCODE = 1;
    static final int LONG_CLICK_MENU_DOWNLOAD = 2;
    static final int LONG_CLICK_MENU_DELETE = 3;

    static final String PATH = "path";
    static final String SEL_ITEM_INDEX = "idx";
    static final String CURRENT_TOP = "curTop";
    static final String TOP_OFFSET = "topOff";
    static final String GRAB_FOCUS = "grab";
    static final String MODE_SWITCH = "modeSwitch";

    private ListableListAdapter listAdapter;
    ListableListAdapter getListAdapter() { return listAdapter; }
    void setListAdapter(ListableListAdapter listAdapter) {
        this.listAdapter = listAdapter;
        getListView().setAdapter(listAdapter);
    }

    private String path;
    protected String getPath() { return path; }
    protected void setPath(String path) { this.path = path; }

    protected List<Listable> getListables() {
        if (mediaList == null)  // FIXME: issue #68
            return new ArrayList<Listable>();
        return mediaList.getListables(getPath());
    }
    protected List<Listable> getListables(String path) {
        if (mediaList == null)  // FIXME: issue #68
            return new ArrayList<Listable>();
        return mediaList.getListables(path);
    }

    private int currentTop = 0;  // top item in the list
    int getCurrentTop() { return currentTop; }
    void setCurrentTop(int currentTop) { this.currentTop = currentTop; }

    private int topOffset = 0;
    int getTopOffset() { return topOffset; }
    void setTopOffset(int topOffset) { this.topOffset = topOffset; }

    private int selItemIndex = 0;
    int getSelItemIndex() { return selItemIndex; }
    void setSelItemIndex(int selItemIndex) { this.selItemIndex = selItemIndex; }

    // these need to be retained after orientation change, so their kept with AppData
    protected MediaList mediaList;
    protected Map<String,StorageGroup> storageGroups;

    private static AppData appData;
    public static AppData getAppData() { return appData; }
    public static void setAppData(AppData data) { appData = data;}

    private AppSettings appSettings;
    public AppSettings getAppSettings() { return appSettings; }

    // it seems there's a reason we keep a ref separate from appSettings.getMediaSettings()
    // (see MainActivity/MediaListActivity/MediaPagerActivity.populate())
    private MediaType mediaType;
    protected MediaType getMediaType() { return mediaType; }
    protected void setMediaType(MediaType mt) { this.mediaType = mt; }

    public String getCharSet() {
        return "UTF-8";
    }

    private MenuItem mediaMenuItem;
    private MenuItem searchMenuItem;
    private MenuItem viewMenuItem;
    private MenuItem sortMenuItem;
    private MenuItem moviesMenuItem;
    private MenuItem tvSeriesMenuItem;
    private MenuItem musicMenuItem;
    private MenuItem mythwebMenuItem;
    private MenuItem stopMenuItem;

    private ProgressBar progressBar;

    private ProgressDialog countdownDialog;
    private int count;
    private Timer timer;

    protected boolean modeSwitch; // tracking for back button

    public void refresh() {
        currentTop = 0;
        topOffset = 0;
        selItemIndex = 0;
    }

    public abstract ListView getListView();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = new AppSettings(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (supportsMusic())
          registerPlaybackReceiver();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "HELLO PAUSE");
        unregisterPlaybackReceiver();
        if (downloadReceivers != null) {
            List<Long> idsToRemove = new ArrayList<Long>();
            for (long downloadId : downloadReceivers.keySet()) {
                unregisterDownloadReceiver(downloadId);
                idsToRemove.add(downloadId);
            }
            for (long id : idsToRemove)
                downloadReceivers.remove(id);
        }
        super.onPause();
    }

    private BroadcastReceiver playbackReceiver;
    private void registerPlaybackReceiver() {
        if (playbackReceiver == null) {
            playbackReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    showStopMenuItem(false);
                }
            };
        }
        registerReceiver(playbackReceiver, new IntentFilter(MusicPlaybackService.ACTION_PLAYBACK_STOPPED));
    }
    private void unregisterPlaybackReceiver() {
        if (playbackReceiver != null) { // null if music not supported
            unregisterReceiver(playbackReceiver);
            playbackReceiver = null;
        }
    }

    private class DownloadBroadcastReceiver extends BroadcastReceiver {
        private Item item;
        private long downloadId;
        public DownloadBroadcastReceiver(Item item, long downloadId) {
            this.item = item;
            this.downloadId = downloadId;
        }
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (downloadId == id) {
                    item.setDownloadId(downloadId);
                    unregisterDownloadReceiver(downloadId);
                    if (downloadReceivers != null) // how else?
                        downloadReceivers.remove(downloadId);
                    onResume();
                }
            }
        }
    }
    private Map<Long,DownloadBroadcastReceiver> downloadReceivers;
    private void registerDownloadReceiver(Item item, long downloadId) {
        if (downloadReceivers == null)
            downloadReceivers = new HashMap<Long,DownloadBroadcastReceiver>();
        DownloadBroadcastReceiver receiver = downloadReceivers.get(downloadId);
        if (receiver != null) {
            unregisterDownloadReceiver(downloadId);
            downloadReceivers.remove(downloadId);
        }
        receiver = new DownloadBroadcastReceiver(item, downloadId);
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadReceivers.put(downloadId, receiver);
    }
    private void unregisterDownloadReceiver(long downloadId) {
        if (downloadReceivers != null) { // how otherwise?
            DownloadBroadcastReceiver receiver = downloadReceivers.get(downloadId);
            if (receiver != null)
                unregisterReceiver(receiver);
        }
    }

    protected ProgressBar createProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        progressBar.setScaleX(0.20f);
        progressBar.setScaleY(0.20f);
        return progressBar;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MediaSettings mediaSettings = appSettings.getMediaSettings();

        mediaMenuItem = menu.findItem(R.id.menu_media);
        if (mediaMenuItem != null) {
            if (mediaList != null)
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(mediaSettings.getType()) + " (" + mediaList.getCount() + ")");
            else
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(mediaSettings.getType()));
            if (mediaSettings.isMusic())
                mediaMenuItem.getSubMenu().findItem(R.id.media_music).setChecked(true);
            else if (mediaSettings.isLiveTv())
                mediaMenuItem.getSubMenu().findItem(R.id.media_tv).setChecked(true);
            else if (mediaSettings.isMovies())
                mediaMenuItem.getSubMenu().findItem(R.id.media_movies).setChecked(true);
            else if (mediaSettings.isTvSeries())
                mediaMenuItem.getSubMenu().findItem(R.id.media_tv_series).setChecked(true);
            else if (mediaSettings.isVideos())
                mediaMenuItem.getSubMenu().findItem(R.id.media_videos).setChecked(true);
            else
                mediaMenuItem.getSubMenu().findItem(R.id.media_recordings).setChecked(true);

            moviesMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_movies);
            showMoviesMenuItem(supportsMovies());
            tvSeriesMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_tv_series);
            showTvSeriesMenuItem(supportsTvSeries());
            musicMenuItem = mediaMenuItem.getSubMenu().findItem(R.id.media_music);
            showMusicMenuItem(supportsMusic());
        }

        searchMenuItem = menu.findItem(R.id.menu_search);
        showSearchMenu(supportsSearch());

        sortMenuItem = menu.findItem(R.id.menu_sort);
        showSortMenu(supportsSort());

        viewMenuItem = menu.findItem(R.id.menu_view);
        showViewMenu(supportsViewMenu());

        stopMenuItem = menu.findItem(R.id.menu_stop);
        showStopMenuItem(isPlayingMusic());

        return super.onPrepareOptionsMenu(menu);
    }

    protected void showSearchMenu(boolean show) {
        if (searchMenuItem != null) {
            searchMenuItem.setEnabled(show);
            searchMenuItem.setVisible(show);
        }
    }

    protected void showMoviesMenuItem(boolean show) {
        if (moviesMenuItem != null) {
            moviesMenuItem.setEnabled(show);
            moviesMenuItem.setVisible(show);
        }
    }

    protected void showTvSeriesMenuItem(boolean show) {
        if (tvSeriesMenuItem != null) {
            tvSeriesMenuItem.setEnabled(show);
            tvSeriesMenuItem.setVisible(show);
        }
    }

    protected void showMusicMenuItem(boolean show) {
        if (musicMenuItem != null) {
            musicMenuItem.setEnabled(show);
            musicMenuItem.setVisible(show);
        }
    }

    protected void showStopMenuItem(boolean show) {
        if (stopMenuItem != null) {
            stopMenuItem.setEnabled(show);
            stopMenuItem.setVisible(show);
        }
    }

    protected void showViewMenu(boolean show) {
        if (viewMenuItem != null) {
            MediaSettings mediaSettings = appSettings.getMediaSettings();
            if (show) {
                viewMenuItem.setIcon(getViewIcon());
                if (mediaSettings.getViewType() == ViewType.detail)
                    viewMenuItem.getSubMenu().findItem(R.id.view_detail).setChecked(true);
                else if (mediaSettings.getViewType() == ViewType.split)
                    viewMenuItem.getSubMenu().findItem(R.id.view_split).setChecked(true);
                else
                    viewMenuItem.getSubMenu().findItem(R.id.view_list).setChecked(true);
            }

            viewMenuItem.setEnabled(show);
            viewMenuItem.setVisible(show);
        }
    }

    private Drawable getViewIcon() {
        return getResources().getDrawable(getResources().getIdentifier(
                "ic_menu_" + appSettings.getMediaSettings().getViewIcon(),
                "drawable", AppSettings.PACKAGE));
    }

    protected void showMythwebMenu(boolean show) {
        if (mythwebMenuItem != null) {
            mythwebMenuItem.setEnabled(show);
            mythwebMenuItem.setVisible(show);
        }
    }

    protected void showSortMenu(boolean show) {
        if (sortMenuItem != null) {
            if (show) {
                MediaSettings mediaSettings = appSettings.getMediaSettings();
                sortMenuItem.setTitle(Localizer.getInstance().getSortLabel(mediaSettings.getSortType()));
                boolean isLiveTv = mediaSettings.getType() == MediaType.liveTv;
                sortMenuItem.getSubMenu().findItem(R.id.sort_byTitle).setVisible(!isLiveTv);
                sortMenuItem.getSubMenu().findItem(R.id.sort_byDate).setVisible(!isLiveTv);
                sortMenuItem.getSubMenu().findItem(R.id.sort_byRating).setVisible(!isLiveTv);
                sortMenuItem.getSubMenu().findItem(R.id.sort_byChannel).setVisible(isLiveTv);
                sortMenuItem.getSubMenu().findItem(R.id.sort_byCallsign).setVisible(isLiveTv);
                if (isLiveTv) {
                    if (mediaSettings.getSortType() == SortType.byCallsign)
                        sortMenuItem.getSubMenu().findItem(R.id.sort_byCallsign).setChecked(true);
                    else
                        sortMenuItem.getSubMenu().findItem(R.id.sort_byChannel).setChecked(true);
                }
                else {
                    if (mediaSettings.getSortType() == SortType.byDate)
                        sortMenuItem.getSubMenu().findItem(R.id.sort_byDate).setChecked(true);
                    else if (mediaSettings.getSortType() == SortType.byRating)
                        sortMenuItem.getSubMenu().findItem(R.id.sort_byRating).setChecked(true);
                    else
                        sortMenuItem.getSubMenu().findItem(R.id.sort_byTitle).setChecked(true);
                }
            }
            sortMenuItem.setEnabled(show);
            sortMenuItem.setVisible(show);
        }
    }

    protected boolean supportsSearch() {
        return getAppSettings().isMythlingMediaServices();
    }

    protected boolean supportsSort() {
        if (mediaList == null)
            return false;
        if (getAppSettings().isMythlingMediaServices() && mediaList.getMediaType() == MediaType.videos)
            return false;  // temporarily until issue #29 is fixed
        return mediaList != null && mediaList.supportsSort();
    }

    protected boolean supportsViewMenu() {
        return mediaList != null && mediaList.canHaveArtwork();
    }

    protected boolean supportsMovies() {
        return getAppSettings().isVideosCategorization();
    }

    protected boolean supportsTvSeries() {
        return getAppSettings().isVideosCategorization();
    }

    protected boolean supportsMusic() {
        return true;
    }

    protected boolean supportsMythwebMenu() {
        return getAppSettings().isMythWebAccessEnabled();
    }

    protected boolean isListView() {
        return appSettings.getMediaSettings().getViewType() == ViewType.list;
    }

    protected boolean isDetailView() {
        return appSettings.getMediaSettings().getViewType() == ViewType.detail;
    }

    protected boolean isSplitView() {
        return appSettings.getMediaSettings().getViewType() == ViewType.split;
    }

    protected boolean isTv() {
        // appSettings can be null during orientation change
        return appSettings == null ? false : appSettings.isTv();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getAppSettings().isFireTv())
            getMenuInflater().inflate(R.menu.firetv_main, menu);
        else
            getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (modeSwitch)
                onBackPressed();
            else
                NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        try {
            if (sortMenuItem != null)
                sortMenuItem.setTitle(Localizer.getInstance().getSortLabel(appSettings.getMediaSettings().getSortType()));
            if (viewMenuItem != null)
                viewMenuItem.setIcon(getViewIcon());

            if (item.getItemId() == R.id.media_music) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.music);
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.music));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.media_videos) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.videos);
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.videos));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.media_recordings) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.recordings);  // clears view type
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.recordings));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.media_tv) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.liveTv);  // clears view type
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.liveTv));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.media_movies) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.movies);
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.movies));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.media_tv_series) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.tvSeries);
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(MediaType.tvSeries));
                showViewMenu(supportsViewMenu());
                showSortMenu(supportsSort());
                ViewType newView = appSettings.getMediaSettings().getViewType();
                if (oldView != newView)
                    applyViewChange(oldView, newView);
                else
                    refresh();
                return true;
            } else if (item.getItemId() == R.id.sort_byTitle) {
                appSettings.setSortType(SortType.byTitle);
                item.setChecked(true);
                sortMenuItem.setTitle(R.string.menu_byTitle);
                sort();
                return true;
            } else if (item.getItemId() == R.id.sort_byDate) {
                appSettings.setSortType(SortType.byDate);
                item.setChecked(true);
                sortMenuItem.setTitle(R.string.menu_byDate);
                sort();
                return true;
            } else if (item.getItemId() == R.id.sort_byRating) {
                appSettings.setSortType(SortType.byRating);
                item.setChecked(true);
                sortMenuItem.setTitle(R.string.menu_byRating);
                sort();
                return true;
            } else if (item.getItemId() == R.id.sort_byChannel) {
                appSettings.setSortType(SortType.byChannel);
                item.setChecked(true);
                sortMenuItem.setTitle(R.string.menu_byChannel);
                sort();
                return true;
            } else if (item.getItemId() == R.id.sort_byCallsign) {
                appSettings.setSortType(SortType.byCallsign);
                item.setChecked(true);
                sortMenuItem.setTitle(R.string.menu_byCallsign);
                sort();
                return true;
            } else if (item.getItemId() == R.id.view_list) {
                appSettings.setViewType(ViewType.list);
                item.setChecked(true);
                viewMenuItem.setIcon(R.drawable.ic_menu_list);
                goListView();
                return true;
            } else if (item.getItemId() == R.id.view_detail) {
                appSettings.setViewType(ViewType.detail);
                item.setChecked(true);
                viewMenuItem.setIcon(R.drawable.ic_menu_detail);
                goDetailView();
                return true;
            } else if (item.getItemId() == R.id.view_split) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setViewType(ViewType.split);
                item.setChecked(true);
                viewMenuItem.setIcon(R.drawable.ic_menu_split);
                goSplitView();
                if (oldView == ViewType.list)
                    initSplitView();
                return true;
            } else if (item.getItemId() == R.id.menu_refresh) {
                refresh();
                return true;
            } else if (item.getItemId() == R.id.menu_settings) {
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_search) {
                return onSearchRequested();
            } else if (item.getItemId() == R.id.menu_guide) {
                startActivity(new Intent(this, appSettings.isFireTv() ? FireTvEpgActivity.class : EpgActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_help) {
                String url = getString(R.string.url_help);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url), getApplicationContext(), WebViewActivity.class));
                return true;
            } else if (item.getItemId() == R.id.menu_stop) {
                Intent stopMusic = new Intent(this, MusicPlaybackService.class);
                stopMusic.setAction(MusicPlaybackService.ACTION_STOP);
                startService(stopMusic);
                return true;
            }
        } catch (BadSettingsException ex) {
            stopProgress();
            Toast.makeText(getApplicationContext(), getString(R.string.bad_setting_) + "\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            stopProgress();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    protected void applyViewChange(ViewType oldView, ViewType newView) {
        if (newView == ViewType.detail && oldView != ViewType.detail) {
            getAppSettings().clearCache();  // refresh after nav
            goDetailView();
        } else if (oldView == ViewType.detail && newView != ViewType.detail) {
            getAppSettings().clearCache();  // refresh after nav
            if (newView == ViewType.split)
                goSplitView();
            else
                goListView();
        } else if (oldView != newView) {
            if (newView == ViewType.split)
                goSplitView();
            else
                goListView();
            refresh();
        }
    }

    protected void showItemInDetailPane(int position) {
        showItemInDetailPane(position, false);
    }

    protected void showItemInDetailPane(int position, boolean grabFocus) {
        ItemDetailFragment detailFragment = new ItemDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(SEL_ITEM_INDEX, position);
        arguments.putBoolean(GRAB_FOCUS, grabFocus);
        detailFragment.setArguments(arguments);
        // FIXME: Issue #69
        getFragmentManager().beginTransaction().replace(R.id.detail_container, detailFragment, DETAIL_FRAGMENT).commit();
    }

    protected void showSubListPane(String path) {
        showSubListPane(path, -1, false);
    }

    protected void showSubListPane(String path, boolean grabFocus) {
        showSubListPane(path, -1, grabFocus);
    }

    protected void showSubListPane(String path, int selIdx) {
        showSubListPane(path, selIdx, false);
    }

    protected void showSubListPane(String path, int selIdx, boolean grabFocus) {
        ItemListFragment listFragment = new ItemListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(PATH, path);
        arguments.putInt(SEL_ITEM_INDEX, selIdx);
        arguments.putBoolean(GRAB_FOCUS, grabFocus);
        listFragment.setArguments(arguments);
        getFragmentManager().beginTransaction().replace(R.id.detail_container, listFragment, LIST_FRAGMENT).commit();
    }

    protected void playItem(final Item item) {
        try {
            if (appSettings.isDevicePlayback()) {
                if (item.isMusic()) {
                    Uri uri;
                    if (item.isDownloaded()) {
                        uri = getDownload(item);
                    }
                    else {
                        String base = appSettings.getMythTvServicesBaseUrlWithCredentials() + "/Content/";
                        if (appSettings.isMythlingMediaServices()) {
                            uri = Uri.parse(base + "GetMusic?Id=" + item.getId());
                        }
                        else {
                            uri = Uri.parse(base + "GetFile?StorageGroup=" + appSettings.getMusicStorageGroup()
                                        + "&FileName=" + URLEncoder.encode(item.getFilePath(), "UTF-8"));
                        }
                    }
                    if (appSettings.isExternalMusicPlayer()) {
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "audio/*");
                        startActivity(intent);
                    }
                    else {
                        startProgress();
                        Toast.makeText(getApplicationContext(), getString(R.string.playing) + ": " + item.getTitle(), Toast.LENGTH_LONG).show();
                        Intent playMusic = new Intent(this, MusicPlaybackService.class);
                        playMusic.setData(uri);
                        playMusic.putExtra(MusicPlaybackService.EXTRA_MESSENGER, new Messenger(new Handler() {
                            public void handleMessage(Message msg) {
                                if (msg.what == MusicPlaybackService.MESSAGE_PLAYER_PREPARED) {
                                    stopProgress();
                                    showStopMenuItem(true);
                                }
                            }
                        }));
                        playMusic.setAction(MusicPlaybackService.ACTION_PLAY);
                        startService(playMusic);
                    }
                } else {
                    if (item.isLiveTv()) {
                        TvShow tvShow = (TvShow) item;
                        if (tvShow.getEndTime().compareTo(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()) < 0) {
                            new AlertDialog.Builder(this)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle(getString(R.string.live_tv))
                                    .setMessage(getString(R.string.show_already_ended_) + "\n" + item.getTitle() + "\n" + tvShow.getChannelInfo() + "\n" + tvShow.getShowTimeInfo())
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                            onResume();
                            return;
                        }
                    }
                    String playbackNetwork = item.isDownloaded() ? PlaybackOptions.NETWORK_DOWNLOAD : appSettings.getPlaybackNetwork();
                    PlaybackOption playbackOption = appSettings.getPlaybackOptions().getOption(item.getType(), item.getFormat(), playbackNetwork);
                    if (appSettings.getMediaSettings().getViewType() != ViewType.list && playbackOption.isAlways()) {
                        // detail or split mode -- no dialog if stream mode pref is set
                        startProgress();
                        if (playbackOption.isHls())
                            new StreamHlsTask(item).execute((URL) null);
                        else
                            playRawVideoStream(item);
                    }
                    else {
                        VideoPlaybackDialog dialog = getVideoPlaybackDialog(item);
                        dialog.show(getFragmentManager(), "StreamVideoDialog");
                    }
                }
            } else {
                // frontend playback
                final FrontendPlayer player;
                if (item.isSearchResult()) {
                    SearchResults searchResults = ((SearchActivity) this).searchResults;
                    String basePath = null;
                    if (item.isMusic())
                        basePath = searchResults.getMusicBase();
                    else if (item.getStorageGroup() == null)
                        basePath = searchResults.getVideoBase();
                    if (basePath != null)
                        player = new SocketFrontendPlayer(appSettings, basePath, item, getCharSet());
                    else
                        player = new ServiceFrontendPlayer(appSettings, item);
                } else {
                    if (item.isMusic() || item.getStorageGroup() == null) // frontend services require storage groups
                        player = new SocketFrontendPlayer(appSettings, mediaList.getBasePath(), item, getCharSet());
                    else
                        player = new ServiceFrontendPlayer(appSettings, item);
                }

                if (player.checkIsPlaying()) {
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.interrupt))
                            .setMessage(getString(R.string.stop_playback_))
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    player.stop();
                                    startFrontendPlayback(item, player);
                                }
                            })
                            .setNegativeButton(getString(R.string.no), null)
                            .show();
                } else {
                    startFrontendPlayback(item, player);
                }
            }
        } catch (Exception ex) {
            stopProgress();
            onResume();
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected void downloadItem(final Item item) {
        try {
            final URL baseUrl = getAppSettings().getMythTvServicesBaseUrlWithCredentials();

            String fileUrl = baseUrl + "/Content/GetFile?";
            if (item.getStorageGroup() == null)
                fileUrl += "StorageGroup=None&";
            else
                fileUrl += "StorageGroup=" + item.getStorageGroup().getName() + "&";
            fileUrl += "FileName=" + URLEncoder.encode(item.getFilePath(), "UTF-8");

            Uri uri = Uri.parse(fileUrl.toString());
            ProxyInfo proxyInfo = MediaStreamProxy.getProxyInfo(uri);
            if (proxyInfo == null)
                proxyInfo = MediaStreamProxy.needsAuthProxy(uri); // required by auth

            if (proxyInfo != null) {
                // needs proxying to support authentication since DownloadManager doesn't support
                MediaStreamProxy proxy = new MediaStreamProxy(proxyInfo, AuthType.valueOf(appSettings.getMythTvServicesAuthType()));
                proxy.init();
                proxy.start();
                fileUrl = "http://" + proxy.getLocalhost().getHostAddress() + ":" + proxy.getPort() + uri.getPath();
                if (uri.getQuery() != null)
                    fileUrl += "?" + uri.getQuery();
            }

            Log.i(TAG, "Media download URL: " + fileUrl);

            stopProgress();

            File downloadFile = null;

            try {
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    File downloadDir = null;
                    try {
                        Context.class.getMethod("getExternalMediaDirs", (Class<?>[]) null);
                        File[] dirs = new File[0];
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            dirs = getApplicationContext().getExternalMediaDirs();
                        }
                        String storageDirName = getString(R.string.storage_dir);
                        if (dirs.length >= 1) {
                            Map<String, File> mediaDirs = new HashMap<String, File>();
                            for (int i = 0; i < dirs.length; i++) {
                                if (dirs[i] != null) { // entry can be null if storage not attached
                                    String label = dirs[i].toString();
                                    if (label.startsWith(storageDirName))
                                        label = label.substring(storageDirName.length());
                                    mediaDirs.put(label, dirs[i]);
                                }
                            }
                            File dl = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mythling");
                            String dlLabel = dl.toString();
                            if (dlLabel.startsWith(storageDirName))
                                dlLabel = dlLabel.substring(storageDirName.length());
                            if (!mediaDirs.containsKey(dlLabel))
                                mediaDirs.put(dlLabel, dl);
                            if (mediaDirs.size() == 1)
                                downloadDir = mediaDirs.values().iterator().next();
                            else
                                downloadDir = mediaDirs.get(getAppSettings().getExternalMediaDir());

                            if (downloadDir == null) {
                                final String[] labels = mediaDirs.keySet().toArray(new String[0]);
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setTitle(getString(R.string.save_to_media_folder))
                                        .setItems(labels, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                getAppSettings().setExternalMediaDir(labels[which].toString());
                                                downloadItem(item);
                                            }
                                        }).show();
                                return;
                            }
                        }
                    } catch (NoSuchMethodException ex) {
                        // not supported by android version
                        downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/mythling");
                    }

                    if (downloadDir != null) {
                        String downloadPath = "/";
                        if (getPath() != null && !getPath().isEmpty() && !getPath().equals("/"))
                            downloadPath += getPath() + "/";
                        File destDir = new File(downloadDir + downloadPath);
                        if (destDir.isDirectory() || destDir.mkdirs()) {
                            downloadFile = new File(destDir + "/" + item.getDownloadFilename());
                            Log.i(TAG, "Downloading to file: " + downloadFile);
                        }
                    }
                }
            } catch (IllegalStateException ex) {
                // store internal
            } catch (Exception ex) {
                // log, report and store internal
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            }

            long downloadId = 0;

            // TODO: prefs
            boolean bypassDownloadManager = false;
            if (bypassDownloadManager) {
                downloadId = downloadFile.hashCode();
                new DownloadTask(uri.toString(), downloadFile).execute();
                item.setDownloadId(downloadId);
            }
            else {
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Request request = new Request(Uri.parse(fileUrl));
                request.setTitle(item.getOneLineTitle());
                request.setDestinationUri(Uri.fromFile(downloadFile));
                request.allowScanningByMediaScanner();
                downloadId = dm.enqueue(request);
                registerDownloadReceiver(item, downloadId);
            }
            Toast.makeText(getApplicationContext(), getString(R.string.downloading_) + item.getOneLineTitle(), Toast.LENGTH_LONG).show();
            getAppData().addDownload(new Download(item.getId(), downloadId, downloadFile.getPath(), new Date()));
            if (item.isRecording() && (mediaList.isMythTv28() || getAppSettings().isMythlingMediaServices()))
                new GetCutListTask((Recording)item, downloadId).execute();
        } catch (Exception ex) {
            stopProgress();
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected Uri getDownload(Item item) throws IOException {
        if (item.getDownloadId() == null)
            return null;
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Uri uri = dm.getUriForDownloadedFile(item.getDownloadId());
        if (uri == null) {
            item.setDownloadId(null);
            throw new IOException("Cannot find download for: " + item);
        }
        else {
            return uri;
        }
    }

    /**
     * Starts a transcode without immediately watching.
     */
    protected void transcodeItem(final Item item) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(getString(R.string.hls_transcode))
                .setMessage(getString(R.string.begin_transcode) +  ":\n" + item.getDialogTitle() + "\n" + getString(R.string.with_video_quality))
                .setPositiveButton(getString(R.string.internal), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new TranscodeVideoTask(item, false).execute(getAppSettings().getMythTvServicesBaseUrl());
                            Toast.makeText(getApplicationContext(), getString(R.string.transcoding_) + item.getOneLineTitle(), Toast.LENGTH_LONG).show();
                        } catch (MalformedURLException ex) {
                            stopProgress();
                            Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNeutralButton(getString(R.string.external), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new TranscodeVideoTask(item, true).execute(getAppSettings().getMythTvServicesBaseUrl());
                            Toast.makeText(getApplicationContext(), getString(R.string.transcoding_) + item.getOneLineTitle(), Toast.LENGTH_LONG).show();
                        } catch (MalformedURLException ex) {
                            stopProgress();
                            Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopProgress();
                        // onResume();
                    }
                })
                .show();
    }

    protected void deleteRecording(final Recording recording) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.delete_recording) +  ":\n" + recording.getDialogTitle())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new DeleteRecordingTask(recording).execute(getAppSettings().getMythTvServicesBaseUrl());
                            // remove in this same thread to avoid confusing user
                            boolean removed = mediaList.removeItem(recording);
                            if (!removed) {
                                // must be categorized
                                Category cat = mediaList.getCategory(recording.getTitle());
                                if (cat != null)
                                    removed = cat.removeItem(recording);
                            }
                            if (removed) {
                                mediaList.setCount(mediaList.getCount() - 1);
                                onResume();
                            }
                        } catch (MalformedURLException ex) {
                            stopProgress();
                            Log.e(TAG, ex.getMessage(), ex);
                            if (getAppSettings().isErrorReportingEnabled())
                                new Reporter(ex).send();
                            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopProgress();
                    }
                })
                .show();
    }

    protected void goListView() {
        findViewById(R.id.detail_container).setVisibility(View.GONE);
        if (!getAppSettings().isTv() && getListView() != null && getListView().getCheckedItemPosition() >= 0) {
            getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
        }
    }

    protected void goDetailView() {
        if (mediaList.getMediaType() == MediaType.recordings && getAppSettings().getMediaSettings().getSortType() == SortType.byTitle) {
            getAppSettings().clearCache(); // refresh since we're switching to flattened hierarchy
            selItemIndex = 0;
        }

        Uri uri = new Uri.Builder().path(getPath()).build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), MediaPagerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SEL_ITEM_INDEX, selItemIndex);
        startActivity(intent);
    }

    public void goMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void goSplitView() {
        findViewById(R.id.detail_container).setVisibility(View.VISIBLE);
    }

    public void sort() throws IOException, JSONException, ParseException {
        startProgress();
        refreshMediaList();
    }

    private void startFrontendPlayback(Item item, final FrontendPlayer player) {
        if (item.isLiveTv()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(item.getTitle())
                    .setMessage(getString(R.string.todo_frontend_live_tv))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
        } else {
            try {
                player.play();

                // reset progress
                count = 0;
                // prepare for a progress bar dialog
                countdownDialog = new ProgressDialog(this);
                countdownDialog.setCancelable(true);
                String msg = getString(R.string.playing) + " " + item.getTitle();
                if (item.getListSubText() != null)
                    msg += "\n" + item.getListSubText();
                countdownDialog.setMessage(msg);
                countdownDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                countdownDialog.setProgressPercentFormat(null);
                countdownDialog.setProgressNumberFormat(null);
                countdownDialog.setMax(10);
                countdownDialog.setCancelable(true);
                countdownDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        stopTimer();
                    }
                });
                countdownDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.stop), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        player.stop();
                        dialog.dismiss();
                        stopTimer();
                    }
                });
                countdownDialog.setCanceledOnTouchOutside(true);
                countdownDialog.show();
                countdownDialog.setProgress(10);

                tick();
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                stopTimer();
            }
        }
    }

    @Override
    protected void onStop() {
        if (countdownDialog != null && countdownDialog.isShowing())
            countdownDialog.dismiss();
        stopTimer();
        super.onStop();
    }

    private void stopTimer() {
        if (timer != null)
            timer.cancel();
        count = 0;
    }

    private void tick() {
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                countdownDialog.setProgress(10 - count);
                if (count == 10) {
                    countdownDialog.dismiss();
                    stopTimer();
                } else {
                    count++;
                    tick();
                }
            }
        }, 1000);
    }

    protected void refreshMediaList() {
        try {
            selItemIndex = 0;
            currentTop = 0;
            topOffset = 0;
            getAppSettings().clearMediaSettings(); // in case prefs changed
            if (getAppSettings().getMediaSettings().getType() == MediaType.movies && !supportsMovies())
                getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));
            if (getAppSettings().getMediaSettings().getType() == MediaType.tvSeries && !supportsTvSeries())
                getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));
            if (getAppSettings().getMediaSettings().getType() == MediaType.music && !supportsMusic())
                getAppSettings().setMediaType(MediaType.valueOf(AppSettings.DEFAULT_MEDIA_TYPE));

            new RefreshTask().execute(getAppSettings().getUrls(getAppSettings().getMediaListUrl()));
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void handleEmptyMediaList() {
        if (isSplitView()) {
            try {
                showSubListPane(null);
            }
            catch (IllegalStateException ex) {
                // "Can not perform this action after onSaveInstanceState"
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
            }
        }
        if (getAppSettings().isTv()) {
            // empty list - set focus on action bar
            setFocusOnActionBar();
        }
    }

    protected void setFocusOnActionBar() {
        int actionBarResId = getResources().getIdentifier("action_bar_container", "id", "android");
        View actionBarView = getWindow().getDecorView().findViewById(actionBarResId);
        if (actionBarView != null)
            actionBarView.requestFocus();
    }

    protected void updateActionMenu() {
        showMoviesMenuItem(supportsMovies());
        showTvSeriesMenuItem(supportsTvSeries());
        showMusicMenuItem(supportsMusic());
        showSortMenu(supportsSort());
        showViewMenu(supportsViewMenu());
        showSearchMenu(supportsSearch());

        if (mediaMenuItem != null)
            mediaMenuItem.setTitle(Localizer.getInstance().getMediaLabel(getAppSettings().getMediaSettings().getType()) + " (" + mediaList.getCount() + ")");

        showStopMenuItem(isPlayingMusic());
    }

    protected void populate() throws IOException, JSONException, ParseException {
        // default does nothing
    }

    private boolean isPlayingMusic() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicPlaybackService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    private class RefreshTask extends AsyncTask<URL,Integer,Long> {
        private String mediaListJson;
        private String storageGroupsJson;

        private Exception ex;
        private String msg;

        protected Long doInBackground(URL... urls) {
            Long before = null;
            try {
                MediaSettings mediaSettings = getAppSettings().getMediaSettings();

                HttpHelper downloader = getAppSettings().getMediaListDownloader(urls);
                before = Long.valueOf(System.currentTimeMillis());
                byte[] bytes = downloader.get();
                mediaListJson = new String(bytes, downloader.getCharSet());
                if (mediaListJson.startsWith("<")) {
                    // just display html
                    ex = new IOException(mediaListJson);
                    return -1L;
                }

                URL sgUrl = new URL(getAppSettings().getMythTvServicesBaseUrl() + "/Myth/GetStorageGroupDirs");
                HttpHelper sgDownloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(sgUrl));
                storageGroupsJson = new String(sgDownloader.get());
                storageGroups = new MythTvParser(getAppSettings(), storageGroupsJson).parseStorageGroups();
                if (mediaSettings.getType() == MediaType.music && storageGroups.get(appSettings.getMusicStorageGroup()) == null
                        && !appSettings.isMythlingMediaServices()) {
                    msg = getString(R.string.music_storage_group_required);
                    return -1L;
                }

                MediaListParser mediaListParser = getAppSettings().getMediaListParser(mediaListJson);
                if (getAppSettings().isMythlingMediaServices()) {
                    mediaList = mediaListParser.parseMediaList(mediaSettings.getType(), storageGroups);
                } else {
                    boolean hasMediaStorageGroup = mediaSettings.getType() == MediaType.recordings || mediaSettings.getType() == MediaType.liveTv
                            || (mediaSettings.getType() != MediaType.music && storageGroups.get(appSettings.getVideoStorageGroup()) != null);
                    if (hasMediaStorageGroup) {
                        mediaList = ((MythTvParser)mediaListParser).parseMediaList(mediaSettings.getType(), storageGroups);
                    } else {
                        // no storage group for media type
                        URL baseUrl = getAppSettings().getMythTvServicesBaseUrl();
                        String basePath = null;
                        if (mediaSettings.getType() == MediaType.videos || mediaSettings.getType() == MediaType.movies || mediaSettings.getType() == MediaType.tvSeries) {
                            // handle videos by getting the base path setting
                            downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetHostName")));
                            String hostName = new MythTvParser(getAppSettings(), new String(downloader.get())).parseString();
                            String key = "VideoStartupDir";
                            downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetSetting?Key=" + key + "&HostName=" + hostName)));
                            basePath = new MythTvParser(getAppSettings(), new String(downloader.get())).parseMythTvSetting(key);
                            if (basePath == null) {
                                // try without host name
                                downloader = getAppSettings().getMediaListDownloader(getAppSettings().getUrls(new URL(baseUrl + "/Myth/GetSetting?Key=" + key)));
                                basePath = new MythTvParser(getAppSettings(), new String(downloader.get())).parseMythTvSetting(key);
                            }
                        }
                        mediaList = ((MythTvParser)mediaListParser).parseMediaList(mediaSettings.getType(), storageGroups, basePath);
                    }
                }

                mediaList.setCharSet(downloader.getCharSet());

                if (mediaList.supportsTranscode() && getAppSettings().isRetrieveTranscodeStatuses())
                    updateTranscodeStatuses(mediaList.getAllItems());

                return 0L;
            } catch (SocketTimeoutException ex) {
                long secs = (System.currentTimeMillis() - before) / 1000;
                String msg = getString(R.string.socket_timeout_after_) + secs + " " + getString(R.string.seconds) + ". "
                        + getString(R.string.socket_timeout_suggest);
                this.ex = new SocketTimeoutException(msg);
                // log the original exception to avoid losing cause,
                // since SocketTimeoutException(String, Throwable) is "internal use only"
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;

            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                stopProgress();
                new Handler().post(new Runnable() {
                    public void run() {
                        handleEmptyMediaList();
                    }
                });
                if (ex != null)
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                else if (msg != null)
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            } else {
                AppData appData = new AppData(getApplicationContext());
                appData.setMediaList(mediaList);
                appData.setStorageGroups(storageGroups);
                setAppData(appData);
                setMediaType(appData.getMediaList().getMediaType());
                getAppSettings().setLastLoad(System.currentTimeMillis());

                try {
                    mediaList.setDownloads(appData.getDownloads());
                    appData.writeStorageGroups(storageGroupsJson);
                    appData.writeMediaList(mediaListJson);
                    stopProgress();
                    populate();
                } catch (Exception ex) {
                    stopProgress();
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }
    }

    private class StreamHlsTask extends AsyncTask<URL,Integer,Long> {
        private Item item;
        private LiveStreamInfo streamInfo;
        private Exception ex;

        public StreamHlsTask(Item item) {
            this.item = item;
        }

        protected Long doInBackground(URL... urls) {
            try {
                Transcoder transcoder = new Transcoder(getAppSettings());

                // TODO: do this retry for tv playback
                int ct = 0;
                int maxTries = 3;
                // empty relative url i think means myth has not started transcoding
                while ((streamInfo == null || streamInfo.getRelativeUrl().isEmpty()) && ct < maxTries) {
                    transcoder.beginTranscode(item);
                    streamInfo = transcoder.getStreamInfo();
                    ct++;
                    Thread.sleep(1000);
                }

                if (streamInfo == null || streamInfo.getRelativeUrl().isEmpty())
                    throw new IOException(getString(R.string.transcoding_not_started));

                transcoder.waitAvailable();

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            if (result != 0L) {
                stopProgress();
                if (ex != null)
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                onResume();
            } else {
                try {
                    playLiveStream(streamInfo, item);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected class TranscodeVideoTask extends AsyncTask<URL,Integer,Long> {
        private Item item;
        private boolean externalVideoQuality; // false means internal
        private Exception ex;

        public TranscodeVideoTask(Item item, boolean externalVideoQuality) {
            this.item = item;
            this.externalVideoQuality = externalVideoQuality;
        }

        protected Long doInBackground(URL... urls) {
            try {
                Transcoder transcoder = new Transcoder(getAppSettings());
                transcoder.beginTranscode(item, externalVideoQuality ? AppSettings.EXTERNAL_VIDEO_QUALITY : AppSettings.INTERNAL_VIDEO_QUALITY);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            stopProgress();
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                onResume();
            }
        }
    }

    /**
     * Uses internal video player.
     */
    protected class PlayWithCutListTask extends GetCutListTask {
        private Uri playbackUri;
        private PlaybackOption playbackOption;

        public PlayWithCutListTask(Uri uri, Recording rec, PlaybackOption opt) {
            super(rec);
            this.playbackUri = uri;
            this.playbackOption = opt;
        }

        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            // play regardless of cutlist success
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(playbackUri, "video/*");
            videoIntent.setClass(getApplicationContext(), VideoPlayerActivity.class);
            videoIntent.putExtra(VideoPlayerActivity.PLAYER, playbackOption.getPlayer());
            if (getRecording().isLengthKnown())
                videoIntent.putExtra(VideoPlayerActivity.ITEM_LENGTH_SECS, getRecording().getLength());
            String streamingAuthType = getAppSettings().getMythTvServicesAuthType();
            if (streamingAuthType != AuthType.None.toString())
                videoIntent.putExtra(VideoPlayerActivity.AUTH_TYPE, streamingAuthType);
            if (getRecording().hasCutList())
                videoIntent.putExtra(VideoPlayerActivity.ITEM_CUT_LIST, getRecording().getCutList());
            startActivity(videoIntent);
        }
    }

    protected class GetCutListTask extends AsyncTask<URL,Integer,Long> {
        private Recording recording;
        protected Recording getRecording() { return recording; }

        private long downloadId;

        public GetCutListTask(Recording rec, long downloadId) {
            this.recording = rec;
            this.downloadId = downloadId;
        }

        public GetCutListTask(Recording rec) {
            this.recording = rec;
        }

        protected Long doInBackground(URL... urls) {
            try {
                AppSettings appSettings = getAppSettings();
                URL url = new URL(appSettings.getCutListBaseUrl() + "ChanId=" + recording.getChannelId() +
                        "&StartTime=" + recording.getStartTimeParam());
                boolean mythlingServices = appSettings.isMythlingMediaServices();
                String authType = mythlingServices ? appSettings.getBackendWebAuthType() : appSettings.getMythTvServicesAuthType();
                HttpHelper downloader = new HttpHelper(appSettings.getUrls(url), authType, appSettings.getPrefs());
                if (mythlingServices)
                    downloader.setCredentials(appSettings.getMythlingServicesUser(), appSettings.getMythlingServicesPassword());
                else
                    downloader.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());

                String cutListJson = new String(downloader.get());
                // use mythtv parser since services are compatible
                ArrayList<Cut> cutList = Cut.parseCutList(new JSONObject(cutListJson));
                if (!cutList.isEmpty())
                    recording.setCutList(cutList);
                return 0L;
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            stopProgress();
            if (result != 0L) {
                Toast.makeText(getApplicationContext(), getString(R.string.unable_to_retrieve_cutlist), Toast.LENGTH_LONG).show();
            }
            else {
                try {
                    if (downloadId > 0 && recording.getCutList() != null)
                        getAppData().addDownloadCutList(recording.getId(), recording.getCutList());
                }
                catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
        }
    }

    protected class DeleteRecordingTask extends AsyncTask<URL,Integer,Long> {
        private Recording recording;
        private Exception ex;

        public DeleteRecordingTask(Recording recording) {
            this.recording = recording;
        }

        protected Long doInBackground(URL... urls) {
            try {
                Recorder recorder = new Recorder(getAppSettings());
                recorder.deleteRecording(recording);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            stopProgress();
            if (result != 0L) {
                if (ex != null)
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                onResume();
            }
        }
    }

    protected class StreamTvTask extends AsyncTask<URL,Integer,Long> {
        private TvShow tvShow;
        private Recording recording;
        private LiveStreamInfo streamInfo;
        private boolean raw;
        private Exception ex;

        public StreamTvTask(TvShow tvShow, boolean raw) {
            this.tvShow = tvShow;
            this.raw = raw;
        }

        protected Long doInBackground(URL... urls) {
            try {
                Recorder recorder = new Recorder(getAppSettings(), storageGroups);
                boolean recordAvail = recorder.scheduleRecording(tvShow);

                if (!recordAvail)
                    recorder.waitAvailable();

                recording = recorder.getRecording();

                if (!raw) {
                    tvShow.setStorageGroup(recorder.getRecording().getStorageGroup());
                    Transcoder transcoder = new Transcoder(getAppSettings());
                    boolean streamAvail = transcoder.beginTranscode(recorder.getRecording());

                    streamInfo = transcoder.getStreamInfo();

                    if (!streamAvail)
                        transcoder.waitAvailable(); // this is the long pole
                }

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            stopProgress();
            if (result != 0L) {
                if (ex instanceof AllTunersInUseException) {
                    new AlertDialog.Builder(MediaActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.recording_conflict))
                            .setMessage(getString(R.string.tuners_in_use_) + ex.getMessage())
                            .setPositiveButton(R.string.ok, null)
                            .show();
                } else {
                    if (ex != null)
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    onResume();
                }
            } else {
                try {
                    recording.setForLiveTv(true); // for playbackOption purposes
                    if (raw)
                        playRawVideoStream(recording);
                    else
                        playLiveStream(streamInfo, recording);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected class DownloadTask extends AsyncTask<URL,Integer,Long> {
        private String url;
        private File file;
        private Exception ex;

        public DownloadTask(String url, File file) {
            this.url = url;
            this.file = file;
        }

        protected Long doInBackground(URL... urls) {
            try {
                new Downloader(url, file).doDownload();
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                Log.e(TAG, ex.getMessage(), ex);
                if (getAppSettings().isErrorReportingEnabled())
                    new Reporter(ex).send();
                return -1L;
            }
        }

        protected void onPostExecute(Long result) {
            Log.i(TAG, "Download complete: " + url);
        }
    }

    /**
     * Must be called from background thread
     */
    protected void updateTranscodeStatuses(List<Item> items) throws IOException {
        URL tcUrl = new URL(appSettings.getMythTvServicesBaseUrl() + "/Content/GetLiveStreamList");
        HttpHelper tcDownloader = new HttpHelper(appSettings.getUrls(tcUrl), appSettings.getMythTvServicesAuthType(), appSettings.getPrefs());
        tcDownloader.setCredentials(appSettings.getMythTvServicesUser(), appSettings.getMythTvServicesPassword());
        String liveStreamJson = new String(tcDownloader.get(), "UTF-8");
        List<LiveStreamInfo> liveStreams = new MythTvParser(appSettings, liveStreamJson).parseStreamInfoList();

        // ugly performance-optimized code follows
        long startTime = System.currentTimeMillis();
        int desiredRes = getAppSettings().getVideoRes();
        int desiredVidBr = getAppSettings().getVideoBitrate();
        int desiredAudBr = getAppSettings().getAudioBitrate();
        int[] resValues = getAppSettings().getVideoResValues();
        int[] vidBrValues = getAppSettings().getVideoBitrateValues();
        int[] audBrValues = getAppSettings().getAudioBitrateValues();
        for (Item item : items) {
            List<String> sgPaths = item.getStorageGroupPaths();
            for (LiveStreamInfo liveStream : liveStreams) {
                if (liveStream.isCompleted()) {
                    if (sgPaths != null) {
                        for (String sgPath : sgPaths) {
                            if (liveStream.getFile().equals(sgPath)) {
                                if (liveStream.matchesQuality(desiredRes, desiredVidBr, desiredAudBr, resValues, vidBrValues, audBrValues)) {
                                    item.setTranscoded(true);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (item.isTranscoded())
                    break; // match already found
            }
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, " -> transcode status check time: " + (System.currentTimeMillis() - startTime) + " ms");
    }

    /**
     * Requires MythTV content.cpp patch to work without storage groups.
     */
    private void playRawVideoStream(Item item) throws IOException, JSONException {

        Uri uri;
        if (item.isDownloaded()) {
            uri = getDownload(item);
        }
        else {
            URL baseUrl = getAppSettings().getMythTvServicesBaseUrlWithCredentials();
            String fileUrl = baseUrl + "/Content/GetFile?";
            if (item.getStorageGroup() == null)
                fileUrl += "StorageGroup=None&";
            else
                fileUrl += "StorageGroup=" + item.getStorageGroup().getName() + "&";
            fileUrl += "FileName=" + URLEncoder.encode(item.getFilePath(), "UTF-8");

            uri = Uri.parse(fileUrl);
        }


        PlaybackOption playbackOption = getPlaybackOption(item, PlaybackOptions.STREAM_FILE);
        boolean isExternalPlayer = playbackOption.isAppPlayer();
        if (!isExternalPlayer && item.isRecording() && isUseCutList()) {
            new PlayWithCutListTask(uri, (Recording)item, playbackOption).execute();
        }
        else {
            stopProgress();
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(uri, "video/*");
            if (!isExternalPlayer) {
                videoIntent.setClass(getApplicationContext(), VideoPlayerActivity.class);
                videoIntent.putExtra(VideoPlayerActivity.PLAYER, playbackOption.getPlayer());
                if (!item.isDownloaded()) {
                    if (item.isLengthKnown())
                        videoIntent.putExtra(VideoPlayerActivity.ITEM_LENGTH_SECS, item.getLength());
                    String streamingAuthType = getAppSettings().getMythTvServicesAuthType();
                    if (streamingAuthType != AuthType.None.toString())
                        videoIntent.putExtra(VideoPlayerActivity.AUTH_TYPE, streamingAuthType);
                }
            }
            startActivity(videoIntent);
        }
    }

    protected void playLiveStream(LiveStreamInfo streamInfo, Item item) throws IOException, JSONException {
        String streamUrl = appSettings.getMythTvServicesBaseUrlWithCredentials() + streamInfo.getRelativeUrl();

        // avoid retrieving unnecessary audio-only streams
        int lastDot = streamUrl.lastIndexOf('.');
        streamUrl = streamUrl.substring(0, lastDot) + ".av" + streamUrl.substring(lastDot);

        stopProgress();

        PlaybackOption playbackOption = getPlaybackOption(item, PlaybackOptions.STREAM_HLS);
        boolean isExternalPlayer = playbackOption.isAppPlayer();
        if (!isExternalPlayer && item.isRecording() && isUseCutList()) {
            new PlayWithCutListTask(Uri.parse(streamUrl), (Recording)item, playbackOption).execute();
        }
        else {
            Intent videoIntent = new Intent(Intent.ACTION_VIEW);
            videoIntent.setDataAndType(Uri.parse(streamUrl), "video/*");
            if (!playbackOption.isAppPlayer()) {
                videoIntent.setClass(getApplicationContext(), VideoPlayerActivity.class);
                videoIntent.putExtra(VideoPlayerActivity.PLAYER, playbackOption.getPlayer());
                if (item.isLengthKnown())
                    videoIntent.putExtra(VideoPlayerActivity.ITEM_LENGTH_SECS, item.getLength());
            }
            startActivity(videoIntent);
        }
    }

    boolean isUseCutList() {
        boolean useCutList = false;
        if (isTv() && mediaList != null)
            useCutList = mediaList.isMythTv28() || getAppSettings().isMythlingMediaServices();
        else // pref may be invalid if not using mythtv 0.28, but just let user be warned
            useCutList = !getAppSettings().getAutoSkip().equals(AppSettings.AUTO_SKIP_OFF);
        return useCutList;
    }

    void initListViewOnItemClickListener() {
        if (getListables().size() > 0) {
            getListView().setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    currentTop = getListView().getFirstVisiblePosition();
                    View topV = getListView().getChildAt(0);
                    topOffset = (topV == null) ? 0 : topV.getTop();
                    selItemIndex = position;
                    Listable listable = getListables().get(position);
                    if (listable instanceof Item) {
                        Item item = (Item) listable;
                        if (isSplitView()) {
                            getListAdapter().setSelection(selItemIndex);
                            getListView().setItemChecked(selItemIndex, true);
                            if (appSettings.isTv()) {
                            	// play the item since already selected
                            	playItem(item);
                            } else {
                                showItemInDetailPane(position, true);
                            }
                        } else {
                            playItem(item);
                        }
                    } else {
                        // must be category
                        Category category = (Category) listable;
                        String cat = category.getName();
                        String catpath = "".equals(getPath()) ? cat : getPath() + "/" + cat;
                        if (isSplitView()) {
                            getListAdapter().setSelection(selItemIndex);
                            getListView().setItemChecked(position, true);
                            showSubListPane(catpath, 0);
                        } else {
                            Uri uri = new Uri.Builder().path(catpath).build();
                            startActivity(new Intent(Intent.ACTION_VIEW, uri, getApplicationContext(), MediaListActivity.class));
                        }
                    }
                }
            });
        }
    }

    void initListViewOnItemSelectedListener() {
        if (getListables().size() > 0 && getAppSettings().isTv()) {
            getListView().setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Listable sel = getListables().get(position);
                    getListView().setItemChecked(selItemIndex, false);
                    selItemIndex = position;
                    if (sel instanceof Item) {
                        showItemInDetailPane(position, false);
                    }
                    else {
                        boolean grab = isSplitView() && getIntent().getBooleanExtra(GRAB_FOCUS, false);
                        showSubListPane(getPath() + "/" + ((Category)sel).getName(), grab);
                    }
                    getIntent().putExtra(GRAB_FOCUS, false);
                }
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            getListView().setOnFocusChangeListener(new OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    getListView().setItemChecked(selItemIndex, !hasFocus);
                }
            });
        }
    }

    void initListViewDpadHandler() {
        if (getAppSettings().isTv()) {
            getListView().setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            Listable listable = getListables().get(getSelItemIndex());
                            if (listable instanceof Category) {
                                getListView().performItemClick(getListAdapter().getView(selItemIndex, null, null),
                                        selItemIndex, getListAdapter().getItemId(selItemIndex));
                                return true;
                            } else if (isSplitView()) {
                                getListAdapter().setSelection(selItemIndex);
                                getListView().setItemChecked(selItemIndex, true);
                                showItemInDetailPane(selItemIndex, true);
                                return true;
                            }
                        }
                        else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT && !"".equals(getPath())) {
                            onBackPressed();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    void initSplitView() {
        if (selItemIndex != -1 && getListables().size() > 0) {
            getListView().setItemChecked(selItemIndex, true);
            Listable preSel = getListables().get(selItemIndex);
            if (preSel instanceof Item)
                showItemInDetailPane(selItemIndex);
            else
                showSubListPane(getPath() + "/" + ((Category)preSel).getName());
        }
    }

    protected void setPathFromIntent() {
        try {
            String newPath = getIntent().getDataString();
            setPath(newPath == null ? "" : URLDecoder.decode(newPath, "UTF-8"));
        }
        catch (UnsupportedEncodingException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            setPath(""); // TODO correct?
        }
    }

    protected PlaybackOption getPlaybackOption(Item item, String streamType) throws IOException, JSONException {
        String playbackNetwork = item.isDownloaded() ? PlaybackOptions.NETWORK_DOWNLOAD : appSettings.getPlaybackNetwork();
        MediaType type = item.getType();
        String format = item.getFormat();
        if (item instanceof Recording && ((Recording)item).isForLiveTv()) {
            type = MediaType.liveTv;
            format = TvShow.LIVE_TV_FORMAT;
        }
        return appSettings.getPlaybackOptions().getOption(type, format, playbackNetwork, streamType);
    }

    protected VideoPlaybackDialog getVideoPlaybackDialog(Item item) {
        final VideoPlaybackDialog dialog = new VideoPlaybackDialog(getAppSettings(), item);
        dialog.setListener(new PlaybackDialogListener() {
            public void onClickPlay(final Item item, final PlaybackOption option) {
                if (PlaybackOptions.PLAYER_LIBVLC.equals(option.getPlayer()) &&
                        !getAppSettings().isCpuCompatibleWithLibVlcPlayer() && !getAppSettings().isIgnoreLibVlcCpuCompatibility()) {
                    PrefDismissDialog dlg = new PrefDismissDialog(getAppSettings(), getString(R.string.title_libvlc_playback),
                        getString(R.string.unsupported_cpu_for_libvlc), AppSettings.IGNORE_LIBVLC_CPU_COMPATIBILITY);
                    dlg.setListener(new PrefDismissListener() {
                            public String getPositiveBtnLabel() {
                                return getString(R.string.play);
                            }
                            public void onClickPositive() {
                                startVideoPlayback(item, option);
                            }
                            public void onClickNegative() {
                                getVideoPlaybackDialog(item).show(getFragmentManager(), "StreamVideoDialog");
                                return;
                            }
                    });
                    dlg.setCheckboxText(getString(R.string.always_ignore_libvlc_cpu_compatibility));
                    dlg.setDefaultChecked(false);
                    dlg.show(getFragmentManager());
                }
                else {
                    startVideoPlayback(item, option);
                }
            }
            public void onClickCancel() {
                stopProgress();
                // onResume();
            }
        });
        return dialog;
    }

    private void startVideoPlayback(Item item, PlaybackOption option) {
        startProgress();
        if (item.isLiveTv()) {
            new StreamTvTask((TvShow)item, !option.isHls()).execute();
        }
        else {
            if (option.isHls()) {
                new StreamHlsTask(item).execute();
            }
            else {
                try {
                    playRawVideoStream(item);
                } catch (Exception ex) {
                    stopProgress();
                    onResume();
                    Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v == getListView()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Listable listable = (Listable)getListView().getItemAtPosition(info.position);
            if (listable instanceof Item) {
                Item item = (Item)listable;
                menu.setHeaderTitle(item.getDialogTitle());
                if (isSplitView()) {
                    getListView().performItemClick(
                            getListView().getChildAt(info.position), info.position,
                            getListView().getAdapter().getItemId(info.position));
                }
                else {
                    getListView().setItemChecked(info.position, true);
                }
                SparseArray<String> menuItems = getLongClickMenuItems(item);
                for (int i = 0; i < menuItems.size(); i++) {
                    int id = menuItems.keyAt(i);
                    menu.add(MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID, id, id, menuItems.get(id));
                }
            }
        }
    }

    SparseArray<String> getLongClickMenuItems(Item item) {
        String[] menuItems = getResources().getStringArray(R.array.item_long_click_menu);
        SparseArray<String> relevantItems = new SparseArray<String>();
        for (int i = 0; i < menuItems.length; i++) {
            if (i == LONG_CLICK_MENU_PLAY)
                relevantItems.put(i, menuItems[i]);
            else if (i == LONG_CLICK_MENU_TRANSCODE && !item.isMusic())
                relevantItems.put(i, menuItems[i]);
            else if (i == LONG_CLICK_MENU_DOWNLOAD && !item.isLiveTv())
                relevantItems.put(i, menuItems[i]);
            else if (i == LONG_CLICK_MENU_DELETE && item.isRecording())
                relevantItems.put(i, menuItems[i]);
        }
        return relevantItems;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            if (item.getItemId() == LONG_CLICK_MENU_PLAY) {
                Item it = (Item)getListView().getItemAtPosition(info.position);
                playItem(it);
                return true;
            } else if (item.getItemId() == LONG_CLICK_MENU_TRANSCODE) {
                Item it = (Item)getListView().getItemAtPosition(info.position);
                transcodeItem(it);
                return true;
            } else if (item.getItemId() == LONG_CLICK_MENU_DOWNLOAD) {
                Item it = (Item)getListView().getItemAtPosition(info.position);
                downloadItem(it);
                return true;
            } else if (item.getItemId() == LONG_CLICK_MENU_DELETE) {
                Recording rec = (Recording)getListView().getItemAtPosition(info.position);
                int size = getListables().size();
                if (size == 1 || size == info.position + 1)
                    setSelItemIndex(getSelItemIndex() - 1);
                deleteRecording(rec);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && getAppSettings().isTv()) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                setFocusOnActionBar();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(SEL_ITEM_INDEX, selItemIndex);
        savedInstanceState.putInt(CURRENT_TOP, currentTop);
        savedInstanceState.putInt(TOP_OFFSET, topOffset);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selItemIndex = savedInstanceState.getInt(SEL_ITEM_INDEX, 0);
        currentTop = savedInstanceState.getInt(CURRENT_TOP, 0);
        topOffset = savedInstanceState.getInt(TOP_OFFSET, 0);
    }

    protected void startProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void stopProgress() {
        progressBar.setVisibility(View.GONE);
    }
}
