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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class SystemPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_system_settings);
        addPreferencesFromResource(R.xml.system_prefs);

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_VERSION);
        pref.setSummary(AppSettings.getMythlingVersion());

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        pref = getPreferenceScreen().findPreference(AppSettings.TUNER_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getTunerTimeout() + " Seconds");

        pref = getPreferenceScreen().findPreference(AppSettings.TUNER_LIMIT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getTunerLimit());

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getTranscodeTimeout() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_JOB_LIMIT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getTranscodeJobLimit());

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_CONNECT_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getHttpConnectTimeout() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_READ_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getHttpReadTimeout() + " " + getString(R.string.seconds));
    }
}
