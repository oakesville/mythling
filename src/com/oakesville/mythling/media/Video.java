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
import java.net.URLEncoder;

import com.oakesville.mythling.media.MediaSettings.MediaType;

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

    public String getTypeTitle() {
        return "Video";
    }

    @Override
    public String getDialogText() {
        StringBuffer buf = new StringBuffer(getTitle());
        if (getYear() > 0)
            buf.append(" (").append(getYear()).append(")");
        if (getRating() > 0)
            buf.append(" ").append(getRatingString(getRating()));
        if (getSubTitle() != null)
            buf.append("\n\"" + getSubTitle() + "\"");
        if (getDirector() != null)
            buf.append("\nDirected by: ").append(getDirector());
        if (getActors() != null)
            buf.append("\nStarring: ").append(getActors());
        if (getSummary() != null)
            buf.append("\n\n").append(getSummary());
        return buf.toString();
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
