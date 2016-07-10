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
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ValidFragment")
public class VideoPlaybackDialog extends DialogFragment {
    private static final String TAG = VideoPlaybackDialog.class.getSimpleName();

    public interface PlaybackDialogListener {
        public void onClickPlay(Item item, PlaybackOption option);
        public void onClickCancel();
    }

    private PlaybackDialogListener listener;
    public void setListener(PlaybackDialogListener listener) {
        this.listener = listener;
    }

    private AppSettings settings;
    private Item item;
    private PlaybackOption playbackOption;
    private AlertDialog dialog;

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
        String typeFormatLabel = "";
        if (item.isDownloaded())
            typeFormatLabel += getString(R.string.downloaded) + " ";
        if (item.isLiveTv())
            typeFormatLabel += item.getTypeLabel();
        else
            typeFormatLabel += item.getTypeLabel() + " " + item.getFormat();
        builder.setTitle(typeFormatLabel + (item.isLiveTv() ? "" : (" " + getString(R.string.file).toLowerCase())));

        builder.setMessage(null); // use dialog_text view
        TextView titleView = (TextView) view.findViewById(R.id.dialog_title_text);
        if (settings.isFireTv())
            titleView.setTextColor(getResources().getColor(R.color.text_light_gray));
        titleView.setText(item.getTitle());

        ViewType viewType = settings.getMediaSettings().getViewType();
        String detail;
        if (viewType == ViewType.list ||
                (viewType == ViewType.split && AppSettings.ARTWORK_NONE.equals(settings.getArtworkStorageGroup(item.getType()))))
            detail = item.getDialogSubText(); // show more details
        else
            detail = item.getListSubText();
        if (item.isLiveTv())
            detail += "\n" + getString(R.string.recording_will_be_scheduled);

        TextView detailView = (TextView) view.findViewById(R.id.dialog_detail_text);
        if (settings.isFireTv())
            detailView.setTextColor(getResources().getColor(R.color.text_light_gray));
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
                        selectPlayer();
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
                userPlayerSelects++;
                playbackOption = createPlaybackOption();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        alwaysCheckbox = (CheckBox) view.findViewById(R.id.dialog_check);
        String alwaysMsg = getString(R.string.always_do_this_for) + " " + typeFormatLabel;
        if (!item.isLiveTv())
            alwaysMsg += " " + getString(R.string.files);
        if (!item.isDownloaded())
            alwaysMsg += " " + getString((settings.isExternalNetwork() ? R.string.on_external_network : R.string.on_internal_network));
        alwaysCheckbox.setText(alwaysMsg);

        builder.setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (!playbackOption.isAlways()) { // if always already set, option widgets are invisible -- no save
                    playbackOption.setAlways(alwaysCheckbox.isChecked());
                    // playbackOption is remembered regardless of alwaysCheckbox
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
                selectPlayer();
                alwaysCheckbox.setChecked(playbackOption.isAlways());
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (settings.isErrorReportingEnabled())
                new Reporter(ex).send();
            Toast.makeText(settings.getAppContext(), getString(R.string.error_) + ex.toString(), Toast.LENGTH_LONG).show();
        }

        dialog = builder.create();
        if (settings.isTv()) {
            dialog.setOnShowListener(new OnShowListener() {
                public void onShow(DialogInterface dlg) {
                    Button posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    posBtn.setFocusable(true);
                    posBtn.requestFocus();
                }
            });
        }
        return dialog;
    }

    private PlaybackOption createPlaybackOption() {
        String[] selectedValues = getPlayerValues();
        String player = selectedValues[playerDropdown.getSelectedItemPosition()];
        String streamMode;
        if (item.isDownloaded())
            streamMode = PlaybackOptions.STREAM_FILE;
        else
            streamMode = streamModeSwitch.isChecked() ? PlaybackOptions.STREAM_HLS : PlaybackOptions.STREAM_FILE;
        return new PlaybackOption(streamMode, player);
    }

    private int userPlayerSelects;  // as opposed to programmatic
    private void selectPlayer() {
        if (userPlayerSelects == 0) { // don't confound user by changing after they've selected
            String[] selectedValues = getPlayerValues();
            for (int i = 0; i < selectedValues.length; i++) {
                if (selectedValues[i].equals(playbackOption.getPlayer())) {
                    userPlayerSelects--;
                    playerDropdown.setSelection(i);
                }
            }
        }
    }

    private String[] getPlayerValues() {
        return getResources().getStringArray(R.array.player_values);
    }

    @Override
    public void onResume() {
        super.onResume();
        int titleResId = getResources().getIdentifier("alertTitle", "id", "android");
        View titleView = getDialog().findViewById(titleResId);
        if (titleView != null && titleView.getParent() instanceof View) {
            View parent = (View) titleView.getParent();
            parent.setMinimumHeight(settings.dpToPx(55)); // holo default is 64dp
        }
    }

    @SuppressLint("InflateParams")
    private View inflateView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        return inflater.inflate(R.layout.playback_dialog, null);
    }
}