/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling.prefs;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import io.oakesville.media.MediaSettings;
import io.oakesville.media.MediaSettings.MediaTypeDeterminer;

public class CategoriesPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PrefsActivity)getActivity()).setActionBarTitle(R.string.title_categories_settings);
        addPreferencesFromResource(R.xml.categories_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
        MediaSettings mediaSettings = appSettings.getMediaSettings();

        Preference pref = getPreferenceScreen().findPreference(AppSettings.CATEGORIZE_VIDEOS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
                appSettings.setVideoCategorization((String) newValue);
                MediaSettings mediaSettings = appSettings.getMediaSettings();
                doEnablement(mediaSettings.getTypeDeterminer());
                String summary = Localizer.getTypeDeterminerLabel(mediaSettings.getTypeDeterminer());
                return super.onPreferenceChange(preference, summary);
            }
        });
        pref.setSummary(Localizer.getTypeDeterminerLabel(mediaSettings.getTypeDeterminer()));
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

    private void doEnablement(MediaTypeDeterminer determiner) {
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