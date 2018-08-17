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

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;

import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class PrefChangeListener implements OnPreferenceChangeListener {
    private final boolean triggerCacheRefresh;
    private final boolean triggerSummaryUpdate;
    private String units;
    private int valuesResId;
    private int entriesResId;

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

    public PrefChangeListener(boolean triggerSummaryUpdate, boolean triggerCacheRefresh, int valuesResId, int entriesResId) {
        this(triggerSummaryUpdate, triggerCacheRefresh);
        this.valuesResId = valuesResId;
        this.entriesResId = entriesResId;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (triggerSummaryUpdate) {
            if (newValue == null) {
                preference.setSummary("");
            }
            else if (valuesResId != 0 && entriesResId != 0)
                preference.setSummary(Localizer.getStringArrayEntry(valuesResId, entriesResId, newValue.toString()));
            else
                preference.setSummary(newValue.toString() + (units == null ? "" : " " + units));
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
