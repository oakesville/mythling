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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Uses OKHTTP.  Avoids timeouts for large downloads.
 * Does not provide any status. Only works for non-auth downloads.
 * TODO: reflect this type of download in status icon
 */
public class Downloader {

    private static final String TAG = Downloader.class.getSimpleName();

    private String url;
    private File file;

    public Downloader(String url, File file) {
        this.url = url;
        this.file = file;
    }

    public void doDownload() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();

        InputStream is = response.body().byteStream();
        try (BufferedInputStream input = new BufferedInputStream(is);
                OutputStream output = new FileOutputStream(file);) {
            byte[] data = new byte[1024 * 8];
            int count = 0;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
        }
    }
}
