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
import com.oakesville.mythling.prefs.SwitchPreference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.widget.Checkable;
import android.widget.Switch;

public class FireTvSwitchPreference extends SwitchPreference {

    private View parentView;

    public FireTvSwitchPreference(Context context) {
        super(context);
    }

    public FireTvSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FireTvSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {

        View checkableView = view.findViewById(R.id.prefs_switch);
        if (checkableView != null && checkableView instanceof Checkable) {
            if (checkableView instanceof Switch) {
                final Switch switchView = (Switch) checkableView;
                ViewParent vp = switchView.getParent();
                if (vp instanceof View) {
                    parentView = (View)vp;
                    parentView.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            switchView.setChecked(switchView.isChecked());
                        }
                    });
                    switchView.setFocusable(false);
                }
            }
        }

        super.onBindView(view);
    }

    @Override
    public boolean isSelectable() {
        return isEnabled();
    }
}
