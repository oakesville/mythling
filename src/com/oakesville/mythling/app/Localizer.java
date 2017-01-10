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
package com.oakesville.mythling.app;

import java.text.SimpleDateFormat;

import com.oakesville.mythling.R;
import com.oakesville.mythling.util.Reporter;

import android.content.Context;
import android.util.Log;
import io.oakesville.media.MediaSettings.MediaType;
import io.oakesville.media.MediaSettings.MediaTypeDeterminer;
import io.oakesville.media.MediaSettings.SortType;

/**
 * Note: initialize() had better have been run before accessing any static methods.
 */
public class Localizer extends io.oakesville.util.Localizer {

    private static final String TAG = Localizer.class.getSimpleName();

    protected static Localizer create() {
        return new Localizer();
    }

    private AppSettings getAppSettings() {
        return (AppSettings)getResources();
    }
    private Context getAppContext() {
        return getAppSettings().getAppContext();
    }

    @Override
    public void initialize(Object appSettings) {
        try {
            super.initialize(appSettings);
            leadingArticles = getAppContext().getResources().getStringArray(R.array.leading_articles);
            dateFormat = new SimpleDateFormat(getStringRes(R.string.date_format));
            timeFormat = new SimpleDateFormat(getStringRes(R.string.time_format));
            dateTimeFormat = new SimpleDateFormat(getStringRes(R.string.date_time_format));
            dateTimeYearFormat = new SimpleDateFormat(getStringRes(R.string.date_time_year_format));
            yearFormat = new SimpleDateFormat(getStringRes(R.string.year_format));
            weekdayDateFormat = new SimpleDateFormat(getStringRes(R.string.weekday_date_format));
            am = AM_PM_FORMAT.format(AM_PM_FORMAT_US.parse("00"));
            pm = AM_PM_FORMAT.format(AM_PM_FORMAT_US.parse("12"));
            abbrevAm = getStringRes(R.string.abbrev_am);
            abbrevPm = getStringRes(R.string.abbrev_pm);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage(), ex);
            if (getAppSettings().isErrorReportingEnabled())
                new Reporter(ex).send();
        }
    }

    /**
     * Note: inefficient.
     */
    public String getString(String english) {
        String resName = english.toLowerCase();
        if (resName.endsWith(": "))
            resName = resName.substring(0, resName.length() - 2) + "_";
        resName.replace(' ', '_');
        return getAppContext().getResources().getString(getStringResId(resName));
    }

    public String getItemTypeLabel(MediaType mediaType) {
        if (mediaType == MediaType.music)
            return getStringRes(R.string.song);
        else if (mediaType == MediaType.videos)
            return getStringRes(R.string.video);
        else if (mediaType == MediaType.recordings)
            return getStringRes(R.string.recording);
        else if (mediaType == MediaType.liveTv)
            return getStringRes(R.string.live_tv);
        else if (mediaType == MediaType.movies)
            return getStringRes(R.string.movie);
        else if (mediaType == MediaType.tvSeries)
            return getStringRes(R.string.tv_episode);
        else if (mediaType == MediaType.images)
            return getStringRes(R.string.image);
        else
            return "";
    }

    public String getSortLabel(SortType sortType) {
        if (sortType == SortType.byDate)
            return getStringRes(R.string.by_date);
        else if (sortType == SortType.byRating)
            return getStringRes(R.string.by_rating);
        else if (sortType == SortType.byCallsign)
            return getStringRes(R.string.by_callsign);
        else if (sortType == SortType.byChannel)
            return getStringRes(R.string.by_channel);
        else
            return getStringRes(R.string.by_title);
    }

    public String getMediaLabel(MediaType mediaType) {
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

    /**
     * Note: inefficient.
     */
    public int getStringResId(String name) {
        return getAppContext().getResources().getIdentifier(name, "string", AppSettings.PACKAGE);
    }

    /**
     * Casts as the Android-specific instance where needed.
     */
    public static Localizer get() {
        return (Localizer) getInstance();
    }

    public static String getStringRes(int resId) {
        return get().getAppContext().getString(resId);
    }

    public static String[] getStringArrayRes(int resId) {
        return get().getAppContext().getResources().getStringArray(resId);
    }

    public static String getStringRes(int resId, String... substs) {
        String str = get().getAppContext().getString(resId);
        for (int i = 0; i < substs.length; i++) {
            str = str.replaceAll("%" + i + "%", substs[i]);
        }
        return str;
    }

    public static String getStringArrayEntry(int valuesResId, int entriesResId, String value) {
        String[] values = getStringArrayRes(valuesResId);
        String[] entries = getStringArrayRes(entriesResId);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value))
                return entries[i];
        }
        return null;
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
