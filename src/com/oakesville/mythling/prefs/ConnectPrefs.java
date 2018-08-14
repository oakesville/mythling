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

public class ConnectPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrefsActivity)getActivity()).setActionBarTitle(R.string.title_connect_settings);
        addPreferencesFromResource(R.xml.connect_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICE_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary("" + appSettings.getMythTvServicePort());

        pref = getPreferenceScreen().findPreference(AppSettings.BACKEND_WEB);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean hasBackendWeb = Boolean.valueOf(newValue.toString());
                getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(hasBackendWeb);

                if (!hasBackendWeb) {
                    SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.MYTHLING_MEDIA_SERVICES);
                    if (swPref.isChecked())
                        swPref.setChecked(false);
                }

                doCategoryEnablement(hasBackendWeb);

                if (hasBackendWeb) {
                    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
                    if (!appSettings.isMythlingMediaServices())
                        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT).setEnabled(false);
                }

                return super.onPreferenceChange(preference, newValue);
            }
        });
        boolean hasBackendWeb = appSettings.isHasBackendWeb();
        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(hasBackendWeb);
        doCategoryEnablement(hasBackendWeb);

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary("" + appSettings.getMythlingWebPort());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_MEDIA_SERVICES);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean useMythlingSvcs = Boolean.valueOf(newValue.toString());
                getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT).setEnabled(useMythlingSvcs);
                return super.onPreferenceChange(preference, newValue);
            }
        });

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getMythlingWebRoot());
        pref.setEnabled(hasBackendWeb && appSettings.isMythlingMediaServices());

        pref = getPreferenceScreen().findPreference(AppSettings.RETRIEVE_TRANSCODE_STATUSES);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true));

        pref = getPreferenceScreen().findPreference(AppSettings.ERROR_REPORTING);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));
    }

    private void doCategoryEnablement(boolean hasBackendWeb) {
        Preference cat = getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES_CATEGORY);
        cat.setEnabled(hasBackendWeb);
    }
}
