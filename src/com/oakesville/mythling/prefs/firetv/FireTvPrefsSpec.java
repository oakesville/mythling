/**
 * Copyright 2014 Donald Oakes
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
package com.oakesville.mythling.prefs.firetv;

import java.util.HashMap;
import java.util.Map;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.DevicePrefsSpec;

public class FireTvPrefsSpec implements DevicePrefsSpec {

    public boolean appliesToDevice(String manufacturer, String model) {
        return "Amazon".equals(manufacturer) && model != null && model.startsWith("AFT");
    }

    public int getPrefsHeadersResource() {
        return R.xml.firetv_prefs_headers;
    }

    public Map<String,Object> getDefaultValues() {
        Map<String,Object> defaults = new HashMap<String,Object>();
        defaults.put(AppSettings.INTERNAL_VIDEO_PLAYER, true);
        defaults.put(AppSettings.INTERNAL_VIDEO_RES, "720");
        defaults.put(AppSettings.INTERNAL_VIDEO_BITRATE, "80000");
        defaults.put(AppSettings.TRANSCODE_TIMEOUT, "45");
        return defaults;
    }

    public boolean supportsWebLinks() {
        return false;
    }
}
