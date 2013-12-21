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
        doEnablement((String)newValue);
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(appSettings.getVideoCategorization());
    doEnablement(appSettings.getVideoCategorization());
    
    pref = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getMovieDirectories());
    
    pref = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getTvSeriesDirectories());    

    pref = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
    pref.setSummary(appSettings.getVideoExcludeDirectories());

    pref = getPreferenceScreen().findPreference(AppSettings.MOVIE_BASE_URL);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary(appSettings.getMovieBaseUrl());

    pref = getPreferenceScreen().findPreference(AppSettings.TV_BASE_URL);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary(appSettings.getTvBaseUrl());
  }
  
  private void doEnablement(String categorization)
  {
    boolean isDirectoriesCat = AppSettings.CATEGORIZATION_DIRECTORIES.equals(categorization);
    
    Preference movieDirs = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
    movieDirs.setEnabled(isDirectoriesCat);
    Preference tvSeriesDirs = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
    tvSeriesDirs.setEnabled(isDirectoriesCat);
    Preference excludeDirs = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
    excludeDirs.setEnabled(isDirectoriesCat);
    
    boolean isNoneCat = AppSettings.CATEGORIZATION_NONE.equals(categorization);
    Preference movieBaseUrl = getPreferenceScreen().findPreference(AppSettings.MOVIE_BASE_URL);
    movieBaseUrl.setEnabled(!isNoneCat);
    Preference tvBaseUrl = getPreferenceScreen().findPreference(AppSettings.TV_BASE_URL);
    tvBaseUrl.setEnabled(!isNoneCat);
  }
}