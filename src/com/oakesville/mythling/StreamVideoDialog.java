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
        builder.setTitle(settings.getMediaSettings().getLabel() + (item.isLiveTv() ? "" : " " + item.getFormat() + " file"));

        if (message != null)
            builder.setMessage(message);

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.dialog_check);
        checkBox.setText("Always do this for " + item.getFormat() + " files");

        // add the buttons
        // TODO file format for LiveTV?
        boolean prefHls = settings.isPreferHls(item.getFormat());
        boolean prefRaw = prefHls ? false : settings.isPreferStreamRaw(item.getFormat());

        if (prefHls) {
            checkBox.setVisibility(View.GONE);
            builder.setPositiveButton(item.isLiveTv() ? "Watch" : "Play", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClickHls();
                }
            });
        } else if (prefRaw) {
            checkBox.setVisibility(View.GONE);
            builder.setPositiveButton(item.isLiveTv() ? "Watch" : "Play", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClickStream();
                }
            });
        } else {
            // no pref
            builder.setPositiveButton("Transcode to HLS", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (checkBox.isChecked())
                                settings.setPreferHls(item.getFormat());
                            listener.onClickHls();
                        }
                    })
                    .setNeutralButton("Stream Raw File", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onClickStream();
                            if (checkBox.isChecked())
                                settings.setPreferStreamRaw(item.getFormat());
                        }
                    });
        }

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onClickCancel();
                    }
                });

        return builder.create();
    }
}