/**
 * Copyright 2013 Donald Oakes
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

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.R;

public class ConnectionsPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_connect_settings);
    addPreferencesFromResource(R.xml.connect_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_SERVICE_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary("" + appSettings.getBackendServicePort());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_WEB_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary("" + appSettings.getBackendWebPort());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_WEB_ROOT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getBackendWebRoot());

    pref = getPreferenceScreen().findPreference(AppSettings.TUNER_TIMEOUT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
    pref.setSummary("" + appSettings.getTunerTimeout() + " Seconds");

    pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_TIMEOUT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
    pref.setSummary("" + appSettings.getTranscodeTimeout() + " Seconds");
  }
  
}
