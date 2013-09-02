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
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.oakesville.mythling.R;

public class MySwitchPreference extends SwitchPreference
{

  private Listener listener = new Listener();

  private class Listener implements CompoundButton.OnCheckedChangeListener
  {
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
      if (!callChangeListener(isChecked))
      {
        // Listener didn't like it, change it back.
        // CompoundButton will make sure we don't recurse.
        buttonView.setChecked(!isChecked);
        return;
      }

      setChecked(isChecked);
    }
  }  

  
  public MySwitchPreference(Context context)
  {
    super(context);
  }
  
  public MySwitchPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }
  
  public MySwitchPreference(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }
  
  @Override
  protected void onBindView(View view) {

      View checkableView = view.findViewById(R.id.prefs_switch);
      if (checkableView != null && checkableView instanceof Checkable) {
          ((Checkable) checkableView).setChecked(isChecked());

          if (checkableView instanceof Switch) {
              final Switch switchView = (Switch) checkableView;
              switchView.setTextOn(getSwitchTextOn());
              switchView.setTextOff(getSwitchTextOff());
              switchView.setOnCheckedChangeListener(listener);
          }
      }

      super.onBindView(view);
  }
  
}
