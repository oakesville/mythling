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
package com.oakesville.mythling.app;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;

import com.oakesville.mythling.R;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.Song;
import com.oakesville.mythling.prefs.DevicePrefsSpec;
import com.oakesville.mythling.prefs.firetv.FireTvPrefsSpec;
import com.oakesville.mythling.util.HttpHelper;
import com.oakesville.mythling.util.HttpHelper.AuthType;
import com.oakesville.mythling.util.MediaListParser;
import com.oakesville.mythling.util.MythTvParser;
import com.oakesville.mythling.util.MythlingParser;

public class AppSettings {
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
    public static final String INTERNAL_VIDEO_PLAYER = "video_player";
    public static final String INTERNAL_MUSIC_PLAYER = "music_player";
    public static final String EXTERNAL_NETWORK = "network_location";
    public static final String CATEGORIZE_VIDEOS = "categorize_videos";
    public static final String MOVIE_DIRECTORIES = "movie_directories";
    public static final String TV_SERIES_DIRECTORIES = "tv_series_directories";
    public static final String VIDEO_EXCLUDE_DIRECTORIES = "video_exclude_directories";
    public static final String HLS_FILE_EXTENSIONS = "hls_file_extensions";
    public static final String STREAM_RAW_FILE_EXTENSIONS = "stream_raw_file_extensions";
    public static final String ARTWORK_SG_VIDEOS = "artwork_sg_videos";
    public static final String ARTWORK_SG_RECORDINGS = "artwork_sg_recordings";
    public static final String ARTWORK_SG_MOVIES = "artwork_sg_movies";
    public static final String ARTWORK_SG_TVSERIES = "artwork_sg_tvseries";
    public static final String DEFAULT_ARTWORK_SG = "Coverart";
    public static final String DEFAULT_ARTWORK_SG_RECORDINGS = "Screenshots";
    public static final String DEFAULT_ARTWORK_SG_RECORDINGS_LABEL = "<Use Preview Image>";
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
    public static final String PREFS_INITIALLY_SET = "prefs_initially_set";

    private Context appContext;

    public Context getAppContext() {
        return appContext;
    }

    private SharedPreferences prefs;

    public SharedPreferences getPrefs() {
        return prefs;
    }

    private static DateFormat dateTimeFormat;

    public AppSettings(Context appContext) {
        this.appContext = appContext;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
                String nowUtc = dateTimeFormat.format(cal.getTime()).replace(' ', 'T');
                url += "Guide/GetProgramGuide?StartTime=" + nowUtc + "&EndTime=" + nowUtc;
            }
        }

        return new URL(url);
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
        if (!DEFAULT_ARTWORK_SG.equals(prefStorageGroup))
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

    public boolean isExternalVideoPlayer() {
        return !getBooleanPref(INTERNAL_VIDEO_PLAYER, false);
    }

    public boolean isExternalMusicPlayer() {
        return !getBooleanPref(INTERNAL_MUSIC_PLAYER, true);
    }

    public boolean isMythlingMediaServices() {
        return getBooleanPref(MYTHLING_MEDIA_SERVICES, false);
    }

    public boolean isHasBackendWeb() {
        return getBooleanPref(BACKEND_WEB, false);
    }

    public boolean isMythWebAccessEnabled() {
        return getBooleanPref(MYTHWEB_ACCESS, false);
    }

    public boolean isErrorReportingEnabled() {
        return getBooleanPref(ERROR_REPORTING, false);
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

    public String getHlsFileExtensions() {
        return getStringPref(HLS_FILE_EXTENSIONS, "");
    }

    public boolean isPreferHls(String fileExtension) {
        String[] hlsFileExtensions = getHlsFileExtensions().split(",");
        for (int i = 0; i < hlsFileExtensions.length; i++) {
            if (hlsFileExtensions[i].equals(fileExtension) || hlsFileExtensions.equals("." + fileExtension))
                return true;
        }
        return false;
    }

    public boolean setPreferHls(String fileExtension) {
        String hlsFileExtensions = getHlsFileExtensions();
        if (!hlsFileExtensions.isEmpty())
            hlsFileExtensions += ",";
        hlsFileExtensions += fileExtension;
        Editor ed = prefs.edit();
        ed.putString(HLS_FILE_EXTENSIONS, hlsFileExtensions);
        return ed.commit();
    }

    public String getStreamRawFileExtensions() {
        return getStringPref(STREAM_RAW_FILE_EXTENSIONS, "");
    }

    public boolean isPreferStreamRaw(String fileExtension) {
        String[] streamRawFileExtensionss = getStreamRawFileExtensions().split(",");
        for (int i = 0; i < streamRawFileExtensionss.length; i++) {
            if (streamRawFileExtensionss[i].equals(fileExtension) || streamRawFileExtensionss[i].equals("." + fileExtension))
                return true;
        }
        return false;
    }

    public boolean setPreferStreamRaw(String fileExtension) {
        String streamRawFileExtensions = getStreamRawFileExtensions();
        if (!streamRawFileExtensions.isEmpty())
            streamRawFileExtensions += ",";
        streamRawFileExtensions += fileExtension;
        Editor ed = prefs.edit();
        ed.putString(STREAM_RAW_FILE_EXTENSIONS, streamRawFileExtensions);
        return ed.commit();
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
            return getStringPref(ARTWORK_SG_VIDEOS, DEFAULT_ARTWORK_SG);
        else if (mediaType == MediaType.recordings)
            return getStringPref(ARTWORK_SG_RECORDINGS, DEFAULT_ARTWORK_SG_RECORDINGS);
        else if (mediaType == MediaType.movies)
            return getStringPref(ARTWORK_SG_MOVIES, DEFAULT_ARTWORK_SG);
        else if (mediaType == MediaType.tvSeries)
            return getStringPref(ARTWORK_SG_TVSERIES, DEFAULT_ARTWORK_SG);
        else
            return DEFAULT_ARTWORK_SG;
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
        return "Height=" + getVideoRes() + "&Bitrate=" + getVideoBitrate() + "&AudioBitrate=" + getAudioBitrate();
    }

    public int getInternalVideoRes() {
        return Integer.parseInt(getStringPref(INTERNAL_VIDEO_RES, "720"));
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

    public int[] getVideoResValues() {
        return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_res_values));
    }

    public int[] getVideoBitrateValues() {
        return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_bitrate_values));
    }

    public int[] getAudioBitrateValues() {
        return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.audio_bitrate_values));
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
            String sortType = getStringPref(SORT_TYPE + ":" + mediaSettings.getType().toString(), "byTitle");
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

    public int getPagerCurrentPosition(MediaType mediaType, String category) {
        return getIntPref(PAGER_CURRENT_POSITION + ":" + mediaType + ":" + category, 0);
    }

    public void setPagerCurrentPosition(MediaType mediaType, String category, int curPos) {
        Editor ed = prefs.edit();
        ed.putInt(PAGER_CURRENT_POSITION + ":" + mediaType + ":" + category, curPos);
        ed.apply();
    }

    public void clearPagerCurrentPosition(MediaType mediaType, String category) {
        Editor ed = prefs.edit();
        ed.remove(PAGER_CURRENT_POSITION + ":" + mediaType + ":" + category);
        ed.apply();
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
                            throw new BadSettingsException("Network > External Backend > IP Retrieval URL");
                        getIpRetrievalUrl();
                    } catch (MalformedURLException ex) {
                        try {
                            String withProtocol = "http://" + getIpRetrievalUrlString();
                            new URL(withProtocol);
                            Editor ed = prefs.edit();
                            ed.putString(IP_RETRIEVAL_URL, withProtocol);
                            ed.commit();
                        } catch (MalformedURLException ex2) {
                            throw new BadSettingsException("Network > External Backend > IP Retrieval URL", ex2);
                        }
                    }
                } else {
                    if (!validateHost(getExternalBackendHost()))
                        throw new BadSettingsException("Network > External Backend > Host");
                }
            } else {
                if (!validateHost(getInternalBackendHost()))
                    throw new BadSettingsException("Network > Internal Backend > Host");
            }

            // backend ports regardless of internal/external network
            try {
                if (getMythTvServicePort() <= 0)
                    throw new BadSettingsException("Connections > Content Services > MythTV Service Port");
            } catch (NumberFormatException ex) {
                throw new BadSettingsException("Connections > Content Services > MythTV Service Port", ex);
            }
            if (isMythlingMediaServices()) {
                if (!isHasBackendWeb())
                    throw new BadSettingsException("Connections > Backend Web Server (Needed for Mythling Media Services)");
                try {
                    if (getMythlingWebPort() <= 0)
                        throw new BadSettingsException("Connections > Backend Web Server > Web Port");
                } catch (NumberFormatException ex) {
                    throw new BadSettingsException("Connections > Backend Web Server > Web Port", ex);
                }
            }

            // services only used for device playback
            if (!getMythTvServicesAuthType().equals(AUTH_TYPE_NONE)) {
                if (getMythTvServicesUser().isEmpty())
                    throw new BadSettingsException("Settings > Credentials > MythTV Services > User");
                if (getMythTvServicesPassword().isEmpty())
                    throw new BadSettingsException("Settings > Credentials > MythTV Services > Password");
            }
        } else {
            if (!validateHost(getFrontendHost()))
                throw new BadSettingsException("Settings > Playback > Frontend Player > Host");
            try {
                if (getFrontendSocketPort() <= 0)
                    throw new BadSettingsException("Settings > Playback > Frontend Player > Socket Port");
            } catch (NumberFormatException ex) {
                throw new BadSettingsException("Settings > Playback > Frontend Player > Socket Port", ex);
            }
            try {
                if (getFrontendServicePort() <= 0)
                    throw new BadSettingsException("Settings > Playback > Frontend Player > Service Port");
            } catch (NumberFormatException ex) {
                throw new BadSettingsException("Settings > Playback > Frontend Player > Service Port", ex);
            }
        }

        if (isMythlingMediaServices()) {
            String authType = getMythlingServicesAuthType();
            if (!authType.equals(AUTH_TYPE_NONE) && !authType.equals(AUTH_TYPE_SAME)) {
                if (getMythlingServicesUser().isEmpty())
                    throw new BadSettingsException("Settings > Credentials > Mythling Services > User");
                if (getMythlingServicesPassword().isEmpty())
                    throw new BadSettingsException("Settings > Credentials > Mythling Services > Password");
            }
        }

        try {
            if (getExpiryMinutes() < 0)
                throw new BadSettingsException("Settings > Data Caching > Expiry Interval");
        } catch (NumberFormatException ex) {
            throw new BadSettingsException("Settings > Data Caching > Expiry Interval", ex);
        }

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

    public boolean isTablet() {
        return appContext.getResources().getBoolean(R.bool.isTablet);
    }

    public boolean isFireTv() {
        return devicePrefsSpec instanceof FireTvPrefsSpec;
    }

    public ViewType getDefaultViewType(MediaType mediaType) {
        if (mediaType == MediaType.videos || mediaType == MediaType.liveTv || mediaType == MediaType.music)
            return ViewType.list; // regardless of device
        return isTablet() || isFireTv() ? ViewType.split : ViewType.list;
    }

    private static String mythlingVersion;

    public void initMythlingVersion() throws NameNotFoundException {
        if (mythlingVersion == null) {
            PackageManager manager = appContext.getPackageManager();
            PackageInfo info = manager.getPackageInfo(appContext.getPackageName(), 0);
            mythlingVersion = info.versionName;
        }
    }

    public static String getMythlingVersion() {
        return mythlingVersion;
    }

    private static DevicePrefsSpec devicePrefsSpec;
    public static DevicePrefsSpec getDevicePrefsConstraints() { return devicePrefsSpec; }
    private static boolean devicePrefsSpecsLoaded;
    public static void loadDevicePrefsConstraints() {
        if (!devicePrefsSpecsLoaded) {
            devicePrefsSpecsLoaded = true;
            // perform this test for all devices that have prefs constraints
            DevicePrefsSpec test = new FireTvPrefsSpec();
            if (test.appliesToDevice(Build.MANUFACTURER, Build.MODEL)) {
                devicePrefsSpec = test;
                return;
            }
        }
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
    public boolean isPrefsInitiallySet() {
        boolean set = prefs.getBoolean(PREFS_INITIALLY_SET, false);
        if (!set) {
            // for mythling 1.0 users who've set their prefs
            set = !"192.168.0.69".equals(getMythlingServiceHost());
            if (!set)
                prefs.edit().putBoolean(PREFS_INITIALLY_SET, true).commit();
        }
        return set;
    }
}
