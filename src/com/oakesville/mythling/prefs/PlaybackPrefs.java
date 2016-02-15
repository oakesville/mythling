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
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

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

        // TODO: CLEAR PLAYBACK OPTIONS

        Preference pref = getPreferenceScreen().findPreference(AppSettings.SKIP_BACK_INTERVAL);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getSkipBackInterval() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.SKIP_FORWARD_INTERVAL);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getSkipForwardInterval() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.JUMP_INTERVAL);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getJumpInterval() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.COMMERCIAL_SKIP);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getCommercialSkip());

        pref = getPreferenceScreen().findPreference(AppSettings.LIBVLC_PARAMETERS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getLibVlcParameters());

        swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.INTERNAL_MUSIC_PLAYER);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));

        pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_HOST);
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
        Preference deviceCatVid = getPreferenceScreen().findPreference(AppSettings.DEVICE_PLAYBACK_CATEGORY_VIDEO);
        deviceCatVid.setEnabled(isDevice);
        Preference deviceCatMus = getPreferenceScreen().findPreference(AppSettings.DEVICE_PLAYBACK_CATEGORY_MUSIC);
        deviceCatMus.setEnabled(isDevice);

        Preference frontendCat = getPreferenceScreen().findPreference(AppSettings.FRONTEND_PLAYBACK_CATEGORY);
        frontendCat.setEnabled(!isDevice);
    }
}
