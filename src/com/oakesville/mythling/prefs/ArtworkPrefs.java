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
import com.oakesville.mythling.media.MediaSettings.MediaType;

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

        pref = getPreferenceScreen().findPreference(AppSettings.MUSIC_ART_LEVEL_SONG);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, true));
    }
}