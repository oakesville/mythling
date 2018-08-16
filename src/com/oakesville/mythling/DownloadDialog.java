package com.oakesville.mythling;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.oakesville.mythling.app.AppSettings;

public class DownloadDialog extends DialogFragment {

    private String[] mediaDirectories;
    public void setMediaDirectories(String[] dirs) {
        this.mediaDirectories = dirs;
    }

    private AdapterView.OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AppSettings appSettings = new AppSettings(getActivity().getApplicationContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = inflateView();
        builder.setView(view);
        builder.setIcon(R.drawable.ic_file_download);
        builder.setTitle(R.string.save_to_media_folder);

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.dialog_check);
        checkBox.setChecked(appSettings.isBypassDownloadManager());

        ListView listView = (ListView) view.findViewById(R.id.dialog_list);
        if (mediaDirectories != null) {
            ListAdapter listAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_1, mediaDirectories);
            listView.setAdapter(listAdapter);
        }
        if (onItemClickListener != null) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    appSettings.setBypassDownloadManager(checkBox.isChecked());
                    onItemClickListener.onItemClick(parent, view, position, id);
                    dismiss();
                }
            });
        }

        if (mediaDirectories == null)
            dismiss(); // close on screen rotation
        return builder.create();
    }

    private View inflateView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        return inflater.inflate(R.layout.download_dialog, null);
    }
}
