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

public class TimeoutPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_timeout_settings);
        addPreferencesFromResource(R.xml.timeout_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.TUNER_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
        pref.setSummary("" + appSettings.getTunerTimeout() + " Seconds");

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
        pref.setSummary("" + appSettings.getTranscodeTimeout() + " Seconds");

        pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_JOB_LIMIT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary("" + appSettings.getTranscodeJobLimit());

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_CONNECT_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
        pref.setSummary("" + appSettings.getHttpConnectTimeout() + " Seconds");

        pref = getPreferenceScreen().findPreference(AppSettings.HTTP_READ_TIMEOUT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
        pref.setSummary("" + appSettings.getHttpReadTimeout() + " Seconds");

    }
}
