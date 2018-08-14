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
import com.oakesville.mythling.app.AppData;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefChangeListener;
import com.oakesville.mythling.prefs.PrefsActivity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class FireTvProgramGuidePrefs extends PreferenceFragment {
    private AppSettings appSettings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrefsActivity)getActivity()).setActionBarTitle(R.string.title_guide);
        addPreferencesFromResource(R.xml.firetv_guide_prefs);

        appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.EPG_CHANNEL_GROUP);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new AppData(getActivity().getApplicationContext()).clearChannelGroups();
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref.setSummary(appSettings.getEpgChannelGroup());

        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.EPG_CHANNEL_ICONS);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                appSettings.setEpgLastLoad(0); // refresh
                return super.onPreferenceChange(preference, newValue);
            }
        });
        swPref.setChecked(appSettings.isEpgChannelIcons());
   }
}