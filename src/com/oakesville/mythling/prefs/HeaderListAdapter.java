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

import java.util.List;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oakesville.mythling.R;

public class HeaderListAdapter extends ArrayAdapter<Header> {
    private static class HeaderViewHolder {
        ImageView icon;
        TextView title;
        TextView summary;
    }

    private LayoutInflater inflater;

    public HeaderListAdapter(Context context, List<Header> objects) {
        super(context, 0, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        View view;

        if (convertView == null) {
            view = inflater.inflate(R.layout.pref_header_item, parent, false);
            holder = new HeaderViewHolder();
            holder.icon = (ImageView) view.findViewById(android.R.id.icon);
            holder.title = (TextView) view.findViewById(android.R.id.title);
            holder.summary = (TextView) view.findViewById(android.R.id.summary);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (HeaderViewHolder) view.getTag();
        }

        // all view fields must be updated every time, because the view may be recycled
        Header header = getItem(position);
        if (holder.icon != null)
            holder.icon.setImageResource(header.iconRes);
        holder.title.setText(header.getTitle(getContext().getResources()));
        if (holder.summary != null) {
            CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }

        if (parent.getPaddingTop() > 8)
            parent.setPadding(parent.getPaddingLeft(), 8, parent.getPaddingRight(), parent.getPaddingBottom());
        return view;
    }
}