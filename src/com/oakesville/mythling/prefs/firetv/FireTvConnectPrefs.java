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

import org.json.JSONException;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.prefs.PrefChangeListener;
import com.oakesville.mythling.util.Reporter;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class FireTvConnectPrefs extends PreferenceFragment {
    private static final String TAG = FireTvConnectPrefs.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_connect);
        addPreferencesFromResource(R.xml.firetv_connect_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        Preference pref = getPreferenceScreen().findPreference(AppSettings.MYTH_BACKEND_INTERNAL_HOST);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, true));
        pref.setSummary(appSettings.getInternalBackendHostPort());

        pref = getPreferenceScreen().
                findPreference(AppSettings.ALWAYS_PROMPT_FOR_PLAYBACK_OPTIONS);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean update = super.onPreferenceChange(preference, newValue);
                if (Boolean.parseBoolean(newValue.toString())) {
                    AppSettings settings = new AppSettings(getPreferenceScreen().getContext());
                    try {
                        settings.getPlaybackOptions().clearAlwaysDoThisSettings();
                    }
                    catch (JSONException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        if (settings.isErrorReportingEnabled())
                            new Reporter(ex).send();
                        settings.getPlaybackOptions().clearAll();
                    }
                }
                return update;
            }
        });

        pref = getPreferenceScreen().findPreference(AppSettings.ERROR_REPORTING);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));

        pref = getPreferenceScreen().findPreference(AppSettings.MYTHLING_VERSION);
        pref.setTitle(appSettings.getMythlingVersion());
    }
}
