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
package com.oakesville.mythling.prefs.firetv;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefChangeListener;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class FireTvBasicPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_basic_setup);
        addPreferencesFromResource(R.xml.firetv_basic_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_INTERNAL_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getInternalBackendHost());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICE_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary("" + appSettings.getMythTvServicePort());

        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_PLAYER);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_VERSION);
        pref.setTitle(AppSettings.getMythlingVersion() + " (sdk " + AppSettings.getAndroidVersion() + ")");
    }
}
