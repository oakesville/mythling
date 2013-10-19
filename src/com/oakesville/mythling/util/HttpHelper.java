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
package com.oakesville.mythling.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.ByteArrayBuffer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.util.Base64;
import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;

public class HttpHelper
{
  private static final String TAG = HttpHelper.class.getSimpleName();

  public enum AuthType
  {
    None,
    Basic,
    Digest
  }
  
  public enum Method
  {
    Get,
    Post
  }

  private URL url;
  private URL ipRetrieval;
  private String user;
  private String password;
  private AuthType authType;
  private Method method;
  private SharedPreferences sharedPrefs;
  
  public HttpHelper(URL[] urls, String authType, SharedPreferences prefs)
  {
    this(urls, AuthType.valueOf(authType), prefs);
  }
  
  public HttpHelper(URL[] urls, AuthType authType, SharedPreferences prefs)
  {
    this.url = urls[0];
    if (urls.length > 1)
      this.ipRetrieval = urls[1];
    this.authType = authType;
    this.sharedPrefs = prefs;
  }
  
  public void setCredentials(String user, String password)
  {
    this.user = user;
    this.password = password;
  }
  
  public AuthType getAuthType() { return authType; }
  public void setAuthType(AuthType authType) { this.authType = authType; }
  
  public byte[] get() throws IOException
  {
    method = Method.Get;
    return request();
  }
  
  public byte[] post() throws IOException
  {
    method = Method.Post;
    return request();
  }
  
  private byte[] request() throws IOException
  {
    if (authType == AuthType.Basic)
      return retrieveWithBasicAuth();
    else if (authType == AuthType.Digest)
      return retrieveWithDigestAuth();
    else
      return retrieveWithNoAuth();
  }
  
  private byte[] retrieveWithNoAuth() throws IOException
  {
    Map<String,String> headers = new HashMap<String,String>();
    headers.put("Accept", "application/json");
    return retrieve(headers);
  }
  
  private byte[] retrieveWithBasicAuth() throws IOException
  {  
    Map<String,String> headers = new HashMap<String,String>();
    headers.put("Accept", "application/json");
    String credentials = Base64.encodeToString((user + ":" + password).getBytes(), Base64.DEFAULT);
    headers.put("Authorization", "Basic " + credentials);
    return retrieve(headers);
  }
  
  private byte[] retrieveWithDigestAuth() throws IOException
  {
    InputStream is = null;
    BufferedInputStream bis = null;
    AndroidHttpClient httpClient = null;
    
    try
    {
      long startTime = System.currentTimeMillis();
      
      httpClient = AndroidHttpClient.newInstance("Android");
      HttpParams httpParams = httpClient.getParams();
      HttpConnectionParams.setConnectionTimeout(httpParams, getConnectTimeout());
      HttpConnectionParams.setSoTimeout(httpParams, getReadTimeout());

      HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

      HttpRequestBase job;
      if (method == Method.Get)
        job = new HttpGet(url.toString());
      else if (method == Method.Post)
      {
        job = new HttpPost(url.toString());
        ((HttpPost)job).setEntity(new ByteArrayEntity("".getBytes()));
      }
      else
        throw new IOException("Unsupported HTTP method: " + method);
      
      job.setHeader("Accept", "application/json");
      HttpResponse response = null;
      try
      {
        response = httpClient.execute(host, job, getDigestAuthContext(url.getHost(), url.getPort(), user, password));
      }
      catch (IOException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        if (ipRetrieval != null)
        {
          // try and retrieve the backend IP
          String ip = retrieveBackendIp();
          host = new HttpHost(ip, url.getPort(), url.getProtocol());
          response = httpClient.execute(host, job, getDigestAuthContext(ip, url.getPort(), user, password));
          // save the retrieved ip as the external static one
          Editor ed = sharedPrefs.edit();
          ed.putString(AppSettings.MYTH_BACKEND_EXTERNAL_HOST, ip);
          ed.commit();
        }
        else
        {
          throw ex;
        }
      }
      is = response.getEntity().getContent();
      bis = new BufferedInputStream(is);
      ByteArrayBuffer baf = new ByteArrayBuffer(50);
      int ch = 0;
      while ((ch = bis.read()) != -1)
      {
        baf.append((byte)ch);
      }
      
      if (BuildConfig.DEBUG)
        Log.d(TAG, "http request time: " + (System.currentTimeMillis() - startTime) + " ms");
      return baf.toByteArray();
    }
    finally
    {
      try
      {
        if (bis != null)
          bis.close();
        if (is != null)
          is.close();
        if (httpClient != null)
          httpClient.close();    
      }
      catch (IOException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
      }
    }
  }
  
  private HttpContext getDigestAuthContext(String host, int port, String user, String password)
  {
    CredentialsProvider cp = new BasicCredentialsProvider();
    AuthScope scope = new AuthScope(host, port);
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, password);
    cp.setCredentials(scope, creds);
    HttpContext credContext = new BasicHttpContext();
    credContext.setAttribute(ClientContext.CREDS_PROVIDER, cp);
    return credContext;
  }
  
  private byte[] retrieve(Map<String,String> headers) throws IOException
  {  
    InputStream is = null;
    BufferedInputStream bis = null;
    URLConnection conn = null;
    try
    {
      long startTime = System.currentTimeMillis();
      conn = url.openConnection();
      prepareConnection(conn, headers);
      try
      {
        is = conn.getInputStream();
      }
      catch (IOException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
        if (ipRetrieval != null)
        {
          // try and retrieve the backend IP
          String ip = retrieveBackendIp();
          url = new URL(url.getProtocol(), ip, url.getPort(), url.getFile());
          conn = url.openConnection();
          prepareConnection(conn, headers);
          is = conn.getInputStream();
        }
        else
        {
          throw ex;
        }
      }
      bis = new BufferedInputStream(is);
      ByteArrayBuffer baf = new ByteArrayBuffer(50);
      int ch = 0;
      while ((ch = bis.read()) != -1)
        baf.append((byte)ch);
      
      if (BuildConfig.DEBUG)
        Log.d(TAG, url + "http request time: " + (System.currentTimeMillis() - startTime) + " ms");
      return baf.toByteArray();
    }
    finally
    {
      try
      {
        if (bis != null)
          bis.close();
        if (is != null)
          is.close();
      }
      catch (IOException ex)
      {
        if (BuildConfig.DEBUG)
          Log.e(TAG, ex.getMessage(), ex);
      }
    }
  }
  
  private void prepareConnection(URLConnection conn, Map<String,String> headers) throws IOException
  {
    conn.setConnectTimeout(getConnectTimeout());
    conn.setReadTimeout(getReadTimeout());
    for (String key : headers.keySet())
      conn.setRequestProperty(key, headers.get(key));
    
    if (method == Method.Post)
    {
      ((HttpURLConnection)conn).setRequestMethod("POST");
    }
  }
  
  private String retrieveBackendIp() throws IOException
  {
    HttpHelper helper = new HttpHelper(new URL[]{ipRetrieval}, AuthType.None, sharedPrefs);
    String backendIp = new String(helper.get());
    if (!AppSettings.validateIp(backendIp))
      throw new IOException("Bad IP Address: " + backendIp);
    Editor ed = sharedPrefs.edit();
    ed.putString("mythbe_external_ip", backendIp);
    ed.commit();
    return backendIp;
  }
  
  // TODO: parameterize
  public int getConnectTimeout()
  {
    return 6000;
  }
  // TODO: parameterize
  public int getReadTimeout()
  {
    return 10000;
  }
  
}
