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

    pref = getPreferenceScreen().findPreference(AppSettings.BACKEND_WEB);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        boolean hasBackendWeb = Boolean.valueOf(newValue.toString());
        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(hasBackendWeb);
        
        if (!hasBackendWeb)
        {
          SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES);
          if (swPref.isChecked())
            swPref.setChecked(false);
          swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.MYTHWEB_ACCESS);
          if (swPref.isChecked())
            swPref.setChecked(false);
        }
        
        doCategoryEnablement(hasBackendWeb);
        
        if (hasBackendWeb)
        {
          AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
          if (!appSettings.isMythlingMediaServices())
            getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT).setEnabled(false);
          if (!appSettings.isMythWebAccessEnabled())
            getPreferenceScreen().findPreference(AppSettings.MYTHWEB_WEB_ROOT).setEnabled(false);
        }
        
        return super.onPreferenceChange(preference, newValue);
      }
    });
    boolean hasBackendWeb = appSettings.isHasBackendWeb();
    getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT).setEnabled(hasBackendWeb);
    doCategoryEnablement(hasBackendWeb);
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary("" + appSettings.getMythlingWebPort());

    pref = getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        boolean useMythlingSvcs = Boolean.valueOf(newValue.toString());
        getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT).setEnabled(useMythlingSvcs);
        return super.onPreferenceChange(preference, newValue);
      }
    });
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_WEB_ROOT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMythlingWebRoot());
    pref.setEnabled(hasBackendWeb && appSettings.isMythlingMediaServices());
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHWEB_ACCESS);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        boolean mythwebEnabled = Boolean.valueOf(newValue.toString());
        getPreferenceScreen().findPreference(AppSettings.MYTHWEB_WEB_ROOT).setEnabled(mythwebEnabled);
        return super.onPreferenceChange(preference, newValue);
      }
    });
    
    pref = getPreferenceScreen().findPreference(AppSettings.MYTHWEB_WEB_ROOT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMythwebWebRoot());
    pref.setEnabled(hasBackendWeb && appSettings.isMythWebAccessEnabled());
    
    pref = getPreferenceScreen().findPreference(AppSettings.ERROR_REPORTING);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));
  }
  
  private void doCategoryEnablement(boolean hasBackendWeb)
  {
    Preference cat = getPreferenceScreen().findPreference(AppSettings.MEDIA_SERVICES_CATEGORY);
    cat.setEnabled(hasBackendWeb);
    cat = getPreferenceScreen().findPreference(AppSettings.MYTHWEB_ACCESS_CATEGORY);
    cat.setEnabled(hasBackendWeb);
  }
}
