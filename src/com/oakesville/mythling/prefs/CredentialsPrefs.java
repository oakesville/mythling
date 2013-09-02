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

public class CredentialsPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_credentials_settings);
    addPreferencesFromResource(R.xml.credentials_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.SERVICES_AUTH_TYPE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doEnableServicesAccessCreds(!"None".equals(newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getServicesAuthType());
    doEnableServicesAccessCreds(!"None".equals(appSettings.getServicesAuthType()));

    pref = getPreferenceScreen().findPreference(AppSettings.SERVICES_ACCESS_USER);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getServicesAccessUser());

    pref = getPreferenceScreen().findPreference(AppSettings.SERVICES_ACCESS_PASSWORD);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
      }
    });
    pref.setSummary(appSettings.getServicesAccessPasswordMasked());

    pref = getPreferenceScreen().findPreference(AppSettings.WEB_AUTH_TYPE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doEnableWebAccessCreds(!"None".equals(newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getWebAuthType());
    doEnableWebAccessCreds(!"None".equals(appSettings.getWebAuthType()));

    pref = getPreferenceScreen().findPreference(AppSettings.WEB_ACCESS_USER);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getWebAccessUser());

    pref = getPreferenceScreen().findPreference(AppSettings.WEB_ACCESS_PASSWORD);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
      }
    });
    pref.setSummary(appSettings.getWebAccessPasswordMasked());    
  }  
  
  private void doEnableServicesAccessCreds(boolean enabled)
  {
    getPreferenceScreen().findPreference(AppSettings.SERVICES_ACCESS_USER).setEnabled(enabled);
    getPreferenceScreen().findPreference(AppSettings.SERVICES_ACCESS_PASSWORD).setEnabled(enabled);
  }
  
  private void doEnableWebAccessCreds(boolean enabled)
  {
    getPreferenceScreen().findPreference(AppSettings.WEB_ACCESS_USER).setEnabled(enabled);
    getPreferenceScreen().findPreference(AppSettings.WEB_ACCESS_PASSWORD).setEnabled(enabled);
  }
}