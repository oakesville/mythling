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

import com.oakesville.mythling.R;

public class MediaSettings {
    public enum MediaType {
        videos,
        music,
        recordings,
        liveTv,
        movies,
        tvSeries,
        images  // only used for cover art
    }

    public enum MediaTypeDeterminer {
        none,
        metadata,
        directories
    }

    public enum ViewType {
        list,
        detail,
        split
    }

    public enum SortType {
        byTitle,
        byDate,
        byRating
    }

    private MediaType type = MediaType.videos;
    public MediaType getType() { return type; }

    private MediaTypeDeterminer typeDeterminer = MediaTypeDeterminer.metadata;
    public MediaTypeDeterminer getTypeDeterminer() {  return typeDeterminer; }
    public void setTypeDeterminer(MediaTypeDeterminer determiner) { this.typeDeterminer = determiner; }

    public void setTypeDeterminer(String determiner) {
        this.typeDeterminer = MediaTypeDeterminer.valueOf(determiner);
    }

    private ViewType viewType = ViewType.list;
    public ViewType getViewType() { return viewType; }
    public void setViewType(ViewType vt) { this.viewType = vt;  }

    public void setViewType(String type) {
        this.viewType = ViewType.valueOf(type);
    }

    private SortType sortType = SortType.byTitle;
    public SortType getSortType() { return sortType; }
    public void setSortType(SortType st) { this.sortType = st; }

    public void setSortType(String type) {
        this.sortType = SortType.valueOf(type);
    }

    public MediaSettings(MediaType type) {
        this.type = type;
    }

    public MediaSettings(String type) {
        this.type = MediaType.valueOf(type);
    }

    public int getViewIcon() {
        if (getViewType() == ViewType.detail)
            return R.drawable.ic_menu_detail;
        else if (getViewType() == ViewType.split)
            return R.drawable.ic_menu_split;
        else
            return R.drawable.ic_menu_list;
    }

    public boolean isMusic() {
        return type == MediaType.music;
    }

    public boolean isVideos() {
        return type == MediaType.videos;
    }

    public boolean isRecordings() {
        return type == MediaType.recordings;
    }

    public boolean isLiveTv() {
        return type == MediaType.liveTv;
    }

    public boolean isMovies() {
        return type == MediaType.movies;
    }

    public boolean isTvSeries() {
        return type == MediaType.tvSeries;
    }

    public String toString() {
        return type.toString();
    }
}
