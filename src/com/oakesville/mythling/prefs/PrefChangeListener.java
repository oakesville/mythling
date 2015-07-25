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
package com.oakesville.mythling.prefs;

import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.oakesville.mythling.app.AppSettings;

public class PrefChangeListener implements OnPreferenceChangeListener {
    private boolean triggerCacheRefresh;
    private boolean triggerSummaryUpdate;
    private String units;

    public void setUnits(String units) {
        this.units = units;
    }

    public PrefChangeListener(boolean triggerSummaryUpdate, boolean triggerCacheRefresh) {
        this.triggerSummaryUpdate = triggerSummaryUpdate;
        this.triggerCacheRefresh = triggerCacheRefresh;
    }

    public PrefChangeListener(boolean triggerSummaryUpdate, boolean triggerCacheRefresh, String units) {
        this(triggerSummaryUpdate, triggerCacheRefresh);
        this.units = units;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (triggerSummaryUpdate) {
            preference.setSummary(newValue == null ? "" : (newValue.toString() + (units == null ? "" : " " + units)));
        }
        if (triggerCacheRefresh) {
            Editor ed = preference.getEditor();
            ed.putLong(AppSettings.LAST_LOAD, 0);
            ed.putLong(AppSettings.EPG_LAST_LOAD, 0);
            ed.commit();
        }
        return true;
    }
}
