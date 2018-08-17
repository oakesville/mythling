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

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

/**
 * Displays a dialog with a message and a checkbox to permanently dismiss
 * based on the supplied preference key.
 */
@SuppressLint("ValidFragment")
public class PrefDismissDialog extends DialogFragment {

    private final String title;
    private final String message;
    private String checkboxText;
    public void setCheckboxText(String checkboxText) {
        this.checkboxText = checkboxText;
    }

    private final AppSettings settings;
    private final String key;

    private boolean checked;
    private boolean defaultChecked = true;
    public void setDefaultChecked(boolean defaultChecked) {
        this.defaultChecked = defaultChecked;
    }

    public interface PrefDismissListener {
        String getPositiveBtnLabel();
        void onClickPositive();
        void onClickNegative();
    }
    private PrefDismissListener listener;
    public void setListener(PrefDismissListener listener) {
        this.listener = listener;
    }

    public PrefDismissDialog(AppSettings settings, String title, String message, String key) {
        this.settings = settings;
        this.title = title;
        this.message = message;
        this.key = key;
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_with_checkbox, null);
        builder.setView(view);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(title);
        if (message != null)
            builder.setMessage(message);

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.dialog_check);
        checkBox.setText(checkboxText == null ? getString(R.string.got_it) : checkboxText);
        checkBox.setChecked(defaultChecked);

        if (listener == null) {
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    checked = checkBox.isChecked();
                    settings.setBooleanPref(key, checked);
                }
            });
        }
        else {
            builder.setPositiveButton(listener.getPositiveBtnLabel(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    checked = checkBox.isChecked();
                    settings.setBooleanPref(key, checked);
                    listener.onClickPositive();
                }
            });
        }

        if (listener != null) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClickNegative();
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new OnShowListener() {
            public void onShow(DialogInterface dialog) {
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
            }
        });

        return dialog;
    }

    public boolean show(FragmentManager manager) {
        super.show(manager, key);
        return checked;
    }
}