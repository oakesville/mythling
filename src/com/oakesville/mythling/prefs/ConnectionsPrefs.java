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
import android.preference.SwitchPreference;

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
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICE_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary("" + appSettings.getMythTvServicePort());

    SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES);
    swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        boolean useMythlingSvcs = Boolean.valueOf(newValue.toString());
        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(useMythlingSvcs);
        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT).setEnabled(useMythlingSvcs);
        return super.onPreferenceChange(preference, newValue);
      }
    });
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(appSettings.isMythlingMediaServices());
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary("" + appSettings.getMythlingWebPort());
    pref.setEnabled(appSettings.isMythlingMediaServices());
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMythlingWebRoot());
    pref.setEnabled(appSettings.isMythlingMediaServices());

    pref = getPreferenceScreen().findPreference(AppSettings.TUNER_TIMEOUT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
    pref.setSummary("" + appSettings.getTunerTimeout() + " Seconds");

    pref = getPreferenceScreen().findPreference(AppSettings.TRANSCODE_TIMEOUT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "Seconds"));
    pref.setSummary("" + appSettings.getTranscodeTimeout() + " Seconds");
  }
  
}
