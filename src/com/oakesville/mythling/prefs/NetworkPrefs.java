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

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class NetworkPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrefsActivity)getActivity()).setActionBarTitle(R.string.title_network_settings);
        addPreferencesFromResource(R.xml.network_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        // these prefs trigger cache refresh
        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.EXTERNAL_NETWORK);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                doCategoryEnablement(Boolean.valueOf(newValue.toString()));
                return super.onPreferenceChange(preference, newValue);
            }
        });
        doCategoryEnablement(appSettings.isExternalNetwork());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_INTERNAL_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean ret = super.onPreferenceChange(preference, newValue);
                preference.setTitle(newValue.toString().isEmpty() ? R.string.title_backend_host_ : R.string.title_backend_host);
                return ret;
            }
        });
        String internalHost = appSettings.getInternalBackendHost();
        pref.setSummary(internalHost);
        if (internalHost.isEmpty())
            pref.setTitle(R.string.title_backend_host_);

        pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_EXTERNAL_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getExternalBackendHost());

        swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.RETRIEVE_IP);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getPreferenceScreen().findPreference(AppSettings.IP_RETRIEVAL_URL).setEnabled(Boolean.valueOf(newValue.toString()));
                return super.onPreferenceChange(preference, newValue);
            }
        });

        pref = getPreferenceScreen().findPreference(AppSettings.IP_RETRIEVAL_URL);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getIpRetrievalUrlString());
        pref.setEnabled(appSettings.isIpRetrieval());
    }

    private void doCategoryEnablement(boolean isExternalNet) {
        Preference internalCat = getPreferenceScreen().findPreference(AppSettings.INTERNAL_BACKEND_CATEGORY);
        internalCat.setEnabled(!isExternalNet);
        Preference externalCat = getPreferenceScreen().findPreference(AppSettings.EXTERNAL_BACKEND_CATEGORY);
        externalCat.setEnabled(isExternalNet);
    }

}
