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

import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

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
                // also clear media storage location
                new AppSettings(getPreferenceScreen().getContext()).setExternalMediaDir("");

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
