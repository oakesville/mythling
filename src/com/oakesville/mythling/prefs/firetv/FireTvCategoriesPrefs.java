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
package com.oakesville.mythling.prefs.firetv;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.prefs.PrefChangeListener;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class FireTvCategoriesPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_categories_settings);
        addPreferencesFromResource(R.xml.firetv_categories_prefs);

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
    }

    private void doEnablement(MediaTypeDeterminer determiner) {
        boolean isDirectoriesCat = MediaTypeDeterminer.directories == determiner;
        Preference movieDirs = getPreferenceScreen().findPreference(AppSettings.MOVIE_DIRECTORIES);
        movieDirs.setEnabled(isDirectoriesCat);
        movieDirs.setSelectable(isDirectoriesCat);
        Preference tvSeriesDirs = getPreferenceScreen().findPreference(AppSettings.TV_SERIES_DIRECTORIES);
        tvSeriesDirs.setEnabled(isDirectoriesCat);
        tvSeriesDirs.setSelectable(isDirectoriesCat);
        Preference excludeDirs = getPreferenceScreen().findPreference(AppSettings.VIDEO_EXCLUDE_DIRECTORIES);
        excludeDirs.setEnabled(isDirectoriesCat);
        excludeDirs.setSelectable(isDirectoriesCat);
    }
}