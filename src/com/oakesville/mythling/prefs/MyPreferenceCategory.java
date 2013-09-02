/**
 * Copyright 2013 Donald Oakes
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

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;

/**
 * Allow UI enablement.
 */
public class MyPreferenceCategory extends PreferenceCategory
{
  public MyPreferenceCategory(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  public MyPreferenceCategory(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public MyPreferenceCategory(Context context)
  {
    this(context, null);
  }
  
  private boolean enabled;

  @Override
  public boolean isEnabled()
  {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
    super.setEnabled(enabled);
  }
}
