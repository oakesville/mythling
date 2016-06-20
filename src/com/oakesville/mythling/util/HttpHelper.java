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
package com.oakesville.mythling.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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
import org.mozilla.universalchardet.UniversalDetector;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.http.AndroidHttpClient;
import android.os.Debug;
import android.util.Base64;
import android.util.Log;

public class HttpHelper {
    private static final String TAG = HttpHelper.class.getSimpleName();

    public enum AuthType {
        None,
        Basic,
        Digest
    }

    public enum Method {
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
    private boolean binary;
    private String charset = "UTF-8";
    private byte[] postContent;

    public String getCharSet() {
        return charset;
    }

    public HttpHelper(URL url) {
        this(new URL[]{url}, AuthType.None, null, false);
    }

    public HttpHelper(URL[] urls, String authType, SharedPreferences prefs) {
        this(urls, AuthType.valueOf(authType), prefs, false);
    }

    public HttpHelper(URL[] urls, String authType, SharedPreferences prefs, boolean binary) {
        this(urls, AuthType.valueOf(authType), prefs, binary);
    }

    public HttpHelper(URL[] urls, AuthType authType, SharedPreferences prefs, boolean binary) {
        this.url = urls[0];
        if (urls.length > 1)
            this.ipRetrieval = urls[1];
        this.authType = authType;
        this.sharedPrefs = prefs;
        this.binary = binary;
    }

    public void setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public byte[] get() throws IOException {
        method = Method.Get;
        return request();
    }

    public byte[] post() throws IOException {
        method = Method.Post;
        return request();
    }

    public byte[] post(byte[] content) throws IOException {
        postContent = content;
        return post();
    }

    private byte[] request() throws IOException {
        if (authType == AuthType.Basic)
            return retrieveWithBasicAuth();
        else if (authType == AuthType.Digest)
            return retrieveWithDigestAuth();
        else
            return retrieveWithNoAuth();
    }

    private byte[] retrieveWithNoAuth() throws IOException {
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Accept", "application/json");
        return retrieve(headers);
    }

    private byte[] retrieveWithBasicAuth() throws IOException {
        Map<String,String> headers = new HashMap<String,String>();
        headers.put("Accept", "application/json");
        String credentials = Base64.encodeToString((user + ":" + password).getBytes(), Base64.DEFAULT);
        headers.put("Authorization", "Basic " + credentials);
        return retrieve(headers);
    }

    private byte[] retrieveWithDigestAuth() throws IOException {
        AndroidHttpClient httpClient = null;
        InputStream is = null;

        try {
            long startTime = System.currentTimeMillis();

            httpClient = AndroidHttpClient.newInstance("Android");
            HttpParams httpParams = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, getConnectTimeout());
            HttpConnectionParams.setSoTimeout(httpParams, getReadTimeout());

            HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

            HttpRequestBase job;
            if (method == Method.Get)
                job = new HttpGet(url.toString());
            else if (method == Method.Post) {
                job = new HttpPost(url.toString());
                ((HttpPost) job).setEntity(new ByteArrayEntity("".getBytes()));
            } else
                throw new IOException("Unsupported HTTP method: " + method);

            job.setHeader("Accept", "application/json");
            HttpResponse response = null;
            try {
                response = httpClient.execute(host, job, getDigestAuthContext(url.getHost(), url.getPort(), user, password));
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (ipRetrieval != null) {
                    // try and retrieve the backend IP
                    String ip = retrieveBackendIp();
                    host = new HttpHost(ip, url.getPort(), url.getProtocol());
                    response = httpClient.execute(host, job, getDigestAuthContext(ip, url.getPort(), user, password));
                    // save the retrieved ip as the external static one
                    Editor ed = sharedPrefs.edit();
                    ed.putString(AppSettings.MYTH_BACKEND_EXTERNAL_HOST, ip);
                    ed.commit();
                } else {
                    throw ex;
                }
            }
            is = response.getEntity().getContent();

            return extractResponseBytes(is, startTime);
        } finally {
            try {
                if (is != null)
                    is.close();
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    public static HttpContext getDigestAuthContext(String host, int port, String user, String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        AuthScope scope = new AuthScope(host, port);
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, password);
        cp.setCredentials(scope, creds);
        HttpContext credContext = new BasicHttpContext();
        credContext.setAttribute(ClientContext.CREDS_PROVIDER, cp);
        return credContext;
    }

    private byte[] retrieve(Map<String,String> headers) throws IOException {
        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            long startTime = System.currentTimeMillis();
            conn = (HttpURLConnection) url.openConnection();
            prepareConnection(conn, headers);

            try {
                if (postContent != null)
                    writeRequestBytes(conn.getOutputStream());
                is = conn.getInputStream();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                if (ipRetrieval != null) {
                    // try and retrieve the backend IP
                    String ip = retrieveBackendIp();
                    url = new URL(url.getProtocol(), ip, url.getPort(), url.getFile());
                    conn = (HttpURLConnection) url.openConnection();
                    prepareConnection(conn, headers);
                    try {
                        is = conn.getInputStream();
                    } catch (IOException ex2) {
                        rethrow(ex2, conn.getResponseMessage());
                    }
                } else {
                    rethrow(ex, conn.getResponseMessage());
                }
            }

            return extractResponseBytes(is, startTime);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    private void prepareConnection(URLConnection conn, Map<String,String> headers) throws IOException {
        conn.setConnectTimeout(getConnectTimeout());
        conn.setReadTimeout(getReadTimeout());
        for (String key : headers.keySet())
            conn.setRequestProperty(key, headers.get(key));

        if (method == Method.Post) {
            ((HttpURLConnection) conn).setRequestMethod("POST");
            if (postContent != null)
                conn.setDoOutput(true);
        }
    }

    private void writeRequestBytes(OutputStream os) throws IOException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(os);
            bos.write(postContent);
        } finally {
            if (bos != null)
                bos.close();
        }
    }

    private byte[] extractResponseBytes(InputStream is, long requestStartTime) throws IOException {
        BufferedInputStream bis = null;
        BufferedReader br = null;

        try {
            ByteArrayBuffer baf = new ByteArrayBuffer(1024);
            bis = new BufferedInputStream(is);
            int b = 0;
            while ((b = bis.read()) != -1)
                baf.append((byte) b);
            long requestEndTime = System.currentTimeMillis();
            byte[] bytes = baf.toByteArray();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, " -> (" + url + ") http request time: " + (requestEndTime - requestStartTime) + " ms");
                // how much memory are we using
                Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
                Debug.getMemoryInfo(memoryInfo);
                Log.d(TAG, " -> response byte array size: " + bytes.length / 1024 + " kb (Pss = " + memoryInfo.getTotalPss() + " kb)");
            }
            if (!binary) {
                // detect character set
                UniversalDetector detector = new UniversalDetector(null);
                detector.handleData(bytes, 0, bytes.length);
                detector.dataEnd();
                String detected = detector.getDetectedCharset();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, " -> charset: " + detected + " - detect time: " + (System.currentTimeMillis() - requestEndTime) + " ms");
                if (detected != null && !detected.equals(charset)) {
                    try {
                        Charset.forName(detected);
                        charset = detected;
                    } catch (UnsupportedCharsetException ex) {
                        // not supported -- stick with UTF-8
                    }
                }
            }

            return bytes;

        } finally {
            try {
                if (bis != null)
                    bis.close();
                if (br != null)
                    br.close();
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    private String retrieveBackendIp() throws IOException {
        HttpHelper helper = new HttpHelper(new URL[]{ipRetrieval}, AuthType.None, sharedPrefs, false);
        String backendIp = new String(helper.get());
        if (!AppSettings.validateIp(backendIp))
            throw new IOException("Bad IP Address: " + backendIp);
        Editor ed = sharedPrefs.edit();
        ed.putString("mythbe_external_ip", backendIp);
        ed.commit();
        return backendIp;
    }

    public int getConnectTimeout() {
        if (sharedPrefs != null)
            return Integer.parseInt(sharedPrefs.getString(AppSettings.HTTP_CONNECT_TIMEOUT, "10").trim()) * 1000;
        else
            return 10000;
    }

    public int getReadTimeout() {
        if (sharedPrefs != null)
            return Integer.parseInt(sharedPrefs.getString(AppSettings.HTTP_READ_TIMEOUT, "30").trim()) * 1000;
        else
            return 30000;
    }

    private void rethrow(IOException ex, String msgPrefix) throws IOException {
        if (msgPrefix == null) {
            throw ex;
        } else {
            IOException re = new IOException(msgPrefix + ": " + ex.getMessage());
            re.setStackTrace(ex.getStackTrace());
            throw re;
        }
    }
}
