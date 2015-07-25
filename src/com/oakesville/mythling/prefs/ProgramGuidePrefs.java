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
import android.preference.SwitchPreference;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;

public class ProgramGuidePrefs extends PreferenceFragment {
    private AppSettings appSettings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_guide_settings);
        addPreferencesFromResource(R.xml.guide_prefs);

        appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean hostedEpg = Boolean.valueOf(newValue.toString());
                getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG_ROOT).setEnabled(hostedEpg);
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref = getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG_ROOT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getHostedEpgRoot());
        pref.setEnabled(appSettings.isHostedEpg());

        pref = getPreferenceScreen().findPreference(AppSettings.EPG_CHANNEL_GROUP);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new AppData(getActivity().getApplicationContext()).clearChannelGroups();
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref.setSummary(appSettings.getEpgChannelGroup());

        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.EPG_OMB);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        swPref.setChecked(appSettings.isEpgOmb());

        pref = getPreferenceScreen().findPreference(AppSettings.EPG_SCALE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "%%") {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, String.valueOf((int)(Float.parseFloat(newValue.toString()) * 100)));
            }
        });
        pref.setSummary(String.valueOf((int)(Float.parseFloat(appSettings.getEpgScale()) * 100)) + "%%");

        pref = getPreferenceScreen().findPreference(AppSettings.EPG_PARAMS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref.setSummary(appSettings.getEpgParams());
    }
}