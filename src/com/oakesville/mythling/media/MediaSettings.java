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
