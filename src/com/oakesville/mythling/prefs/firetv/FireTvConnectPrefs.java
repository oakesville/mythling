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

public class FireTvConnectPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_connect_settings);
        addPreferencesFromResource(R.xml.firetv_connect_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.BACKEND_WEB);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean hasBackendWeb = Boolean.valueOf(newValue.toString());
                getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES_CATEGORY).setEnabled(hasBackendWeb);

                Preference webPortPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
                webPortPref.setEnabled(hasBackendWeb);
                webPortPref.setSelectable(hasBackendWeb);

                Preference webRootPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
                webRootPref.setEnabled(false);
                webRootPref.setSelectable(false);
                SwitchPreference svcsSwPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.MYTHLING_MEDIA_SERVICES);
                svcsSwPref.setEnabled(hasBackendWeb);
                svcsSwPref.setSelectable(hasBackendWeb);

                if (!hasBackendWeb) {
                    if (svcsSwPref.isChecked()) {
                        svcsSwPref.setChecked(false);
                        new AppSettings(getPreferenceScreen().getContext()).setMythlingMediaServices(false);
                    }
                }

                return super.onPreferenceChange(preference, newValue);
            }
        });
        boolean hasBackendWeb = appSettings.isHasBackendWeb();
        Preference webPortPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
        webPortPref.setEnabled(hasBackendWeb);
        webPortPref.setSelectable(hasBackendWeb);
        getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES_CATEGORY).setEnabled(hasBackendWeb);

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary("" + appSettings.getMythlingWebPort());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_MEDIA_SERVICES);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean useMythlingSvcs = Boolean.valueOf(newValue.toString());
                Preference webRootPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
                webRootPref.setEnabled(useMythlingSvcs);
                webRootPref.setSelectable(useMythlingSvcs);
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref.setEnabled(hasBackendWeb);
        pref.setSelectable(hasBackendWeb);

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getMythlingWebRoot());
        boolean webRootEnabled = hasBackendWeb && appSettings.isMythlingMediaServices();
        pref.setEnabled(webRootEnabled);
        pref.setSelectable(webRootEnabled);

        pref = getPreferenceScreen().findPreference(AppSettings.ERROR_REPORTING);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));
    }

}