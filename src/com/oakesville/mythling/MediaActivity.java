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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NavUtils;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;

import com.oakesville.mythling.StreamVideoDialog.StreamDialogListener;
import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.BadSettingsException;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.firetv.FireTvEpgActivity;
import com.oakesville.mythling.media.AllTunersInUseException;
import com.oakesville.mythling.media.Category;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.Listable;
import com.oakesville.mythling.media.LiveStreamInfo;
import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.Recording;
import com.oakesville.mythling.media.SearchResults;
import com.oakesville.mythling.media.StorageGroup;
import com.oakesville.mythling.media.TvShow;
import com.oakesville.mythling.prefs.PrefsActivity;
import com.oakesville.mythling.util.FrontendPlayer;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.Recorder;
import com.oakesville.mythling.util.Reporter;
import com.oakesville.mythling.util.ServiceFrontendPlayer;
import com.oakesville.mythling.util.SocketFrontendPlayer;
import com.oakesville.mythling.util.Transcoder;

/**
 * Base class for the different ways to view collections of MythTV media.
 */
public abstract class MediaActivity extends Activity {
    private static final String TAG = MediaActivity.class.getSimpleName();

    static final String DETAIL_FRAGMENT = "detailFragment";
    static final String LIST_FRAGMENT = "listFragment";
    static final int MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID = 1;
    static final int LIST_FRAGMENT_CONTEXT_MENU_GROUP_ID = 2;

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
        return mediaList.getListables(getPath());
    }
    protected List<Listable> getListables(String path) {
        if (mediaList == null)  // TODO: how can mediaList be null?
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

    private BroadcastReceiver playbackBroadcastReceiver;

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
    protected boolean refreshing;

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
          registerPlaybackBroadcastReceiver(true);
    }

    @Override
    public void onPause() {
        registerPlaybackBroadcastReceiver(false);
        super.onPause();
    }

    private void registerPlaybackBroadcastReceiver(boolean register) {
        if (register) {
            if (playbackBroadcastReceiver == null) {
                playbackBroadcastReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        showStopMenuItem(false);
                    }
                };
            }
            registerReceiver(playbackBroadcastReceiver, new IntentFilter(MusicPlaybackService.ACTION_PLAYBACK_STOPPED));
        } else {
            if (playbackBroadcastReceiver != null) {
                unregisterReceiver(playbackBroadcastReceiver);
                playbackBroadcastReceiver = null;
            }
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(mediaSettings.getType()) + " (" + mediaList.getCount() + ")");
            else
                mediaMenuItem.setTitle(Localizer.getMediaLabel(mediaSettings.getType()));
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
                viewMenuItem.setIcon(mediaSettings.getViewIcon());
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
                sortMenuItem.setTitle(Localizer.getSortLabel(mediaSettings.getSortType()));
                if (mediaSettings.getSortType() == SortType.byDate)
                    sortMenuItem.getSubMenu().findItem(R.id.sort_byDate).setChecked(true);
                else if (mediaSettings.getSortType() == SortType.byRating)
                    sortMenuItem.getSubMenu().findItem(R.id.sort_byRating).setChecked(true);
                else
                    sortMenuItem.getSubMenu().findItem(R.id.sort_byTitle).setChecked(true);
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
        return getAppSettings().isMythlingMediaServices();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
                sortMenuItem.setTitle(Localizer.getSortLabel(appSettings.getMediaSettings().getSortType()));
            if (viewMenuItem != null)
                viewMenuItem.setIcon(appSettings.getMediaSettings().getViewIcon());

            if (item.getItemId() == R.id.media_music) {
                ViewType oldView = appSettings.getMediaSettings().getViewType();
                appSettings.setMediaType(MediaType.music);
                item.setChecked(true);
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.music));
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.videos));
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.recordings));
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.liveTv));
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.movies));
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
                mediaMenuItem.setTitle(Localizer.getMediaLabel(MediaType.tvSeries));
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
            if (BuildConfig.DEBUG)
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
            AppSettings appSettings = getAppSettings();

            if (appSettings.isDevicePlayback()) {

                if (item.isMusic()) {
                    String musicUrl = appSettings.getMythTvServicesBaseUrlWithCredentials() + "/Content/GetMusic?Id=" + item.getId();
                    if (appSettings.isExternalMusicPlayer()) {
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(musicUrl), "audio/*");
                        startActivity(intent);
                    }
                    else {
                        startProgress();
                        Toast.makeText(getApplicationContext(), getString(R.string.playing) + " '" + item.getTitle() + "'", Toast.LENGTH_LONG).show();
                        Intent playMusic = new Intent(this, MusicPlaybackService.class);
                        playMusic.setData(Uri.parse(musicUrl));
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
                    StreamVideoDialog dialog = new StreamVideoDialog(getAppSettings(), item);
                    dialog.setMessage(item.getTitle());
                    dialog.setListener(new StreamDialogListener() {
                        public void onClickHls() {
                            startProgress();
                            if (item.isLiveTv())
                                new StreamTvTask((TvShow) item, false).execute();
                            else
                                new StreamHlsTask(item).execute();
                        }

                        public void onClickStream() {
                            startProgress();
                            if (item.isLiveTv())
                                new StreamTvTask((TvShow) item, true).execute();
                            else
                                playRawVideoStream(item);
                        }

                        public void onClickCancel() {
                            stopProgress();
                            onResume();
                        }
                    });

                    if (appSettings.getMediaSettings().getViewType() == ViewType.list) {
                        // list mode - show info dialog
                        String dialogMessage = item.getDialogText();
                        if (item.isLiveTv()) {
                            TvShow tvShow = (TvShow) item;
                            if (tvShow.getEndTime().compareTo(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime()) < 0) {
                                new AlertDialog.Builder(this)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setTitle(getString(R.string.live_tv))
                                        .setMessage(getString(R.string.show_already_ended_) + item.getTitle() + "\n" + tvShow.getChannelInfo() + "\n" + tvShow.getShowTimeInfo())
                                        .setPositiveButton("OK", null)
                                        .show();
                                onResume();
                                return;
                            }
                            dialogMessage += "\n\n" + getString(R.string.recording_will_be_scheduled);
                        }
                        dialog.setMessage(dialogMessage);
                        dialog.show(getFragmentManager(), "StreamVideoDialog");
                    } else  {
                        // detail or split mode -- no dialog unless preferred stream mode is unknown
                        if (appSettings.isPreferHls(item.getFormat())) {
                            startProgress();
                            new StreamHlsTask(item).execute((URL) null);
                        } else if (appSettings.isPreferStreamRaw(item.getFormat())) {
                            startProgress();
                            playRawVideoStream(item);
                        } else {
                            dialog.show(getFragmentManager(), "StreamVideoDialog");
                        }
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
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Starts a transcode without immediately watching.
     */
    protected void transcodeItem(final Item item) {
        item.setPath("");
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(getString(R.string.transcode))
                .setMessage(getString(R.string.begin_transcode) +  " '" + item.getTitle() + "'?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new TranscodeVideoTask(item).execute(getAppSettings().getMythTvServicesBaseUrl());
                        } catch (MalformedURLException ex) {
                            stopProgress();
                            if (BuildConfig.DEBUG)
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
                        onResume();
                    }
                })
                .show();
    }

    protected void deleteRecording(final Recording recording) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.delete_recording) +  " '" + recording.getTitle() + "'?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new DeleteRecordingTask(recording).execute(getAppSettings().getMythTvServicesBaseUrl());
                            boolean removed = false;
                            String path = recording.getPath();
                            if (path.startsWith("/"))
                                path = path.substring(1);
                            if (path.equals(""))
                                removed = mediaList.removeItem(recording);
                            else
                                removed = mediaList.getCategory(path).removeItem(recording);
                            if (removed) {
                                mediaList.setCount(mediaList.getCount() - 1);
                            }
                            onResume();
                        } catch (MalformedURLException ex) {
                            stopProgress();
                            if (BuildConfig.DEBUG)
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
                        onResume();
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
                countdownDialog.setMessage(getString(R.string.playing) + " " + item.getTypeLabel() + ": " + item.getLabel());
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
                // countdownDialog.setProgressDrawable(getResources().getDrawable(R.drawable.countdown_bar));
                countdownDialog.show();
                countdownDialog.setProgress(10);

                tick();
            } catch (Exception ex) {
                if (BuildConfig.DEBUG)
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

            refreshing = true;
            new RefreshTask().execute(getAppSettings().getUrls(getAppSettings().getMediaListUrl()));
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    protected void handleEmptyMediaList() {
        if (isSplitView())
            showSubListPane(null);
        if (getAppSettings().isTv()) {
            // empty list - set focus on action bar
            int actionBarResId = getResources().getIdentifier("action_bar_container", "id", "android");
            getWindow().getDecorView().findViewById(actionBarResId).requestFocus();
        }
    }

    protected void updateActionMenu() {
        showMoviesMenuItem(supportsMovies());
        showTvSeriesMenuItem(supportsTvSeries());
        showMusicMenuItem(supportsMusic());
        showSortMenu(supportsSort());
        showViewMenu(supportsViewMenu());
        showSearchMenu(supportsSearch());

        if (mediaMenuItem != null)
            mediaMenuItem.setTitle(Localizer.getMediaLabel(getAppSettings().getMediaSettings().getType()) + " (" + mediaList.getCount() + ")");

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

    protected Transcoder getTranscoder(Item item) {
        if (item.getStorageGroup() == null)
            return new Transcoder(getAppSettings(), mediaList.getBasePath());
        else
            return new Transcoder(getAppSettings(), item.getStorageGroup());
    }

    private class RefreshTask extends AsyncTask<URL,Integer,Long> {
        private String mediaListJson;
        private String storageGroupsJson;

        private Exception ex;

        protected Long doInBackground(URL... urls) {
            try {
                MediaSettings mediaSettings = getAppSettings().getMediaSettings();

                HttpHelper downloader = getAppSettings().getMediaListDownloader(urls);
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

                MediaListParser mediaListParser = getAppSettings().getMediaListParser(mediaListJson);
                if (getAppSettings().isMythlingMediaServices()) {
                    mediaList = mediaListParser.parseMediaList(mediaSettings.getType(), storageGroups);
                } else {
                    boolean hasMediaStorageGroup = mediaSettings.getType() == MediaType.recordings || mediaSettings.getType() == MediaType.liveTv
                            || (mediaSettings.getType() != MediaType.music && storageGroups.get(appSettings.getVideoStorageGroup()) != null);
                    if (hasMediaStorageGroup) {
                        mediaList = ((MythTvParser) mediaListParser).parseMediaList(mediaSettings.getType(), storageGroups);
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
                        mediaList = ((MythTvParser) mediaListParser).parseMediaList(mediaSettings.getType(), storageGroups, basePath);
                    }
                }

                mediaList.setCharSet(downloader.getCharSet());

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
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
            } else {
                AppData appData = new AppData(getApplicationContext());
                appData.setMediaList(mediaList);
                appData.setStorageGroups(storageGroups);
                setAppData(appData);
                setMediaType(appData.getMediaList().getMediaType());
                getAppSettings().setLastLoad(System.currentTimeMillis());

                try {
                    appData.writeStorageGroups(storageGroupsJson);
                    appData.writeMediaList(mediaListJson);
                    stopProgress();
                    populate();
                } catch (Exception ex) {
                    stopProgress();
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                }
            }
            refreshing = false;
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
                Transcoder transcoder = getTranscoder(item);

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
                if (BuildConfig.DEBUG)
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
                    playLiveStream(streamInfo);
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG)
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
        private Exception ex;

        public TranscodeVideoTask(Item item) {
            this.item = item;
        }

        protected Long doInBackground(URL... urls) {
            try {
                Transcoder transcoder = getTranscoder(item);
                transcoder.beginTranscode(item);
                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
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
                if (BuildConfig.DEBUG)
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
                    Transcoder transcoder = getTranscoder(tvShow);
                    boolean streamAvail = transcoder.beginTranscode(recorder.getRecording());

                    streamInfo = transcoder.getStreamInfo();

                    if (!streamAvail)
                        transcoder.waitAvailable(); // this is the long pole
                }

                return 0L;
            } catch (Exception ex) {
                this.ex = ex;
                if (BuildConfig.DEBUG)
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
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    if (ex != null)
                        Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                    onResume();
                }
            } else {
                try {
                    if (raw)
                        playRawVideoStream(recording);
                    else
                        playLiveStream(streamInfo);
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                    if (getAppSettings().isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Requires MythTV content.cpp patch to work without storage groups (or maybe create
     * a bogus SG called None that points to your backend videos base location).
     */
    private void playRawVideoStream(Item item) {
        try {
            final URL baseUrl = getAppSettings().getMythTvServicesBaseUrlWithCredentials();
            String itemPath = item.isRecording() || item.getPath().isEmpty() ? item.getFileName() : item.getPath() + "/" + item.getFileName();
            String fileUrl = baseUrl + "/Content/GetFile?FileName=" + URLEncoder.encode(itemPath, "UTF-8");
            if (item.getStorageGroup() == null)
                fileUrl += "&StorageGroup=None";
            else
                fileUrl += "&StorageGroup=" + item.getStorageGroup().getName();

            stopProgress();
            if (appSettings.isExternalVideoPlayer()) {
                Intent toStart = new Intent(Intent.ACTION_VIEW);
                toStart.setDataAndType(Uri.parse(fileUrl), "video/*");
                startActivity(toStart);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl), getApplicationContext(), VideoViewActivity.class));
            }
        } catch (IOException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    protected void playLiveStream(LiveStreamInfo streamInfo) throws IOException {
        String streamUrl = appSettings.getMythTvServicesBaseUrlWithCredentials() + streamInfo.getRelativeUrl();

        // avoid retrieving unnecessary audio-only streams
        int lastDot = streamUrl.lastIndexOf('.');
        streamUrl = streamUrl.substring(0, lastDot) + ".av" + streamUrl.substring(lastDot);

        stopProgress();
        if (appSettings.isExternalVideoPlayer()) {
            Intent toStart = new Intent(Intent.ACTION_VIEW);
            toStart.setDataAndType(Uri.parse(streamUrl), "video/*");
            startActivity(toStart);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl), getApplicationContext(), VideoViewActivity.class));
        }
    }

    void initListViewOnItemClickListener() {
        if (getListables().size() > 0) {
            getListView().setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    currentTop = getListView().getFirstVisiblePosition();
                    View topV = getListView().getChildAt(0);
                    topOffset = (topV == null) ? 0 : topV.getTop();
                    selItemIndex = position;
                    boolean isItem = getListables().get(position) instanceof Item;
                    if (isItem) {
                        Item item = (Item) getListables().get(position);
                        if (isSplitView()) {
                            getListAdapter().setSelection(selItemIndex);
                            getListView().setItemChecked(selItemIndex, true);
                            if (appSettings.isTv()) {
                            	// play the item since already selected
                            	item.setPath(getPath());
                            	playItem(item);
                            } else {
                                showItemInDetailPane(position, true);
                            }
                        } else {
                            item.setPath(getPath());
                            playItem(item);
                        }
                    } else {
                        // must be category
                        String cat = ((TextView)view).getText().toString();
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
                        showSubListPane(getPath() + "/" + sel.getLabel(), grab);
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
        if (selItemIndex != -1) {
            getListView().setItemChecked(selItemIndex, true);
            Listable preSel = getListables().get(selItemIndex);
            if (preSel instanceof Item)
                showItemInDetailPane(selItemIndex);
            else
                showSubListPane(getPath() + "/" + preSel.getLabel());
        }
    }

    protected void setPathFromIntent() {
        try {
            String newPath = getIntent().getDataString();
            setPath(newPath == null ? "" : URLDecoder.decode(newPath, "UTF-8"));
        }
        catch (UnsupportedEncodingException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(getApplicationContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            setPath(""); // TODO correct?
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v == getListView()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Listable listable = (Listable)getListView().getItemAtPosition(info.position);
            if (listable instanceof Item && !((Item)listable).isLiveTv() && !((Item)listable).isMusic()) {
                Item item = (Item)listable;
                menu.setHeaderTitle(item.getTitle());
                String[] menuItems = getResources().getStringArray(R.array.item_long_click_menu);
                for (int i = 0; i < menuItems.length; i++)
                    menu.add(MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID, i, i, menuItems[i]);
                if (item.isRecording())
                    menu.add(MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID, 2, 2, getString(R.string.delete));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == MEDIA_ACTIVITY_CONTEXT_MENU_GROUP_ID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            if (item.getItemId() == 0) {
                playItem((Item)getListView().getItemAtPosition(info.position));
                return true;
            } else if (item.getItemId() == 1) {
                transcodeItem((Item)getListView().getItemAtPosition(info.position));
                return true;
            } else if (item.getItemId() == 2) {
                Recording rec = (Recording)getListView().getItemAtPosition(info.position);
                rec.setPath(path);
                deleteRecording(rec);
                return true;
            }
        }
        return false;
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
