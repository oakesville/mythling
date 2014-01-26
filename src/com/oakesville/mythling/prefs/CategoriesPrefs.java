package com.oakesville.mythling.prefs;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.MediaSettings;
import com.oakesville.mythling.app.MediaSettings.MediaTypeDeterminer;

public class CategoriesPrefs extends PreferenceFragment
{
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    getActivity().getActionBar().setTitle(R.string.title_categories_settings);
    addPreferencesFromResource(R.xml.categories_prefs);
    
    AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
    MediaSettings mediaSettings = appSettings.getMediaSettings();
    
    Preference pref = getPreferenceScreen().findPreference(AppSettings.CATEGORIZE_VIDEOS);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true)
    {
      public boolean onPreferenceChange(Preference preference, Object newValue)
      {
        new AppSettings(getPreferenceScreen().getContext()).getMediaSettings().setTypeDeterminer((String)newValue);
        doEnablement(MediaTypeDeterminer.valueOf((String)newValue));
        return super.onPreferenceChange(preference, newValue);
      }
    });
    pref.setSummary(mediaSettings.getTypeDeterminerLabel());
    doEnablement(mediaSettings.getTypeDeterminer());
    
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

    pref = getPreferenceScreen().findPreference(AppSettings.CUSTOM_BASE_URL);
    pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
    pref.setSummary(appSettings.getCustomBaseUrl());
  }
  
  private void doEnablement(MediaTypeDeterminer determiner)
  {
    boolean isDirectoriesCat = MediaTypeDeterminer.directories == determiner;
    
    Preference movieDirs = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
    movieDirs.setEnabled(isDirectoriesCat);
    Preference tvSeriesDirs = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
    tvSeriesDirs.setEnabled(isDirectoriesCat);
    Preference excludeDirs = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
    excludeDirs.setEnabled(isDirectoriesCat);
    
    boolean isNoneCat = MediaTypeDeterminer.none == determiner;
    Preference movieBaseUrl = getPreferenceScreen().findPreference(AppSettings.MOVIE_BASE_URL);
    movieBaseUrl.setEnabled(!isNoneCat);
    Preference tvBaseUrl = getPreferenceScreen().findPreference(AppSettings.TV_BASE_URL);
    tvBaseUrl.setEnabled(!isNoneCat);
    Preference customBaseUrl = getPreferenceScreen().findPreference(AppSettings.CUSTOM_BASE_URL);
    customBaseUrl.setEnabled(!isNoneCat);
  }
}