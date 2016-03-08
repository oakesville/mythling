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
package com.oakesville.mythling.media;

import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Cut implements Serializable {

    public int start; // seconds
    public int end; // seconds

    public Cut(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean equals(Cut other) {
        return other != null && other.start == this.start && other.end == this.end;
    }

    public String toString() {
        return start + " - " + end;
    }

    /**
     * Uses MythTV format.
     */
    public static ArrayList<Cut> parseCutList(JSONObject jsonObj) throws JSONException {
        ArrayList<Cut> cuts = new ArrayList<Cut>();
        JSONObject cutList = jsonObj.getJSONObject("CutList");
        JSONArray cuttings = cutList.getJSONArray("Cuttings");
        int curCutStart = -1;
        for (int i = 0; i < cuttings.length(); i++) {
            JSONObject cutting = cuttings.getJSONObject(i);
            int mark = Integer.parseInt(cutting.getString("Mark"));
            long offset = Long.parseLong(cutting.getString("Offset"));
            if (mark == 4)
                curCutStart = (int)(offset / 1000);
            else if (mark == 5)
                cuts.add(new Cut(curCutStart, (int)(offset / 1000)));
        }
        return cuts;
    }

    public static JSONObject toJson(ArrayList<Cut> cutList) throws JSONException {
        JSONObject cutListJson = new JSONObject();
        JSONArray cuttings = new JSONArray();
        for (Cut cut : cutList) {
            JSONObject startCutJson = new JSONObject();
            startCutJson.put("Mark", "4");
            startCutJson.put("Offset", String.valueOf(cut.start * 1000));
            cuttings.put(startCutJson);
            JSONObject endCutJson = new JSONObject();
            endCutJson.put("Mark", "5");
            endCutJson.put("Offset", String.valueOf(cut.end * 1000));
            cuttings.put(endCutJson);
        }
        cutListJson.put("Cuttings", cuttings);
        return cutListJson;
    }

}
