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
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;

public class CategoriesPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_categories_settings);
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
                String summary = mediaSettings.getTypeDeterminerLabel();
                return super.onPreferenceChange(preference, summary);
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

        pref = getPreferenceScreen().findPreference(AppSettings.HLS_FILE_EXTENSIONS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getHlsFileExtensions());

        pref = getPreferenceScreen().findPreference(AppSettings.STREAM_RAW_FILE_EXTENSIONS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getStreamRawFileExtensions());

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