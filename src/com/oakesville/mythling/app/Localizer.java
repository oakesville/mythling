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
package com.oakesville.mythling.app;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
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

    public static final DateFormat SERVICE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat SERVICE_DATE_TIME_RAW_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static {
        SERVICE_DATE_TIME_RAW_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    public static final DateFormat SERVICE_DATE_TIME_ZONE_FORMAT = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss Z");

    public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    public static DateFormat getIsoDateFormat() {
        return ISO_DATE_FORMAT;
    }

    private static String[] leadingArticles = new String[] { "A", "An", "The" };

    private static DateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
    public static DateFormat getDateFormat() { return dateFormat; }

    private static DateFormat timeFormat = new SimpleDateFormat("h:mm a");
    public static DateFormat getTimeFormat() { return timeFormat; }
    public static String getTimeAbbrev(Date d) {
        return abbrev(timeFormat.format(d));
    }

    private static DateFormat dateTimeFormat = new SimpleDateFormat("MMM d  h:mm a");
    public static DateFormat getDateTimeFormat() { return dateTimeFormat; }
    public static String getDateTimeAbbrev(Date d) {
        return abbrev(dateTimeFormat.format(d));
    }

    private static DateFormat dateTimeYearFormat = new SimpleDateFormat("MMM d yyyy  h:mm a");
    public static DateFormat getDateTimeYearFormat() { return dateTimeYearFormat; }
    public static String getDateTimeYearAbbrev(Date d) {
        return abbrev(dateTimeYearFormat.format(d));
    }

    private static DateFormat weekdayDateFormat = new SimpleDateFormat("EEE, MMM d");
    public static DateFormat getWeekdayDateFormat() { return weekdayDateFormat; }

    private static DateFormat AM_PM_FORMAT = new SimpleDateFormat("a");
    private static DateFormat AM_PM_FORMAT_US = new SimpleDateFormat("kk", Locale.US);
    private static String am = "AM";
    private static String pm = "PM";
    private static String abbrevAm = "a";
    private static String abbrevPm = "p";
    private static String abbrev(String in) {
        return in.replace(" " + am, abbrevAm).replace(" " + pm, abbrevPm);
    }

    private static DateFormat yearFormat = new SimpleDateFormat("yyyy");
    public static DateFormat getYearFormat() { return yearFormat; }

    private static AppSettings appSettings;

    private static Context getAppContext() {
        return appSettings.getAppContext();
    }

    public static void initialize(AppSettings appSettings) {
        Localizer.appSettings = appSettings;
        try {
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
