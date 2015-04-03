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

import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class CachingPrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_cache_settings);
        addPreferencesFromResource(R.xml.cache_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());
        Preference pref = getPreferenceScreen().findPreference(AppSettings.CACHE_EXPIRE_MINUTES);
        PrefChangeListener changeListener = new PrefChangeListener(true, true);
        changeListener.setUnits(getString(R.string.minutes));
        pref.setOnPreferenceChangeListener(changeListener);
        pref.setSummary(appSettings.getExpiryMinutes() + " " + getString(R.string.minutes));

        Preference refresh = findPreference("cache_refresh");
        refresh.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                Editor ed = pref.getEditor();
                ed.putLong(AppSettings.LAST_LOAD, 0);
                ed.commit();

                new AlertDialog.Builder(getPreferenceScreen().getContext())
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(getString(R.string.app_data))
                        .setMessage(getString(R.string.cache_cleared))
                        .setPositiveButton(getString(R.string.ok), null)
                        .show();

                return true;
            }
        });
    }
}
