/**
 * Copyright 2016 Donald Oakes
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
package com.oakesville.mythling.media;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.oakesville.mythling.app.Localizer;

public class Download {

    private String itemId;
    public String getItemId() { return itemId; }

    private long downloadId;
    public long getDownloadId() { return downloadId; }

    private String path;
    public String getPath() { return path; }

    private Date started;
    public Date getStarted() { return started; }

    public Download(String itemId, long downloadId, String path, Date started) {
        this.itemId = itemId;
        this.downloadId = downloadId;
        this.path = path;
        this.started = started;
    }

    public Download(JSONObject json) throws JSONException, ParseException {
        this.itemId = json.getString("itemId");
        this.downloadId = json.getLong("downloadId");
        if (json.has("path"))
            this.path = json.getString("path");
        this.started = Localizer.SERVICE_DATE_TIME_RAW_FORMAT.parse(json.getString("started"));
        if (json.has("CutList"))
            this.cutList = Cut.parseCutList(json);
    }

    private ArrayList<Cut> cutList;
    public ArrayList<Cut> getCutList() { return cutList; }
    public void setCutList(ArrayList<Cut> cutList) { this.cutList = cutList; }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("itemId", itemId);
        json.put("downloadId", downloadId);
        if (path != null)
            json.put("path", path);
        json.put("started", Localizer.SERVICE_DATE_TIME_RAW_FORMAT.format(started));
        if (cutList != null) {
            JSONObject cutListJson = Cut.toJson(cutList);
            json.put("CutList", cutListJson);
        }
        return json;
    }

}
