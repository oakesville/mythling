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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Item;

public class StreamVideoDialog extends DialogFragment {
    public interface StreamDialogListener {
        public void onClickHls();
        public void onClickStream();
        public void onClickCancel();
    }

    private StreamDialogListener listener;
    public void setListener(StreamDialogListener listener) {
        this.listener = listener;
    }

    private String message;
    public void setMessage(String message) { this.message = message;  }

    private AppSettings settings;
    private Item item;

    public StreamVideoDialog(AppSettings settings, Item item) {
        this.settings = settings;
        this.item = item;
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_with_checkbox, null);
        builder.setView(view);
        builder.setIcon(R.drawable.ic_action_play);
        builder.setTitle(item.getTypeLabel() + (item.isLiveTv() ? "" : " " + item.getFormat() + " " + getString(R.string.file)));

        if (message != null)
            builder.setMessage(message);

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.dialog_check);
        checkBox.setText(getString(R.string.always_do_this_for) + " " + item.getFormat() + " " + getString(R.string.files));

        // add the buttons
        // TODO file format for LiveTV?
        boolean prefHls = settings.isPreferHls(item.getFormat());
        boolean prefRaw = prefHls ? false : settings.isPreferStreamRaw(item.getFormat());

        if (prefHls) {
            checkBox.setVisibility(View.GONE);
            builder.setPositiveButton(getString(item.isLiveTv() ? R.string.watch : R.string.play), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClickHls();
                }
            });
        } else if (prefRaw) {
            checkBox.setVisibility(View.GONE);
            builder.setPositiveButton(getString(item.isLiveTv() ? R.string.watch : R.string.play), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClickStream();
                }
            });
        } else {
            // no pref
            builder.setPositiveButton(getString(R.string.transcode_to_hls), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (checkBox.isChecked())
                                settings.setPreferHls(item.getFormat());
                            listener.onClickHls();
                        }
                    })
                    .setNeutralButton(getString(R.string.stream_raw_file), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onClickStream();
                            if (checkBox.isChecked())
                                settings.setPreferStreamRaw(item.getFormat());
                        }
                    });
        }

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onClickCancel();
                    }
                });

        return builder.create();
    }
}