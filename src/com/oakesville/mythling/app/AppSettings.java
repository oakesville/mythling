/**
 * Copyright 2013 Donald Oakes
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
import android.preference.PreferenceManager;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.app.MediaSettings.SortType;
import com.oakesville.mythling.app.MediaSettings.ViewType;
import com.oakesville.mythling.util.HttpHelper;

public class AppSettings
{
  public static final String DEVICE_PLAYBACK_CATEGORY = "device_playback_cat";
  public static final String FRONTEND_PLAYBACK_CATEGORY = "frontend_playback_cat";
  public static final String INTERNAL_BACKEND_CATEGORY = "internal_backend_cat";
  public static final String EXTERNAL_BACKEND_CATEGORY = "external_backend_cat";
  public static final String MYTHLING_SERVICE_ACCESS_CATEGORY = "mythling_service_access_cat";
  public static final String MYTH_BACKEND_INTERNAL_HOST = "mythbe_internal_host";
  public static final String MYTH_BACKEND_EXTERNAL_HOST = "mythbe_external_host";
  public static final String MEDIA_SERVICES = "media_services";
  public static final String MYTHLING_WEB_PORT = "mythling_web_port";
  public static final String MYTHLING_WEB_ROOT = "mythling_web_root";
  public static final String MYTHTV_SERVICE_PORT = "mythtv_service_port";
  public static final String MYTH_FRONTEND_HOST = "mythfe_host";
  public static final String MYTH_FRONTEND_PORT = "mythfe_port";
  public static final String MEDIA_TYPE = "media_type";
  public static final String VIEW_TYPE = "view_type";
  public static final String SORT_TYPE = "sort_type";
  public static final String PLAYBACK_MODE = "playback_mode";
  public static final String VIDEO_PLAYER = "video_player";
  public static final String NETWORK_LOCATION = "network_location";
  public static final String CATEGORIZE_VIDEOS = "categorize_videos";
  public static final String MOVIE_DIRECTORIES = "movie_directories";
  public static final String TV_SERIES_DIRECTORIES = "tv_series_directories";
  public static final String VIDEO_EXCLUDE_DIRECTORIES = "video_exclude_directories";
  public static final String INTERNAL_VIDEO_RES = "internal_video_res";
  public static final String EXTERNAL_VIDEO_RES = "external_video_res";
  public static final String INTERNAL_VIDEO_BITRATE = "internal_video_bitrate";
  public static final String EXTERNAL_VIDEO_BITRATE = "external_video_bitrate";
  public static final String INTERNAL_AUDIO_BITRATE = "internal_audio_bitrate";
  public static final String EXTERNAL_AUDIO_BITRATE = "external_audio_bitrate";
  public static final String BUILT_IN_PLAYER_BUFFER_SIZE = "built_in_player_buffer_size";
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
  public static final String TUNER_TIMEOUT = "tuner_timeout";
  public static final String TRANSCODE_TIMEOUT = "transcode_timeout";
  public static final String MOVIE_CURRENT_POSITION = "movie_current_position";
  public static final String DEFAULT_MEDIA_TYPE = "recordings";
  public static final String MOVIE_BASE_URL = "movie_base_url";
  public static final String TV_BASE_URL = "tv_base_url";
  public static final String CUSTOM_BASE_URL = "custom_base_url";
  public static final String THEMOVIEDB_BASE_URL = "http://www.themoviedb.org/movie/";
  public static final String THETVDB_BASE_URL = "http://www.thetvdb.com";
  
  private Context appContext;
  public Context getAppContext() { return appContext; }
  
  private SharedPreferences prefs;
  public SharedPreferences getPrefs() { return prefs; }
  
  private static DateFormat dateTimeFormat; 
  
  public AppSettings(Context appContext)
  {
    this.appContext = appContext;
    this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
    dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }
  
  public URL getMythlingWebBaseUrl() throws MalformedURLException
  {
    String ip = getMythlingServiceHost();
    int port = getMythlingServicePort();
    String root = getMythlingWebRoot();
    return new URL("http://" + ip + ":" + port + (root == null || root.length() == 0 ? "" : "/" + root));
  }
  
  public URL getMediaListUrl(MediaType mediaType) throws MalformedURLException, UnsupportedEncodingException
  {
    MediaSettings mediaSettings = getMediaSettings();
    String url;
    if (isMythlingMediaServices())
    {
      url = getMythlingWebBaseUrl() + "/media.php?type=" + mediaType.toString();
      url += getVideoTypeParams();
      if (mediaSettings.getSortType() == SortType.byYear)
        url += "&orderBy=year";
      else if (mediaSettings.getSortType() == SortType.byRating)
        url += "&orderBy=userrating%20desc";
    }
    else
    {
      url = getMythTvServicesBaseUrl() + "/";
      if (mediaType == MediaType.videos || mediaType == MediaType.movies || mediaType == MediaType.tvSeries)
        url += "Video/GetVideoList";
      else if (mediaType == MediaType.recordings)
        url += "Dvr/GetRecordedList?Descending=true";
      else if (mediaType == MediaType.liveTv)
      {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String nowUtc = dateTimeFormat.format(cal.getTime()).replace(' ', 'T');
        url += "Guide/GetProgramGuide?StartTime=" + nowUtc + "&EndTime=" + nowUtc;
      }
    }
      
    return new URL(url);
  }
  
  public URL getMediaListUrl() throws MalformedURLException, UnsupportedEncodingException
  {
    return getMediaListUrl(getMediaSettings().getType());
  }
  
  /**
   * If not empty, always begins with '&'.
   */
  public String getVideoTypeParams() throws UnsupportedEncodingException
  {
    String params = "";
    if (getMediaSettings().getTypeDeterminer() == MediaTypeDeterminer.directories)
    {
        String movieDirs = getMovieDirectories();
        if (movieDirs != null && !movieDirs.trim().isEmpty())
          params += "&movieDirs=" + URLEncoder.encode(movieDirs.trim(), "UTF-8");
        String tvSeriesDirs = getTvSeriesDirectories();
        if (tvSeriesDirs != null && !tvSeriesDirs.trim().isEmpty())
          params += "&tvSeriesDirs=" + URLEncoder.encode(tvSeriesDirs.trim(), "UTF-8");
        String videoExcludeDirs = getVideoExcludeDirectories();
        if (videoExcludeDirs != null && !videoExcludeDirs.trim().isEmpty())
          params += "&videoExcludeDirs=" + URLEncoder.encode(videoExcludeDirs.trim(), "UTF-8");
    }
    else if (getMediaSettings().getTypeDeterminer() == MediaTypeDeterminer.metadata)
    {
      params += "&categorizeUsingMetadata=true";
    }
    return params;
  }
  
  public URL getSearchUrl(String query) throws MalformedURLException, UnsupportedEncodingException
  {
    if (isMythlingMediaServices())
    {
      return new URL(getMythlingWebBaseUrl() + "/media.php?type=search&query=" + URLEncoder.encode(query, "UTF-8")
          + getVideoTypeParams());
    }
    else
    {
      return null;
    }
  }

  public URL getMythTvServicesBaseUrl() throws MalformedURLException
  {
    String ip = getMythTvServiceHost();
    int servicePort = getMythServicePort();
    return new URL("http://" + ip + ":" + servicePort);    
  }
  
  public URL getArtworkUrl(String storageGroup, String fileName) throws MalformedURLException
  {
    return new URL(getMythTvServicesBaseUrl() + "/Content/GetImageFile?StorageGroup=" + storageGroup + "&FileName=" + fileName);    
  }
  
  public int getMythServicePort()
  {
    if (isServiceProxy())
      return getServiceProxyPort();
    else
      return getMythTvServicePort();
  }
  
  public int getMythTvServicePort()
  {
    return Integer.parseInt(prefs.getString(MYTHTV_SERVICE_PORT, "6544").trim()); 
  }
  
  public int getMythlingServicePort()
  {
    if (isServiceProxy())
      return getServiceProxyPort();
    else
      return getMythlingWebPort();
  }
  
  public int getMythlingWebPort()
  {
    return Integer.parseInt(prefs.getString(MYTHLING_WEB_PORT, "80").trim());
  }
  
  public String getMythlingWebRoot()
  {
    return prefs.getString(MYTHLING_WEB_ROOT, "mythling");
  }
  
  public String getFrontendHost()
  {
    return prefs.getString(MYTH_FRONTEND_HOST, "192.168.0.68").trim();
  }
  
  public int getFrontendControlPort()
  {    
    return Integer.parseInt(prefs.getString(MYTH_FRONTEND_PORT, "6546").trim());
  }
  
  public boolean isDevicePlayback()
  {
    return !prefs.getBoolean(PLAYBACK_MODE, false);
  }
  
  public boolean isExternalPlayer()
  {
    return !prefs.getBoolean(VIDEO_PLAYER, false);
  }
  
  public boolean isMythlingMediaServices()
  {
    return prefs.getBoolean(MEDIA_SERVICES, false);
  }

  public int getBuiltInPlayerBufferSize()
  {
    return Integer.parseInt(prefs.getString(BUILT_IN_PLAYER_BUFFER_SIZE, "8000"));
  }

  public boolean isExternalNetwork()
  {
    return prefs.getBoolean(NETWORK_LOCATION, false);
  }
  
  public String getInternalBackendHost()
  {
    return prefs.getString(MYTH_BACKEND_INTERNAL_HOST, "192.168.0.70").trim();
  }
  
  public String getExternalBackendHost()
  {
    return prefs.getString(MYTH_BACKEND_EXTERNAL_HOST, "192.168.0.69").trim();
  }
  
  public String getMovieDirectories()
  {
    return prefs.getString(MOVIE_DIRECTORIES, "");
  }
  
  public String getTvSeriesDirectories()
  {
    return prefs.getString(TV_SERIES_DIRECTORIES, "");
  }

  public String getVideoExcludeDirectories()
  {
    return prefs.getString(VIDEO_EXCLUDE_DIRECTORIES, "");
  }
  
  public String getMovieBaseUrl()
  {
    return prefs.getString(MOVIE_BASE_URL, THEMOVIEDB_BASE_URL);
  }
  
  public String getTvBaseUrl()
  {
    return prefs.getString(TV_BASE_URL, THETVDB_BASE_URL);
  }
  
  public String getCustomBaseUrl()
  {
    return prefs.getString(CUSTOM_BASE_URL, "");
  }
  
  public String getMythTvServiceHost()
  {
    if (isServiceProxy())
      return getServiceProxyIp();
    else
      return getBackendHost();
  }
  
  public String getMythlingServiceHost()
  {
    if (isServiceProxy())
      return getServiceProxyIp();
    else
      return getBackendHost();
  }
  
  public String getBackendHost()
  {
    if (isExternalNetwork())
      return getExternalBackendHost();
    else
      return getInternalBackendHost();
  }
  
  public URL[] getUrls(URL url) throws MalformedURLException
  {
    if (isExternalNetwork() && isIpRetrieval())
      return new URL[] { url, getIpRetrievalUrl() };
    else
      return new URL[] { url };
  }
  
  public int getVideoRes()
  {
    if (isExternalNetwork())
      return getExternalVideoRes();
    else
      return getInternalVideoRes();
  }
  
  public int getVideoBitrate()
  {
    if (isExternalNetwork())
      return getExternalVideoBitrate();
    else
      return getInternalVideoBitrate();
  }
  
  public int getAudioBitrate()
  {
    if (isExternalNetwork())
      return getExternalAudioBitrate();
    else
      return getInternalAudioBitrate();
  }
  
  public String getVideoQualityParams()
  {
    return "Height=" + getVideoRes() + "&Bitrate=" + getVideoBitrate() + "&AudioBitrate=" + getAudioBitrate();
  }

  public int getInternalVideoRes()
  {
    return Integer.parseInt(prefs.getString(INTERNAL_VIDEO_RES, "720"));
  }
  
  public int getExternalVideoRes()
  {
    return Integer.parseInt(prefs.getString(EXTERNAL_VIDEO_RES, "240"));
  }
  
  public int getInternalVideoBitrate()
  {
    return Integer.parseInt(prefs.getString(INTERNAL_VIDEO_BITRATE, "1600000"));
  }

  public int getExternalVideoBitrate()
  {
    return Integer.parseInt(prefs.getString(EXTERNAL_VIDEO_BITRATE, "400000"));
  }
  
  public int getInternalAudioBitrate()
  {
    return Integer.parseInt(prefs.getString(INTERNAL_AUDIO_BITRATE, "64000"));
  }

  public int getExternalAudioBitrate()
  {
    return Integer.parseInt(prefs.getString(EXTERNAL_AUDIO_BITRATE, "32000"));
  }
  
  public int[] getVideoResValues()
  {
    return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_res_values));
  }

  public int[] getVideoBitrateValues()
  {
    return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.video_bitrate_values));
  }

  public int[] getAudioBitrateValues()
  {
    return stringArrayToIntArray(appContext.getResources().getStringArray(R.array.audio_bitrate_values));
  }
  
  private int[] stringArrayToIntArray(String[] stringVals)
  {
    int[] values = new int[stringVals.length];
    for (int i = 0; i < stringVals.length; i++)
      values[i] = Integer.parseInt(stringVals[i]);
    return values;
  }

  private MediaSettings mediaSettings;
  public MediaSettings getMediaSettings()
  {
    if (mediaSettings == null)
    {
      String mediaType = prefs.getString(MEDIA_TYPE, DEFAULT_MEDIA_TYPE);
      mediaSettings = new MediaSettings(mediaType);
      String typeDeterminer = prefs.getString(CATEGORIZE_VIDEOS, MediaTypeDeterminer.metadata.toString());
      mediaSettings.setTypeDeterminer(typeDeterminer);
      String viewType = prefs.getString(VIEW_TYPE + ":" + mediaSettings.getType().toString(), "list");
      mediaSettings.setViewType(viewType);
      String sortType = prefs.getString(SORT_TYPE + ":" + mediaSettings.getType().toString(), "byTitle");
      mediaSettings.setSortType(sortType);
    }
    return mediaSettings;
  }
  
  public boolean setMediaType(MediaType mediaType)
  {
    mediaSettings = null;
    Editor ed = prefs.edit();
    ed.putString(MEDIA_TYPE, mediaType.toString());
    return ed.commit();
  }
  
  public boolean setViewType(ViewType type)
  {
    mediaSettings = null;
    Editor ed = prefs.edit();
    ed.putString(VIEW_TYPE + ":" + mediaSettings.getType().toString(), type.toString());
    return ed.commit();
  }
  
  public boolean setSortType(SortType type)
  {
    mediaSettings = null;
    Editor ed = prefs.edit();
    ed.putString(SORT_TYPE + ":" + mediaSettings.getType().toString(), type.toString());
    return ed.commit();
  }

  public int getMovieCurrentPosition(String category)
  {
    return prefs.getInt(MOVIE_CURRENT_POSITION + ":" + category, 0);
  }
  
  public void setMovieCurrentPosition(String category, int curPos)
  {
    Editor ed = prefs.edit();
    ed.putInt(MOVIE_CURRENT_POSITION + ":" + category, curPos);
    ed.apply();
  }
  
  public int getExpiryMinutes()
  {
    return Integer.parseInt(prefs.getString(CACHE_EXPIRE_MINUTES, "30").trim());
  }
  
  public long getLastLoad()
  { 
    return prefs.getLong(LAST_LOAD, 0l);
  }
  public boolean setLastLoad(long ll)
  {
    Editor ed = prefs.edit();
    ed.putLong(LAST_LOAD, ll);
    return ed.commit();
  }
  
  public URL getIpRetrievalUrl() throws MalformedURLException
  {
    return new URL(getIpRetrievalUrlString());
  }
  
  public String getIpRetrievalUrlString()
  {
    return prefs.getString(IP_RETRIEVAL_URL, "").trim();
  }
  
  public boolean isIpRetrieval()
  {
    return prefs.getBoolean(RETRIEVE_IP, false);
  }
  
  public String getMythTvServicesAuthType()
  {
    return prefs.getString(MYTHTV_SERVICES_AUTH_TYPE, "None");
  }
  public String getMythTvServicesUser()
  {
    return prefs.getString(MYTHTV_SERVICES_USER, "").trim();
  }
  public String getMythTvServicesPassword()
  {
    return prefs.getString(MYTHTV_SERVICES_PASSWORD, "").trim();
  }
  public String getMythTvServicesPasswordMasked()
  {
    return getMasked(getMythTvServicesPassword());
  }
  
  public String getMythlingServicesAuthType()
  {
    return prefs.getString(MYTHLING_SERVICES_AUTH_TYPE, "None");
  }
  public String getMythlingServicesUser()
  {
    return prefs.getString(MYTHLING_SERVICES_USER, "").trim();
  }
  public String getMythlingServicesPassword()
  {
    return prefs.getString(MYTHLING_SERVICES_PASSWORD, "").trim();
  }
  public String getMythlingServicesPasswordMasked()
  {
    return getMasked(getMythlingServicesPassword());
  }
  
  public static String getMasked(String in)
  {
    String masked = "";
    for (int i = 0; i < in.length(); i++)
      masked += "*";
    return masked;
  }
  
  public int getTunerTimeout()
  {
    return Integer.parseInt(prefs.getString(TUNER_TIMEOUT, "30").trim());
  }
  
  public int getTranscodeTimeout()
  {
    return Integer.parseInt(prefs.getString(TRANSCODE_TIMEOUT, "30").trim());
  }
  

  // change these values and recompile to route service calls through a reverse proxy
  private boolean serviceProxy = false;
  private String serviceProxyIp = "192.168.0.100";
  private int serviceProxyPort = 8888;
  public boolean isServiceProxy()
  {
    return serviceProxy;
  }
  public String getServiceProxyIp()
  {
    return serviceProxyIp;
  }
  public int getServiceProxyPort()
  {
    return serviceProxyPort;
  }

  public static final String IPADDRESS_PATTERN = 
      "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
  
  private static Pattern ipAddressPattern;
  public static boolean validateIp(String ip)
  {
    if (ipAddressPattern == null)
      ipAddressPattern = Pattern.compile(IPADDRESS_PATTERN);
    Matcher matcher = ipAddressPattern.matcher(ip);
    return matcher.matches();
  }
  
  public boolean validateHost(String host)
  {
    if (host == null)
      return false;
    if (Character.isDigit(host.charAt(0)))
      return validateIp(host);
    
    return true;
  }
  
  public void validate() throws BadSettingsException
  {
    if (isDevicePlayback())
    {
      if (isExternalNetwork())
      {
        if (isIpRetrieval())
        {
          try
          {
            if (getIpRetrievalUrlString().isEmpty())
              throw new BadSettingsException("Network > External Backend > IP Retrieval URL");
            getIpRetrievalUrl();
          }
          catch (MalformedURLException ex)
          {
            try
            {
              String withProtocol = "http://" + getIpRetrievalUrlString(); 
              new URL(withProtocol);
              Editor ed = prefs.edit();
              ed.putString(IP_RETRIEVAL_URL, withProtocol);
              ed.commit();
            }
            catch (MalformedURLException ex2)
            {
              throw new BadSettingsException("Network > External Backend > IP Retrieval URL", ex2);
            }
          }
        }
        else
        {
          if (!validateHost(getExternalBackendHost()))
            throw new BadSettingsException("Network > External Backend > Host");
        }
      }
      else
      {
        if (!validateHost(getInternalBackendHost()))
          throw new BadSettingsException("Network > Internal Backend > Host");
      }
      
      // backend ports regardless of internal/external network
      try
      {
        if (getMythTvServicePort() <= 0)
          throw new BadSettingsException("Connections > Content Services > MythTV Service Port");
      }
      catch (NumberFormatException ex)
      {
        throw new BadSettingsException("Connections > Content Services > MythTV Service Port", ex);
      }
      try
      {
        if (isMythlingMediaServices() && getMythlingWebPort() <= 0)
          throw new BadSettingsException("Connections > Media Services > Mythling Web Port");
      }
      catch (NumberFormatException ex)
      {
        throw new BadSettingsException("Connections > Media Services > Mythling Web Port", ex);
      }
      
      // services only used for device playback
      if (!getMythTvServicesAuthType().equals("None"))
      {
        if (getMythTvServicesUser().isEmpty())
          throw new BadSettingsException("Settings > Credentials > MythTV Services > User");
        if (getMythTvServicesPassword().isEmpty())
          throw new BadSettingsException("Settings > Credentials > MythTV Services > Password");
      }      
    }
    else
    {
      if (!validateHost(getFrontendHost()))
        throw new BadSettingsException("Settings > Playback > Frontend Player > Host");
      try
      {
        if (getFrontendControlPort() <=0 )
          throw new BadSettingsException("Settings > Playback > Frontend Player > Control Port");
      }
      catch (NumberFormatException ex)
      {
        throw new BadSettingsException("Settings > Playback > Frontend Player > Control Port", ex);
      }
    }
    
    if (isMythlingMediaServices() && !getMythlingServicesAuthType().equals("None"))
    {
      if (getMythlingServicesUser().isEmpty())
        throw new BadSettingsException("Settings > Credentials > Mythling Services > User");
      if (getMythlingServicesPassword().isEmpty())
        throw new BadSettingsException("Settings > Credentials > Mythling Services > Password");
    }
    
    try
    {
      if (getExpiryMinutes() < 0)
        throw new BadSettingsException("Settings > Data Caching > Expiry Interval");
    }
    catch (NumberFormatException ex)
    {
      throw new BadSettingsException("Settings > Data Caching > Expiry Interval", ex);
    }
    
  }
  
  public HttpHelper getMediaListDownloader(URL[] urls)
  {
    HttpHelper downloader;
    if (isMythlingMediaServices())
    {
      downloader = new HttpHelper(urls, getMythlingServicesAuthType(), getPrefs());
      downloader.setCredentials(getMythlingServicesUser(), getMythlingServicesPassword());
    }
    else
    {
      downloader = new HttpHelper(urls, getMythTvServicesAuthType(), getPrefs());
      downloader.setCredentials(getMythTvServicesUser(), getMythTvServicesPassword());
    }
    return downloader;
  }
  
  
}
