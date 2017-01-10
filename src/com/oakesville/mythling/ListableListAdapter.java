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

import java.util.HashMap;
import java.util.Map;

import com.oakesville.mythling.app.AppSettings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import io.oakesville.media.Item;
import io.oakesville.media.Listable;

public class ListableListAdapter extends ArrayAdapter<Listable> {

    private int selection = -1;
    public int getSelection() { return selection; }
    public void setSelection(int sel) { this.selection = sel;  }

    private boolean isTv;

    public ListableListAdapter(Context context, Listable[] listables, boolean isTv) {
        super(context, R.layout.list_item, R.id.item_label, listables);
        this.isTv = isTv;
    }

    private Map<String,Integer> iconToResId = new HashMap<String,Integer>();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = super.getView(position, convertView, parent);
        if (isTv)
            rowView.setBackgroundResource(R.drawable.selectable_list_item);
        Listable listable = getItem(position);

        // icon
        Integer imageRes = iconToResId.get(listable.getIcon());
        if (imageRes == null) {
            imageRes = getContext().getResources().getIdentifier("ic_" + listable.getIcon(), "drawable", AppSettings.PACKAGE);
        }
        ImageView imageView = (ImageView) rowView.findViewById(R.id.item_icon);
        if (imageRes == 0) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(imageRes);
        }


        // sublabel
        TextView subLabelText = (TextView) rowView.findViewById(R.id.item_sublabel);
        String subLabel = listable.getListSubText();
        subLabelText.setVisibility(subLabelText == null ? View.GONE : View.VISIBLE);
        subLabelText.setText(subLabel);
        if (subLabel == null)
            subLabelText.getLayoutParams().height = 0;
        else
            subLabelText.getLayoutParams().height = LayoutParams.WRAP_CONTENT;

        // status icons
        View transcodedIcon = rowView.findViewById(R.id.item_transcoded);
        View downloadedIcon = rowView.findViewById(R.id.item_downloaded);
        if (listable instanceof Item) {
            Item item = (Item) listable;
            transcodedIcon.setVisibility(item.isTranscoded() ? View.VISIBLE : View.GONE);
            downloadedIcon.setVisibility(item.isDownloaded() ? View.VISIBLE : View.GONE);
        }
        else {
            transcodedIcon.setVisibility(View.GONE);
            downloadedIcon.setVisibility(View.GONE);
        }

        return rowView;
    }
}