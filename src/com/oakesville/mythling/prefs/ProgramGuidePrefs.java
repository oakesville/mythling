/**
 * Copyright 2015 Donald Oakes
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
import android.preference.SwitchPreference;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class ProgramGuidePrefs extends PreferenceFragment {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getActionBar().setTitle(R.string.title_guide_settings);
        addPreferencesFromResource(R.xml.guide_prefs);

        AppSettings appSettings = new AppSettings(getPreferenceScreen().getContext());

        // TODO pref change listener that refreshes browser cache instead of data cache

        Preference pref = getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(false, false) {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean hostedEpg = Boolean.valueOf(newValue.toString());
                getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG_ROOT).setEnabled(hostedEpg);
                return super.onPreferenceChange(preference, newValue);
            }
        });
        pref = getPreferenceScreen().findPreference(AppSettings.HOSTED_EPG_ROOT);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false));
        pref.setSummary(appSettings.getHostedEpgRoot());
        pref.setEnabled(appSettings.isHostedEpg());

        pref = getPreferenceScreen().findPreference(AppSettings.EPG_SCALE);
        pref.setOnPreferenceChangeListener(new PrefChangeListener(true, false, "%%") {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return super.onPreferenceChange(preference, String.valueOf((int)(Float.parseFloat(newValue.toString()) * 100)));
            }
        });
        pref.setSummary(String.valueOf((int)(Float.parseFloat(appSettings.getEpgScale()) * 100)) + "%%");

        SwitchPreference swPref = (SwitchPreference) getPreferenceScreen().findPreference(AppSettings.EPG_OMB);
        swPref.setOnPreferenceChangeListener(new PrefChangeListener(false, false));
        swPref.setChecked(appSettings.isEpgOmb());

    }
}