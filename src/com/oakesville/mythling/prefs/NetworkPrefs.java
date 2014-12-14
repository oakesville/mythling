/**
 * Copyright 2014 Donald Oakes
 *
 * This file is part of Mythling.
 *
 * Mythling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mythling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mythling.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.oakesville.mythling.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.R;

public class NetworkPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_network_settings);
        addPreferencesFromResource(R.xml.network_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        // these prefs triggers cache refresh
        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.NETWORK_LOCATION);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                doCategoryEnablement(Boolean.valueOf(newValue.toString()));
                return super.onPreferenceChange(preference, newValue);
            }
        });
        doCategoryEnablement(appSettings.isExternalNetwork());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_INTERNAL_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getInternalBackendHost());

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
