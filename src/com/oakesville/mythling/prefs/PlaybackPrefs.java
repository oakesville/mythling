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

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.R;

public class PlaybackPrefs extends PreferenceFragment
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_playback_settings);
    addPreferencesFromResource(R.xml.playback_prefs);

    // none of these prefs trigger cache refresh
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    
    SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.PLAYBACK_MODE);
    swPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doCategoryEnablement(!Boolean.valueOf(newValue.toString()));
        return true;  
      }
    });
    doCategoryEnablement(appSettings.isDevicePlayback());

// XXX internal player
    swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.VIDEO_PLAYER);
    swPref.setEnabled(false);
//    swPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
//    {
//      public boolean onPreferenceChange(Preference preference, Object newValue)
//      {
//        doBufferSizeEnablement(!Boolean.valueOf(newValue.toString()));
//        return true;  
//      }
//    });
    
//    Preference pref = getPreferenceScreen().findPreference(AppSettings.BUILT_IN_PLAYER_BUFFER_SIZE);
//    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, " kb"));
//    pref.setSummary(appSettings.getBuiltInPlayerBufferSize() + " kb");
//
//    doBufferSizeEnablement(!appSettings.isExternalPlayer());
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_HOST);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary(appSettings.getFrontendHost());

    pref = getPreferenceScreen().findPreference(AppSettings.MYTH_FRONTEND_PORT);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary("" + appSettings.getFrontendControlPort());
  }
  
  private void doCategoryEnablement(boolean isDevice)
  {
    Preference deviceCat = getPreferenceScreen().findPreference(AppSettings.DEVICE_PLAYBACK_CATEGORY);
    deviceCat.setEnabled(isDevice);
    // XXX internal player
    SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.VIDEO_PLAYER);
    swPref.setEnabled(false);
    
    Preference frontendCat = getPreferenceScreen().findPreference(AppSettings.FRONTEND_PLAYBACK_CATEGORY);
    frontendCat.setEnabled(!isDevice);
  }

// XXX internal player
//  private void doBufferSizeEnablement(boolean isInternal)
//  {
//    Preference pref = getPreferenceScreen().findPreference(AppSettings.BUILT_IN_PLAYER_BUFFER_SIZE);
//    pref.setEnabled(isInternal);
//  }
  
}
