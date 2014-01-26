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

import com.oakesville.mythling.app.AppSettings;

import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class PrefChangeListener implements OnPreferenceChangeListener
{
  private boolean triggerCacheRefresh;
  private boolean triggerSummaryUpdate;
  private String units;
  public void setUnits(String units) { this.units = units; }
  
  public PrefChangeListener(boolean triggerSummaryUpdate, boolean triggerCacheRefresh)
  {
    this.triggerSummaryUpdate = triggerSummaryUpdate;
    this.triggerCacheRefresh = triggerCacheRefresh;
  }
  
  public PrefChangeListener(boolean triggerSummaryUpdate, boolean triggerCacheRefresh, String units)
  {
    this(triggerSummaryUpdate, triggerCacheRefresh);
    this.units = units;
  }

  public boolean onPreferenceChange(Preference preference, Object newValue)
  {
    if (triggerSummaryUpdate)
    {
      preference.setSummary(newValue == null ? "" : (newValue.toString() + (units == null ? "" : " " + units)));
    }
    if (triggerCacheRefresh)
    {
      Editor ed = preference.getEditor();
      ed.putLong(AppSettings.LAST_LOAD, 0);
      ed.commit();
    }
    return true;
  }
}
