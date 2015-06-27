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
package com.oakesville.mythling;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.oakesville.mythling.media.Listable;

public class ListableListAdapter extends ArrayAdapter<Listable> {

    private int selection = -1;
    public int getSelection() { return selection; }
    public void setSelection(int sel) { this.selection = sel;  }

    public ListableListAdapter(Context context, Listable[] listables) {
        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, listables);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
}