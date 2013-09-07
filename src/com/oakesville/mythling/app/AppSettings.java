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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.oakesville.mythling.app.MediaSettings.MediaType;
import com.oakesville.mythling.app.MediaSettings.SortType;
import com.oakesville.mythling.app.MediaSettings.ViewType;
import com.oakesville.mythling.R;

public class AppSettings
{
  public static final String DEVICE_PLAYBACK_CATEGORY = "device_playback_cat";
  public static final String FRONTEND_PLAYBACK_CATEGORY = "frontend_playback_cat";
  public static final String INTERNAL_BACKEND_CATEGORY = "internal_backend_cat";
  public static final String EXTERNAL_BACKEND_CATEGORY = "external_backend_cat";
  public static final String MYTH_BACKEND_INTERNAL_IP = "mythbe_internal_ip";
  public static final String MYTH_BACKEND_EXTERNAL_IP = "mythbe_external_ip";
  public static final String MYTH_BACKEND_WEB_PORT = "mythbe_web_port";
  public static final String MYTH_BACKEND_SERVICE_PORT = "mythbe_service_port";
  public static final String MYTH_FRONTEND_IP = "mythfe_ip";
  public static final String MYTH_FRONTEND_PORT = "mythfe_port";
  public static final String MEDIA_TYPE = "media_type";
  public static final String VIEW_TYPE = "view_type";
  public static final String SORT_TYPE = "sort_type";
  public static final String PLAYBACK_MODE = "playback_mode";
  public static final String VIDEO_PLAYER = "video_player";
  public static final String NETWORK_LOCATION = "network_location";
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
  public static final String SERVICES_AUTH_TYPE = "services_auth_type";
  public static final String SERVICES_ACCESS_USER = "services_access_user";
  public static final String SERVICES_ACCESS_PASSWORD = "services_access_password";
  public static final String WEB_AUTH_TYPE = "web_auth_type";
  public static final String WEB_ACCESS_USER = "web_access_user";
  public static final String WEB_ACCESS_PASSWORD = "web_access_password";
  public static final String TUNER_TIMEOUT = "tuner_timeout";
  public static final String TRANSCODE_TIMEOUT = "transcode_timeout";
  public static final String MOVIE_CURRENT_POSITION = "movie_current_position";
  
  private Context appContext;
  public Context getAppContext() { return appContext; }
  
  private SharedPreferences prefs;
  public SharedPreferences getPrefs() { return prefs; }
  
  public AppSettings(Context appContext)
  {
    this.appContext = appContext;
    this.prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
  }
  
  public URL getWebBaseUrl() throws MalformedURLException
  {
    String ip = getBackendIp();
    int port = getBackendWebPort();
    return new URL("http://" + ip + ":" + port);
  }
  
  public URL getCategoriesUrl() throws MalformedURLException
  {
    MediaSettings mediaSettings = getMediaSettings();
    String url = getWebBaseUrl() + "/works.php?type=" + mediaSettings.getType().toString();
    if (mediaSettings.getSortType() == SortType.byYear)
      url += "&orderBy=year";
    else if (mediaSettings.getSortType() == SortType.byRating)
      url += "&orderBy=userrating%20desc";
      
    return new URL(url);
  }
  
  public URL getSearchUrl(String query) throws MalformedURLException, UnsupportedEncodingException
  {
    return new URL(getWebBaseUrl() + "/works.php?type=search&query=" + URLEncoder.encode(query, "UTF-8"));    
  }

  public URL getServicesBaseUrl() throws MalformedURLException
  {
    String ip = getServiceIp();
    int servicePort = getServicePort();
    return new URL("http://" + ip + ":" + servicePort);    
  }
  
  public int getServicePort()
  {
    if (isServiceProxy())
      return getServiceProxyPort();
    else
      return getBackendServicePort();
  }
  
  public int getBackendServicePort()
  {
    return Integer.parseInt(prefs.getString(MYTH_BACKEND_SERVICE_PORT, "6544").trim()); 
  }
  
  public int getBackendWebPort()
  {
    return Integer.parseInt(prefs.getString(MYTH_BACKEND_WEB_PORT, "80").trim());
  }
  
  public String getFrontendIp()
  {
    return prefs.getString(MYTH_FRONTEND_IP, "192.168.0.68").trim();
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

  public int getBuiltInPlayerBufferSize()
  {
    return Integer.parseInt(prefs.getString(BUILT_IN_PLAYER_BUFFER_SIZE, "8000"));
  }

  public boolean isExternalNetwork()
  {
    return prefs.getBoolean(NETWORK_LOCATION, false);
  }
  
  public String getInternalBackendIp()
  {
    return prefs.getString(MYTH_BACKEND_INTERNAL_IP, "192.168.0.70").trim();
  }
  
  public String getExternalBackendIp()
  {
    return prefs.getString(MYTH_BACKEND_EXTERNAL_IP, "192.168.0.69").trim();
  }
  
  public String getServiceIp()
  {
    if (isServiceProxy())
      return getServiceProxyIp();
    else
      return getBackendIp();
  }
  
  public String getBackendIp()
  {
    if (isExternalNetwork())
      return getExternalBackendIp();
    else
      return getInternalBackendIp();
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

  public MediaSettings getMediaSettings()
  {
    String mediaType = prefs.getString(MEDIA_TYPE, "videos");
    MediaSettings mediaSettings = new MediaSettings(mediaType);
    String viewType = prefs.getString(VIEW_TYPE + ":" + mediaSettings.getType().toString(), "list");
    mediaSettings.setViewType(viewType);
    String sortType = prefs.getString(SORT_TYPE + ":" + mediaSettings.getType().toString(), "byTitle");
    mediaSettings.setSortType(sortType);
    return mediaSettings;
  }
  
  public boolean setMediaType(MediaType mediaType)
  {
    Editor ed = prefs.edit();
    ed.putString(MEDIA_TYPE, mediaType.toString());
    return ed.commit();
  }
  
  public boolean setViewType(ViewType type)
  {
    MediaSettings mediaSettings = getMediaSettings();
    Editor ed = prefs.edit();
    ed.putString(VIEW_TYPE + ":" + mediaSettings.getType().toString(), type.toString());
    return ed.commit();
  }
  
  public boolean setSortType(SortType type)
  {
    MediaSettings mediaSettings = getMediaSettings();
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
  
  public String getServicesAuthType()
  {
    return prefs.getString(SERVICES_AUTH_TYPE, "None");
  }
  public String getServicesAccessUser()
  {
    return prefs.getString(SERVICES_ACCESS_USER, "").trim();
  }
  public String getServicesAccessPassword()
  {
    return prefs.getString(SERVICES_ACCESS_PASSWORD, "").trim();
  }
  public String getServicesAccessPasswordMasked()
  {
    return getMasked(getServicesAccessPassword());
  }
  
  public String getWebAuthType()
  {
    return prefs.getString(WEB_AUTH_TYPE, "None");
  }
  public String getWebAccessUser()
  {
    return prefs.getString(WEB_ACCESS_USER, "").trim();
  }
  public String getWebAccessPassword()
  {
    return prefs.getString(WEB_ACCESS_PASSWORD, "").trim();
  }
  public String getWebAccessPasswordMasked()
  {
    return getMasked(getWebAccessPassword());
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
  private String serviceProxyIp = "192.168.0.9";
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
  
  private Pattern ipAddressPattern;
  public boolean validateIp(String ip)
  {
    if (ipAddressPattern == null)
      ipAddressPattern = Pattern.compile(IPADDRESS_PATTERN);
    Matcher matcher = ipAddressPattern.matcher(ip);
    return matcher.matches();
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
          if (!validateIp(getExternalBackendIp()))
            throw new BadSettingsException("Network > External Backend > IP Address");
        }
      }
      else
      {
        if (!validateIp(getInternalBackendIp()))
          throw new BadSettingsException("Network > Internal Backend > IP Address");
      }
      
      // backend ports regardless of internal/external network
      try
      {
        if (getBackendServicePort() <= 0)
          throw new BadSettingsException("Connections > Backend Ports > Service Port");
      }
      catch (NumberFormatException ex)
      {
        throw new BadSettingsException("Connections > Backend Ports > Service Port", ex);
      }
      try
      {
        if (getBackendWebPort() <= 0)
          throw new BadSettingsException("Connections > Backend Ports > Web Port");
      }
      catch (NumberFormatException ex)
      {
        throw new BadSettingsException("Connections > Backend Ports > Web Port", ex);
      }
      
      // services only used for device playback
      if (!getServicesAuthType().equals("None"))
      {
        if (getServicesAccessUser().isEmpty())
          throw new BadSettingsException("Settings > Credentials > Services Access > User");
        if (getServicesAccessPassword().isEmpty())
          throw new BadSettingsException("Settings > Credentials > Services Access > Password");
      }      
    }
    else
    {
      if (!validateIp(getFrontendIp()))
        throw new BadSettingsException("Settings > Playback > Frontend Player > IP Address");
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
    
    // web access needed for any type of playback
    if (!getWebAuthType().equals("None"))
    {
      if (getWebAccessUser().isEmpty())
        throw new BadSettingsException("Settings > Credentials > Web Access > User");
      if (getWebAccessPassword().isEmpty())
        throw new BadSettingsException("Settings > Credentials > Web Access > Password");
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

  
}
