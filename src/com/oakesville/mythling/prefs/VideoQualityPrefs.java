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

public class VideoQualityPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_quality_settings);
    addPreferencesFromResource(R.xml.quality_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    Preference pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_RES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "p"));
    pref.setSummary(appSettings.getInternalVideoRes() + "p");

    pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_VIDEO_BITRATE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k")
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString()))/1000);
      }
    });
    pref.setSummary(appSettings.getInternalVideoBitrate()/1000 + "k");
    
    pref = getPreferenceScreen().findPreference(AppSettings.INTERNAL_AUDIO_BITRATE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k")
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString()))/1000);
      }
    });
    pref.setSummary(appSettings.getInternalAudioBitrate()/1000 + "k");
    
    pref = getPreferenceScreen().findPreference(AppSettings.EXTERNAL_VIDEO_RES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary(appSettings.getExternalVideoRes() + "p");
    
    pref = getPreferenceScreen().findPreference(AppSettings.EXTERNAL_VIDEO_BITRATE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k")
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString()))/1000);
      }
    });
    pref.setSummary(appSettings.getExternalVideoBitrate()/1000 + "k");
    
    pref = getPreferenceScreen().findPreference(AppSettings.EXTERNAL_AUDIO_BITRATE);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "k")
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        return super.onPreferenceChange(preference, (Integer.parseInt(newValue.toString()))/1000);
      }
    });
    pref.setSummary(appSettings.getExternalAudioBitrate()/1000 + "k");
  }  
}