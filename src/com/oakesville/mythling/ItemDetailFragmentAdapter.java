/**
 * Copyright 2017 Donald Oakes
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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter class for bridging an ItemDetailFragment to an android.support.v4.app.Fragment
 */
public class ItemDetailFragmentAdapter extends Fragment {
    private ItemDetailFragment idFrag = new ItemDetailFragment();

    public void setIdx(int idx) { idFrag.setIdx(idx); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        idFrag.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        idFrag.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        idFrag.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return idFrag.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        idFrag.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        idFrag.onResume();
    }
}
