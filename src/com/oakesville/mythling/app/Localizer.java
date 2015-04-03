/**
 * Copyright 2015 Donald Oakes
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
package com.oakesville.mythling.app;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.media.MediaSettings.MediaTypeDeterminer;
import com.oakesville.mythling.media.MediaSettings.SortType;
import com.oakesville.mythling.util.Reporter;

/**
 * Note: initialize() had better have been run before accessing any static methods.
 */
public class Localizer {

    private static final String TAG = Localizer.class.getSimpleName();

    private static String[] leadingArticles = new String[] { "A", "An", "The" };

    private static AppSettings appSettings;

    private static Context getAppContext() {
        return appSettings.getAppContext();
    }

    public static void initialize(AppSettings appSettings) {
        Localizer.appSettings = appSettings;
        try {
            leadingArticles = getAppContext().getResources().getStringArray(R.array.leading_articles);
        } catch (NotFoundException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
        }
    }

    public static String stripLeadingArticle(String in) {
        for (String leadingArticle : leadingArticles) {
            if (in.startsWith(leadingArticle + " "))
                return in.substring(leadingArticle.length() + 1);
        }
        return in;
    }

    public static String getStringRes(int resId, String... substs) {
        String str = getAppContext().getString(resId);
        for (int i = 0; i < substs.length; i++) {
            str = str.replaceAll("%" + i + "%", substs[i]);
        }
        return str;
    }

    public static String getStringRes(int resId) {
        return getAppContext().getString(resId);
    }

    public static String getItemTypeLabel(MediaType mediaType) {
        if (mediaType == MediaType.music)
            return getStringRes(R.string.song);
        else if (mediaType == MediaType.videos)
            return getStringRes(R.string.video);
        else if (mediaType == MediaType.recordings)
            return getStringRes(R.string.recording);
        else if (mediaType == MediaType.liveTv)
            return getStringRes(R.string.tv_show);
        else if (mediaType == MediaType.movies)
            return getStringRes(R.string.movie);
        else if (mediaType == MediaType.tvSeries)
            return getStringRes(R.string.tv_episode);
        else if (mediaType == MediaType.images)
            return getStringRes(R.string.image);
        else
            return "";
    }

    public static String getSortLabel(SortType sortType) {
        if (sortType == SortType.byDate)
            return getStringRes(R.string.by_date);
        else if (sortType == SortType.byRating)
            return getStringRes(R.string.by_rating);
        else
            return getStringRes(R.string.by_title);
    }

    public static String getMediaLabel(MediaType mediaType) {
        if (mediaType == MediaType.music)
            return getStringRes(R.string.menu_music);
        else if (mediaType == MediaType.videos)
            return getStringRes(R.string.menu_videos);
        else if (mediaType == MediaType.recordings)
            return getStringRes(R.string.menu_recordings);
        else if (mediaType == MediaType.liveTv)
            return getStringRes(R.string.menu_tv);
        else if (mediaType == MediaType.movies)
            return getStringRes(R.string.menu_movies);
        else if (mediaType == MediaType.tvSeries)
            return getStringRes(R.string.menu_tv_series);
        else
            return "";
    }

    public static String getTypeDeterminerLabel(MediaTypeDeterminer typeDeterminer) {
        if (MediaTypeDeterminer.metadata == typeDeterminer)
            return getStringRes(R.string.cat_metadata);
        else if (MediaTypeDeterminer.directories == typeDeterminer)
            return getStringRes(R.string.cat_directories);
        else if (MediaTypeDeterminer.none == typeDeterminer)
            return getStringRes(R.string.cat_none);
        else
            return null;
    }
}
