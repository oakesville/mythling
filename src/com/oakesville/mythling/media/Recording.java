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
package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.TextBuilder;

public class Recording extends TvShow {

    private int recordId;
    public int getRecordId() { return recordId; }
    public void setRecordRuleId(int rid) { this.recordId = rid; }

    private String internetRef;
    public String getInternetRef() { return internetRef; }
    public void setInternetRef(String inetRef) { this.internetRef = inetRef; }

    private int season;
    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    private int episode;
    public int getEpisode() { return episode; }
    public void setEpisode(int episode) { this.episode = episode; }

    private String recordingGroup;
    public String getRecordingGroup() { return recordingGroup; }
    public void setRecordingGroup(String recGroup) { this.recordingGroup = recGroup; }

    private boolean recorded; // recording completed
    public boolean isRecorded() { return recorded; }
    public void setRecorded(boolean recorded) { this.recorded = recorded; }

    private ArrayList<Cut> commercialCutList;
    public ArrayList<Cut> getCommercialCutList() { return commercialCutList; }
    public void setCommercialCutList(ArrayList<Cut> cutList) { this.commercialCutList = cutList; }
    public boolean hasCommercialCutList() {
        return commercialCutList != null && commercialCutList.size() > 0;
    }

    public Recording(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.recordings;
    }

    @Override
    public String getListSubText() {
        TextBuilder tb = new TextBuilder();
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendRating(getRating());
        tb.appendQuotedLine(getSubTitle());
        tb.appendLine(getShowDateTimeInfo());
        tb.append(getChannelInfo());
        if (!isShowMovie()) {
            if (season > 0 && episode > 0) {
                tb.appendLine().appendSeasonEpisode(season, episode);
                if (isRepeat())
                    tb.append(getAirDateInfo());
            }
            else if (isRepeat()) {
                tb.appendLine(getAirDateInfo());
            }
        }
        return tb.toString();
    }

    @Override
    public String getDialogSubText() {
        TextBuilder tb = new TextBuilder();
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendRating(getRating());
        tb.appendQuotedLine(getSubTitle());
        tb.appendLine(getSummary());
        return tb.toString();
    }

    @Override
    public String getSummary() {
        TextBuilder tb = new TextBuilder(getShowDateTimeInfo());
        tb.append(getChannelInfo());
        if (!isShowMovie()) {
            if (season > 0 && episode > 0) {
                tb.appendLine().appendSeasonEpisode(season, episode);
                if (isRepeat())
                    tb.append(getAirDateInfo());
            }
            else if (isRepeat()) {
                tb.appendLine(getAirDateInfo());
            }
        }
        tb.appendLine(getDescription());
        return tb.toString();
    }

    @Override
    public ArtworkDescriptor getArtworkDescriptor(String storageGroup) {
        final boolean usePreviewImage = AppSettings.DEFAULT_ARTWORK_SG_RECORDINGS.equals(storageGroup);
        if (getInternetRef() == null && !usePreviewImage)
            return null;

        return new ArtworkDescriptor(storageGroup) {
            public String getArtworkPath() {
                return getStorageGroup() + "/" + getId();
            }

            public String getArtworkContentServicePath() throws UnsupportedEncodingException {
                if (usePreviewImage) {
                    return "GetPreviewImage?ChanId=" + getChannelId() + "&StartTime=" + getStartTimeParam();
                } else {
                    String type = "coverart";
                    if ("Fanart".equals(getStorageGroup()))
                        type = "fanart";
                    else if ("Banners".equals(getStorageGroup()))
                        type = "banners";
                    String path = "GetRecordingArtwork?Inetref=" + getInternetRef() + "&Type=" + type;
                    if (season > 0)
                        path += "&Season=" + season;
                    return path;
                }
            }
        };
    }

    @Override
    public int getLength() {
        if (getStartTime() == null || getEndTime() == null)
            return super.getLength();
        else
            return (int)((getEndTime().getTime() - getStartTime().getTime()) / 1000);
    }

    @Override
    public boolean isLengthKnown() {
        int length = getLength();
        return length > 0 && isRecorded();
    }
}
