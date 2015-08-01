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

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ListView;

import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.Listable;
import com.oakesville.mythling.media.Recording;

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
            // only has effect for Fire TV, which is fine
            adapter.setSelection(preSelIdx);
            getListView().setSelection(preSelIdx);
            getListView().setItemChecked(preSelIdx, true);
            getListView().requestFocus();
        }

        registerForContextMenu(getListView());

        if (mediaActivity.getAppSettings().isTv()) {
            getListView().setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) {
                            mediaActivity.getListView().requestFocus();
                            return true;
                        }
                        else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            int pos = getListView().getSelectedItemPosition();
                            getListView().performItemClick(getListAdapter().getView(pos, null, null), pos, getListAdapter().getItemId(pos));
                            return true;
                        }
                    }
                    return false;
                }
            });

            boolean grab = getArguments() == null ? false : getArguments().getBoolean(MediaActivity.GRAB_FOCUS);
            if (grab)
                getListView().requestFocus();
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        Uri uri = new Uri.Builder().path(path).build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri, mediaActivity.getApplicationContext(), MediaListActivity.class);
        intent.putExtra(MediaActivity.CURRENT_TOP, listView.getFirstVisiblePosition());
        View topV = listView.getChildAt(0);
        intent.putExtra(MediaActivity.TOP_OFFSET, (topV == null) ? 0 : topV.getTop());
        intent.putExtra(MediaActivity.SEL_ITEM_INDEX, position);
        intent.putExtra(MediaActivity.GRAB_FOCUS, true);
        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v == getListView()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Listable listable = (Listable)getListView().getItemAtPosition(info.position);
            if (listable instanceof Item && !((Item)listable).isLiveTv() && !((Item)listable).isMusic()) {
                Item item = (Item)listable;
                menu.setHeaderTitle(item.getLabel());
                String[] menuItems = getResources().getStringArray(R.array.item_long_click_menu);
                for (int i = 0; i < menuItems.length; i++)
                    menu.add(MediaActivity.LIST_FRAGMENT_CONTEXT_MENU_GROUP_ID, i, i, menuItems[i]);
                if (item.isRecording())
                    menu.add(MediaActivity.LIST_FRAGMENT_CONTEXT_MENU_GROUP_ID, 2, 2, getString(R.string.delete));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == MediaActivity.LIST_FRAGMENT_CONTEXT_MENU_GROUP_ID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            if (item.getItemId() == 0) {
                Item it = (Item)getListView().getItemAtPosition(info.position);
                it.setPath(path);
                mediaActivity.playItem(it);
                return true;
            } else if (item.getItemId() == 1) {
                Item it = (Item)getListView().getItemAtPosition(info.position);
                it.setPath(path);
                mediaActivity.transcodeItem(it);
                return true;
            } else if (item.getItemId() == 2) {
                Recording rec = (Recording)getListView().getItemAtPosition(info.position);
                rec.setPath(path);
                mediaActivity.deleteRecording(rec, info.position);
                return true;
            }
        }
        return false;
    }
 }
