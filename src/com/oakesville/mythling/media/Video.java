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
import java.net.URLEncoder;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.TextBuilder;

public class Video extends Item {

    private int year;
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    private String director;
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    private String actors;
    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors; }

    private String summary;
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    private String artwork;
    public String getArtwork() { return artwork; }
    public void setArtwork(String artwork) { this.artwork = artwork; }

    private String internetRef;
    public String getInternetRef() { return internetRef; }
    public void setInternetRef(String inetRef) { this.internetRef = inetRef; }

    private String pageUrl;
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public Video(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.videos;
    }

    @Override
    public String getDialogText() {
        TextBuilder tb = new TextBuilder(getTitle());
        tb.appendYear(getYear());
        tb.appendRating(getRating());
        tb.appendQuotedLine(getSubTitle());
        if (getDirector() != null)
            tb.appendLine(Localizer.getStringRes(R.string.directed_by_)).append(getDirector());
        if (getActors() != null)
            tb.appendLine(Localizer.getStringRes(R.string.starring_)).append(getActors());
        if (getSummary() != null)
            tb.appendLine(getSummary());
        return tb.toString();
    }

    @Override
    public ArtworkDescriptor getArtworkDescriptor(String storageGroup) {
        if (artwork == null)
            return null;

        return new ArtworkDescriptor(storageGroup) {
            public String getArtworkPath() {
                return getStorageGroup() + "/" + artwork;
            }

            public String getArtworkContentServicePath() throws UnsupportedEncodingException {
                return "GetImageFile?StorageGroup=" + URLEncoder.encode(getStorageGroup(), "UTF-8") + "&FileName=" + URLEncoder.encode(artwork, "UTF-8");
            }
        };
    }
}
