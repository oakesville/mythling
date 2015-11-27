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

import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.Listable;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListableListAdapter extends ArrayAdapter<Listable> {

    private int selection = -1;
    public int getSelection() { return selection; }
    public void setSelection(int sel) { this.selection = sel;  }

    public ListableListAdapter(Context context, Listable[] listables) {
        super(context, R.layout.list_item, R.id.item_label, listables);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = super.getView(position, convertView, parent);
        Listable listable = getItem(position);
        int imageRes = listable.getIconResourceId();
        if (imageRes != 0) {
            ImageView imageView = (ImageView) rowView.findViewById(R.id.item_icon);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(imageRes);
        }
        String subLabel = listable.getListSubText();
        if (subLabel != null) {
            TextView subLabelText = (TextView) rowView.findViewById(R.id.item_sublabel);
            subLabelText.setVisibility(View.VISIBLE);
            subLabelText.setText(subLabel);
        }
        if (listable instanceof Item) {
            Item item = (Item) listable;
            if (item.isDownloaded())
                ((ImageView)rowView.findViewById(R.id.item_downloaded)).setVisibility(View.VISIBLE);
            if (item.isTranscoded())
                ((ImageView)rowView.findViewById(R.id.item_transcoded)).setVisibility(View.VISIBLE);
        }
        return rowView;
    }
}