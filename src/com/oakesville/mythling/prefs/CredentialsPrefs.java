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

public class CredentialsPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrefsActivity)getActivity()).setActionBarTitle(R.string.title_credentials_settings);
        addPreferencesFromResource(R.xml.credentials_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_AUTH_TYPE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                doEnableMythTvAccessCreds(!AppSettings.AUTH_TYPE_NONE.equals(newValue));
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref.setSummary(appSettings.getMythTvServicesAuthType());
        doEnableMythTvAccessCreds(!AppSettings.AUTH_TYPE_NONE.equals(appSettings.getMythTvServicesAuthType()));

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_USER);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getMythTvServicesUser());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_PASSWORD);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
            }
        });
        pref.setSummary(appSettings.getMythTvServicesPasswordMasked());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_AUTH_TYPE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (AppSettings.AUTH_TYPE_SAME.equals(newValue)) {
                    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
                    String user = appSettings.getMythTvServicesUser();
                    appSettings.setMythlingServicesUser(user);
                    getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_USER).setSummary(user);
                    appSettings.setMythlingServicesPassword(appSettings.getMythTvServicesPassword());
                    getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_PASSWORD).setSummary(appSettings.getMythTvServicesPasswordMasked());
                }
                doEnableMythlingAccessCreds(!AppSettings.AUTH_TYPE_NONE.equals(newValue) & !AppSettings.AUTH_TYPE_SAME.equals(newValue));
                return super.onPreferenceChange(preference, newValue);
            }
        });
        String webAuthType = appSettings.getMythlingServicesAuthType();
        pref.setSummary(webAuthType);
        doBackendWebCredsEnablement(appSettings.isHasBackendWeb());
        doEnableMythlingAccessCreds(appSettings.isHasBackendWeb() && !AppSettings.AUTH_TYPE_NONE.equals(webAuthType) && !AppSettings.AUTH_TYPE_SAME.equals(webAuthType));

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_USER);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getBackendWebUser());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_PASSWORD);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
            }
        });
        pref.setSummary(appSettings.getBackendWebPasswordMasked());
    }

    private void doEnableMythTvAccessCreds(boolean enabled) {
        Preference userPref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_USER);
        userPref.setEnabled(enabled);
        userPref.setSelectable(enabled);  // for Fire TV
        Preference pwPref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_PASSWORD);
        pwPref.setEnabled(enabled);
        pwPref.setSelectable(enabled); // for Fire TV
    }

    private void doEnableMythlingAccessCreds(boolean enabled) {
        Preference userPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_USER);
        userPref.setEnabled(enabled);
        userPref.setSelectable(enabled); // for Fire TV
        Preference pwPref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_PASSWORD);
        pwPref.setEnabled(enabled);
        pwPref.setSelectable(enabled);  // for Fire TV
    }

    private void doBackendWebCredsEnablement(boolean hasBackendWeb) {
        Preference mythlingAccessCat = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICE_ACCESS_CATEGORY);
        mythlingAccessCat.setEnabled(hasBackendWeb);
        Preference authTypePref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_AUTH_TYPE);
        authTypePref.setEnabled(hasBackendWeb);
        authTypePref.setSelectable(hasBackendWeb); // for Fire TV
    }

}