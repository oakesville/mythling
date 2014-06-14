package com.oakesville.mythling.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public class ArtworkPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_artwork_settings);
    addPreferencesFromResource(R.xml.artwork_prefs);
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

    Preference pref = getPreferenceScreen().findPreference(AppSettings.ARTWORK_SG_RECORDINGS);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        if (AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS.equals(newValue))
        {
          preference.setSummary(AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS_LABEL);
          return true;
        }
        else
        {
          return super.onPreferenceChange(preference, newValue);
        }
      }
    });
    String sg = appSettings.getArtworkStorageGroup(MediaType.recordings);
    if ("Screenshots".equals(sg))
      pref.setSummary(AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS_LABEL);
    else
      pref.setSummary(sg);
    
    pref = getPreferenceScreen().findPreference(AppSettings.ARTWORK_SG_VIDEOS);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getArtworkStorageGroup(MediaType.videos));

    pref = getPreferenceScreen().findPreference(AppSettings.ARTWORK_SG_MOVIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getArtworkStorageGroup(MediaType.movies));
    
    pref = getPreferenceScreen().findPreference(AppSettings.ARTWORK_SG_TVSERIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getArtworkStorageGroup(MediaType.tvSeries));
    
    pref = getPreferenceScreen().findPreference(AppSettings.ALBUM_ART_LEVEL);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true));
  }  
}