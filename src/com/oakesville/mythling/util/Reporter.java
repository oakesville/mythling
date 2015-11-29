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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;

import android.util.Log;

public class Reporter {
    private static final String TAG = Reporter.class.getSimpleName();
    public static final String ERROR_REPORTING_URL = "http://54.187.163.194/mythling/report";

    private Throwable throwable;
    private String message;

    public Reporter(Throwable t) {
        this.throwable = t;
        this.message = t.getMessage();
    }

    public Reporter(String message) {
        this.message = message;
    }

    /**
     * Report the error in a background thread.
     */
    public void send() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    JSONObject json = buildJson();
                    HttpHelper helper = new HttpHelper(new URL(ERROR_REPORTING_URL));
                    String response = new String(helper.post(json.toString(2).getBytes()));
                    JSONObject responseJson = new JSONObject(response);
                    JSONObject reportResponse = responseJson.getJSONObject("reportResponse");
                    String status = reportResponse.getString("status");
                    if (!"success".equals(status))
                        throw new IOException("Error response from reporting site: " + reportResponse.getString("message"));
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG)
                        Log.e(TAG, ex.getMessage(), ex);
                }
            }
        }).start();
    }

    private JSONObject buildJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject report = new JSONObject();
        json.put("report", report);
        report.put("source", "Mythling v" + AppSettings.getMythlingVersion() +
                " (sdk " + AppSettings.getAndroidVersion() + ")");
        report.put("message", message == null ? "No message" : message);
        if (throwable != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(out));
            report.put("stackTrace", new String(out.toByteArray()));
        }
        return json;
    }
}
