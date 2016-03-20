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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;

import com.oakesville.mythling.util.HttpHelper.AuthType;

import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.util.Base64;
import android.util.Log;

/**
 * Proxies media streaming requests, providing HTTP Basic and Digest
 * authentication for players that don't natively support these options.
 */
public class MediaStreamProxy implements Runnable {
    private static final String TAG = MediaStreamProxy.class.getSimpleName();

    private InetAddress localhost;
    public InetAddress getLocalhost() { return localhost; }

    private int port;
    public int getPort() { return port; }

    private AndroidHttpClient httpClient;
    private boolean isRunning = true;
    private ServerSocket socket;
    private Thread thread;

    private ProxyInfo proxyInfo;
    private AuthType authType;

    public MediaStreamProxy(ProxyInfo proxyInfo, AuthType authType) {
        this.proxyInfo = proxyInfo;
        this.authType = authType;
    }

    public void init() throws IOException {
        localhost = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        socket = new ServerSocket(port, 0, localhost);
        socket.setSoTimeout(5000);
        port = socket.getLocalPort();
    }

    public void start() {
        if (socket == null)
            throw new IllegalStateException("Cannot start proxy; not initialized.");
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;
        if (thread == null)
            throw new IllegalStateException("Cannot stop proxy; not started.");

        if (httpClient != null)
            httpClient.close();

        thread.interrupt();
        try {
            thread.join(5000);
        } catch (InterruptedException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }

    public void run() {
        Log.i(TAG, "Media stream proxying thru " + localhost.getHostAddress() + ":" + port);
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null)
                    continue;
                Log.d(TAG, "Proxy connected");
                HttpRequest request = readRequest(client);
                processRequest(request, client);
            } catch (SocketTimeoutException ex) {
                // do nothing
            } catch (IOException ex) {
                Log.e(TAG, "Proxy error connecting to client", ex);
            }
        }
        Log.d(TAG, "Proxy interrupted. Shutting down.");
    }

    private HttpRequest readRequest(Socket client) throws IOException {
        HttpRequest request = null;
        InputStream is;
        String firstLine;
        is = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
        firstLine = reader.readLine();

        if (firstLine == null) {
            Log.i(TAG, "Proxy client closed connection without a request.");
            return request;
        }

        StringTokenizer st = new StringTokenizer(firstLine);
        String method = st.nextToken();
        String uri = st.nextToken();
        Log.d(TAG, uri);
        String realUri = uri.substring(1);
        Log.d(TAG, realUri);
        request = new BasicHttpRequest(method, realUri);
        return request;
    }

    private void processRequest(HttpRequest request, Socket client) throws IllegalStateException, IOException {
        if (request == null)
            return;

        Log.d(TAG, "Proxy processing");

        HttpResponse realResponse = download();
        if (realResponse == null)
            return;

        Log.d(TAG, "Proxy downloading...");

        InputStream data = realResponse.getEntity().getContent();
        StatusLine line = realResponse.getStatusLine();
        HttpResponse response = new BasicHttpResponse(line);
        response.setHeaders(realResponse.getAllHeaders());

        Log.d(TAG, "Proxy reading headers");
        StringBuilder httpString = new StringBuilder();
        httpString.append(response.getStatusLine().toString());
        httpString.append("\n");
        for (Header h : response.getAllHeaders()) {
            // TODO: this is disabled until it is made optional
//            if (h.getName().equals("Content-Type") && proxyInfo.isMpeg())
//                httpString.append(h.getName()).append(": ").append("video/mpeg").append("\n");
//            else
            httpString.append(h.getName()).append(": ").append(h.getValue()).append("\n");
        }
        httpString.append("\n");
        Log.d(TAG, "Proxy headers done");

        try {
            byte[] buffer = httpString.toString().getBytes();
            int readBytes;
            Log.d(TAG, "writing to client");
            client.getOutputStream().write(buffer, 0, buffer.length);

            // start streaming content
            byte[] buff = new byte[1024 * 50];
            while (isRunning && (readBytes = data.read(buff, 0, buff.length)) != -1) {
                client.getOutputStream().write(buff, 0, readBytes);
            }
        } finally {
            if (data != null)
                data.close();
            client.close();
        }
    }

    private HttpResponse download() throws IOException {

        httpClient = AndroidHttpClient.newInstance("Android");

        URL netUrl = proxyInfo.netUrl;
        HttpHost host = new HttpHost(netUrl.getHost(), netUrl.getPort(), netUrl.getProtocol());

        HttpRequestBase request = new HttpGet(netUrl.toString());
        HttpResponse response = null;
        Log.d(TAG, "Proxy starting download");
        if (authType == AuthType.Digest) {
            HttpContext context = HttpHelper.getDigestAuthContext(netUrl.getHost(), netUrl.getPort(), proxyInfo.user, proxyInfo.password);
            response = httpClient.execute(host, request, context);
        }
        else if (authType == AuthType.Basic) {
            String credentials = Base64.encodeToString((proxyInfo.user + ":" + proxyInfo.password).getBytes(), Base64.DEFAULT);
            request.setHeader("Authorization", "Basic " + credentials);
            response = httpClient.execute(host, request);
        }
        else {
            response = httpClient.execute(host, request);
        }
        Log.d(TAG, "Proxy response downloaded");
        return response;
    }

    public static class ProxyInfo {
        public ProxyInfo() {};
        public ProxyInfo(URL netUrl) { this.netUrl = netUrl; }

        private URL netUrl;
        public URL getNetUrl() { return netUrl; }
        public void setNetUrl(URL url) { this.netUrl = url; }

        private String user;
        private String password;
    }


    /**
     * Returns ProxyInfo if proxy is needed, null otherwise.
     */
    public static ProxyInfo needsAuthProxy(Uri mediaUri) throws MalformedURLException {
        ProxyInfo proxyInfo = null;
        String userInfo = mediaUri.getUserInfo();
        int colon = userInfo == null ? -1 : userInfo.indexOf(':');
        if (colon > 0) {
            proxyInfo = new ProxyInfo();
            proxyInfo.user = userInfo.substring(0, colon);
            proxyInfo.password = userInfo.substring(colon + 1);
            proxyInfo.netUrl = getNetUrl(mediaUri);
        }

        return proxyInfo;
    }

    public static URL getNetUrl(Uri mediaUri) throws MalformedURLException {
        String netUrl = mediaUri.getScheme() + "://" + mediaUri.getHost() + ":" + mediaUri.getPort();
        netUrl += mediaUri.getPath();
        if (mediaUri.getQuery() != null)
            netUrl += "?" + mediaUri.getQuery();
        return new URL(netUrl);
    }
}
