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

public class CredentialsPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_credentials_settings);
    addPreferencesFromResource(R.xml.credentials_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_AUTH_TYPE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doEnableMythTvAccessCreds(!"None".equals(newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getMythTvServicesAuthType());
    doEnableMythTvAccessCreds(!"None".equals(appSettings.getMythTvServicesAuthType()));

    pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_USER);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMythTvServicesUser());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_PASSWORD);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
      }
    });
    pref.setSummary(appSettings.getMythTvServicesPasswordMasked());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_AUTH_TYPE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doEnableMythlingAccessCreds(!"None".equals(newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getMythlingServicesAuthType());
    doBackendWebCredsEnablement(appSettings.isHasBackendWeb());    
    doEnableMythlingAccessCreds(appSettings.isHasBackendWeb() && !"None".equals(appSettings.getMythlingServicesAuthType()));

    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_USER);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMythlingServicesUser());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_PASSWORD);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, AppSettings.getMasked(newValue.toString()));
      }
    });
    pref.setSummary(appSettings.getMythlingServicesPasswordMasked());
    
  }  
  
  private void doEnableMythTvAccessCreds(boolean enabled)
  {
    getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_USER).setEnabled(enabled);
    getPreferenceScreen().findPreference(AppSettings.MYTHTV_SERVICES_PASSWORD).setEnabled(enabled);
  }
  
  private void doEnableMythlingAccessCreds(boolean enabled)
  {
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_USER).setEnabled(enabled);
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_PASSWORD).setEnabled(enabled);
  }
  
  private void doBackendWebCredsEnablement(boolean hasBackendWeb)
  {
    Preference mythlingAccessCat = getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICE_ACCESS_CATEGORY);
    mythlingAccessCat.setEnabled(hasBackendWeb);
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_SERVICES_AUTH_TYPE).setEnabled(hasBackendWeb);
  }
  
}