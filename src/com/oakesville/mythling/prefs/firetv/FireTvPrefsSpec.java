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
package com.oakesville.mythling.prefs.firetv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppResources;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.DevicePrefsSpec;

import android.content.Context;

public class FireTvPrefsSpec implements DevicePrefsSpec {

    private String playbackOptionsJson;

    public FireTvPrefsSpec(Context appContext) throws IOException {
        AppResources appResources = new AppResources(appContext);
        playbackOptionsJson = appResources.readJsonString(R.raw.firetv_playback_options);
    }

    public boolean appliesToDevice(String manufacturer, String model) {
        return "Amazon".equals(manufacturer) && model != null && model.startsWith("AFT");
    }

    public int getPrefsHeadersResource() {
        return R.xml.firetv_prefs_headers;
    }

    public Map<String,Object> getDefaultValues() {
        Map<String,Object> defaults = new HashMap<>();
        defaults.put(AppSettings.INTERNAL_VIDEO_RES, "720");
        defaults.put(AppSettings.TRANSCODE_TIMEOUT, "60");
        defaults.put(AppSettings.EPG_PARAMS, "bufferSize=0"); // guideInterval added dynamically
        defaults.put(AppSettings.PLAYBACK_OPTIONS_JSON, playbackOptionsJson);
        defaults.put(AppSettings.PROMPT_FOR_PLAYBACK_OPTIONS, false);
        return defaults;
    }

    public boolean supportsWebLinks() {
        return false;
    }
}
