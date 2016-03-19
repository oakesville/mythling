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
package com.oakesville.mythling.app;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oakesville.mythling.R;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.PlaybackOptions;
import com.oakesville.mythling.media.Song;
import com.oakesville.mythling.prefs.DevicePrefsSpec;
import com.oakesville.mythling.prefs.firetv.FireTvPrefsSpec;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.HttpHelper.AuthType;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.MythlingParser;
import com.oakesville.mythling.util.Reporter;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

public class AppSettings {
    private static final String TAG = AppSettings.class.getSimpleName();

    public static final String DEVICE_PLAYBACK_CATEGORY_VIDEO = "device_playback_cat_video";
    public static final String DEVICE_PLAYBACK_CATEGORY_MUSIC = "device_playback_cat_music";
    public static final String FRONTEND_PLAYBACK_CATEGORY = "frontend_playback_cat";
    public static final String INTERNAL_BACKEND_CATEGORY = "internal_backend_cat";
    public static final String EXTERNAL_BACKEND_CATEGORY = "external_backend_cat";
    public static final String MYTHLING_SERVICE_ACCESS_CATEGORY = "mythling_service_access_cat";
    public static final String MEDIA_SERVICES_CATEGORY = "media_services_cat";
    public static final String MYTHWEB_ACCESS_CATEGORY = "mythweb_access_cat";
    public static final String MYTHWEB_ACCESS = "mythweb_access";
    public static final String ERROR_REPORTING = "error_reporting";
    public static final String MYTH_BACKEND_INTERNAL_HOST = "mythbe_internal_host";
    public static final String MYTH_BACKEND_EXTERNAL_HOST = "mythbe_external_host";
    public static final String RETRIEVE_TRANSCODE_STATUSES = "retrieve_transcode_statuses";
    public static final String BACKEND_WEB = "backend_web";
    public static final String MYTHLING_MEDIA_SERVICES = "media_services";
    public static final String MYTHLING_WEB_PORT = "mythling_web_port";
    public static final String MYTHLING_WEB_ROOT = "mythling_web_root";
    public static final String MYTHWEB_WEB_ROOT = "mythweb_web_root";
    public static final String MYTHTV_SERVICE_PORT = "mythtv_service_port";
    public static final String MYTH_FRONTEND_HOST = "mythfe_host";
    public static final String MYTH_FRONTEND_SOCKET_PORT = "mythfe_socket_port";
    public static final String MYTH_FRONTEND_SERVICE_PORT = "mythfe_service_port";
    public static final String MEDIA_TYPE = "media_type";
    public static final String VIEW_TYPE = "view_type";
    public static final String SORT_TYPE = "sort_type";
    public static final String FRONTEND_PLAYBACK = "playback_mode";
    public static final String INTERNAL_MUSIC_PLAYER = "music_player";
    public static final String EXTERNAL_NETWORK = "network_location";
    public static final String CATEGORIZE_VIDEOS = "categorize_videos";
    public static final String MOVIE_DIRECTORIES = "movie_directories";
    public static final String TV_SERIES_DIRECTORIES = "tv_series_directories";
    public static final String VIDEO_EXCLUDE_DIRECTORIES = "video_exclude_directories";
    public static final String ARTWORK_SG_VIDEOS = "artwork_sg_videos";
    public static final String ARTWORK_SG_RECORDINGS = "artwork_sg_recordings";
    public static final String ARTWORK_SG_MOVIES = "artwork_sg_movies";
    public static final String ARTWORK_SG_TVSERIES = "artwork_sg_tvseries";
    public static final String ARTWORK_SG_COVERART = "Coverart";
    public static final String ARTWORK_SG_FANART = "Fanart";
    public static final String ARTWORK_SG_BANNERS = "Banners";
    public static final String ARTWORK_SG_SCREENSHOTS = "Screenshots";
    public static final String DEFAULT_ARTWORK_SG_RECORDINGS = ARTWORK_SG_SCREENSHOTS;
    public static final String DEFAULT_ARTWORK_SG_VIDEOS = ARTWORK_SG_BANNERS;
    public static final String DEFAULT_ARTWORK_SG_MOVIES = ARTWORK_SG_COVERART;
    public static final String DEFAULT_ARTWORK_SG_TV_SERIES = ARTWORK_SG_COVERART;
    public static final String MUSIC_ART_LEVEL_SONG = "album_art_level";
    public static final String INTERNAL_VIDEO_RES = "internal_video_res";
    public static final String EXTERNAL_VIDEO_RES = "external_video_res";
    public static final String INTERNAL_VIDEO_BITRATE = "internal_video_bitrate";
    public static final String EXTERNAL_VIDEO_BITRATE = "external_video_bitrate";
    public static final String INTERNAL_AUDIO_BITRATE = "internal_audio_bitrate";
    public static final String EXTERNAL_AUDIO_BITRATE = "external_audio_bitrate";
    public static final String CACHE_EXPIRE_MINUTES = "cache_expiry";
    public static final String LAST_LOAD = "last_load";
    public static final String RETRIEVE_IP = "retrieve_ip";
    public static final String IP_RETRIEVAL_URL = "ip_retrieval_url";
    public static final String MYTHTV_SERVICES_AUTH_TYPE = "mythtv_services_auth_type";
    public static final String MYTHTV_SERVICES_USER = "mythtv_services_user";
    public static final String MYTHTV_SERVICES_PASSWORD = "mythtv_services_password";
    public static final String MYTHLING_SERVICES_AUTH_TYPE = "mythling_services_auth_type";
    public static final String MYTHLING_SERVICES_USER = "mythling_services_user";
    public static final String MYTHLING_SERVICES_PASSWORD = "mythling_services_password";
    public static final String MYTHLING_VERSION = "mythling_version";
    public static final String TUNER_TIMEOUT = "tuner_timeout";
    public static final String TUNER_LIMIT = "tuner_limit";
    public static final String TRANSCODE_TIMEOUT = "transcode_timeout";
    public static final String TRANSCODE_JOB_LIMIT = "transcode_job_limit";
    public static final String HTTP_CONNECT_TIMEOUT = "http_connect_timeout";
    public static final String HTTP_READ_TIMEOUT = "http_read_timeout";
    public static final String PAGER_CURRENT_POSITION = "movie_current_position";
    public static final String DEFAULT_MEDIA_TYPE = "recordings";
    public static final String MOVIE_BASE_URL = "movie_base_url";
    public static final String TV_BASE_URL = "tv_base_url";
    public static final String CUSTOM_BASE_URL = "custom_base_url";
    public static final String THEMOVIEDB_BASE_URL = "http://www.themoviedb.org/movie/";
    public static final String THETVDB_BASE_URL = "http://www.thetvdb.com";
    public static final String AUTH_TYPE_NONE = "None";
    public static final String AUTH_TYPE_SAME = "(Same as MythTV Services)";
    public static final String GUIDE_HTML = "guide.html";
    public static final String GUIDE_OMB_HTML = "guide-omb.html";
    public static final String HOSTED_EPG = "hosted_epg";
    public static final String MYTHLING_EPG = "mythling-epg";
    public static final String HOSTED_EPG_ROOT = "hosted_epg_root";
    public static final String EPG_CHANNEL_GROUP = "epg_channel_group";
    public static final String EPG_CHANNEL_ICONS = "epg_channel_icons";
    public static final String EPG_OMB = "epg_omb";
    public static final String EPG_SCALE = "epg_scale";
    public static final String EPG_PARAMS = "epg_params";
    public static final String EPG_SKIP_INTERVAL = "epg_skip_interval";
    public static final String EPG_LAST_LOAD = "epg_last_load";
    public static final String EPG_INITIALLY_ACCESSED = "epg_initially_accessed";
    public static final String PREFS_INITIALLY_SET = "prefs_initially_set";
    public static final String ALWAYS_PROMPT_FOR_PLAYBACK_OPTIONS = "always_prompt_for_playback_options";
    public static final String SAVE_POSITION_ON_EXIT = "save_position_on_exit";
    public static final String VIDEO_SAVED_POSITION = "video_saved_position";
    public static final String SKIP_FORWARD_INTERVAL = "skip_forward_interval";
    public static final String SKIP_BACK_INTERVAL = "skip_back_interval";
    public static final String JUMP_INTERVAL = "jump_interval";
    public static final String AUTO_SKIP = "auto_skip";
    public static final String AUTO_SKIP_OFF = "auto_skip_off";
    public static final String AUTO_SKIP_ON = "auto_skip_on";
    public static final String AUTO_SKIP_NOTIFY = "auto_skip_notify";
    public static final String LIBVLC_PARAMETERS = "libvlc_parameters";
    public static final String SEEK_CORRECTION_TOLERANCE = "seek_correction_tolerance";
    public static final String IGNORE_LIBVLC_CPU_COMPATIBILITY = "ignore_libvlc_cpu_compatibility";
    public static final String EXTERNAL_VIDEO_QUALITY = "external_video_quality";
    public static final String INTERNAL_VIDEO_QUALITY = "internal_video_quality";
    public static final String PLAYBACK_OPTIONS_JSON = "playback_options_json";
    public static final String EXTERNAL_STORAGE_DIRECTORY = "mythling";

    private Context appContext;
    public Context getAppContext() { return appContext; }

    private SharedPreferences prefs;
    public SharedPreferences getPrefs() { return prefs; }

    public AppSettings(Context appContext) {
        this.appContext = appContext;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        if (!devicePrefsSpecsLoaded) {
            // initialize static device pref constraints
            try {
                // perform this test for all supported devices with pref constraints
                DevicePrefsSpec test = new FireTvPrefsSpec(getAppContext());
                if (test.appliesToDevice(Build.MANUFACTURER, Build.MODEL)) {
                    devicePrefsSpec = test;
                }
                devicePrefsSpecsLoaded = true;
            }
            catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (isErrorReportingEnabled())
                    new Reporter(ex).send();
                Toast.makeText(appContext, appContext.getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
            }
        }

        // initialize mythling version for static access
        getMythlingVersion();
        // initialize localizer
        if (!Localizer.isInitialized())
            Localizer.initialize(this);
    }

    public URL getMythlingWebBaseUrl() throws MalformedURLException {
        String ip = getMythlingServiceHost();
        int port = getMythlingServicePort();
        String root = getMythlingWebRoot();
        return new URL("http://" + ip + ":" + port + (root == null || root.length() == 0 ? "" : "/" + root));
    }

    public String getMythWebUrl() {
        String host = getBackendHost();
        int port = getMythlingWebPort();
        String root = getMythwebWebRoot();
        return "http://" + host + ":" + port + "/" + root;
    }

    public URL getMediaListUrl(MediaType mediaType) throws MalformedURLException, UnsupportedEncodingException {
        MediaSettings mediaSettings = getMediaSettings();
        String url;
        if (isMythlingMediaServices()) {
            url = getMythlingWebBaseUrl() + "/media.php?type=" + mediaType.toString();
            url += getVideoTypeParams() + getArtworkParams(mediaType);
            if (mediaSettings.getSortType() == SortType.byDate)
                url += "&sort=date";
            else if (mediaSettings.getSortType() == SortType.byRating)
                url += "&sort=rating";
            else if (mediaSettings.getSortType() == SortType.byCallsign)
                url += "&sort=callsign";
            else if (mediaType == MediaType.recordings && getMediaSettings().getViewType() == ViewType.detail)
                url += "&flatten=true";
        } else {
            url = getMythTvServicesBaseUrl() + "/";
            if (mediaType == MediaType.videos || mediaType == MediaType.movies || mediaType == MediaType.tvSeries)
                url += "Video/GetVideoList";
            else if (mediaType == MediaType.recordings)
                url += "Dvr/GetRecordedList?Descending=true";
            else if (mediaType == MediaType.liveTv) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                String nowUtc = Localizer.SERVICE_DATE_TIME_RAW_FORMAT.format(cal.getTime()).replace(' ', 'T');
                url += "Guide/GetProgramGuide?StartTime=" + nowUtc + "&EndTime=" + nowUtc;
            }
        }

        return new URL(url);
    }

    public URL getCutListBaseUrl() throws MalformedURLException, UnsupportedEncodingException {
        if (isMythlingMediaServices())
            return new URL(getMythlingWebBaseUrl() + "/media.php?type=cutList&");
        else
            return new URL(getMythTvServicesBaseUrl() + "/Dvr/GetRecordedCommBreak?OffsetType=Duration&");
    }

    public URL getMediaListUrl() throws MalformedURLException, UnsupportedEncodingException {
        return getMediaListUrl(getMediaSettings().getType());
    }

    /**
     * If not empty, always begins with '&'.
     */
    public String getVideoTypeParams() throws UnsupportedEncodingException {
        String params = "";
        if (getMediaSettings().getTypeDeterminer() == MediaTypeDeterminer.directories) {
            String movieDirs = getMovieDirectories();
            if (movieDirs != null && !movieDirs.trim().isEmpty())
                params += "&movieDirs=" + URLEncoder.encode(movieDirs.trim(), "UTF-8");
            String tvSeriesDirs = getTvSeriesDirectories();
            if (tvSeriesDirs != null && !tvSeriesDirs.trim().isEmpty())
                params += "&tvSeriesDirs=" + URLEncoder.encode(tvSeriesDirs.trim(), "UTF-8");
            String videoExcludeDirs = getVideoExcludeDirectories();
            if (videoExcludeDirs != null && !videoExcludeDirs.trim().isEmpty())
                params += "&videoExcludeDirs=" + URLEncoder.encode(videoExcludeDirs.trim(), "UTF-8");
        } else if (getMediaSettings().getTypeDeterminer() == MediaTypeDeterminer.metadata) {
            params += "&categorizeUsingMetadata=true";
        }
        return params;
    }

    public boolean isVideosCategorization() {
        return getMediaSettings().getTypeDeterminer() != MediaTypeDeterminer.none;
    }

    /**
     * If not empty, always begins with '&'.
     */
    public String getArtworkParams(MediaType mediaType) throws UnsupportedEncodingException {
        String params = "";
        String prefStorageGroup = getArtworkStorageGroup(mediaType);
        params += "&artworkStorageGroup=" + prefStorageGroup;
        if (!isAlbumArtAlbumLevel())
            params += "&albumArtSongLevel=true";
        return params;
    }

    public URL getSearchUrl(String query) throws MalformedURLException, UnsupportedEncodingException {
        if (isMythlingMediaServices()) {
            return new URL(getMythlingWebBaseUrl() + "/media.php?type=search&query=" + URLEncoder.encode(query, "UTF-8")
                    + getVideoTypeParams());
        } else {
            return null;
        }
    }

    public URL getMythTvServicesBaseUrl() throws MalformedURLException {
        String ip = getMythTvServiceHost();
        int servicePort = getMythServicePort();
        return new URL("http://" + ip + ":" + servicePort);
    }

    /**
     * Excludes credentials (used by webview for comparison in shouldIntercept).
     */
    public URL getEpgBaseUrl() throws MalformedURLException, UnsupportedEncodingException {
        if (isMythlingMediaServices()) {
            int port = getMythlingWebPort();
            // extraneous port 80 causes mismatch in EpgActivity shouldInterceptRequest
            return new URL("http://" + getMythlingServiceHost() + (port == 80 ? "" : ":" + getMythlingWebPort()) + "/" + getHostedEpgRoot());
        }
        else {
            return new URL(getMythTvServicesBaseUrl() + "/" + getHostedEpgRoot());
        }
    }

    /**
     * Returns the base guide service URL without parameters.
     */
    public URL getGuideServiceUrl() throws MalformedURLException, UnsupportedEncodingException {
        if (isMythlingMediaServices()) {
            int port = getMythlingWebPort();
            // extraneous port 80 causes mismatch in EpgActivity shouldInterceptRequest
            return new URL("http://" + getMythlingServiceHost() + (port == 80 ? "" : ":" + getMythlingWebPort()) +
                    "/" + getMythlingWebRoot() + "/media.php?type=guide");
        }
        else {
            return new URL(getMythTvServicesBaseUrl() + "/Guide/GetProgramGuide");
        }
    }

    /**
     * Params are added separately in epg activities.
     */
    public URL getEpgUrl() throws MalformedURLException, UnsupportedEncodingException {
        URL epgUrl = getEpgBaseUrl();
        if (isEpgOmb())
            return new URL(epgUrl + "/" + GUIDE_OMB_HTML);
        else
            return new URL(epgUrl + "/" + GUIDE_HTML);
    }

    public boolean isHostedEpg() {
        return getBooleanPref(HOSTED_EPG, false);
    }

    public String getHostedEpgRoot() {
        return getStringPref(HOSTED_EPG_ROOT, MYTHLING_EPG);
    }

    public String getEpgChannelGroup() {
        return getStringPref(EPG_CHANNEL_GROUP, "");
    }

    public boolean isEpgChannelIcons() {
        return getBooleanPref(EPG_CHANNEL_ICONS, false);
    }

    public boolean isEpgOmb() {
        return getBooleanPref(EPG_OMB, Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !isFireTv());
    }

    public String getEpgScale() {
        String def = "1.0";
        if (isPhone())
            def = "0.5";
        else if (isTv())
            def = "1.5";
        return getStringPref(EPG_SCALE, def);
    }

    public String getEpgParams() {
        return getStringPref(EPG_PARAMS, "");
    }

    /**
     * in hours (TODO: allow user to set)
     */
    public int getEpgSkipInterval() {
        return getIntPref(EPG_SKIP_INTERVAL, 12);
    }

    public long getEpgLastLoad() {
        return getLongPref(EPG_LAST_LOAD, 0);
    }

    public boolean setEpgLastLoad(long ll) {
        Editor ed = prefs.edit();
        ed.putLong(EPG_LAST_LOAD, ll);
        return ed.commit();
    }

    public URL getMythTvServicesBaseUrlWithCredentials() throws MalformedURLException, UnsupportedEncodingException {
        String host = getMythTvServiceHost();
        int servicePort = getMythServicePort();
        if (AuthType.None.toString().equals(getMythTvServicesAuthType())) {
            return new URL("http://" + host + ":" + servicePort);
        } else {
            String encodedUser = URLEncoder.encode(getMythTvServicesUser(), "UTF-8");
            String encodedPw = URLEncoder.encode(getMythTvServicesPassword(), "UTF-8");
            return new URL("http://" + encodedUser + ":" + encodedPw + "@" + host + ":" + servicePort);
        }
    }

    public URL getMythlingServicesBaseUrlWithCredentials() throws MalformedURLException, UnsupportedEncodingException {
        String host = getMythlingServiceHost();
        int servicePort = getMythlingServicePort();
        if (AuthType.None.toString().equals(getMythlingServicesAuthType())) {
            return new URL("http://" + host + ":" + servicePort);
        } else {
            String encodedUser = URLEncoder.encode(getBackendWebUser(), "UTF-8");
            String encodedPw = URLEncoder.encode(getBackendWebPassword(), "UTF-8");
            return new URL("http://" + encodedUser + ":" + encodedPw + "@" + host + ":" + servicePort);
        }
    }

    public URL getMythTvContentServiceBaseUrl() throws MalformedURLException {
        return new URL(getMythTvServicesBaseUrl() + "/Content");
    }

    public URL getArtworkBaseUrl(String storageGroup) throws MalformedURLException {
        return new URL(getMythTvContentServiceBaseUrl() + "/GetImageFile?StorageGroup=" + storageGroup);
    }

    public int getMythServicePort() {
        if (isServiceProxy())
            return getServiceProxyPort();
        else
            return getMythTvServicePort();
    }

    public int getMythTvServicePort() {
        return Integer.parseInt(getStringPref(MYTHTV_SERVICE_PORT, "6544").trim());
    }

    public int getMythlingServicePort() {
        if (isServiceProxy())
            return getServiceProxyPort();
        else
            return getMythlingWebPort();
    }

    public int getMythlingWebPort() {
        return Integer.parseInt(getStringPref(MYTHLING_WEB_PORT, "80").trim());
    }

    public String getMythlingWebRoot() {
        return getStringPref(MYTHLING_WEB_ROOT, "mythling");
    }

    public String getMythwebWebRoot() {
        return getStringPref(MYTHWEB_WEB_ROOT, "mythweb");
    }

    public String getFrontendHost() {
        return getStringPref(MYTH_FRONTEND_HOST, "192.168.0.68").trim();
    }

    public int getFrontendSocketPort() {
        return Integer.parseInt(getStringPref(MYTH_FRONTEND_SOCKET_PORT, "6546").trim());
    }

    public int getFrontendServicePort() {
        return Integer.parseInt(getStringPref(MYTH_FRONTEND_SERVICE_PORT, "6547").trim());
    }

    public URL getFrontendServiceBaseUrl() throws MalformedURLException {
        String ip = getFrontendHost();
        int servicePort = getFrontendServicePort();
        return new URL("http://" + ip + ":" + servicePort);
    }

    public boolean isDevicePlayback() {
        return !getBooleanPref(FRONTEND_PLAYBACK, false);
    }

    public boolean isCpuCompatibleWithLibVlcPlayer() {
        for (String cpu : getSupportedCpus()) {
            Log.d(TAG, "Supported cpu: " + cpu);
            if (cpu.equals("armeabi-v7a"))
                return true;
        }
        return false;
    }

    public boolean isIgnoreLibVlcCpuCompatibility() {
        return getBooleanPref(IGNORE_LIBVLC_CPU_COMPATIBILITY, false);
    }

    public int getSkipBackInterval() {
        return Integer.parseInt(getStringPref(SKIP_BACK_INTERVAL, "10"));
    }

    public int getSkipForwardInterval() {
        return Integer.parseInt(getStringPref(SKIP_FORWARD_INTERVAL, "30"));
    }

    public int getJumpInterval() {
        return Integer.parseInt(getStringPref(JUMP_INTERVAL, "600"));
    }

    public String getAutoSkip() {
        return getStringPref(AUTO_SKIP, AUTO_SKIP_OFF);
    }

    public boolean setAutoSkip(String option) {
        return setStringPref(AUTO_SKIP, option);
    }

    public boolean isAlwaysPromptForPlaybackOptions() {
        return getBooleanPref(ALWAYS_PROMPT_FOR_PLAYBACK_OPTIONS, true);
    }

    public void setAlwaysPromptForPlaybackOptions(boolean alwaysPrompt) {
        setBooleanPref(ALWAYS_PROMPT_FOR_PLAYBACK_OPTIONS, alwaysPrompt);
    }

    public boolean isSavePositionOnExit() {
        return getBooleanPref(SAVE_POSITION_ON_EXIT, true);
    }

    public int getSeekCorrectionTolerance() {
        String pref = getStringPref(SEEK_CORRECTION_TOLERANCE, "0");
        return pref.isEmpty() ? 0 : Integer.parseInt(pref);
    }
    public boolean setSeekCorrectionTolerance(int tol) {
        return setStringPref(SEEK_CORRECTION_TOLERANCE, String.valueOf(tol));
    }

    public String getLibVlcParameters() {
        return getStringPref(LIBVLC_PARAMETERS, "");
    }

    public List<String> getVlcOptions() {
        List<String> options = null;
        String params = getLibVlcParameters();
        for (String param : params.split("\\s+")) {
            if (param.startsWith("--")) {
                if (options == null)
                    options = new ArrayList<String>();
                options.add(param);
            }
        }
        return options;
    }

    public List<String> getVlcMediaOptions() {
        List<String> options = new ArrayList<String>();
        String params = getLibVlcParameters();
        for (String param : params.split("\\s+")) {
            if (param.indexOf(":") >= 0) {
                options.add(param);
            }
        }
        return options;
    }

    public String getPlaybackOptionsJson() {
        return getStringPref(PLAYBACK_OPTIONS_JSON, "{}");
    }

    public boolean setPlaybackOptionsJson(String json) {
        Editor ed = prefs.edit();
        ed.putString(PLAYBACK_OPTIONS_JSON, json);
        return ed.commit();
    }

    public boolean isExternalMusicPlayer() {
        return !getBooleanPref(INTERNAL_MUSIC_PLAYER, true);
    }

    public boolean isMythlingMediaServices() {
        return getBooleanPref(MYTHLING_MEDIA_SERVICES, false);
    }

    public boolean setMythlingMediaServices(boolean mythlingSvcs) {
        Editor ed = prefs.edit();
        ed.putBoolean(MYTHLING_MEDIA_SERVICES, mythlingSvcs);
        return ed.commit();
    }

    public boolean isHasBackendWeb() {
        return getBooleanPref(BACKEND_WEB, false);
    }

    public boolean isRetrieveTranscodeStatuses() {
        return getBooleanPref(RETRIEVE_TRANSCODE_STATUSES, true);
    }

    public boolean isMythWebAccessEnabled() {
        return getBooleanPref(MYTHWEB_ACCESS, false);
    }

    public boolean isErrorReportingEnabled() {
        return getBooleanPref(ERROR_REPORTING, false);
    }

    public void logAndReport(String tag, Throwable t) {
        Log.e(tag, t.getMessage(), t);
        if (isErrorReportingEnabled())
            new Reporter(t).send();
    }

    public boolean isExternalNetwork() {
        return getBooleanPref(EXTERNAL_NETWORK, false);
    }

    public String getInternalBackendHost() {
        return getStringPref(MYTH_BACKEND_INTERNAL_HOST, "192.168.0.69").trim();
    }

    public String getExternalBackendHost() {
        return getStringPref(MYTH_BACKEND_EXTERNAL_HOST, "192.168.0.69").trim();
    }

    public String getMovieDirectories() {
        return getStringPref(MOVIE_DIRECTORIES, "");
    }

    public String[] getMovieDirs() {
        String[] movieDirs = getMovieDirectories().split(",");
        for (int i = 0; i < movieDirs.length; i++) {
            if (!movieDirs[i].endsWith("/"))
                movieDirs[i] += "/";
        }
        return movieDirs;
    }

    public String getTvSeriesDirectories() {
        return getStringPref(TV_SERIES_DIRECTORIES, "");
    }

    public String[] getTvSeriesDirs() {
        String[] tvDirs = getTvSeriesDirectories().split(",");
        for (int i = 0; i < tvDirs.length; i++) {
            if (!tvDirs[i].endsWith("/"))
                tvDirs[i] += "/";
        }
        return tvDirs;
    }

    public String getVideoExcludeDirectories() {
        return getStringPref(VIDEO_EXCLUDE_DIRECTORIES, "");
    }

    public String[] getVidExcludeDirs() {
        String[] vidExcludeDirs = getVideoExcludeDirectories().split(",");
        for (int i = 0; i < vidExcludeDirs.length; i++) {
            if (!vidExcludeDirs[i].endsWith("/"))
                vidExcludeDirs[i] += "/";
        }
        return vidExcludeDirs;
    }

    public String getMovieBaseUrl() {
        return getStringPref(MOVIE_BASE_URL, THEMOVIEDB_BASE_URL);
    }

    public String getTvBaseUrl() {
        return getStringPref(TV_BASE_URL, THETVDB_BASE_URL);
    }

    public String getCustomBaseUrl() {
        return getStringPref(CUSTOM_BASE_URL, "");
    }

    public String getMythTvServiceHost() {
        if (isServiceProxy())
            return getServiceProxyIp();
        else
            return getBackendHost();
    }

    public String getMythlingServiceHost() {
        if (isServiceProxy())
            return getServiceProxyIp();
        else
            return getBackendHost();
    }

    public String getBackendHost() {
        if (isExternalNetwork())
            return getExternalBackendHost();
        else
            return getInternalBackendHost();
    }

    public URL[] getUrls(URL url) throws MalformedURLException {
        if (isExternalNetwork() && isIpRetrieval())
            return new URL[]{url, getIpRetrievalUrl()};
        else
            return new URL[]{url};
    }

    public String getVideoStorageGroup() {
        // TODO prefs
        return "Videos";
    }

    public String getArtworkStorageGroup(MediaType mediaType) {
        if (mediaType == MediaType.music)
            return isAlbumArtAlbumLevel() ? Song.ARTWORK_LEVEL_ALBUM : Song.ARTWORK_LEVEL_SONG;
        else if (mediaType == MediaType.videos)
            return getStringPref(ARTWORK_SG_VIDEOS, DEFAULT_ARTWORK_SG_VIDEOS);
        else if (mediaType == MediaType.recordings)
            return getStringPref(ARTWORK_SG_RECORDINGS, DEFAULT_ARTWORK_SG_RECORDINGS);
        else if (mediaType == MediaType.movies)
            return getStringPref(ARTWORK_SG_MOVIES, DEFAULT_ARTWORK_SG_MOVIES);
        else if (mediaType == MediaType.tvSeries)
            return getStringPref(ARTWORK_SG_TVSERIES, DEFAULT_ARTWORK_SG_TV_SERIES);
        else
            return DEFAULT_ARTWORK_SG_VIDEOS;
    }

    public boolean isAlbumArtAlbumLevel() {
        return !getBooleanPref(MUSIC_ART_LEVEL_SONG, false);
    }

    public int getVideoRes() {
        if (isExternalNetwork())
            return getExternalVideoRes();
        else
            return getInternalVideoRes();
    }

    public int getVideoBitrate() {
        if (isExternalNetwork())
            return getExternalVideoBitrate();
        else
            return getInternalVideoBitrate();
    }

    public int getAudioBitrate() {
        if (isExternalNetwork())
            return getExternalAudioBitrate();
        else
            return getInternalAudioBitrate();
    }

    public String getVideoQualityParams() {
        return getVideoQualityParams(null);
    }

    public String getVideoQualityParams(String videoQuality) {
        if (EXTERNAL_VIDEO_QUALITY.equals(videoQuality))
            return "Height=" + getExternalVideoRes() + "&Bitrate=" + getExternalVideoBitrate() + "&AudioBitrate=" + getExternalAudioBitrate();
        else if (INTERNAL_VIDEO_QUALITY.equals(videoQuality))
            return "Height=" + getInternalVideoRes() + "&Bitrate=" + getInternalVideoBitrate() + "&AudioBitrate=" + getInternalAudioBitrate();
        else
            return "Height=" + getVideoRes() + "&Bitrate=" + getVideoBitrate() + "&AudioBitrate=" + getAudioBitrate();
    }

    public int getInternalVideoRes() {
        return Integer.parseInt(getStringPref(INTERNAL_VIDEO_RES, "480"));
    }

    public int getExternalVideoRes() {
        return Integer.parseInt(getStringPref(EXTERNAL_VIDEO_RES, "240"));
    }

    public int getInternalVideoBitrate() {
        return Integer.parseInt(getStringPref(INTERNAL_VIDEO_BITRATE, "600000"));
    }

    public int getExternalVideoBitrate() {
        return Integer.parseInt(getStringPref(EXTERNAL_VIDEO_BITRATE, "400000"));
    }

    public int getInternalAudioBitrate() {
        return Integer.parseInt(getStringPref(INTERNAL_AUDIO_BITRATE, "64000"));
    }

    public int getExternalAudioBitrate() {
        return Integer.parseInt(getStringPref(EXTERNAL_AUDIO_BITRATE, "64000"));
    }

    private int[] videoResValues;
    public int[] getVideoResValues() {
        if (videoResValues == null)
            videoResValues = stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_res_values));
        return videoResValues;
    }

    private int[] videoBitrateValues;
    public int[] getVideoBitrateValues() {
        if (videoBitrateValues == null)
            videoBitrateValues = stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_bitrate_values));
        return videoBitrateValues;
    }

    private int[] audioBitrateValues;
    public int[] getAudioBitrateValues() {
        if (audioBitrateValues == null)
            audioBitrateValues = stringArrayToIntArray(appContext.getResources().getStringArray(R.array.audio_bitrate_values));
        return audioBitrateValues;
    }

    /**
     * seconds
     */
    public int getVideoPlaybackPosition(Uri uri) {
        return getIntPref(VIDEO_SAVED_POSITION + "_" + uri, 0);
    }

    public boolean setVideoPlaybackPosition(Uri uri, int position) {
        Editor ed = prefs.edit();
        ed.putInt(VIDEO_SAVED_POSITION + "_" + uri, position);
        return ed.commit();
    }

    public boolean clearVideoPlaybackPosition(Uri uri) {
        Editor ed = prefs.edit();
        ed.remove(VIDEO_SAVED_POSITION + "_" + uri);
        return ed.commit();
    }

    private int[] stringArrayToIntArray(String[] stringVals) {
        int[] values = new int[stringVals.length];
        for (int i = 0; i < stringVals.length; i++)
            values[i] = Integer.parseInt(stringVals[i]);
        return values;
    }

    private MediaSettings mediaSettings;

    public MediaSettings getMediaSettings() {
        if (mediaSettings == null) {
            String mediaType = getStringPref(MEDIA_TYPE, DEFAULT_MEDIA_TYPE);
            mediaSettings = new MediaSettings(mediaType);
            String typeDeterminer = getStringPref(CATEGORIZE_VIDEOS, MediaTypeDeterminer.metadata.toString());
            mediaSettings.setTypeDeterminer(typeDeterminer);
            String viewType = getStringPref(VIEW_TYPE + ":" + mediaSettings.getType().toString(), getDefaultViewType(mediaSettings.getType()).toString());
            mediaSettings.setViewType(viewType);
            String sortType = getStringPref(SORT_TYPE + ":" + mediaSettings.getType().toString(), mediaSettings.isLiveTv() ? "byChannel" : "byTitle");
            mediaSettings.setSortType(sortType);
        }
        return mediaSettings;
    }

    public void clearMediaSettings() {
        mediaSettings = null;
    }

    public boolean setMediaType(MediaType mediaType) {
        Editor ed = prefs.edit();
        ed.putString(MEDIA_TYPE, mediaType.toString());
        boolean res = ed.commit();
        mediaSettings = null;
        return res;
    }

    public boolean setVideoCategorization(String videosCategorization) {
        Editor ed = prefs.edit();
        ed.putString(CATEGORIZE_VIDEOS, videosCategorization);
        boolean res = ed.commit();
        mediaSettings = null;
        return res;
    }

    public boolean setViewType(ViewType type) {
        Editor ed = prefs.edit();
        ed.putString(VIEW_TYPE + ":" + getMediaSettings().getType().toString(), type.toString());
        boolean res = ed.commit();
        mediaSettings = null;
        return res;
    }

    public boolean setSortType(SortType type) {
        Editor ed = prefs.edit();
        ed.putString(SORT_TYPE + ":" + getMediaSettings().getType().toString(), type.toString());
        boolean res = ed.commit();
        mediaSettings = null;
        return res;
    }

    public int getExpiryMinutes() {
        return Integer.parseInt(getStringPref(CACHE_EXPIRE_MINUTES, "30").trim());
    }

    public long getLastLoad() {
        return getLongPref(LAST_LOAD, 0l);
    }

    public boolean setLastLoad(long ll) {
        Editor ed = prefs.edit();
        ed.putLong(LAST_LOAD, ll);
        return ed.commit();
    }

    public boolean clearCache() {
        return setLastLoad(0);
    }

    public URL getIpRetrievalUrl() throws MalformedURLException {
        return new URL(getIpRetrievalUrlString());
    }

    public String getIpRetrievalUrlString() {
        return getStringPref(IP_RETRIEVAL_URL, "").trim();
    }

    public boolean isIpRetrieval() {
        return getBooleanPref(RETRIEVE_IP, false);
    }

    public String getMythTvServicesAuthType() {
        return getStringPref(MYTHTV_SERVICES_AUTH_TYPE, AUTH_TYPE_NONE);
    }

    public String getMythTvServicesUser() {
        return getStringPref(MYTHTV_SERVICES_USER, "").trim();
    }

    public String getMythTvServicesPassword() {
        return getStringPref(MYTHTV_SERVICES_PASSWORD, "").trim();
    }

    public String getMythTvServicesPasswordMasked() {
        return getMasked(getMythTvServicesPassword());
    }

    /**
     * backendWeb methods will redirect to mythtv auth settings if AUTH_TYPE_SAME
     */
    public String getBackendWebAuthType() {
        String authType = getMythlingServicesAuthType();
        if (AUTH_TYPE_SAME.equals(authType))
            authType = getMythTvServicesAuthType();
        return authType;
    }

    public String getMythlingServicesAuthType() {
        return getStringPref(MYTHLING_SERVICES_AUTH_TYPE, AUTH_TYPE_NONE);
    }

    public String getBackendWebUser() {
        if (AUTH_TYPE_SAME.equals(getMythlingServicesAuthType()))
            return getMythTvServicesUser();
        else
            return getMythlingServicesUser();
    }

    public String getMythlingServicesUser() {
        return getStringPref(MYTHLING_SERVICES_USER, "").trim();
    }

    public boolean setMythlingServicesUser(String user) {
        Editor ed = prefs.edit();
        ed.putString(MYTHLING_SERVICES_USER, user);
        return ed.commit();
    }

    public String getBackendWebPassword() {
        if (AUTH_TYPE_SAME.equals(getMythlingServicesAuthType()))
            return getMythTvServicesPassword();
        else
            return getMythlingServicesPassword();
    }

    public String getMythlingServicesPassword() {
        return getStringPref(MYTHLING_SERVICES_PASSWORD, "").trim();
    }

    public boolean setMythlingServicesPassword(String password) {
        Editor ed = prefs.edit();
        ed.putString(MYTHLING_SERVICES_PASSWORD, password);
        return ed.commit();
    }

    public String getBackendWebPasswordMasked() {
        String pw = AUTH_TYPE_SAME.equals(getMythlingServicesAuthType()) ? getMythTvServicesPassword() : getMythlingServicesPassword();
        return getMasked(pw);
    }

    public String getMythlingServicesPasswordMasked() {
        return getMasked(getMythlingServicesPassword());
    }

    public static String getMasked(String in) {
        String masked = "";
        for (int i = 0; i < in.length(); i++)
            masked += "*";
        return masked;
    }

    public int getTunerTimeout() {
        return Integer.parseInt(getStringPref(TUNER_TIMEOUT, "30").trim());
    }

    public int getTunerLimit() {
        return Integer.parseInt(getStringPref(TUNER_LIMIT, "0").trim());
    }

    public int getTranscodeTimeout() {
        return Integer.parseInt(getStringPref(TRANSCODE_TIMEOUT, "30").trim());
    }

    public int getTranscodeJobLimit() {
        return Integer.parseInt(getStringPref(TRANSCODE_JOB_LIMIT, "3").trim());
    }

    public int getHttpConnectTimeout() {
        return Integer.parseInt(getStringPref(HTTP_CONNECT_TIMEOUT, "6").trim());
    }

    public int getHttpReadTimeout() {
        return Integer.parseInt(getStringPref(HTTP_READ_TIMEOUT, "10").trim());
    }

    // change these values and recompile to route service calls through a dev-time reverse proxy
    private boolean serviceProxy = false;
    private String serviceProxyIp = "192.168.0.100";
    private int serviceProxyPort = 8888;

    public boolean isServiceProxy() {
        return serviceProxy;
    }

    public String getServiceProxyIp() {
        return serviceProxyIp;
    }

    public int getServiceProxyPort() {
        return serviceProxyPort;
    }

    public static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private static Pattern ipAddressPattern;

    public static boolean validateIp(String ip) {
        if (ipAddressPattern == null)
            ipAddressPattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = ipAddressPattern.matcher(ip);
        return matcher.matches();
    }

    public boolean validateHost(String host) {
        if (host == null)
            return false;
        if (Character.isDigit(host.charAt(0)))
            return validateIp(host);

        return true;
    }

    public void validate() throws BadSettingsException {
        if (isDevicePlayback()) {
            if (isExternalNetwork()) {
                if (isIpRetrieval()) {
                    try {
                        if (getIpRetrievalUrlString().isEmpty())
                            bse(R.string.title_prefs_network, R.string.title_ip_retrieval_url);
                        getIpRetrievalUrl();
                    } catch (MalformedURLException ex) {
                        try {
                            String withProtocol = "http://" + getIpRetrievalUrlString();
                            new URL(withProtocol);
                            Editor ed = prefs.edit();
                            ed.putString(IP_RETRIEVAL_URL, withProtocol);
                            ed.commit();
                        } catch (MalformedURLException ex2) {
                            bse(ex2, R.string.title_prefs_network, R.string.title_ip_retrieval_url);
                        }
                    }
                } else {
                    if (!validateHost(getExternalBackendHost()))
                        bse(R.string.title_prefs_network, R.string.title_external_backend, R.string.title_backend_host);
                }
            } else {
                if (!validateHost(getInternalBackendHost()))
                    bse(R.string.title_prefs_network, R.string.title_internal_backend, R.string.title_backend_host);
            }

            // backend ports regardless of internal/external network
            try {
                if (getMythTvServicePort() <= 0)
                    bse(R.string.title_prefs_connections, R.string.title_content_services, R.string.title_mythtv_service_port);
            } catch (NumberFormatException ex) {
                bse(ex, R.string.title_prefs_connections, R.string.title_content_services, R.string.title_mythtv_service_port);
            }
            if (isMythlingMediaServices()) {
                if (!isHasBackendWeb())
                    bse(appContext.getString(R.string.needed_for_mythling_svcs), R.string.title_prefs_connections, R.string.title_web_server);
                try {
                    if (getMythlingWebPort() <= 0)
                        bse(R.string.title_prefs_connections, R.string.title_web_server, R.string.title_web_port);
                } catch (NumberFormatException ex) {
                    bse(ex, R.string.title_prefs_connections, R.string.title_web_server, R.string.title_web_port);
                }
            }

            // services only used for device playback
            if (!getMythTvServicesAuthType().equals(AUTH_TYPE_NONE)) {
                if (getMythTvServicesUser().isEmpty())
                    bse(R.string.title_prefs_credentials, R.string.title_mythtv_services_user);
                if (getMythTvServicesPassword().isEmpty())
                    bse(R.string.title_prefs_credentials, R.string.title_mythtv_services_password);
            }
        } else {
            if (!validateHost(getFrontendHost()))
                bse(R.string.title_prefs_playback, R.string.title_frontend_player, R.string.title_frontend_host);
            try {
                if (getFrontendSocketPort() <= 0)
                    bse(R.string.title_prefs_playback, R.string.title_frontend_player, R.string.title_frontend_socket_port);
            } catch (NumberFormatException ex) {
                bse(ex, R.string.title_prefs_playback, R.string.title_frontend_player, R.string.title_frontend_socket_port);
            }
            try {
                if (getFrontendServicePort() <= 0)
                    bse(R.string.title_prefs_playback, R.string.title_frontend_player, R.string.title_frontend_service_port);
            } catch (NumberFormatException ex) {
                bse(ex, R.string.title_prefs_playback, R.string.title_frontend_player, R.string.title_frontend_service_port);
            }
        }

        if (isMythlingMediaServices()) {
            String authType = getMythlingServicesAuthType();
            if (!authType.equals(AUTH_TYPE_NONE) && !authType.equals(AUTH_TYPE_SAME)) {
                if (getMythlingServicesUser().isEmpty())
                    bse(R.string.title_prefs_credentials, R.string.title_web_user);
                if (getMythlingServicesPassword().isEmpty())
                    bse(R.string.title_prefs_credentials, R.string.title_web_password);
            }
        }

        try {
            if (getExpiryMinutes() < 0)
                bse(R.string.title_prefs_caching, R.string.title_cache_expiry);
        } catch (NumberFormatException ex) {
            bse(ex, R.string.title_prefs_caching, R.string.title_cache_expiry);
        }
    }

    private void bse(String msg, Throwable th, int... resIds) throws BadSettingsException {
        String m = "";
        for (int i = 0; i < resIds.length; i++) {
            m += appContext.getString(resIds[i]);
            if (i < resIds.length - 1)
                m += " > ";
        }
        if (msg != null)
            m += " (" + msg + ")";
        if (th == null)
            throw new BadSettingsException(m);
        else
            throw new BadSettingsException(m, th);
    }

    private void bse(String msg, int... resIds) throws BadSettingsException {
        bse(msg, null, resIds);
    }

    private void bse(Throwable th, int... resIds) throws BadSettingsException {
        bse(null, th, resIds);
    }

    private void bse(int...resIds) throws BadSettingsException {
        bse(null, null, resIds);
    }

    public HttpHelper getMediaListDownloader(URL[] urls) {
        HttpHelper downloader;
        if (isMythlingMediaServices()) {
            downloader = new HttpHelper(urls, getBackendWebAuthType(), getPrefs());
            downloader.setCredentials(getBackendWebUser(), getBackendWebPassword());
        } else {
            downloader = new HttpHelper(urls, getMythTvServicesAuthType(), getPrefs());
            downloader.setCredentials(getMythTvServicesUser(), getMythTvServicesPassword());
        }
        return downloader;
    }

    public MediaListParser getMediaListParser(String json) {
        if (isMythlingMediaServices())
            return new MythlingParser(this, json);
        else
            return new MythTvParser(this, json);
    }

    public boolean isPhone() {
        return !isTablet() && !isTv();
    }

    public boolean isTablet() {
        return appContext.getResources().getBoolean(R.bool.isTablet);
    }

    private Boolean isTv;
    public boolean isTv() {
        if (isTv == null) {
            UiModeManager modeMgr = (UiModeManager)appContext.getSystemService(Context.UI_MODE_SERVICE);
            isTv = modeMgr.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        }
        return isTv;
    }

    public boolean isFireTv() {
        return devicePrefsSpec instanceof FireTvPrefsSpec;
    }

    public ViewType getDefaultViewType(MediaType mediaType) {
        if (mediaType == MediaType.liveTv)
            return ViewType.list; // regardless of device
        return isTablet() || isTv() ? ViewType.split : ViewType.list;
    }

    private static String mythlingVersion;

    public String getMythlingVersion() {
        if (mythlingVersion == null) {
            PackageManager manager = appContext.getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(appContext.getPackageName(), 0);
                mythlingVersion = info.versionName;
            } catch (NameNotFoundException ex) { // should never happen
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
        return mythlingVersion;
    }
    /**
     * may return null if getMythlingVersion() never called
     * and AppSettings never instantiated.
     */
    public static String staticGetMythlingVersion() {
        return mythlingVersion;
    }

    public static int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    @SuppressWarnings("deprecation")
    public static String[] getSupportedCpus() {
        try {
            try {
                return new String[] { Build.CPU_ABI, Build.CPU_ABI2 };
            }
            catch (NoSuchFieldError er) {
                return new String[] { Build.CPU_ABI };
            }
        }
        catch (Throwable th) {
            return new String[0];
        }
    }

    public String getPlaybackNetwork() {
        if (isExternalNetwork())
            return PlaybackOptions.NETWORK_EXTERNAL;
        else
            return PlaybackOptions.NETWORK_INTERNAL;
    }

    private PlaybackOptions playbackOptions;
    public PlaybackOptions getPlaybackOptions() {
        if (playbackOptions == null)
            playbackOptions = new PlaybackOptions(this);
        return playbackOptions;
    }

    private static boolean devicePrefsSpecsLoaded;
    private static DevicePrefsSpec devicePrefsSpec;
    public DevicePrefsSpec getDevicePrefsConstraints() {
        return devicePrefsSpec;
    }

    public boolean deviceSupportsWebLinks() {
        DevicePrefsSpec deviceConstraints = getDevicePrefsConstraints();
        return deviceConstraints == null || deviceConstraints.supportsWebLinks();
    }

    public boolean getBooleanPref(String key, boolean defValue) {
        boolean deviceDefault = defValue;
        if (devicePrefsSpec != null) {
            Object val = devicePrefsSpec.getDefaultValues().get(key);
            if (val != null)
                deviceDefault = (Boolean)val;
        }
        return prefs.getBoolean(key, deviceDefault);
    }

    public boolean setBooleanPref(String key, boolean value) {
        Editor ed = prefs.edit();
        ed.putBoolean(key, value);
        return ed.commit();
    }

    public boolean setStringPref(String key, String value) {
        Editor ed = prefs.edit();
        ed.putString(key, value);
        return ed.commit();
    }

    public long getLongPref(String key, long defValue) {
        long deviceDefault = defValue;
        if (devicePrefsSpec != null) {
            Object val = devicePrefsSpec.getDefaultValues().get(key);
            if (val != null)
                deviceDefault = (Long)val;
        }
        return prefs.getLong(key, deviceDefault);
    }

    public int getIntPref(String key, int defValue) {
        int deviceDefault = defValue;
        if (devicePrefsSpec != null) {
            Object val = devicePrefsSpec.getDefaultValues().get(key);
            if (val != null)
                deviceDefault = (Integer)val;
        }
        return prefs.getInt(key, deviceDefault);
    }

    public float getFloatPref(String key, int defValue) {
        float deviceDefault = defValue;
        if (devicePrefsSpec != null) {
            Object val = devicePrefsSpec.getDefaultValues().get(key);
            if (val != null)
                deviceDefault = (Float)val;
        }
        return prefs.getFloat(key, deviceDefault);
    }

    public String getStringPref(String key, String defValue) {
        String deviceDefault = defValue;
        if (devicePrefsSpec != null) {
            Object val = devicePrefsSpec.getDefaultValues().get(key);
            if (val != null)
                deviceDefault = (String)val;
        }
        return prefs.getString(key, deviceDefault);
    }

    /**
     * returns true only once (when newly installed)
     */
    public boolean isFirstRun() {
        boolean set = prefs.getBoolean(PREFS_INITIALLY_SET, false);
        if (!set)
            prefs.edit().putBoolean(PREFS_INITIALLY_SET, true).commit();
        return !set;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = appContext.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static String getExternalStorageDir() {
        return EXTERNAL_STORAGE_DIRECTORY;
    }
}
