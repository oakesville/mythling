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

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class SystemPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_system_settings);
        addPreferencesFromResource(R.xml.system_prefs);

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_VERSION);
        pref.setSummary(AppSettings.getMythlingVersion());

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        pref = getPreferenceScreen().findPreference(AppSettings.TUNER_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getTunerTimeout() + " Seconds");

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getTranscodeTimeout() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_JOB_LIMIT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getTranscodeJobLimit());

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_CONNECT_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getHttpConnectTimeout() + " " + getString(R.string.seconds));

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_READ_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, getString(R.string.seconds)));
        pref.setSummary("" + appSettings.getHttpReadTimeout() + " " + getString(R.string.seconds));
    }
}
