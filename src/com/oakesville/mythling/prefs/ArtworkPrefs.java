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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import io.oakesville.media.MediaSettings.MediaType;

public class ArtworkPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_artwork_settings);
        addPreferencesFromResource(R.xml.artwork_prefs);
        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.ARTWORK_SG_RECORDINGS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS.equals(newValue)) {
                    preference.setSummary(getString(R.string.artwork_sg_use_preview));
                    return true;
                } else {
                    return super.onPreferenceChange(preference, newValue);
                }
            }
        });
        String sg = appSettings.getArtworkStorageGroup(MediaType.recordings);
        if ("Screenshots".equals(sg))
            pref.setSummary(getString(R.string.artwork_sg_use_preview));
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

        pref = getPreferenceScreen().findPreference(AppSettings.MUSIC_ART);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getMusicArt());
    }
}