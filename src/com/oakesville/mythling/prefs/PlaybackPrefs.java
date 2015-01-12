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
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class PlaybackPrefs extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_playback_settings);
        addPreferencesFromResource(R.xml.playback_prefs);

        // none of these prefs trigger cache refresh

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.FRONTEND_PLAYBACK);
        swPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                doCategoryEnablement(!Boolean.valueOf(newValue.toString()));
                return true;
            }
        });
        doCategoryEnablement(appSettings.isDevicePlayback());

        swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_PLAYER);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getFrontendHost());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_SOCKET_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getFrontendSocketPort());

        pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_SERVICE_PORT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getFrontendServicePort());
    }

    private void doCategoryEnablement(boolean isDevice) {
        Preference deviceCat = getPreferenceScreen().findPreference(AppSettings.DEVICE_PLAYBACK_CATEGORY);
        deviceCat.setEnabled(isDevice);

        Preference frontendCat = getPreferenceScreen().findPreference(AppSettings.FRONTEND_PLAYBACK_CATEGORY);
        frontendCat.setEnabled(!isDevice);
    }
}
