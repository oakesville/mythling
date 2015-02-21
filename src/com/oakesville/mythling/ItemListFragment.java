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

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.Item;

public class ItemListFragment extends ListFragment {

    private MediaActivity mediaActivity;
    private ListableListAdapter adapter;
    private String path;
    private int preSelIdx = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        path = getArguments().getString(MediaActivity.PATH);
        preSelIdx = getArguments().getInt(MediaActivity.SEL_ITEM_INDEX);
        mediaActivity = (MediaActivity) activity;
        populate();
    }

    private void populate() {
        adapter = new ListableListAdapter(mediaActivity, mediaActivity.getListables(path).toArray(new Listable[0]));
        setListAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        if (mediaActivity.getAppSettings().isFireTv()) {
            getListView().setSelector(R.drawable.firetv_list_selector);
            getListView().setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
            getListView().setDividerHeight(1);
        }

        if (preSelIdx >= 0) {
            // only has effect for Fire TV (due to choice mode?), which is fine
            adapter.setSelection(preSelIdx);
            getListView().setSelection(preSelIdx);
            getListView().setItemChecked(preSelIdx, true);
            getListView().requestFocus();
        }

        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Listable sel = mediaActivity.getListables(path).get(position);
                if (sel instanceof Item) {
                    mediaActivity.playItem((Item)sel);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        Listable sel = mediaActivity.getListables(path).get(position);
        boolean isItem = sel instanceof Item;
        Uri.Builder builder = new Uri.Builder();
        if (isItem)
            builder.path(path);
        else
            builder.path(path + "/" + sel.getLabel());
        Uri uri = builder.build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaListActivity.class);
        if (isItem) {
            intent.putExtra(MediaActivity.CURRENT_TOP, listView.getFirstVisiblePosition());
            View topV = listView.getChildAt(0);
            intent.putExtra(MediaActivity.TOP_OFFSET, (topV == null) ? 0 : topV.getTop());
            intent.putExtra(MediaActivity.SEL_ITEM_INDEX, position);
        }

        startActivity(intent);
    }
 }
