package com.oakesville.mythling.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class CategoriesPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_categories_settings);
    addPreferencesFromResource(R.xml.categories_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.CATEGORIZE_VIDEOS);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        doEnableDirectoryPrefs("Directories".equals(newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getVideoCategorization());
    doEnableDirectoryPrefs("Directories".equals(appSettings.getVideoCategorization()));
    

    pref = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMovieDirectories());
    
    pref = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getTvSeriesDirectories());    

    pref = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getVideoExcludeDirectories());

  }
  
  private void doEnableDirectoryPrefs(boolean isDirectoriesCat)
  {
    Preference movieDirs = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
    movieDirs.setEnabled(isDirectoriesCat);
    Preference tvSeriesDirs = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
    tvSeriesDirs.setEnabled(isDirectoriesCat);
    Preference excludeDirs = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
    excludeDirs.setEnabled(isDirectoriesCat);
  }
}