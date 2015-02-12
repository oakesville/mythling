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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oakesville.mythling.app.Listable;
import com.oakesville.mythling.media.Item;

public class ItemListFragment extends ListFragment {

    private MediaActivity mediaActivity;
    private ArrayAdapter<Listable> adapter;
    private String path;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        path = getArguments().getString("path");
        mediaActivity = (MediaActivity) activity;
        adapter = new ArrayAdapter<Listable>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, mediaActivity.getItems(path).toArray(new Listable[0]));
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        Listable sel = mediaActivity.getItems(path).get(position);
        boolean isItem = sel instanceof Item;
        Uri.Builder builder = new Uri.Builder();
        if (isItem)
            builder.path(path);
        else
            builder.path(path + "/" + sel.getLabel());
        Uri uri = builder.build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaListActivity.class);
        if (isItem) {
            intent.putExtra("curTop", listView.getFirstVisiblePosition());
            View topV = listView.getChildAt(0);
            intent.putExtra("topOff", (topV == null) ? 0 : topV.getTop());
            intent.putExtra("idx", position);
        }

        startActivity(intent);
    }
 }
