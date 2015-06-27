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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefChangeListener;
import com.oakesville.mythling.R;

public class FireTvVideoQualityPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_quality_settings);
        addPreferencesFromResource(R.xml.firetv_quality_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
        Preference pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_RES);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "p"));
        pref.setSummary(appSettings.getInternalVideoRes() + "p");

        pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_BITRATE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k") {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString())) / 1000);
            }
        });
        pref.setSummary(appSettings.getInternalVideoBitrate() / 1000 + "k");

        pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_AUDIO_BITRATE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k") {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString())) / 1000);
            }
        });
        pref.setSummary(appSettings.getInternalAudioBitrate() / 1000 + "k");

    }
}