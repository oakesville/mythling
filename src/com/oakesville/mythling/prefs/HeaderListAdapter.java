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

import java.util.List;

import android.content.Context;
import android.preference.PreferenceActivity.Header;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oakesville.mythling.R;

class HeaderListAdapter extends ArrayAdapter<Header> {
    private static class HeaderViewHolder {
        ImageView icon;
        TextView title;
        TextView summary;
    }

    private final LayoutInflater inflater;

    public HeaderListAdapter(Context context, List<Header> objects) {
        super(context, 0, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
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
        assert header != null;
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