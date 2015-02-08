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
package com.oakesville.mythling.media;

import java.io.UnsupportedEncodingException;

import com.oakesville.mythling.app.AppSettings;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Recording extends TvShow {
    private int recordId;

    public int getRecordId() {
        return recordId;
    }

    public void setRecordRuleId(int rid) {
        this.recordId = rid;
    }

    private String internetRef;

    public String getInternetRef() {
        return internetRef;
    }

    public void setInternetRef(String inetRef) {
        this.internetRef = inetRef;
    }

    private int season;

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    private String recordingGroup;

    public String getRecordingGroup() {
        return recordingGroup;
    }

    public void setRecordingGroup(String recGroup) {
        this.recordingGroup = recGroup;
    }

    public Recording(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.recordings;
    }

    public String getTypeTitle() {
        return "Recording";
    }

    @Override
    protected String getExtraText() {
        StringBuffer buf = new StringBuffer();
        if (isShowMovie() && getYear() > 0)
            buf.append(" (").append(getYear()).append(")");
        if (getRating() > 0)
            buf.append(" ").append(getRatingString(getRating()));
        if (getSubTitle() != null)
            buf.append("\n\"").append(getSubTitle()).append("\"");
        buf.append("\n").append(getShowDateTimeInfo());
        buf.append(getChannelInfo());
        if (!isShowMovie() && isRepeat())
            buf.append(getAirDateInfo());
        return buf.toString();
    }

    @Override
    public String getDialogText() {
        StringBuffer buf = new StringBuffer(getTitle());
        if (isShowMovie() && getYear() > 0)
            buf.append(" (").append(getYear()).append(")");
        if (getRating() > 0)
            buf.append(" ").append(getRatingString(getRating()));
        if (getSubTitle() != null)
            buf.append("\n\"").append(getSubTitle()).append("\"");
        buf.append(getSummary());
        return buf.toString();
    }

    @Override
    public String getSummary() {
        StringBuffer summary = new StringBuffer();
        summary.append(getShowDateTimeInfo());
        summary.append(getChannelInfo());
        if (!isShowMovie() && isRepeat())
            summary.append(getAirDateInfo());
        if (getDescription() != null)
            summary.append("\n").append(getDescription());
        return summary.toString();
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
}
