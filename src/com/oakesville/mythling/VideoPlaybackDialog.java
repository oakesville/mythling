/**
 * Copyright 2016 Donald Oakes
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

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.Item;
import com.oakesville.mythling.media.MediaSettings.ViewType;
import com.oakesville.mythling.media.PlaybackOptions;
import com.oakesville.mythling.media.PlaybackOptions.PlaybackOption;
import com.oakesville.mythling.util.Reporter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class VideoPlaybackDialog extends DialogFragment {
    private static final String TAG = VideoPlaybackDialog.class.getSimpleName();

    public interface StreamDialogListener {
        public void onClickPlay(Item item, PlaybackOption option);
        public void onClickCancel();
    }

    private StreamDialogListener listener;
    public void setListener(StreamDialogListener listener) {
        this.listener = listener;
    }

    private AppSettings settings;
    private Item item;
    private PlaybackOption playbackOption;

    private Switch streamModeSwitch;
    private Spinner playerDropdown;
    private CheckBox alwaysCheckbox;

    public VideoPlaybackDialog(AppSettings settings, Item item) {
        this.settings = settings;
        this.item = item;
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = inflateView();
        builder.setView(view);
        builder.setIcon(R.drawable.ic_action_play);
        builder.setTitle(item.getTypeLabel() + (item.isLiveTv() ? "" : " " + item.getFormat() + " " + getString(R.string.file)));

        builder.setMessage(null); // use dialog_text view
        TextView titleView = (TextView) view.findViewById(R.id.dialog_title_text);
        titleView.setText(item.getTitle());

        String detail;
        if (settings.getMediaSettings().getViewType() == ViewType.list)
            detail = item.getDialogSubText(); // show more details
        else
            detail = item.getListSubText();
        if (item.isLiveTv())
            detail += "\n" + getString(R.string.recording_will_be_scheduled);

        TextView detailView = (TextView) view.findViewById(R.id.dialog_detail_text);
        detailView.setText(detail);

        streamModeSwitch = (Switch) view.findViewById(R.id.stream_switch);
        streamModeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                playbackOption = createPlaybackOption();
                try {
                    // maybe a different player option for this stream mode
                    PlaybackOption optionForStream = settings.getPlaybackOptions().getOption(item.getType(), item.getFormat(),
                            settings.getPlaybackNetwork(), playbackOption.getStream());
                    if (!optionForStream.getPlayer().equals(playbackOption.getPlayer())) {
                        playbackOption.setPlayer(optionForStream.getPlayer());
                        selectPlayer(true);
                    }

                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (settings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(settings.getAppContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        playerDropdown = (Spinner) view.findViewById(R.id.player_dropdown);
        playerDropdown.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                playbackOption = createPlaybackOption();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        alwaysCheckbox = (CheckBox) view.findViewById(R.id.dialog_check);
        String alwaysMsg = getString(R.string.always_do_this_for) + " ";
        if (item.isDownloaded())
            alwaysMsg += getString(R.string.downloaded).toLowerCase() + " " + item.getFormat() + " " + getString(R.string.files);
        else
            alwaysMsg += item.getFormat() + " " + getString(R.string.files) + " " +
                getString((settings.isExternalNetwork() ? R.string.on_external_network : R.string.on_internal_network));
        alwaysCheckbox.setText(alwaysMsg);

        builder.setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // playbackOption is remembered regardless of alwaysCheckbox
                playbackOption.setAlways(alwaysCheckbox.isChecked());
                try {
                    String network = item.isDownloaded() ? PlaybackOptions.NETWORK_DOWNLOAD : settings.getPlaybackNetwork();
                    settings.getPlaybackOptions().setOption(item.getType(), item.getFormat(), network, playbackOption);
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    if (settings.isErrorReportingEnabled())
                        new Reporter(ex).send();
                    Toast.makeText(settings.getAppContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
                }
                listener.onClickPlay(item, playbackOption);
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                listener.onClickCancel();
            }
        });

        try {
            String playbackNetwork = item.isDownloaded() ? PlaybackOptions.NETWORK_DOWNLOAD : settings.getPlaybackNetwork();
            playbackOption = settings.getPlaybackOptions().getOption(item.getType(), item.getFormat(), playbackNetwork);
            streamModeSwitch.setVisibility(playbackOption.isAlways() || item.isDownloaded() ? View.GONE : View.VISIBLE);
            playerDropdown.setVisibility(playbackOption.isAlways() ? View.GONE : View.VISIBLE);
            alwaysCheckbox.setVisibility(playbackOption.isAlways() ? View.GONE : View.VISIBLE);
            if (!playbackOption.isAlways()) {
                if (!item.isDownloaded())
                    streamModeSwitch.setChecked(playbackOption.isHls());
                selectPlayer(false);
                alwaysCheckbox.setChecked(playbackOption.isAlways());
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (settings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(settings.getAppContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }

        return builder.create();
    }

    private PlaybackOption createPlaybackOption() {
        String[] selectedValues = getResources().getStringArray(R.array.player_values);
        String player = selectedValues[playerDropdown.getSelectedItemPosition()];
        String streamMode;
        if (item.isDownloaded())
            streamMode = PlaybackOptions.STREAM_FILE;
        else
            streamMode = streamModeSwitch.isChecked() ? PlaybackOptions.STREAM_HLS : PlaybackOptions.STREAM_FILE;
        return new PlaybackOption(streamMode, player);
    }

    private void selectPlayer(boolean animate) {
        String[] selectedValues = getResources().getStringArray(R.array.player_values);
        for (int i = 0; i < selectedValues.length; i++) {
            if (selectedValues[i].equals(playbackOption.getPlayer()))
                playerDropdown.setSelection(i, animate);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        int titleResId = getResources().getIdentifier("alertTitle", "id", "android");
        View titleView = getDialog().findViewById(titleResId);
        if (titleView != null && titleView.getParent() instanceof View) {
            View parent = (View) titleView.getParent();
            parent.setMinimumHeight(settings.dpToPx(55)); // default is 64dp
        }
    }

    @SuppressLint("InflateParams")
    private View inflateView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        return inflater.inflate(R.layout.video_playback_dialog, null);
    }
}