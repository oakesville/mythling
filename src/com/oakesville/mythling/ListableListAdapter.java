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
package com.oakesville.mythling;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.oakesville.mythling.app.Listable;

public class ListableListAdapter extends ArrayAdapter<Listable> {

    private Context context;

    private int selection = -1;
    public void setSelection(int position) {
        this.selection = position;
        notifyDataSetChanged();
    }

    public ListableListAdapter(Context context, Listable[] listables) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, listables);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (selection == position)
            view.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
        else
            view.setBackgroundColor(Color.TRANSPARENT);

        return view;
    }
}